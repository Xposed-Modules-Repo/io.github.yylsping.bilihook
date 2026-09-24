package io.github.yylsping.bilihook;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Per-quality-service state for one user initiated premium quality switch.
 *
 * <p>The state never supplies a qn to an arbitrary request. The host player keeps the selected
 * qn on its own quality-service instance and copies it to the current playable item. This class
 * only scopes the account gate and correlates that native request with its completion.</p>
 */
final class PremiumQualityState {
    static final int PREMIUM_QUALITY_MIN = 112;

    enum Phase {
        SELECTED,
        AUTHORIZED,
        REQUESTED
    }

    static final class Snapshot {
        final long generation;
        final long requestId;
        final String contentKey;
        final int target;
        final Phase phase;

        Snapshot(long generation, long requestId, String contentKey, int target, Phase phase) {
            this.generation = generation;
            this.requestId = requestId;
            this.contentKey = contentKey;
            this.target = target;
            this.phase = phase;
        }
    }

    private static final class Transaction {
        final long generation;
        final String contentKey;
        final int target;
        final long expiresAt;
        long requestId;
        Phase phase;

        Transaction(long generation, String contentKey, int target, long expiresAt) {
            this.generation = generation;
            this.contentKey = contentKey;
            this.target = target;
            this.expiresAt = expiresAt;
            this.phase = Phase.SELECTED;
        }

        Snapshot snapshot() {
            return new Snapshot(generation, requestId, contentKey, target, phase);
        }
    }

    private final IdentityHashMap<Object, Transaction> transactions = new IdentityHashMap<>();
    private long nextGeneration;
    private long nextRequestId;

    synchronized Snapshot select(Object owner, String contentKey, int quality, boolean loggedIn,
            long now, long ttlMillis) {
        purgeExpired(now);
        if (owner == null) return null;
        if (!loggedIn || !isPremium(quality) || isEmpty(contentKey)) {
            transactions.remove(owner);
            return null;
        }
        Transaction transaction = new Transaction(++nextGeneration, contentKey, quality,
                now + ttlMillis);
        transactions.put(owner, transaction);
        return transaction.snapshot();
    }

    synchronized Snapshot authorize(Object owner, String contentKey, int quality, long now) {
        Transaction transaction = valid(owner, now);
        if (!matches(transaction, contentKey, quality)
                || transaction.phase != Phase.SELECTED) {
            return null;
        }
        transaction.phase = Phase.AUTHORIZED;
        return transaction.snapshot();
    }

    synchronized Snapshot startRequest(Object owner, String contentKey, int quality, long now) {
        Transaction transaction = valid(owner, now);
        if (!matches(transaction, contentKey, quality)
                || transaction.phase != Phase.AUTHORIZED) {
            return null;
        }
        transaction.phase = Phase.REQUESTED;
        transaction.requestId = ++nextRequestId;
        return transaction.snapshot();
    }

    synchronized Snapshot current(Object owner, String contentKey, long now) {
        Transaction transaction = valid(owner, now);
        return transaction != null && transaction.contentKey.equals(contentKey)
                ? transaction.snapshot() : null;
    }

    synchronized boolean complete(Object owner, String contentKey, int expectedQuality,
            long generation, long now) {
        Transaction transaction = valid(owner, now);
        if (!matches(transaction, contentKey, expectedQuality)
                || transaction.generation != generation) {
            return false;
        }
        transactions.remove(owner);
        return true;
    }

    synchronized void clear(Object owner) {
        if (owner != null) transactions.remove(owner);
    }

    synchronized int size(long now) {
        purgeExpired(now);
        return transactions.size();
    }

    private Transaction valid(Object owner, long now) {
        if (owner == null) return null;
        Transaction transaction = transactions.get(owner);
        if (transaction != null && now > transaction.expiresAt) {
            transactions.remove(owner);
            return null;
        }
        return transaction;
    }

    private void purgeExpired(long now) {
        Iterator<Map.Entry<Object, Transaction>> iterator = transactions.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now > iterator.next().getValue().expiresAt) iterator.remove();
        }
    }

    private static boolean matches(Transaction transaction, String contentKey, int quality) {
        return transaction != null
                && transaction.target == quality
                && transaction.contentKey.equals(contentKey);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    static boolean isPremium(int quality) {
        return quality >= PREMIUM_QUALITY_MIN;
    }

    static boolean matchesCompletion(Snapshot transaction, boolean success,
            int expectedQuality, int actualQuality) {
        if (transaction == null) return false;
        if (success) return actualQuality == transaction.target;
        return transaction.phase == Phase.REQUESTED
                && expectedQuality == transaction.target;
    }
}

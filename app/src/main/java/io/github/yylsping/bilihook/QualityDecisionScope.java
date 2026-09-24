package io.github.yylsping.bilihook;

/**
 * Thread-scoped premium capability for the host's own startup/auto quality decision
 * (UGC {@code i6} / PGC {@code Z6}).
 *
 * <p>The host persists the user's last explicitly selected quality ceiling
 * ({@code pref_player_mediaSource_quality_wifi_key}) and re-reads it into the new
 * quality-service instance for every video. Its native strategy then picks the
 * highest available entry not above that ceiling, but skips VIP-flagged entries
 * for non-VIP accounts. This scope only lets that native decision consider the
 * premium entries while it runs; it never stores a qn, never touches request
 * builders, and never applies outside the decision call.</p>
 */
final class QualityDecisionScope {
    private final ThreadLocal<Boolean> active = new ThreadLocal<>();

    static boolean shouldGrant(boolean loggedIn, int ceiling) {
        return loggedIn && PremiumQualityState.isPremium(ceiling);
    }

    boolean enter(boolean loggedIn, int ceiling) {
        if (!shouldGrant(loggedIn, ceiling)) return false;
        active.set(Boolean.TRUE);
        return true;
    }

    void exit() {
        active.remove();
    }

    boolean isActive() {
        return active.get() != null;
    }
}

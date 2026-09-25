package io.github.yylsping.bilihook;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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
 *
 * <p>Each {@link #enter} pushes an independent frame, so nested decisions on the
 * same thread compose correctly: the innermost frame alone decides the current
 * state, a non-premium inner frame does not inherit an outer premium frame, and
 * popping the inner frame restores the outer one. The {@link ThreadLocal} itself
 * is removed once the outermost frame exits, and frames on different threads never
 * interact.</p>
 */
final class QualityDecisionScope {
    private final ThreadLocal<ArrayDeque<Boolean>> frames = new ThreadLocal<>();
    private final AtomicBoolean unmatchedExitLogged = new AtomicBoolean();

    static boolean shouldGrant(boolean loggedIn, int ceiling) {
        return loggedIn && PremiumQualityState.isPremium(ceiling);
    }

    /**
     * Pushes a new frame and returns whether it grants the premium capability.
     * A refused frame is still pushed so that the matching {@link #exit()} pops
     * exactly this frame instead of an outer one.
     */
    boolean enter(boolean loggedIn, int ceiling) {
        boolean grant = shouldGrant(loggedIn, ceiling);
        ArrayDeque<Boolean> stack = frames.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            frames.set(stack);
        }
        stack.push(grant);
        return grant;
    }

    /** Pops the current frame; clears the ThreadLocal once the outermost frame exits. */
    void exit() {
        ArrayDeque<Boolean> stack = frames.get();
        if (stack == null || stack.isEmpty()) {
            if (BuildConfig.DEBUG && unmatchedExitLogged.compareAndSet(false, true)) {
                HookRuntime.log("7.42.0 decision scope: unmatched exit ignored");
            }
            return;
        }
        stack.pop();
        if (stack.isEmpty()) frames.remove();
    }

    boolean isActive() {
        ArrayDeque<Boolean> stack = frames.get();
        return stack != null && !stack.isEmpty() && stack.peek();
    }

    /** Current frame depth on this thread; 0 means the ThreadLocal is clean. */
    int depth() {
        ArrayDeque<Boolean> stack = frames.get();
        return stack == null ? 0 : stack.size();
    }
}

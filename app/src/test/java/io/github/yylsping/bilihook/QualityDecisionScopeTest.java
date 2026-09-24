package io.github.yylsping.bilihook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class QualityDecisionScopeTest {
    @Test
    public void grantRequiresLoginAndPremiumCeiling() {
        assertFalse(QualityDecisionScope.shouldGrant(false, 120));
        assertFalse(QualityDecisionScope.shouldGrant(true, 80));
        assertFalse(QualityDecisionScope.shouldGrant(true, 111));
        assertFalse(QualityDecisionScope.shouldGrant(true, 0));
        assertFalse(QualityDecisionScope.shouldGrant(true, -1));
        assertTrue(QualityDecisionScope.shouldGrant(true, 112));
        assertTrue(QualityDecisionScope.shouldGrant(true, 116));
        assertTrue(QualityDecisionScope.shouldGrant(true, 120));
    }

    @Test
    public void scopeIsActiveOnlyBetweenEnterAndExit() {
        QualityDecisionScope scope = new QualityDecisionScope();
        assertFalse(scope.isActive());

        assertTrue(scope.enter(true, 120));
        assertTrue(scope.isActive());
        scope.exit();
        assertFalse(scope.isActive());
    }

    @Test
    public void refusedEnterDoesNotActivateScope() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertFalse(scope.enter(true, 80));
        assertFalse(scope.isActive());
        assertFalse(scope.enter(false, 120));
        assertFalse(scope.isActive());
    }

    @Test
    public void exitWithoutEnterIsSafe() {
        QualityDecisionScope scope = new QualityDecisionScope();
        scope.exit();
        assertFalse(scope.isActive());
    }

    @Test
    public void scopeIsThreadLocal() throws Exception {
        final QualityDecisionScope scope = new QualityDecisionScope();
        scope.enter(true, 116);

        final boolean[] otherThreadActive = new boolean[1];
        Thread other = new Thread(new Runnable() {
            @Override
            public void run() {
                otherThreadActive[0] = scope.isActive();
            }
        });
        other.start();
        other.join();

        assertFalse(otherThreadActive[0]);
        assertTrue(scope.isActive());
        scope.exit();
    }

    @Test
    public void decisionScopeIsIndependentFromSwitchTransaction() {
        PremiumQualityState state = new PremiumQualityState();
        QualityDecisionScope scope = new QualityDecisionScope();
        Object owner = new Object();

        // A completed manual-switch transaction must not be required for, nor consumed by,
        // the preference-driven auto decision on the next content.
        PremiumQualityState.Snapshot transaction =
                state.select(owner, "video-a", 120, true, 100L, 1_000L);
        state.authorize(owner, "video-a", 120, 101L);
        assertTrue(state.complete(owner, "video-a", 120, transaction.generation, 102L));
        assertTrue(state.size(103L) == 0);

        assertTrue(scope.enter(true, 120));
        assertTrue(scope.isActive());
        scope.exit();
    }

    @Test
    public void manualNormalSelectionResetsCeilingBelowPremium() {
        // After the user manually picks 80 the host persists 80; the decision scope must
        // stay closed so no premium auto decision can happen afterwards.
        assertFalse(QualityDecisionScope.shouldGrant(true, 80));
        assertFalse(QualityDecisionScope.shouldGrant(true, 64));
        assertFalse(QualityDecisionScope.shouldGrant(true, 32));
        assertFalse(QualityDecisionScope.shouldGrant(true, 16));
    }
}

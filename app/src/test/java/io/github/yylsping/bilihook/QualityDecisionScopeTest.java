package io.github.yylsping.bilihook;

import static org.junit.Assert.assertEquals;
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
    public void falseEnterPushesInactiveFrame() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertFalse(scope.enter(true, 80));
        assertFalse(scope.isActive());
        assertEquals(1, scope.depth());

        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
    }

    @Test
    public void trueEnterPushesActiveFrame() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertTrue(scope.enter(true, 120));
        assertTrue(scope.isActive());
        assertEquals(1, scope.depth());

        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
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
        scope.exit();
        assertFalse(scope.enter(false, 120));
        assertFalse(scope.isActive());
        scope.exit();
        assertEquals(0, scope.depth());
    }

    @Test
    public void nestedGrantedFramesStayActiveUntilOutermostExit() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertTrue(scope.enter(true, 120));
        assertTrue(scope.enter(true, 116));
        assertTrue(scope.isActive());
        assertEquals(2, scope.depth());

        scope.exit();
        assertTrue("inner exit must not clear the outer frame", scope.isActive());
        assertEquals(1, scope.depth());

        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
    }

    @Test
    public void innerRefusedFrameDoesNotInheritOuterGrant() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertTrue(scope.enter(true, 120));
        assertFalse(scope.enter(true, 80));
        assertFalse("innermost frame alone decides the state", scope.isActive());
        assertEquals(2, scope.depth());

        scope.exit();
        assertTrue("popping the inner frame restores the outer grant", scope.isActive());

        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
    }

    @Test
    public void innerGrantedFrameInsideRefusedOuter() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertFalse(scope.enter(true, 80));
        assertFalse(scope.isActive());
        assertTrue(scope.enter(true, 120));
        assertTrue(scope.isActive());

        scope.exit();
        assertFalse(scope.isActive());
        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
    }

    @Test
    public void threeLevelNestingRestoresFrameByFrame() {
        QualityDecisionScope scope = new QualityDecisionScope();

        assertTrue(scope.enter(true, 120));
        assertFalse(scope.enter(true, 64));
        assertTrue(scope.enter(true, 116));
        assertTrue(scope.isActive());
        assertEquals(3, scope.depth());

        scope.exit();
        assertFalse(scope.isActive());
        scope.exit();
        assertTrue(scope.isActive());
        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
    }

    @Test
    public void outermostExitClearsThreadLocal() {
        QualityDecisionScope scope = new QualityDecisionScope();

        scope.enter(true, 120);
        scope.exit();
        assertEquals(0, scope.depth());
        assertFalse(scope.isActive());

        // A later unmatched exit must still find a clean thread state.
        scope.exit();
        assertEquals(0, scope.depth());
        assertFalse(scope.isActive());
    }

    @Test
    public void sequentialEnterExitLeavesNoResidue() {
        QualityDecisionScope scope = new QualityDecisionScope();

        for (int i = 0; i < 5; i++) {
            assertTrue(scope.enter(true, 120));
            assertTrue(scope.isActive());
            scope.exit();
            assertFalse(scope.enter(true, 80));
            assertFalse(scope.isActive());
            scope.exit();
        }
        assertEquals(0, scope.depth());
        assertFalse(scope.isActive());
    }

    @Test
    public void exitWithoutEnterIsSafe() {
        QualityDecisionScope scope = new QualityDecisionScope();
        scope.exit();
        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());

        // The scope keeps working normally after unmatched exits.
        assertTrue(scope.enter(true, 120));
        assertTrue(scope.isActive());
        scope.exit();
        assertFalse(scope.isActive());
        assertEquals(0, scope.depth());
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
    public void nestedFramesOnOtherThreadDoNotPolluteThisThread() throws Exception {
        final QualityDecisionScope scope = new QualityDecisionScope();
        scope.enter(true, 120);

        Thread other = new Thread(new Runnable() {
            @Override
            public void run() {
                // A refused frame nested inside a granted frame on another thread.
                scope.enter(true, 120);
                scope.enter(true, 80);
                scope.exit();
                scope.exit();
            }
        });
        other.start();
        other.join();

        assertTrue(scope.isActive());
        assertEquals(1, scope.depth());
        scope.exit();
        assertEquals(0, scope.depth());
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

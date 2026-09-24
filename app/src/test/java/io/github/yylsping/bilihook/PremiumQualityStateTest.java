package io.github.yylsping.bilihook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PremiumQualityStateTest {
    @Test
    public void transactionIsBoundToOwnerContentAndTarget() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();

        PremiumQualityState.Snapshot selected = state.select(
                owner, "video-a", 120, true, 100L, 1_000L);
        assertNotNull(selected);
        assertNull(state.authorize(new Object(), "video-a", 120, 101L));
        assertNull(state.authorize(owner, "video-b", 120, 101L));
        assertNull(state.authorize(owner, "video-a", 116, 101L));

        PremiumQualityState.Snapshot authorized =
                state.authorize(owner, "video-a", 120, 101L);
        assertNotNull(authorized);
        assertEquals(selected.generation, authorized.generation);
    }

    @Test
    public void requestConsumptionIsSingleShot() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        state.select(owner, "video-a", 120, true, 100L, 1_000L);
        state.authorize(owner, "video-a", 120, 101L);

        PremiumQualityState.Snapshot request =
                state.startRequest(owner, "video-a", 120, 102L);
        assertNotNull(request);
        assertTrue(request.requestId > 0L);
        assertNull(state.startRequest(owner, "video-a", 120, 103L));
    }

    @Test
    public void staleTransactionExpires() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        state.select(owner, "video-a", 120, true, 100L, 1_000L);

        assertNotNull(state.current(owner, "video-a", 1_100L));
        assertNull(state.current(owner, "video-a", 1_101L));
        assertEquals(0, state.size(1_101L));
    }

    @Test
    public void normalQualityClearsOldPremiumTransaction() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        state.select(owner, "video-a", 120, true, 100L, 1_000L);

        assertNull(state.select(owner, "video-a", 80, true, 101L, 1_000L));
        assertNull(state.current(owner, "video-a", 101L));
    }

    @Test
    public void loggedOutSelectionCannotActivateAfterLogin() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();

        assertNull(state.select(owner, "video-a", 120, false, 100L, 1_000L));
        assertNull(state.authorize(owner, "video-a", 120, 101L));
    }

    @Test
    public void unrelatedCompletionDoesNotClearActiveTransaction() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        PremiumQualityState.Snapshot transaction =
                state.select(owner, "video-a", 120, true, 100L, 1_000L);
        state.authorize(owner, "video-a", 120, 101L);

        assertFalse(state.complete(owner, "video-a", 116,
                transaction.generation, 102L));
        assertFalse(state.complete(owner, "video-b", 120,
                transaction.generation, 102L));
        assertFalse(state.complete(owner, "video-a", 120,
                transaction.generation + 1L, 102L));
        assertNotNull(state.current(owner, "video-a", 102L));
        assertTrue(state.complete(owner, "video-a", 120,
                transaction.generation, 103L));
        assertNull(state.current(owner, "video-a", 103L));
    }

    @Test
    public void multiplePlayerIdentitiesAreIsolated() {
        PremiumQualityState state = new PremiumQualityState();
        Object first = new Object();
        Object second = new Object();
        state.select(first, "video-a", 120, true, 100L, 1_000L);
        state.select(second, "video-b", 116, true, 100L, 1_000L);

        assertNotNull(state.authorize(first, "video-a", 120, 101L));
        assertNotNull(state.authorize(second, "video-b", 116, 101L));
        assertEquals(2, state.size(101L));
    }

    @Test
    public void rapidVideoChangeCannotReuseOldSelection() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        state.select(owner, "video-a", 120, true, 100L, 1_000L);

        assertNull(state.authorize(owner, "video-b", 120, 101L));
        PremiumQualityState.Snapshot replacement =
                state.select(owner, "video-b", 116, true, 102L, 1_000L);
        assertNotNull(replacement);
        assertNotNull(state.authorize(owner, "video-b", 116, 103L));
    }

    @Test
    public void premiumBoundaryIsExplicit() {
        assertFalse(PremiumQualityState.isPremium(111));
        assertTrue(PremiumQualityState.isPremium(112));
    }

    @Test
    public void directSuccessMatchesActualQualityNotMutableExpectedField() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        PremiumQualityState.Snapshot transaction =
                state.select(owner, "video-a", 120, true, 100L, 1_000L);
        state.authorize(owner, "video-a", 120, 101L);
        transaction = state.current(owner, "video-a", 101L);

        assertTrue(PremiumQualityState.matchesCompletion(transaction, true, 80, 120));
        assertFalse(PremiumQualityState.matchesCompletion(transaction, true, 80, 116));
        assertFalse(PremiumQualityState.matchesCompletion(transaction, false, 80, 80));
    }

    @Test
    public void resolverFailureMatchesOnlyItsPendingExpectedTarget() {
        PremiumQualityState state = new PremiumQualityState();
        Object owner = new Object();
        state.select(owner, "video-a", 120, true, 100L, 1_000L);
        state.authorize(owner, "video-a", 120, 101L);
        PremiumQualityState.Snapshot transaction =
                state.startRequest(owner, "video-a", 120, 102L);

        assertTrue(PremiumQualityState.matchesCompletion(transaction, false, 120, 80));
        assertFalse(PremiumQualityState.matchesCompletion(transaction, false, 116, 80));
    }
}

package io.github.yylsping.bilihook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class Bili742AdHooksTest {
    @Test
    public void recognizesOnlyExplicitPegasusAdCases() {
        assertTrue(Bili742AdHooks.isAdEnumName("SMALL_COVER_V5_AD"));
        assertTrue(Bili742AdHooks.isAdEnumName("AD_BANNER"));
        assertTrue(Bili742AdHooks.isAdEnumName("FOO_AD_BAR"));
        assertFalse(Bili742AdHooks.isAdEnumName("POPULAR_TOP_ENTRANCE"));
        assertFalse(Bili742AdHooks.isAdEnumName(null));
    }
}

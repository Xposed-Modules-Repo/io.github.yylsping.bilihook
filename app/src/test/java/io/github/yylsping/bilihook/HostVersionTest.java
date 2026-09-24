package io.github.yylsping.bilihook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class HostVersionTest {
    @Test
    public void resolvesOnlyExactSupportedVersions() {
        assertEquals(HostVersion.BILI_7_4_0, HostVersion.resolve(7040300L, "7.4.0"));
        assertEquals(HostVersion.BILI_7_42_0, HostVersion.resolve(7420400L, "7.42.0"));
        assertTrue(HostVersion.BILI_7_4_0.isSupported());
        assertTrue(HostVersion.BILI_7_42_0.isSupported());
    }

    @Test
    public void rejectsMismatchedOrUnknownVersions() {
        assertEquals(HostVersion.UNSUPPORTED, HostVersion.resolve(7420400L, "7.4.0"));
        assertEquals(HostVersion.UNSUPPORTED, HostVersion.resolve(7040300L, "7.42.0"));
        assertEquals(HostVersion.UNSUPPORTED, HostVersion.resolve(7420401L, "7.42.0"));
        assertEquals(HostVersion.UNSUPPORTED, HostVersion.resolve(7420400L, null));
        assertFalse(HostVersion.UNSUPPORTED.isSupported());
    }
}

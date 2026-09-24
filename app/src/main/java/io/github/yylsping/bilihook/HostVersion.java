package io.github.yylsping.bilihook;

/** Exact host versions supported by this module. */
enum HostVersion {
    BILI_7_4_0(7040300L, "7.4.0"),
    BILI_7_42_0(7420400L, "7.42.0"),
    UNSUPPORTED(-1L, "");

    final long versionCode;
    final String versionName;

    HostVersion(long versionCode, String versionName) {
        this.versionCode = versionCode;
        this.versionName = versionName;
    }

    static HostVersion resolve(long versionCode, String versionName) {
        for (HostVersion version : values()) {
            if (version != UNSUPPORTED
                    && version.versionCode == versionCode
                    && version.versionName.equals(versionName)) {
                return version;
            }
        }
        return UNSUPPORTED;
    }

    boolean isSupported() {
        return this != UNSUPPORTED;
    }
}

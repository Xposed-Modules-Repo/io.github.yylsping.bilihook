package io.github.yylsping.bilihook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug-only investigation hooks for the 7.42.0 native quality-decision chain.
 * They observe the host preference lifecycle (N2 -> s1/J0 -> provider -> i6/Z6) and
 * never mutate arguments, results, or resources. Installed only in DEBUG builds.
 */
final class Bili742DebugProbe {
    private static final String UGC_QUALITY_SERVICE =
            "com.bilibili.playerbizcommon.features.quality.PlayerQualityService";
    private static final String PGC_QUALITY_SERVICE =
            "com.bilibili.bangumi.ui.page.detail.playerV2.widget.quality.l";
    private static final String PLAY_INDEX = "com.bilibili.lib.media.resource.PlayIndex";

    private Bili742DebugProbe() {}

    static int install(ClassLoader classLoader) {
        int count = 0;
        count += installUgc(classLoader);
        count += installPgc(classLoader);
        return count;
    }

    private static int installUgc(ClassLoader classLoader) {
        int count = 0;
        final Class<?> service;
        try {
            service = Reflect.findClass(UGC_QUALITY_SERVICE, classLoader);
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc service missing", error);
            return 0;
        }
        try {
            Reflect.findAndHookMethod(service, "N2", Integer.TYPE, new HookRuntime.Callback() {
                @Override
                protected void beforeHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc N2(savePref) quality=" + param.getArg(0));
                }
            });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc N2 unavailable", error);
        }
        try {
            Reflect.findAndHookMethod(service, "s1", new HookRuntime.Callback() {
                @Override
                protected void afterHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc s1() init " + fields(param.thisObject,
                            new String[]{"e", "f", "h", "k", "d"}));
                }
            });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc s1 unavailable", error);
        }
        try {
            Reflect.findAndHookMethod(service, "i6", Boolean.TYPE, new HookRuntime.Callback() {
                @Override
                protected void beforeHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc i6(" + param.getArg(0) + ") enter "
                            + fields(param.thisObject, new String[]{"e", "f", "h"})
                            + " " + playIndexes(param.thisObject));
                }

                @Override
                protected void afterHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc i6 => " + param.getResult());
                }
            });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc i6 unavailable", error);
        }
        try {
            Reflect.findAndHookMethod(service, "r2", new HookRuntime.Callback() {
                @Override
                protected void beforeHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc r2() enter " + fields(param.thisObject,
                            new String[]{"e", "f", "h", "k"}));
                }

                @Override
                protected void afterHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc r2() exit " + fields(param.thisObject,
                            new String[]{"e", "f", "h", "k"}));
                }
            });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc r2 unavailable", error);
        }
        try {
            Class<?> mediaResourceClass = Reflect.findClass(
                    "com.bilibili.lib.media.resource.MediaResource", classLoader);
            Reflect.findAndHookMethod(mediaResourceClass, "a", org.json.JSONObject.class,
                    new HookRuntime.Callback() {
                        @Override
                        protected void afterHookedMethod(HookRuntime.HookParam param) {
                            probe("mediaResource parsed " + audioInfo(param.thisObject));
                        }
                    });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: mediaResource parse unavailable", error);
        }
        try {
            Class<?> mediaResourceClass = Reflect.findClass(
                    "com.bilibili.lib.media.resource.MediaResource", classLoader);
            Reflect.findAndHookMethod(service, "s3", mediaResourceClass,
                    new HookRuntime.Callback() {
                        @Override
                        protected void afterHookedMethod(HookRuntime.HookParam param) {
                            probe("ugc resource audio " + audioInfo(param.getArg(0)));
                        }
                    });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc resource audio unavailable", error);
        }
        try {
            Class<?> provider = Reflect.findClass(UGC_QUALITY_SERVICE + "$c", classLoader);
            Class<?> resolveFrom = Reflect.findClass(
                    "tv.danmaku.biliplayerv2.service.IVideoQualityProvider$ResolveFrom",
                    classLoader);
            Reflect.findAndHookMethod(provider, "a", resolveFrom, new HookRuntime.Callback() {
                @Override
                protected void afterHookedMethod(HookRuntime.HookParam param) {
                    probe("ugc qualityProvider.a(" + param.getArg(0) + ") => " + param.getResult());
                }
            });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: ugc provider unavailable", error);
        }
        return count;
    }

    private static int installPgc(ClassLoader classLoader) {
        int count = 0;
        final Class<?> service;
        try {
            service = Reflect.findClass(PGC_QUALITY_SERVICE, classLoader);
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: pgc service missing", error);
            return 0;
        }
        try {
            Reflect.findAndHookMethod(service, "J0", new HookRuntime.Callback() {
                @Override
                protected void afterHookedMethod(HookRuntime.HookParam param) {
                    probe("pgc J0() init " + fields(param.thisObject,
                            new String[]{"g", "i", "l", "h"}));
                }
            });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: pgc J0 unavailable", error);
        }
        try {
            Reflect.findAndHookMethod(service, "Z6", Boolean.TYPE, Boolean.TYPE,
                    new HookRuntime.Callback() {
                        @Override
                        protected void beforeHookedMethod(HookRuntime.HookParam param) {
                            probe("pgc Z6(" + param.getArg(0) + "," + param.getArg(1) + ") enter "
                                    + fields(param.thisObject, new String[]{"g", "i"}));
                        }

                        @Override
                        protected void afterHookedMethod(HookRuntime.HookParam param) {
                            probe("pgc Z6 => " + param.getResult());
                        }
                    });
            count++;
        } catch (Throwable error) {
            HookRuntime.log("7.42.0 probe: pgc Z6 unavailable", error);
        }
        return count;
    }

    private static String fields(Object receiver, String[] names) {
        StringBuilder out = new StringBuilder("{");
        for (String name : names) {
            if (out.length() > 1) out.append(',');
            out.append(name).append('=');
            try {
                Object value = Reflect.getObjectField(receiver, name);
                out.append(value);
            } catch (Throwable ignored) {
                out.append('?');
            }
        }
        return out.append('}').toString();
    }

    private static String playIndexes(Object service) {
        try {
            Object mediaResource = Reflect.callMethod(service, "i1");
            if (mediaResource == null) return "resource=null";
            Object selected = Reflect.callMethod(mediaResource, "s");
            Object vodIndex = Reflect.getObjectField(mediaResource, "b");
            Object list = vodIndex == null ? null : Reflect.getObjectField(vodIndex, "a");
            if (!(list instanceof List)) return "list=null";
            List<String> parts = new ArrayList<>();
            for (Object item : (List<?>) list) {
                if (item == null) continue;
                int qn = Reflect.getIntField(item, "b");
                boolean needVip = Boolean.TRUE.equals(Reflect.getObjectField(item, "s"));
                boolean needLogin = Boolean.TRUE.equals(Reflect.getObjectField(item, "t"));
                parts.add(qn + (needVip ? ":vip" : "") + (needLogin ? ":login" : ""));
            }
            return "selected=" + (selected == null ? "?" : Reflect.getIntField(selected, "b"))
                    + " available=" + parts;
        } catch (Throwable error) {
            return "playIndexes err " + error;
        }
    }

    private static String audioInfo(Object mediaResource) {
        if (mediaResource == null) return "resource=null";
        try {
            StringBuilder out = new StringBuilder("{");
            Object dash = Reflect.callMethod(mediaResource, "h");
            Object audios = dash == null ? null : Reflect.callMethod(dash, "c");
            out.append("dashAudio=").append(dashIds(audios));
            out.append(", hires=").append(enhancement(Reflect.getObjectField(mediaResource, "m")));
            out.append(", dolby=").append(enhancement(Reflect.getObjectField(mediaResource, "l")));
            return out.append('}').toString();
        } catch (Throwable error) {
            return "audioInfo err " + error;
        }
    }

    private static String enhancement(Object resource) throws Throwable {
        if (resource == null) return "null";
        Object list = Reflect.getObjectField(resource, "b");
        return "{type=" + Reflect.getIntField(resource, "a")
                + ", needVip=" + Reflect.getObjectField(resource, "c")
                + ", audio=" + dashIds(list) + '}';
    }

    private static String dashIds(Object list) throws Throwable {
        if (!(list instanceof List)) return "null";
        List<String> parts = new ArrayList<>();
        for (Object item : (List<?>) list) {
            if (item == null) continue;
            Object url = Reflect.callMethod(item, "i");
            parts.add(Reflect.callMethod(item, "p") + ":"
                    + (url instanceof String && !((String) url).isEmpty()));
        }
        return parts.toString();
    }

    private static void probe(String message) {
        HookRuntime.log("7.42.0 probe " + message);
    }
}

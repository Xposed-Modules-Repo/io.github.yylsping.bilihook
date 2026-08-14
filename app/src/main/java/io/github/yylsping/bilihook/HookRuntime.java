package io.github.yylsping.bilihook;

import android.util.Log;

import java.lang.reflect.Executable;
import java.util.Arrays;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/** Minimal callback adapter for the libxposed API 102 interceptor chain. */
final class HookRuntime {
    private static final String TAG = "bili hook";
    private static volatile XposedModule module;

    private HookRuntime() {}

    static void attach(XposedModule value) {
        module = value;
    }

    static void log(String message) {
        XposedModule value = module;
        if (value != null) value.log(Log.INFO, TAG, message);
        else Log.i(TAG, message);
    }

    static void log(String message, Throwable error) {
        XposedModule value = module;
        if (value != null) value.log(Log.ERROR, TAG, message, error);
        else Log.e(TAG, message, error);
    }

    static XposedInterface.HookHandle hook(Executable executable, Callback callback) {
        XposedModule value = module;
        if (value == null) throw new IllegalStateException("libxposed is not attached");
        executable.setAccessible(true);
        return value.hook(executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> intercept(chain, callback));
    }

    static Callback returnConstant(Object value) {
        return new Callback() {
            @Override
            protected void beforeHookedMethod(HookParam param) {
                param.setResult(value);
            }
        };
    }

    private static Object intercept(XposedInterface.Chain chain, Callback callback)
            throws Throwable {
        HookParam param = new HookParam(chain.getThisObject(), chain.getArgs().toArray());
        try {
            callback.beforeHookedMethod(param);
        } catch (Throwable callbackError) {
            log("before-hook failed for " + chain.getExecutable(), callbackError);
            return chain.proceed();
        }

        if (!param.returnEarly) {
            try {
                param.result = chain.proceed(param.args);
            } catch (Throwable originalError) {
                param.throwable = originalError;
            }
        }

        try {
            callback.afterHookedMethod(param);
        } catch (Throwable callbackError) {
            log("after-hook failed for " + chain.getExecutable(), callbackError);
        }
        if (param.throwable != null) throw param.throwable;
        return param.result;
    }

    abstract static class Callback {
        protected void beforeHookedMethod(HookParam param) throws Throwable {}
        protected void afterHookedMethod(HookParam param) throws Throwable {}
    }

    static final class HookParam {
        final Object thisObject;
        final Object[] args;
        private Object result;
        private Throwable throwable;
        private boolean returnEarly;

        HookParam(Object thisObject, Object[] args) {
            this.thisObject = thisObject;
            this.args = args;
        }

        Object getResult() {
            return result;
        }

        void setResult(Object value) {
            result = value;
            throwable = null;
            returnEarly = true;
        }

        @Override
        public String toString() {
            return "HookParam{" + thisObject + ", args=" + Arrays.toString(args) + '}';
        }
    }
}

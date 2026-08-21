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

    static XposedInterface.HookHandle hookReturnConstant(Executable executable, Object constant) {
        XposedModule value = module;
        if (value == null) throw new IllegalStateException("libxposed is not attached");
        executable.setAccessible(true);
        return value.hook(executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> constant);
    }

    private static Object intercept(XposedInterface.Chain chain, Callback callback)
            throws Throwable {
        HookParam param = new HookParam(chain);
        try {
            callback.beforeHookedMethod(param);
        } catch (Throwable callbackError) {
            log("before-hook failed for " + chain.getExecutable(), callbackError);
            return chain.proceed();
        }

        if (!param.returnEarly) {
            try {
                param.result = param.proceed();
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
        private final XposedInterface.Chain chain;
        final Object thisObject;
        private Object[] modifiedArgs;
        private Object result;
        private Throwable throwable;
        private boolean returnEarly;

        HookParam(XposedInterface.Chain chain) {
            this.chain = chain;
            this.thisObject = chain.getThisObject();
        }

        Object getArg(int index) {
            Object[] args = modifiedArgs;
            return args == null ? chain.getArg(index) : args[index];
        }

        void setArg(int index, Object value) {
            Object[] args = modifiedArgs;
            if (args == null) {
                args = chain.getArgs().toArray();
                modifiedArgs = args;
            }
            args[index] = value;
        }

        Object getResult() {
            return result;
        }

        void setResult(Object value) {
            result = value;
            throwable = null;
            returnEarly = true;
        }

        private Object proceed() throws Throwable {
            Object[] args = modifiedArgs;
            return args == null ? chain.proceed() : chain.proceed(args);
        }

        @Override
        public String toString() {
            Object[] args = modifiedArgs;
            if (args == null) args = chain.getArgs().toArray();
            return "HookParam{" + thisObject + ", args=" + Arrays.toString(args) + '}';
        }
    }
}

package io.github.bilihook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cached standard-reflection helpers; no legacy Xposed helper dependency. */
final class Reflect {
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private Reflect() {}

    static Class<?> findClass(String name, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, false, loader);
    }

    static Class<?> findClassIfExists(String name, ClassLoader loader) {
        try {
            return findClass(name, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Method findMethodExact(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        String key = methodKey(type, name, parameters);
        Method cached = METHODS.get(key);
        if (cached != null) return cached;
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                Method raced = METHODS.putIfAbsent(key, method);
                return raced == null ? method : raced;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(key);
    }

    static void findAndHookMethod(Class<?> type, String name, Object... signature)
            throws NoSuchMethodException {
        if (signature.length == 0 || !(signature[signature.length - 1] instanceof HookRuntime.Callback)) {
            throw new IllegalArgumentException("hook callback missing for " + type.getName() + '.' + name);
        }
        Class<?>[] parameters = new Class<?>[signature.length - 1];
        for (int index = 0; index < parameters.length; index++) {
            if (!(signature[index] instanceof Class<?>)) {
                throw new IllegalArgumentException("invalid parameter type at " + index);
            }
            parameters[index] = (Class<?>) signature[index];
        }
        HookRuntime.hook(findMethodExact(type, name, parameters),
                (HookRuntime.Callback) signature[signature.length - 1]);
    }

    static Object callMethod(Object receiver, String name, Object... args) throws Throwable {
        if (receiver == null) throw new NullPointerException("receiver");
        return invoke(resolveCompatibleMethod(receiver.getClass(), name, false, args), receiver, args);
    }

    static Object callStaticMethod(Class<?> type, String name, Object... args) throws Throwable {
        return invoke(resolveCompatibleMethod(type, name, true, args), null, args);
    }

    static Object newInstance(Class<?> type, Object... args) throws Throwable {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (!compatible(constructor.getParameterTypes(), args)) continue;
            constructor.setAccessible(true);
            try {
                return constructor.newInstance(args);
            } catch (InvocationTargetException error) {
                throw error.getCause();
            }
        }
        throw new NoSuchMethodException(type.getName() + " constructor " + argumentTypes(args));
    }

    static Object getObjectField(Object receiver, String name) throws Throwable {
        return field(receiver.getClass(), name).get(receiver);
    }

    static void setObjectField(Object receiver, String name, Object value) throws Throwable {
        field(receiver.getClass(), name).set(receiver, value);
    }

    static int getIntField(Object receiver, String name) throws Throwable {
        return field(receiver.getClass(), name).getInt(receiver);
    }

    static void setIntField(Object receiver, String name, int value) throws Throwable {
        field(receiver.getClass(), name).setInt(receiver, value);
    }

    private static Object invoke(Method method, Object receiver, Object[] args) throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException error) {
            throw error.getCause();
        }
    }

    private static Method resolveCompatibleMethod(
            Class<?> type, String name, boolean requireStatic, Object[] args)
            throws NoSuchMethodException {
        String key = type.getName() + '#' + name + '#' + requireStatic + argumentTypes(args);
        Method cached = METHODS.get(key);
        if (cached != null) return cached;
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!name.equals(method.getName())
                        || requireStatic != Modifier.isStatic(method.getModifiers())
                        || !compatible(method.getParameterTypes(), args)) {
                    continue;
                }
                method.setAccessible(true);
                Method raced = METHODS.putIfAbsent(key, method);
                return raced == null ? method : raced;
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(key);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        String key = type.getName() + '#' + name;
        Field cached = FIELDS.get(key);
        if (cached != null) return cached;
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                Field raced = FIELDS.putIfAbsent(key, field);
                return raced == null ? field : raced;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(key);
    }

    private static boolean compatible(Class<?>[] parameters, Object[] args) {
        if (parameters.length != args.length) return false;
        for (int index = 0; index < parameters.length; index++) {
            Object argument = args[index];
            if (argument == null) {
                if (parameters[index].isPrimitive()) return false;
                continue;
            }
            if (!box(parameters[index]).isAssignableFrom(argument.getClass())) return false;
        }
        return true;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        return Void.class;
    }

    private static String methodKey(Class<?> type, String name, Class<?>[] parameters) {
        return type.getName() + '#' + name + Arrays.toString(parameters);
    }

    private static String argumentTypes(Object[] args) {
        StringBuilder value = new StringBuilder("(");
        for (Object arg : args) value.append(arg == null ? "null" : arg.getClass().getName()).append(';');
        return value.append(')').toString();
    }
}

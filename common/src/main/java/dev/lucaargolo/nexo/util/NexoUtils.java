package dev.lucaargolo.nexo.util;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.NexoException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public final class NexoUtils {

    private NexoUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> T getField(@NotNull Class<?> clazz, @NotNull String name, @NotNull Object instance) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Exception e) {
            throw new NexoException("Failed to read field " + clazz.getName() + "." + name, e);
        }
    }

    public static void setField(@NotNull Class<?> clazz, @NotNull String name, @NotNull Object instance, Object value) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new NexoException("Failed to set field " + clazz.getName() + "." + name, e);
        }
    }

    public static <T> T loadPlatformClass(NexoMinecraft nexo, Class<T> clazz, Object... parameters) {
        return loadPlatformClass(nexo, null, clazz, parameters);
    }

    public static <T> T loadPlatformClass(NexoMinecraft nexo, String mod, Class<T> clazz, Object... parameters) {
        String originalName = clazz.getName();

        String commonClassPrefix = mod == null ? nexo.getPlatform() : nexo.isModLoaded(mod) ? nexo.getPlatform() : "Empty";
        String commonClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + commonClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);
        String clientClassPrefix = "Client" + commonClassPrefix;
        String clientClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + clientClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);

        if (nexo.getSide().isClient()) {
            try {
                Class<? extends T> clientPlatformClass = clazz.getClassLoader().loadClass(clientClassName).asSubclass(clazz);
                return instantiate(clientPlatformClass, parameters);
            } catch (Exception ignored) {
            }
        }
        try {
            Class<? extends T> commonPlatformClass = clazz.getClassLoader().loadClass(commonClassName).asSubclass(clazz);
            return instantiate(commonPlatformClass, parameters);
        } catch (Exception exception) {
            throw new NexoException("Failed to load platform class for " + clazz.getName(), exception);
        }
    }

    private static <T> T instantiate(Class<? extends T> type, Object[] parameters) throws ReflectiveOperationException {
        Constructor<?> selected = null;
        int selectedScore = Integer.MAX_VALUE;
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] constructorTypes = constructor.getParameterTypes();
            if (constructorTypes.length != parameters.length) continue;
            int score = 0;
            for (int i = 0; i < constructorTypes.length; i++) {
                int parameterScore = getParameterScore(constructorTypes[i], parameters[i]);
                if (parameterScore < 0) {
                    score = -1;
                    break;
                }
                score += parameterScore;
            }
            if (score >= 0 && score < selectedScore) {
                selected = constructor;
                selectedScore = score;
            }
        }
        if (selected == null) throw new NoSuchMethodException(type.getName());

        Class<?>[] selectedTypes = selected.getParameterTypes();
        Object[] invocationParameters = parameters;
        for (int i = 0; i < selectedTypes.length; i++) {
            if (selectedTypes[i].isPrimitive()) {
                if (invocationParameters == parameters) invocationParameters = parameters.clone();
                invocationParameters[i] = convertPrimitive(selectedTypes[i], parameters[i]);
            }
        }
        @SuppressWarnings("unchecked")
        T instance = (T) selected.newInstance(invocationParameters);
        return instance;
    }

    private static int getParameterScore(Class<?> type, Object parameter) {
        if (parameter == null) return type.isPrimitive() ? -1 : 0;
        if (!type.isPrimitive()) {
            if (type == parameter.getClass()) return 0;
            return type.isInstance(parameter) ? 1 : -1;
        }

        Class<?> sourceType = getPrimitiveType(parameter);
        if (sourceType == null || type == void.class) return -1;
        if (type == boolean.class || sourceType == boolean.class) return type == sourceType ? 2 : -1;
        if (type == sourceType) return 2;
        if (canWidenPrimitive(sourceType, type)) return 3 + primitiveRank(type) - primitiveRank(sourceType);
        return 16 + Math.abs(primitiveRank(type) - primitiveRank(sourceType));
    }

    private static Class<?> getPrimitiveType(Object parameter) {
        if (parameter instanceof Boolean) return boolean.class;
        if (parameter instanceof Byte) return byte.class;
        if (parameter instanceof Short) return short.class;
        if (parameter instanceof Character) return char.class;
        if (parameter instanceof Integer) return int.class;
        if (parameter instanceof Long) return long.class;
        if (parameter instanceof Float) return float.class;
        if (parameter instanceof Double) return double.class;
        return null;
    }

    private static boolean canWidenPrimitive(Class<?> sourceType, Class<?> targetType) {
        if (sourceType == byte.class) return targetType != char.class;
        if (sourceType == short.class) return targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        if (sourceType == char.class) return targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        if (sourceType == int.class) return targetType == long.class || targetType == float.class || targetType == double.class;
        if (sourceType == long.class) return targetType == float.class || targetType == double.class;
        return sourceType == float.class && targetType == double.class;
    }

    private static int primitiveRank(Class<?> type) {
        if (type == byte.class) return 0;
        if (type == short.class) return 1;
        if (type == int.class || type == char.class) return 2;
        if (type == long.class) return 3;
        if (type == float.class) return 4;
        return 5;
    }

    @SuppressWarnings("UnnecessaryBoxing")
    private static Object convertPrimitive(Class<?> type, Object parameter) {
        if (type == boolean.class || type == char.class && parameter instanceof Character) return parameter;
        if (type == char.class) return Character.valueOf((char) ((Number) parameter).intValue());

        Number number = parameter instanceof Character character ? Integer.valueOf(character) : (Number) parameter;
        if (type == byte.class) return Byte.valueOf(number.byteValue());
        if (type == short.class) return Short.valueOf(number.shortValue());
        if (type == int.class) return Integer.valueOf(number.intValue());
        if (type == long.class) return Long.valueOf(number.longValue());
        if (type == float.class) return Float.valueOf(number.floatValue());
        if (type == double.class) return Double.valueOf(number.doubleValue());
        throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
    }

}

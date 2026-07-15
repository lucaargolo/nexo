package dev.lucaargolo.nexo.util;

import dev.lucaargolo.nexo.api.NexoException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
public final class NexoUtils {

    private NexoUtils() {}

    public static <T> Class<T> type(Class<?> type) {
        return (Class<T>) type;
    }

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

}

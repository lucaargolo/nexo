package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

public abstract class DataBase<T> extends Feature<DataBase<T>> {

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public final Class<DataBase<T>> type() {
        return (Class<DataBase<T>>) (Class<?>) DataBase.class;
    }

    public abstract T initial();

    @NotNull
    public abstract ByteBuffer write(@NotNull T value);

    @NotNull
    public abstract T read(@NotNull ByteBuffer buffer);

    @NotNull
    public abstract JsonElement serialize(@NotNull T value);

    @NotNull
    public abstract T deserialize(@NotNull JsonElement element);

    public boolean persistent() {
        return true;
    }

    public boolean synced() {
        return true;
    }

    public abstract static class StringData extends DataBase<String> {

        @Override
        public @NotNull ByteBuffer write(@NotNull String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        }

        @Override
        public @NotNull String read(@NotNull ByteBuffer buffer) {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public @NotNull JsonElement serialize(@NotNull String value) {
            return new JsonPrimitive(value);
        }

        @Override
        public @NotNull String deserialize(@NotNull JsonElement element) {
            return element.getAsString();
        }

    }

    public abstract static class Constrained<D extends Comparable<D>> extends StringData {

        public abstract Class<D> dataType();

        @NotNull
        public abstract Collection<D> values();

        @NotNull
        public abstract String serialize(@NotNull D value);

        @NotNull
        public abstract Optional<D> deserialize(@NotNull String string);

        public String name() {
            return this.location().path();
        }

    }

}

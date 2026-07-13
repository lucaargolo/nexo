package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;

public abstract class DataBase<T> extends Feature<DataBase<T>> {

    public DataBase(@NotNull Location location) {
        super(location);
    }

    @Override
    public final @NotNull Type<DataBase<T>> type() {
        return Type.data();
    }

    @NotNull
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

    @NotNull
    public T cast(@NotNull Object value) {
        throw new UnsupportedOperationException("Can't cast value for non-constrained data");
    }

    public abstract static class Constrained<D extends Comparable<D>> extends DataBase<D> {

        public Constrained(@NotNull Location location) {
            super(location);
        }

        @NotNull
        public abstract Class<D> valueClass();

        @NotNull
        public abstract Collection<D> values();

        @NotNull
        public abstract String toString(@NotNull D value);

        @NotNull
        public abstract Optional<D> fromString(@NotNull String string);

        @Override
        public @NotNull D cast(@NotNull Object value) {
            return valueClass().cast(value);
        }

        public String name() {
            return this.location().path();
        }

    }

}

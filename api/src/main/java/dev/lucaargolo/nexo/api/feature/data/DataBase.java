package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class DataBase<T> extends Feature<DataBase<?>, Unit<DataBase<?>, ?>> {

    public DataBase(@NotNull Location location) {
        super(location);
    }

    public DataBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<DataBase<?>, Unit<DataBase<?>, ?>> type() {
        return Type.data();
    }

    public abstract @NotNull T initial();

    public abstract @NotNull ByteBuffer write(@NotNull T value);

    public abstract @NotNull T read(@NotNull ByteBuffer buffer);

    public abstract @NotNull JsonElement serialize(@NotNull T value);

    public abstract @NotNull T deserialize(@NotNull JsonElement element);

    public boolean persistent() {
        return true;
    }

    public boolean synced() {
        return true;
    }

    public @NotNull T cast(@NotNull Object value) {
        throw new UnsupportedOperationException("Can't cast value for non-constrained data");
    }

    public abstract static class Constrained<T extends Comparable<T>> extends DataBase<T> {

        public Constrained(@NotNull Location location) {
            super(location);
        }

        public abstract @NotNull Class<T> valueClass();

        public abstract @NotNull Collection<T> values();

        public abstract @NotNull String toString(@NotNull T value);

        public abstract @NotNull Optional<T> fromString(@NotNull String string);

        @Override
        public @NotNull T cast(@NotNull Object value) {
            return valueClass().cast(value);
        }

        public String name() {
            return location().path();
        }

    }

}

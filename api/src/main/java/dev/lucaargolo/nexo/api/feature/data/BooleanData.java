package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class BooleanData extends DataBase.Constrained<Boolean> {

    @NotNull
    private final Boolean initial;

    public BooleanData(@NotNull Location location, @NotNull Boolean initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    public @NotNull Class<Boolean> valueClass() {
        return Boolean.class;
    }

    @Override
    public @NotNull Collection<Boolean> values() {
        return List.of(Boolean.TRUE, Boolean.FALSE);
    }

    @Override
    public @NotNull String toString(@NotNull Boolean value) {
        return value.toString();
    }

    @Override
    public @NotNull Optional<Boolean> fromString(@NotNull String string) {
        return switch (string) {
            case "true" -> Optional.of(Boolean.TRUE);
            case "false" -> Optional.of(Boolean.FALSE);
            default -> Optional.empty();
        };
    }

    @Override
    public @NotNull Boolean initial() {
        return initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Boolean data) {
        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES);
        buffer.put((byte) (data ? 1 : 0));
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Boolean read(@NotNull ByteBuffer buffer) {
        return buffer.get() != 0;
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Boolean data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Boolean deserialize(@NotNull JsonElement element) {
        return element.getAsBoolean();
    }

}

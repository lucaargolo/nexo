package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class BooleanData extends DataBase<Boolean> {

    @NotNull
    private final Boolean initial;
    @NotNull
    private final Location location;

    public BooleanData(@NotNull Boolean initial, @NotNull Location location) {
        this.initial = initial;
        this.location = location;
    }

    @Override
    public Boolean initial() {
        return this.initial;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
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

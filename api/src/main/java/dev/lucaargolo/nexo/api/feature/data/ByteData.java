package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class ByteData extends DataBase<Byte> {

    @NotNull
    private final Byte initial;
    @NotNull
    private final Location location;

    public ByteData(@NotNull Byte initial, @NotNull Location location) {
        this.initial = initial;
        this.location = location;
    }

    @Override
    public Byte initial() {
        return this.initial;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Byte data) {
        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Byte read(@NotNull ByteBuffer buffer) {
        return buffer.get();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Byte data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Byte deserialize(@NotNull JsonElement element) {
        return element.getAsByte();
    }

}

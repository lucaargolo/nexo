package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class ByteData extends DataBase<Byte> {

    @NotNull
    private final Byte initial;

    public ByteData(@NotNull Location location, @NotNull Byte initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    @NotNull
    public Byte initial() {
        return this.initial;
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

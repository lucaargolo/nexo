package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class ShortData extends DataBase<Short> {

    @NotNull
    private final Short initial;

    public ShortData(@NotNull Short initial) {
        this.initial = initial;
    }

    @Override
    public @NotNull Short initial() {
        return initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Short data) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Short read(@NotNull ByteBuffer buffer) {
        return buffer.getShort();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Short data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Short deserialize(@NotNull JsonElement element) {
        return element.getAsShort();
    }

}

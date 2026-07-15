package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class LongData extends DataBase<Long> {

    @NotNull
    private final Long initial;

    public LongData(@NotNull Location location, @NotNull Long initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    public @NotNull Long initial() {
        return initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Long data) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Long read(@NotNull ByteBuffer buffer) {
        return buffer.getLong();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Long data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Long deserialize(@NotNull JsonElement element) {
        return element.getAsLong();
    }

}

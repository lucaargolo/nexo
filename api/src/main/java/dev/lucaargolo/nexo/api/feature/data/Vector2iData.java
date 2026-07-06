package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.nio.ByteBuffer;

public final class Vector2iData extends NexoData<Vector2i> {

    private static final int BYTES = Integer.BYTES * 2;

    @NotNull
    private final Location location;

    public Vector2iData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Vector2i data) {
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putInt(data.x);
        buffer.putInt(data.y);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Vector2i read(@NotNull ByteBuffer buffer) {
        return new Vector2i(buffer.getInt(), buffer.getInt());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Vector2i data) {
        JsonArray array = new JsonArray();
        array.add(data.x);
        array.add(data.y);
        return array;
    }

    @Override
    public @NotNull Vector2i deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector2i(array.get(0).getAsInt(), array.get(1).getAsInt());
    }

}

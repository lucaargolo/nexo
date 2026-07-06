package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.nio.ByteBuffer;

public final class Vector2dData extends NexoData<Vector2d> {

    private static final int BYTES = Double.BYTES * 2;

    @NotNull
    private final Location location;

    public Vector2dData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Vector2d data) {
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putDouble(data.x);
        buffer.putDouble(data.y);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Vector2d read(@NotNull ByteBuffer buffer) {
        return new Vector2d(buffer.getDouble(), buffer.getDouble());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Vector2d data) {
        JsonArray array = new JsonArray();
        array.add(data.x);
        array.add(data.y);
        return array;
    }

    @Override
    public @NotNull Vector2d deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector2d(array.get(0).getAsDouble(), array.get(1).getAsDouble());
    }

}

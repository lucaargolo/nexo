package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.nio.ByteBuffer;

public final class Vector3dData extends NexoData<Vector3d> {

    private static final int BYTES = Double.BYTES * 3;

    @NotNull
    private final Location location;

    public Vector3dData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Vector3d data) {
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putDouble(data.x);
        buffer.putDouble(data.y);
        buffer.putDouble(data.z);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Vector3d read(@NotNull ByteBuffer buffer) {
        return new Vector3d(buffer.getDouble(), buffer.getDouble(), buffer.getDouble());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Vector3d data) {
        JsonArray array = new JsonArray();
        array.add(data.x);
        array.add(data.y);
        array.add(data.z);
        return array;
    }

    @Override
    public @NotNull Vector3d deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector3d(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

}

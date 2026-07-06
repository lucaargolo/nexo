package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public final class Vector3fData extends NexoData<Vector3f> {

    private static final int BYTES = Float.BYTES * 3;

    @NotNull
    private final Location location;

    public Vector3fData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Vector3f data) {
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putFloat(data.x);
        buffer.putFloat(data.y);
        buffer.putFloat(data.z);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Vector3f read(@NotNull ByteBuffer buffer) {
        return new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Vector3f data) {
        JsonArray array = new JsonArray();
        array.add(data.x);
        array.add(data.y);
        array.add(data.z);
        return array;
    }

    @Override
    public @NotNull Vector3f deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

}

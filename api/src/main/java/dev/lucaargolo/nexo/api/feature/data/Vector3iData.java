package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.nio.ByteBuffer;

public final class Vector3iData extends NexoData<Vector3i> {

    private static final int BYTES = Integer.BYTES * 3;

    @NotNull
    private final Location location;

    public Vector3iData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Vector3i data) {
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putInt(data.x);
        buffer.putInt(data.y);
        buffer.putInt(data.z);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Vector3i read(@NotNull ByteBuffer buffer) {
        return new Vector3i(buffer.getInt(), buffer.getInt(), buffer.getInt());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Vector3i data) {
        JsonArray array = new JsonArray();
        array.add(data.x);
        array.add(data.y);
        array.add(data.z);
        return array;
    }

    @Override
    public @NotNull Vector3i deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector3i(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

}

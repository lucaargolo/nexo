package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class LocationData extends NexoData<Location> {

    @NotNull
    private final Location location;

    public LocationData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Location data) {
        byte[] nsBytes = data.namespace().getBytes(StandardCharsets.UTF_8);
        byte[] pathBytes = data.path().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + nsBytes.length + Integer.BYTES + pathBytes.length);
        buffer.putInt(nsBytes.length);
        buffer.put(nsBytes);
        buffer.putInt(pathBytes.length);
        buffer.put(pathBytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Location read(@NotNull ByteBuffer buffer) {
        int nsLen = buffer.getInt();
        byte[] nsBytes = new byte[nsLen];
        buffer.get(nsBytes);
        String namespace = new String(nsBytes, StandardCharsets.UTF_8);
        int pathLen = buffer.getInt();
        byte[] pathBytes = new byte[pathLen];
        buffer.get(pathBytes);
        String path = new String(pathBytes, StandardCharsets.UTF_8);
        return Location.of(namespace, path);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Location data) {
        return new JsonPrimitive(data.namespace() + ":" + data.path());
    }

    @Override
    public @NotNull Location deserialize(@NotNull JsonElement element) {
        String[] parts = element.getAsString().split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Location string: " + element.getAsString());
        }
        return Location.of(parts[0], parts[1]);
    }

}

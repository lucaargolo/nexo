package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SetData<D> extends DataBase<Set<D>> {

    private final @NotNull DataBase<D> data;

    public SetData(@NotNull DataBase<D> data) {
        this.data = data;
    }

    public @NotNull DataBase<D> data() {
        return this.data;
    }

    @Override
    public @NotNull Set<D> initial() {
        return Set.of();
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Set<D> value) {
        ByteBuffer[] buffers = new ByteBuffer[value.size()];
        int bytes = Integer.BYTES;
        int index = 0;
        for (D element : value) {
            ByteBuffer buffer = this.data.write(element);
            buffers[index++] = buffer;
            bytes += buffer.remaining();
        }
        ByteBuffer result = ByteBuffer.allocate(bytes);
        result.putInt(value.size());
        for (ByteBuffer buffer : buffers) {
            result.put(buffer);
        }
        result.flip();
        return result;
    }

    @Override
    public @NotNull Set<D> read(@NotNull ByteBuffer buffer) {
        int size = buffer.getInt();
        Set<D> result = new LinkedHashSet<>(size);
        for (int index = 0; index < size; index++) {
            result.add(this.data.read(buffer));
        }
        return Set.copyOf(result);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Set<D> value) {
        JsonArray array = new JsonArray(value.size());
        for (D element : value) {
            array.add(this.data.serialize(element));
        }
        return array;
    }

    @Override
    public @NotNull Set<D> deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        Set<D> values = new LinkedHashSet<>(array.size());
        for (JsonElement item : array) {
            values.add(this.data.deserialize(item));
        }
        return Set.copyOf(values);
    }

}

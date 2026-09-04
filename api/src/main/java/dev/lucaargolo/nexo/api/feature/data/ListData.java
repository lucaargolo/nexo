package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ListData<D> extends DataBase<List<D>> {

    private final @NotNull DataBase<D> data;

    public ListData(@NotNull DataBase<D> data) {
        this.data = data;
    }

    public @NotNull DataBase<D> data() {
        return this.data;
    }

    @Override
    public @NotNull List<D> initial() {
        return List.of();
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull List<D> value) {
        ByteBuffer[] buffers = new ByteBuffer[value.size()];
        int bytes = Integer.BYTES;
        for (int index = 0; index < value.size(); index++) {
            ByteBuffer buffer = this.data.write(value.get(index));
            buffers[index] = buffer;
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
    public @NotNull List<D> read(@NotNull ByteBuffer buffer) {
        int size = buffer.getInt();
        List<D> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(this.data.read(buffer));
        }
        return List.copyOf(result);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull List<D> value) {
        JsonArray array = new JsonArray(value.size());
        for (D element : value) {
            array.add(this.data.serialize(element));
        }
        return array;
    }

    @Override
    public @NotNull List<D> deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        List<D> result = new ArrayList<>(array.size());
        for (JsonElement item : array) {
            result.add(this.data.deserialize(item));
        }
        return List.copyOf(result);
    }

}

package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.Nexo;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public final class ArrayData<D> extends DataBase<D[]> {

    private final @NotNull DataBase<D> data;
    private final @NotNull IntFunction<D[]> factory;

    public ArrayData(@NotNull DataBase<D> data, @NotNull IntFunction<D[]> factory) {
        this.data = data;
        this.factory = factory;
    }

    public ArrayData(@NotNull DataBase<D> data, @NotNull Class<D> componentType) {
        this(data, size -> createArray(componentType, size));
    }

    public @NotNull DataBase<D> data() {
        return this.data;
    }

    @Override
    public @NotNull D[] initial() {
        return this.factory.apply(0);
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull D @NotNull [] value) {
        ByteBuffer[] buffers = new ByteBuffer[value.length];
        int bytes = Integer.BYTES;
        for (int index = 0; index < value.length; index++) {
            ByteBuffer buffer = this.data.write(value[index]);
            buffers[index] = buffer;
            bytes += buffer.remaining();
        }
        ByteBuffer result = ByteBuffer.allocate(bytes);
        result.putInt(value.length);
        for (ByteBuffer buffer : buffers) {
            result.put(buffer);
        }
        result.flip();
        return result;
    }

    @Override
    public @NotNull D[] read(@NotNull ByteBuffer buffer) {
        int size = buffer.getInt();
        D[] result = this.factory.apply(size);
        for (int index = 0; index < size; index++) {
            result[index] = this.data.read(buffer);
        }
        return result;
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D @NotNull [] value) {
        JsonArray array = new JsonArray(value.length);
        for (D element : value) {
            array.add(this.data.serialize(element));
        }
        return array;
    }

    @Override
    public @NotNull D[] deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        D[] result = this.factory.apply(array.size());
        for (int index = 0; index < array.size(); index++) {
            result[index] = this.data.deserialize(array.get(index));
        }
        return result;
    }

    private static <D> @NotNull D[] createArray(@NotNull Class<D> componentType, int size) {
        Object array = Array.newInstance(componentType, size);
        return Nexo.<D[]>type(array.getClass()).cast(array);
    }

}

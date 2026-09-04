package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ItemData extends DataBase<ItemUnit> {

    private final @NotNull Nexo nexo;

    public ItemData(@NotNull Nexo nexo) {
        this.nexo = nexo;
    }

    @Override
    public @NotNull ItemUnit initial() {
        throw new UnsupportedOperationException("ItemData has no initial value");
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull ItemUnit value) {
        byte[] bytes = this.serialize(value).toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull ItemUnit read(@NotNull ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0 || length > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid item data length: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return this.deserialize(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)));
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull ItemUnit value) {
        JsonObject serialized = new JsonObject();
        serialized.addProperty("item", value.feature().location().toString());
        JsonObject data = new JsonObject();
        for (DataBase<?> itemData : value.data()) {
            serializeData(value, itemData, data);
        }
        serialized.add("data", data);
        return serialized;
    }

    @Override
    public @NotNull ItemUnit deserialize(@NotNull JsonElement element) {
        JsonObject serialized = element.getAsJsonObject();
        ItemBase feature = this.nexo.getFeature(Feature.Type.ITEM, Location.parse(serialized.getAsJsonPrimitive("item").getAsString()));
        if (feature == null) {
            throw new IllegalArgumentException("Unknown item feature in item data: " + serialized.getAsJsonPrimitive("item").getAsString());
        }
        ItemUnit item = this.nexo.unit(feature);
        if (item == null) {
            throw new IllegalArgumentException("Cannot create item unit for item data: " + feature.location());
        }
        JsonObject data = serialized.getAsJsonObject("data");
        for (var entry : data.entrySet()) {
            DataBase<?> itemData = this.nexo.getFeature(Feature.Type.data(), Location.parse(entry.getKey()));
            if (itemData == null) {
                throw new IllegalArgumentException("Unknown data feature in item data: " + entry.getKey());
            }
            deserializeData(item, itemData, entry.getValue());
        }
        return item;
    }

    private static void serializeData(@NotNull ItemUnit item, @NotNull DataBase<?> data, @NotNull JsonObject target) {
        Class<DataBase<Object>> type = Nexo.type(DataBase.class);
        serializeTypedData(item, type.cast(data), target);
    }

    private static <D> void serializeTypedData(@NotNull ItemUnit item, @NotNull DataBase<D> data, @NotNull JsonObject target) {
        D value = item.getData(data);
        if (value != null) {
            target.add(data.location().toString(), data.serialize(value));
        }
    }

    private static void deserializeData(@NotNull ItemUnit item, @NotNull DataBase<?> data, @NotNull JsonElement value) {
        Class<DataBase<Object>> type = Nexo.type(DataBase.class);
        deserializeTypedData(item, type.cast(data), value);
    }

    private static <D> void deserializeTypedData(@NotNull ItemUnit item, @NotNull DataBase<D> data, @NotNull JsonElement value) {
        item.setData(data, data.deserialize(value));
    }

}

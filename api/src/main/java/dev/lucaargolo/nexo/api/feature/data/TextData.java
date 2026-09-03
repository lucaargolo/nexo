package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class TextData extends DataBase<Text> {

    @NotNull
    public static final TextData TEXT = new TextData(Location.of("nexo", "text"), Text.literal(""));

    @NotNull
    private final Text initial;

    public TextData(@NotNull Location location, @NotNull Text initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    public @NotNull Text initial() {
        return initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Text value) {
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Text read(@NotNull ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0 || length > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid text length: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return Text.parse(new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Text value) {
        return new JsonPrimitive(value.toString());
    }

    @Override
    public @NotNull Text deserialize(@NotNull JsonElement element) {
        return Text.parse(element.getAsString());
    }

}

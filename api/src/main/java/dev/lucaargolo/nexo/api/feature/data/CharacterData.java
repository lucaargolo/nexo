package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class CharacterData extends DataBase<Character> {

    @NotNull
    private final Location location;

    public CharacterData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Character data) {
        ByteBuffer buffer = ByteBuffer.allocate(Character.BYTES);
        buffer.putChar(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Character read(@NotNull ByteBuffer buffer) {
        return buffer.getChar();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Character data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Character deserialize(@NotNull JsonElement element) {
        return element.getAsString().charAt(0);
    }

}

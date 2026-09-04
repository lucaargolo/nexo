package dev.lucaargolo.nexo.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class MinecraftPropertyData<T extends Comparable<T>> extends DataBase.Constrained<T> {

    private final @NotNull Property<T> property;
    private final @NotNull T initial;

    public MinecraftPropertyData(@NotNull Property<T> property, @NotNull T initial) {
        this.property = property;
        this.initial = initial;
    }

    @Override
    public @NotNull String name() {
        return this.property.getName();
    }

    @Override
    public @NotNull Class<T> valueClass() {
        return this.property.getValueClass();
    }

    @Override
    public @NotNull Collection<T> values() {
        return this.property.getPossibleValues();
    }

    @Override
    public @NotNull String toString(@NotNull T value) {
        return this.property.getName(value);
    }

    @Override
    public @NotNull Optional<T> fromString(@NotNull String string) {
        return this.property.getValue(string);
    }

    @Override
    public @NotNull T initial() {
        return this.initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull T data) {
        return StandardCharsets.UTF_8.encode(this.property.getName(data));
    }

    @Override
    public @NotNull T read(@NotNull ByteBuffer buffer) {
        String string = StandardCharsets.UTF_8.decode(buffer).toString();
        return this.property.getValue(string).orElseThrow(() -> new IllegalArgumentException("Unknown value '" + string + "' for property " + this.property));
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull T data) {
        return new JsonPrimitive(this.property.getName(data));
    }

    @Override
    public @NotNull T deserialize(@NotNull JsonElement element) {
        String string = element.getAsString();
        return this.property.getValue(string).orElseThrow(() -> new IllegalArgumentException("Unknown value '" + string + "' for property " + this.property));
    }

    public static <T extends Comparable<T>> MinecraftPropertyData<T> of(@NotNull Property<T> property, @NotNull Function<Property<T>, T> provider) {
        return new MinecraftPropertyData<T>(property, provider.apply(property));
    }

}

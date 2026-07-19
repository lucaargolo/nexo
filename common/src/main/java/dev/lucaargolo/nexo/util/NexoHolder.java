package dev.lucaargolo.nexo.util;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class NexoHolder<T> implements Supplier<T> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final ResourceKey<T> key;
    @NotNull
    private final Location location;
    @NotNull
    private final Holder<T> holder;

    public NexoHolder(@NotNull NexoMinecraft nexo, @NotNull Holder<T> holder) {
        this.nexo = nexo;
        this.key = holder.unwrapKey().orElseThrow();
        this.location = NexoMinecraft.id(key);
        this.holder = holder;
    }

    public @NotNull NexoMinecraft nexo() {
        return nexo;
    }

    public @NotNull ResourceKey<T> key() {
        return key;
    }

    public @NotNull Location location() {
        return location;
    }

    @Override
    public T get() {
        return this.holder.value();
    }

    public Stream<TagKey<T>> tags() {
        return this.holder.tags();
    }

    public Holder<T> holder() {
        return this.holder;
    }
}

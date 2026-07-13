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

public class NexoHolder<R, T extends R> implements Supplier<T> {

    @NotNull
    private final NexoMinecraft nexo;

    @NotNull
    private final ResourceKey<R> key;
    @NotNull
    private final Location location;

    @Nullable
    private Supplier<T> supplier;
    @Nullable
    private Holder<R> holder;
    @Nullable
    private Class<T> type;


    public NexoHolder(@NotNull NexoMinecraft nexo, @NotNull ResourceKey<R> key, @NotNull Class<T> type) {
        this.nexo = nexo;
        this.key = key;
        this.location = NexoMinecraft.id(key);
        this.type = type;
    }

    public NexoHolder(@NotNull NexoMinecraft nexo, @NotNull ResourceKey<R> key, @NotNull Supplier<T> supplier) {
        this.nexo = nexo;
        this.key = key;
        this.location = NexoMinecraft.id(key);
        this.supplier = supplier;
    }

    public NexoHolder(@NotNull NexoMinecraft nexo, @NotNull Holder<R> holder, @NotNull Class<T> type) {
        this.nexo = nexo;
        this.key = holder.unwrapKey().orElseThrow();
        this.location = NexoMinecraft.id(key);
        this.holder = holder;
        this.type = type;
    }



    public ResourceKey<R> key() {
        return key;
    }

    public Location location() {
        return location;
    }

    @Override
    public T get() {
        if(this.supplier != null) {
            return this.supplier.get();
        }else if(this.type != null) {
            if (this.holder == null) {
                this.holder = this.nexo.getRegistry().registry(key.registryKey()).flatMap(r -> r.getHolder(key)).orElseThrow();
            }
            return this.type.cast(this.holder.value());
        }else{
            throw new IllegalStateException("NexoHolder has no supplier or type to satisfy holder");
        }
    }

    public Stream<TagKey<R>> tags() {
        if (this.holder == null) {
            this.holder = this.nexo.getRegistry().registry(key.registryKey()).flatMap(r -> r.getHolder(key)).orElseThrow();
        }
        return this.holder.tags();
    }

    public Holder<R> holder() {
        if (this.holder == null) {
            this.holder = this.nexo.getRegistry().registry(key.registryKey()).flatMap(r -> r.getHolder(key)).orElseThrow();
        }
        return this.holder;
    }
}

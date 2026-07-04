package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class MinecraftFeature<T, D extends IFeature<D>> implements IFeature<D> {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<T> holder;
    @Nullable
    private final D delegate;

    protected MinecraftFeature(@NotNull Holder<T> holder, @Nullable D delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public @NotNull Holder<T> getHolder() {
        return holder;
    }

    @Nullable
    public D getDelegate() {
        return delegate;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

}

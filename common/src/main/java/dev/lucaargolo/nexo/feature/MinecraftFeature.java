package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.feature.IFeature;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MinecraftFeature<T> implements IFeature {

    private final Location location;
    private final Holder<T> holder;

    protected MinecraftFeature(Holder<T> holder) {
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
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

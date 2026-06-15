package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Tag;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MinecraftFeature<T> implements Feature {

    private final Identifier id;
    private final Holder<T> holder;

    protected MinecraftFeature(Holder<T> holder) {
        this.holder = holder;
        this.id = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    @Override
    public @NotNull Identifier id() {
        return id;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }
}

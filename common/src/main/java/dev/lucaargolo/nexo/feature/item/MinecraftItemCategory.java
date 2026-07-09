package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItemCategory extends NexoItemCategory implements MinecraftFeature<NexoItemCategory, CreativeModeTab> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final Location location;
    @NotNull
    private final Holder<CreativeModeTab> holder;
    @Nullable
    private final NexoItemCategory delegate;

    public MinecraftItemCategory(@NotNull NexoMinecraft nexo, @NotNull Holder<CreativeModeTab> holder, @Nullable NexoItemCategory delegate) {
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftItemCategory(@NotNull NexoMinecraft nexo, @NotNull Holder<CreativeModeTab> holder) {
        this(nexo, holder, null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull Holder<CreativeModeTab> holder() {
        return this.holder;
    }

    @Override
    public @Nullable NexoItemCategory delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull Location location() {
        return this.location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static MinecraftItemCategory register(NexoRegistryHandler<?> helper, ResourceLocation id, NexoItemCategory category) {
        Holder<CreativeModeTab> holder = helper.registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, helper.createCreativeTab(category));
        return new MinecraftItemCategory(helper.nexo(), holder, category);
    }

}

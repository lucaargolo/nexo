package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItemCategory extends NexoItemCategory implements MinecraftFeature<NexoItemCategory, CreativeModeTab> {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<CreativeModeTab> holder;
    @Nullable
    private final NexoItemCategory delegate;

    public MinecraftItemCategory(Holder<CreativeModeTab> holder, NexoItemCategory delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftItemCategory(Holder<CreativeModeTab> holder) {
        this(holder, null);
    }

    @Override
    public @NotNull Holder<CreativeModeTab> holder() {
        return holder;
    }

    @Override
    public @Nullable NexoItemCategory delegate() {
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

    public static MinecraftItemCategory register(ResourceLocation id, NexoItemCategory category) {
        Holder<CreativeModeTab> holder = NexoMinecraft.getHelper().registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id,
                NexoMinecraft.getHelper().createCreativeTab(category));
        return new MinecraftItemCategory(holder, category);
    }

}

package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.item.BaseItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItemCategory extends BaseItemCategory {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<CreativeModeTab> holder;
    @Nullable
    private final BaseItemCategory delegate;

    public MinecraftItemCategory(Holder<CreativeModeTab> holder, BaseItemCategory delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftItemCategory(Holder<CreativeModeTab> holder) {
        this(holder, null);
    }

    public @NotNull Holder<CreativeModeTab> getHolder() {
        return holder;
    }

    @Nullable
    public BaseItemCategory getDelegate() {
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

    public static MinecraftItemCategory register(ResourceLocation id, BaseItemCategory category) {
        Holder<CreativeModeTab> holder = NexoMinecraft.getHelper().registerFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id,
                NexoMinecraft.getHelper().createCreativeTab(category));
        return new MinecraftItemCategory(holder, category);
    }

}

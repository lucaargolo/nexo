package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItemCategory extends ItemCategoryBase implements MinecraftFeature<ItemCategoryBase, CreativeModeTab> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<CreativeModeTab, CreativeModeTab> holder;
    @Nullable
    private final ItemCategoryBase delegate;

    public MinecraftItemCategory(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<CreativeModeTab, CreativeModeTab> holder, @Nullable ItemCategoryBase delegate) {
        super(holder.location());
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
    }

    public MinecraftItemCategory(@NotNull NexoMinecraft nexo, @NotNull Holder<CreativeModeTab> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, CreativeModeTab.class), null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull NexoHolder<CreativeModeTab, CreativeModeTab> holder() {
        return this.holder;
    }

    @Override
    public @Nullable ItemCategoryBase delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static MinecraftItemCategory register(NexoRegistryHandler<?> helper, ResourceLocation id, ItemCategoryBase category) {
        NexoHolder<CreativeModeTab, CreativeModeTab> holder = helper.registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, helper.createCreativeTab(category));
        return new MinecraftItemCategory(helper.nexo(), holder, category);
    }

}

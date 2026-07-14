package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftItemCategory extends ItemCategoryBase {

    private static final ConcurrentHashMap<Location, ItemCategoryBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<CreativeModeTab, CreativeModeTab>> HOLDER_MAP = new ConcurrentHashMap<>();

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<CreativeModeTab, CreativeModeTab> holder;

    private MinecraftItemCategory(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<CreativeModeTab, CreativeModeTab> holder) {
        super(holder.location());
        this.nexo = nexo;
        this.holder = holder;
    }

    private MinecraftItemCategory(@NotNull NexoMinecraft nexo, @NotNull Holder<CreativeModeTab> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, CreativeModeTab.class));
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static ItemCategoryBase lookup(NexoRegistryHandler<?> helper, Location location) {
        return FEATURE_MAP.computeIfAbsent(location, l -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            MinecraftItemCategory category = BuiltInRegistries.CREATIVE_MODE_TAB.getHolder(id).map(h -> new MinecraftItemCategory(helper.nexo(), h)).orElse(null);
            if (category != null) HOLDER_MAP.put(location, category.holder);
            return category;
        });
    }

    public static ItemCategoryBase register(NexoRegistryHandler<?> helper, ResourceLocation id, ItemCategoryBase category) {
        NexoHolder<CreativeModeTab, CreativeModeTab> holder = helper.registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, helper.createCreativeTab(category));
        FEATURE_MAP.put(category.location(), category);
        HOLDER_MAP.put(category.location(), holder);
        return category;
    }

    public static CreativeModeTab craft(ItemCategoryBase category) {
        return Objects.requireNonNull(HOLDER_MAP.get(category.location()).get());
    }

}

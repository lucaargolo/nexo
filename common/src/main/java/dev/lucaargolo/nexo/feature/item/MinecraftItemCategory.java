package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftItemCategory extends ItemCategoryBase {

    public static final ConcurrentHashMap<ItemCategoryBase, List<ItemBase>> ITEM_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Location, ItemCategoryBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<CreativeModeTab>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<ItemCategoryBase, Holder<CreativeModeTab>> CONVERT = new Bijection<>() {
        @Override
        public Holder<CreativeModeTab> forward(ItemCategoryBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public ItemCategoryBase backward(Holder<CreativeModeTab> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    @NotNull
    private final Holder<CreativeModeTab> holder;

    private MinecraftItemCategory(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<CreativeModeTab> holder) {
        super(MinecraftRoleType.uncraft(nexo, Type.ITEM_CATEGORY, holder));
        this.identify(nexo, nexo.getRegistryHandler().identity(holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static ItemCategoryBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static ItemCategoryBase register(NexoMinecraft<?, ?, ?, ?> nexo, ItemCategoryBase category) {
        ItemCategoryBase registered = FEATURE_MAP.get(category.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(category.location());
        FEATURE_MAP.put(category.location(), category);
        nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, MinecraftFeatureType.ITEM_CATEGORY.craft(nexo, category));
        return category;
    }

    public static ItemCategoryBase index(NexoMinecraft<?, ?, ?, ?> nexo, Holder<CreativeModeTab> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftItemCategory(nexo, holder));
    }

    public static CreativeModeTab craft(NexoMinecraft<?, ?, ?, ?> nexo, ItemCategoryBase category) {
        return nexo.getRegistryHandler().craftCreativeTab(category);
    }

}

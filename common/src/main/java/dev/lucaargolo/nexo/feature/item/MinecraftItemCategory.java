package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
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

    //TODO: Improve this
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

    private MinecraftItemCategory(NexoRegistryHandler<?> helper, @NotNull Holder<CreativeModeTab> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(helper, Type.ITEM_CATEGORY, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static ItemCategoryBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static ItemCategoryBase register(NexoRegistryHandler<?> helper, ItemCategoryBase category) {
        ItemCategoryBase registered = FEATURE_MAP.get(category.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(category.location());
        FEATURE_MAP.put(category.location(), category);
        helper.registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, MinecraftFeatureType.ITEM_CATEGORY.craft(helper, category));
        return category;
    }

    public static ItemCategoryBase index(NexoRegistryHandler<?> helper, Holder<CreativeModeTab> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftItemCategory(helper, holder));
    }

    public static CreativeModeTab craft(NexoRegistryHandler<?> helper, ItemCategoryBase category) {
        return helper.craftCreativeTab(category);
    }

}

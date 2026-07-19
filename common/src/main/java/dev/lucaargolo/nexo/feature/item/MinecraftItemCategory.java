package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
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

    //TODO: Improve this
    public static final ConcurrentHashMap<ItemCategoryBase, List<ItemBase>> ITEM_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Location, ItemCategoryBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<CreativeModeTab>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<ItemCategoryBase, NexoHolder<CreativeModeTab>> CONVERT = new Bijection<>() {
        @Override
        public NexoHolder<CreativeModeTab> forward(ItemCategoryBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public ItemCategoryBase backward(NexoHolder<CreativeModeTab> holder) {
            return FEATURE_MAP.get(holder.location());
        }
    };

    @NotNull
    private final NexoHolder<CreativeModeTab> holder;

    private MinecraftItemCategory(NexoRegistryHandler<?> helper, @NotNull NexoHolder<CreativeModeTab> holder) {
        super(holder.location(), MinecraftRoleType.uncraft(helper, Type.ITEM_CATEGORY, holder));
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
        NexoHolder<CreativeModeTab> holder = helper.registerBuiltinFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, MinecraftFeatureType.ITEM_CATEGORY.craft(helper, category));
        FEATURE_MAP.put(category.location(), category);
        HOLDER_MAP.put(category.location(), holder);
        return category;
    }

    public static NexoHolder<CreativeModeTab> index(NexoRegistryHandler<?> helper, CreativeModeTab category) {
        ResourceLocation location = Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(category));
        Location featureLocation = NexoMinecraft.id(location);
        NexoHolder<CreativeModeTab> indexed = HOLDER_MAP.get(featureLocation);
        if (indexed != null) {
            return indexed;
        }
        Holder<CreativeModeTab> h = BuiltInRegistries.CREATIVE_MODE_TAB.getHolder(location).orElseThrow();
        NexoHolder<CreativeModeTab> holder = new NexoHolder<>(helper.nexo(), h, CreativeModeTab.class);
        FEATURE_MAP.putIfAbsent(featureLocation, new MinecraftItemCategory(helper, holder));
        HOLDER_MAP.put(featureLocation, holder);
        return holder;
    }

    public static CreativeModeTab craft(NexoRegistryHandler<?> helper, ItemCategoryBase category) {
        return helper.createCreativeTab(category);
    }

}

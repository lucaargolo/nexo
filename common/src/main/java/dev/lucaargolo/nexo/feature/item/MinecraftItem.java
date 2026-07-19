package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftItem extends ItemBase {

    private static final ConcurrentHashMap<Location, ItemBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<Item>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<ItemBase, NexoHolder<Item>> CONVERT = new Bijection<>() {
        @Override
        public NexoHolder<Item> forward(ItemBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public ItemBase backward(NexoHolder<Item> holder) {
            return FEATURE_MAP.get(holder.location());
        }
    };

    private final @NotNull NexoRegistryHandler<?> helper;
    private final @NotNull NexoHolder<Item> holder;

    private boolean computedCategory = false;
    private @Nullable ItemCategoryBase category;

    private MinecraftItem(@NotNull NexoRegistryHandler<?> helper, @NotNull NexoHolder<Item> holder) {
        super(holder.location(), MinecraftRoleType.uncraft(helper, Type.ITEM, holder));
        this.helper = helper;
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable StaticRenderer<Graphics3D, ItemUnit<?>> renderer() {
        //TODO: This
        return null;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        if(!this.computedCategory) {
            this.computedCategory = true;
            Item item = MinecraftFeatureType.ITEM.convert(this);
            for(CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                for(ItemStack stack : tab.getDisplayItems()) {
                    if(stack.getItem() == item) {
                        this.category = MinecraftFeatureType.ITEM_CATEGORY.convert(this.helper, tab);
                        return this.category;
                    }
                }
            }
        }
        return this.category;
    }

    public static ItemBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static ItemBase register(NexoRegistryHandler<?> helper, ItemBase item) {
        ItemBase registered = FEATURE_MAP.get(item.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(item.location());
        NexoHolder<Item> holder = helper.registerBuiltinFeature(BuiltInRegistries.ITEM, id, MinecraftFeatureType.ITEM.craft(helper, item));
        FEATURE_MAP.put(item.location(), item);
        HOLDER_MAP.put(item.location(), holder);
        if(item.category() != null) {
            MinecraftItemCategory.ITEM_MAP.computeIfAbsent(item.category(), c -> new LinkedList<>()).add(item);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    public static NexoHolder<Item> index(NexoRegistryHandler<?> helper, Item item) {
        Holder<Item> h = item.builtInRegistryHolder();
        Location location = NexoMinecraft.id(h);
        NexoHolder<Item> indexed = HOLDER_MAP.get(location);
        if (indexed != null) {
            return indexed;
        }
        NexoHolder<Item> holder = new NexoHolder<>(helper.nexo(), h, Item.class);
        FEATURE_MAP.putIfAbsent(location, new MinecraftItem(helper, holder));
        HOLDER_MAP.put(location, holder);
        return holder;
    }

    public static Item craft(NexoRegistryHandler<?> helper, ItemBase item) {
        return new Item(new Item.Properties());
    }

}

package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MinecraftItem extends ItemBase {

    private static final ConcurrentHashMap<Location, ItemBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<Item>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<ItemBase, Holder<Item>> CONVERT = new Bijection<>() {
        @Override
        public Holder<Item> forward(ItemBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public ItemBase backward(Holder<Item> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull Holder<Item> holder;

    private boolean computedCategory;
    private @Nullable ItemCategoryBase category;

    private final @NotNull List<@NotNull DataBase<?>> initialData;

    private MinecraftItem(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<Item> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(nexo, Type.ITEM, holder));
        this.nexo = nexo;
        this.holder = holder;
        Item item = holder.value();
        List<@NotNull DataBase<?>> initialData = new ArrayList<>();
        for (TypedDataComponent<?> component : item.components()) {
            DataBase<?> data = componentData(component);
            if (data != null) {
                initialData.add(data);
            }
        }
        this.initialData = List.copyOf(initialData);
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> initialData() {
        return this.initialData;
    }

    private <D> @Nullable DataBase<?> componentData(@NotNull TypedDataComponent<D> component) {
        ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
        if (id == null) {
            return null;
        }
        Holder<DataComponentType<?>> holder = BuiltInRegistries.DATA_COMPONENT_TYPE.getHolderOrThrow(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, id));
        return new MinecraftData.Initial<>(this.nexo, holder, component.value());
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable StaticRenderer<Graphics3D, ItemUnit<?>> renderer() {
        // Minecraft-backed features are created from vanilla holders and carry no user-supplied renderer.
        return null;
    }

    @Override
    public <V extends Unit<?, ?>> @NotNull Map<String, Function<ItemUnit<?>, ? extends @Nullable Vault<V>>> vaults(@NotNull Class<V> type) {
        if (!MinecraftContainerVault.supports(type)) {
            return Map.of();
        }
        return Map.of(MinecraftContainerVault.KEY, unit -> unit.vault(type, MinecraftContainerVault.KEY));
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        if (!this.computedCategory) {
            this.computedCategory = true;
            Item item = MinecraftFeatureType.ITEM.convert(this);
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (stack.getItem() == item) {
                        this.category = MinecraftFeatureType.ITEM_CATEGORY.convert(this.nexo, tab);
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

    public static ItemBase register(NexoMinecraft<?, ?, ?, ?> nexo, ItemBase item) {
        ItemBase registered = FEATURE_MAP.get(item.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(item.location());
        FEATURE_MAP.put(item.location(), item);
        if (item.category() != null) {
            MinecraftItemCategory.ITEM_MAP.computeIfAbsent(item.category(), c -> new LinkedList<>()).add(item);
        }
        Holder<Item> itemHolder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.ITEM, id, MinecraftFeatureType.ITEM.craft(nexo, item));
        nexo.getRegistryHandler().registerVaults(MinecraftFeatureType.ITEM, item, itemHolder::value);
        return item;
    }

    public static ItemBase index(NexoMinecraft<?, ?, ?, ?> nexo, Holder<Item> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftItem(nexo, holder));
    }

    public static <M extends Item> Item craft(NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<Item.Properties, M> factory, ItemBase item) {
        extender.override(Utils.At.AFTER_SUPER, "inventoryTick", void.class, ItemStack.class, Level.class, Entity.class, int.class, boolean.class, (feature, stack, level, entity, slotId, selected) -> {
            Ticker<ItemUnit<?>> ticker = item.ticker();
            if (ticker != null) {
                ticker.tick(nexo.stackToUnit(stack));
            }
            return null;
        });

        Item.Properties properties = new Item.Properties();
        for (DataBase<?> data : item.initialData()) {
            properties = setInitialComponent(properties, data);
        }
        if (factory != null) {
            return factory.apply(properties);
        }
        return extender.instantiate(properties);
    }

    private static @NotNull <D> Item.Properties setInitialComponent(Item.Properties properties, DataBase<D> data) {
        if (data instanceof MinecraftData<?>) {
            return properties;
        }
        Holder<DataComponentType<D>> holder = MinecraftData.holder(data);
        return properties.component(holder.value(), data.initial());
    }

}

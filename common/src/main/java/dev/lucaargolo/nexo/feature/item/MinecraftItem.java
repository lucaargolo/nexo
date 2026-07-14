package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftItem extends ItemBase {

    private static final ConcurrentHashMap<Location, ItemBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<Item, Item>> HOLDER_MAP = new ConcurrentHashMap<>();

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<Item, Item> holder;

    private MinecraftItem(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<Item, Item> holder) {
        super(holder.location());
        this.nexo = nexo;
        this.holder = holder;
    }

    private MinecraftItem(@NotNull NexoMinecraft nexo, Holder<Item> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, Item.class));
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Model model() {
        //TODO: This
        return null;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        //TODO: This
        return null;
    }

    public static ItemBase lookup(NexoRegistryHandler<?> helper, Location location) {
        return FEATURE_MAP.computeIfAbsent(location, l -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            MinecraftItem item = BuiltInRegistries.ITEM.getHolder(id).map(h -> new MinecraftItem(helper.nexo(), h)).orElse(null);
            if (item != null) HOLDER_MAP.put(location, item.holder);
            return item;
        });
    }

    public static ItemBase register(NexoRegistryHandler<?> helper, ResourceLocation id, ItemBase item) {
        NexoHolder<Item, Item> holder = helper.registerBuiltinFeature(BuiltInRegistries.ITEM, id, () -> {
            if (item.hasComponent(BlockItemComponent.class)) {
                BlockItemComponent component = item.getComponent(BlockItemComponent.class);
                assert component != null;
                Block block = MinecraftFeatureType.BLOCK.craft(component.block());
                return new BlockItem(block, new Item.Properties());
            } else {
                return new Item(new Item.Properties());
            }
        });
        FEATURE_MAP.put(item.location(), item);
        HOLDER_MAP.put(item.location(), holder);
        return item;
    }

    public static Item craft(ItemBase item) {
        return Objects.requireNonNull(HOLDER_MAP.get(item.location()).get());
    }

}

package dev.lucaargolo.nexo;

import com.google.common.collect.Maps;
import dev.lucaargolo.nexo.api.NexoMod;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.IItem;
import dev.lucaargolo.nexo.api.feature.IItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import dev.lucaargolo.nexo.feature.MinecraftItem;
import dev.lucaargolo.nexo.feature.MinecraftItemCategory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FabricNexoMinecraft extends NexoMinecraft implements ModInitializer {

    private final Map<Class<? extends IFeature>, Map<Location, IFeature>> FEATURE_REGISTRY = Maps.newHashMap();

    @Override
    public void onInitialize() {
        this.init();
    }

    @Override
    public String getPlatform() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public @Nullable NexoMod getMod(String id) {
        return this.modDiscovery.getMod(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, I feature) {
        Location location = feature.location();
        if (IBlock.class == type && feature instanceof IBlock block) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            Holder.Reference<Block> holder = Registry.registerForHolder(
                BuiltInRegistries.BLOCK,
                blockId,
                new Block(BlockBehaviour.Properties.of())
            );
            MinecraftBlock minecraftBlock = emit(new FeatureRegisteredEvent<>(location, new MinecraftBlock(holder, block)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftBlock);
            return (T) minecraftBlock;
        }else if(IItem.class == type && feature instanceof IItem item) {
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            Holder.Reference<Item> holder = Registry.registerForHolder(
                BuiltInRegistries.ITEM,
                itemId,
                new Item(new Item.Properties())
            );
            MinecraftItem minecraftItem = emit(new FeatureRegisteredEvent<>(location, new MinecraftItem(holder, item)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItem);
            return (T) minecraftItem;
        }else if(type.isAssignableFrom(IItemCategory.class) && feature instanceof IItemCategory itemCategory) {
            ResourceLocation itemCategoryId = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            Holder.Reference<CreativeModeTab> holder = Registry.registerForHolder(
                    BuiltInRegistries.CREATIVE_MODE_TAB,
                    itemCategoryId,
                    FabricItemGroup.builder().build()
            );
            MinecraftItemCategory minecraftItemCategory = emit(new FeatureRegisteredEvent<>(location, new MinecraftItemCategory(holder, itemCategory)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItemCategory);
            return (T) minecraftItemCategory;
        }
        return null;
    }

    @Override
    public @NotNull <T extends IFeature> Map<Location, IFeature> getFeatureRegistry(Class<T> type) {
        return FEATURE_REGISTRY.getOrDefault(type, Map.of());
    }
}

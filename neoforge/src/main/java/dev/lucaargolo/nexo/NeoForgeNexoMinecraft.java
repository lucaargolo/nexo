package dev.lucaargolo.nexo;

import com.google.common.collect.Maps;
import dev.lucaargolo.nexo.api.NexoMod;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.IItem;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import dev.lucaargolo.nexo.feature.MinecraftItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NexoMinecraft.MOD_ID)
public class NeoForgeNexoMinecraft extends NexoMinecraft {

    private final IEventBus modBus;

    private final Map<Class<? extends IFeature>, Map<Location, IFeature>> FEATURE_REGISTRY = Maps.newHashMap();
    private final Map<String, DeferredRegister.Blocks> BLOCKS = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegister.Items> ITEMS = new ConcurrentHashMap<>();

    public NeoForgeNexoMinecraft(IEventBus modBus) {
        this.modBus = modBus;
        this.init();
    }

    public IEventBus getModBus() {
        return modBus;
    }

    @Override
    public String getPlatform() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public @Nullable NexoMod getMod(String id) {
        return this.modDiscovery.getMod(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, I feature) {
        Location location = feature.location();
        if (type.isAssignableFrom(IBlock.class) && feature instanceof IBlock block) {
            DeferredRegister.Blocks registry = BLOCKS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister.Blocks dr = DeferredRegister.createBlocks(ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<Block, ? extends Block> holder = registry.register(location.path(), () -> new Block(BlockBehaviour.Properties.of()));
            MinecraftBlock minecraftBlock = emit(new FeatureRegisteredEvent<>(location, new MinecraftBlock(holder, block)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftBlock);
            return (T) minecraftBlock;
        }else if(type.isAssignableFrom(IItem.class) && feature instanceof IItem item) {
            DeferredRegister.Items registry = ITEMS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister.Items dr = DeferredRegister.createItems(ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<Item, ? extends Item> holder = registry.register(location.path(), () -> new Item(new Item.Properties()));
            MinecraftItem minecraftItem = emit(new FeatureRegisteredEvent<>(location, new MinecraftItem(holder, item)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItem);
            return (T) minecraftItem;
        }
        return null;
    }

    @Override
    public @NotNull <T extends IFeature> Map<Location, IFeature> getFeatureRegistry(Class<T> type) {
        return FEATURE_REGISTRY.getOrDefault(type, Map.of());
    }
}

package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NexoMinecraft.MOD_ID)
public class NeoForgeNexoMinecraft extends NexoMinecraft {

    private final IEventBus modBus;
    private final Map<String, DeferredRegister.Blocks> BLOCKS = new ConcurrentHashMap<>();

    public NeoForgeNexoMinecraft(IEventBus modBus) {
        this.modBus = modBus;
        this.init();
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        this.getModDiscovery().finish();
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
    @SuppressWarnings("unchecked")
    public @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, Location location, I feature) {
        if (type.isAssignableFrom(IBlock.class) && feature instanceof IBlock block) {
            DeferredRegister.Blocks registry = BLOCKS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister.Blocks dr = DeferredRegister.createBlocks(ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<Block, ? extends Block> holder = registry.register(location.path(), () -> new Block(BlockBehaviour.Properties.of()));
            MinecraftBlock minecraftBlock = new MinecraftBlock(holder);
            cacheBlock(location, minecraftBlock);
            return (T) minecraftBlock;
        }
        return null;
    }
}

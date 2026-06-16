package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
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
    public @Nullable <T extends Feature> T add(Identifier id, T feature) {
        if (feature instanceof Block block) {
            DeferredRegister.Blocks registry = BLOCKS.computeIfAbsent(id.namespace(), ns -> {
                DeferredRegister.Blocks dr = DeferredRegister.createBlocks(ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<net.minecraft.world.level.block.Block, ? extends net.minecraft.world.level.block.Block> holder = registry.register(id.path(), () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of()));
            cacheBlock(id, new MinecraftBlock(holder));
            return feature;
        }
        return null;
    }
}

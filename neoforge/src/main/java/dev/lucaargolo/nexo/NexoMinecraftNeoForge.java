package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import dev.lucaargolo.nexo.neoforge.NeoForgeNexoModContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(NexoMinecraft.MOD_ID)
public class NexoMinecraftNeoForge extends NexoMinecraft {

    private static final Logger LOGGER = LoggerFactory.getLogger("Nexo|NeoForgeRegistration");

    private final IEventBus modBus;
    private final Map<String, DeferredRegister.Blocks> BLOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    public NexoMinecraftNeoForge(IEventBus modBus) {
        this.modBus = modBus;
        this.init();
        modBus.addListener(this::onLoadComplete);
    }

    @SuppressWarnings("unchecked")
    private void onLoadComplete(FMLLoadCompleteEvent event) {
        try {
            ModList modList = ModList.get();

            // Replace immutable mods list with mutable copy
            Field modsField = ModList.class.getDeclaredField("mods");
            modsField.setAccessible(true);
            List<Object> modsList = (List<Object>) modsField.get(modList);
            modsList = new ArrayList<>(modsList);
            modsField.set(modList, modsList);

            // Replace immutable indexedMods with mutable copy
            Field indexedModsField = ModList.class.getDeclaredField("indexedMods");
            indexedModsField.setAccessible(true);
            Map<String, Object> indexedMods = (Map<String, Object>) indexedModsField.get(modList);
            indexedMods = new HashMap<>(indexedMods);
            indexedModsField.set(modList, indexedMods);

            // Replace immutable sortedContainers (what getSortedMods() returns — the mod list screen reads this)
            Field sortedContainersField = ModList.class.getDeclaredField("sortedContainers");
            sortedContainersField.setAccessible(true);
            List<Object> sortedContainers = (List<Object>) sortedContainersField.get(modList);
            sortedContainers = new ArrayList<>(sortedContainers);
            sortedContainersField.set(modList, sortedContainers);

            // Also add to sortedList (getMods() for IModInfo access)
            Field sortedField = ModList.class.getDeclaredField("sortedList");
            sortedField.setAccessible(true);
            List<Object> sortedList = (List<Object>) sortedField.get(modList);
            sortedList = new ArrayList<>(sortedList);
            sortedField.set(modList, sortedList);

            for (NexoMod mod : this.getModDiscovery().getMods()) {
                NeoForgeNexoModContainer container = new NeoForgeNexoModContainer(mod);
                modsList.add(container);
                indexedMods.put(mod.modId(), container);
                sortedContainers.add(container);
                sortedList.add(container.getModInfo());
                LOGGER.info("Registered Nexo mod '{}' in NeoForge ModList", mod.modId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register Nexo mods in NeoForge ModList", e);
        }
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

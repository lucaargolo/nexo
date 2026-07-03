package dev.lucaargolo.nexo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod(NexoMinecraft.MOD_ID)
public class NeoForgeNexoMinecraft extends NexoMinecraft {

    private final IEventBus modBus;


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
    public @Nullable Mod getMod(@NotNull String id) {
        return this.modDiscovery.getMod(id);
    }

}

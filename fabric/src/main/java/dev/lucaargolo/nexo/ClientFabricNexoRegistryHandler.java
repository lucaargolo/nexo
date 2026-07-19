package dev.lucaargolo.nexo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

public class ClientFabricNexoRegistryHandler extends FabricNexoRegistryHandler {

    public ClientFabricNexoRegistryHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public RegistryAccess getLocalRegistry() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            Level level = minecraft.level;
            if (level != null) {
                return level.registryAccess();
            } else {
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return null;
    }
}

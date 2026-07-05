package dev.lucaargolo.nexo;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

public class ClientFabricNexoPlatformHelper extends FabricNexoPlatformHelper {

    public ClientFabricNexoPlatformHelper(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public RegistryAccess getRegistryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.isSameThread()) {
            Level level = minecraft.level;
            if (level != null) {
                return level.registryAccess();
            }else{
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return super.getRegistryAccess();
    }
}

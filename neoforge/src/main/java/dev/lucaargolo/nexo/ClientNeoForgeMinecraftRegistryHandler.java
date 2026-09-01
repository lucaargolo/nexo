package dev.lucaargolo.nexo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class ClientNeoForgeMinecraftRegistryHandler extends NeoForgeMinecraftRegistryHandler {

    public ClientNeoForgeMinecraftRegistryHandler(NeoForgeNexoMinecraft nexo) {
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

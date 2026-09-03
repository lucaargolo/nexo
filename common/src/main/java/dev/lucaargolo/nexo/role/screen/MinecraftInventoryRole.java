package dev.lucaargolo.nexo.role.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.screen.InventoryRole;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MinecraftInventoryRole {

    public static MinecraftRoleType.Info<Screen, MinecraftScreen.ScreenParameters<?>> craftScreen(NexoMinecraft<?, ?, ?, ?> nexo, ScreenBase<?> base) {
        if (base.role() instanceof InventoryRole(@NotNull Map<Location, InventoryRole.Config> vaults)) {
            Utils.Extender<Screen> extender = Utils.extend(nexo, AbstractContainerScreen.class);
            extender.override(Utils.At.REPLACE, "renderBg", void.class, GuiGraphics.class, float.class, int.class, int.class, (screen, graphics, partial, mouseX, mouseY) -> {
                return null;
            });
            return new MinecraftRoleType.Info<>(extender, parameters -> extender.instantiate(parameters.menu(), parameters.inventory(), parameters.title()));
        }
        return null;
    }

    public static MinecraftRoleType.Info<AbstractContainerMenu, MinecraftScreen.MenuParameters<?>> craftMenu(NexoMinecraft<?, ?, ?, ?> nexo, ScreenBase<?> base) {
        if (base.role() instanceof InventoryRole(@NotNull Map<Location, InventoryRole.Config> vaults)) {
            Utils.Extender<AbstractContainerMenu> extender = Utils.extend(nexo, AbstractContainerMenu.class);
            return new MinecraftRoleType.Info<>(extender, parameters -> extender.instantiate(parameters.pType(), parameters.id()));
        }
        return null;
    }


    public static InventoryRole uncraftScreen(NexoMinecraft<?, ?, ?, ?> nexo, MinecraftScreen.ScreenCrafter screen) {
        //TODO
        return null;
    }

    public static InventoryRole uncraftMenu(NexoMinecraft<?, ?, ?, ?> nexo, MinecraftScreen.MenuCrafter<?> menu) {
        //TODO
        return null;
    }

}

package dev.lucaargolo.nexo.role.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.screen.InventoryRole;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.unit.MinecraftVaultContainer;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.List;

public class MinecraftInventoryRole {

    public static MinecraftRoleType.Info<Screen, MinecraftScreen.ScreenParameters<?>> craftScreen(NexoMinecraft nexo, ScreenBase<?> base) {
        if (base.role() instanceof InventoryRole(@NotNull List<InventoryRole.Config> configs)) {
            Utils.Extender<Screen> extender = Utils.extend(nexo, AbstractContainerScreen.class);
            extender.override("renderBg", void.class, GuiGraphics.class, float.class, int.class, int.class, (screen, superCall, graphics, partial, mouseX, mouseY) -> {
                return null;
            });
            return new MinecraftRoleType.Info<>(extender, parameters -> extender.instantiate(parameters.menu(), parameters.inventory(), parameters.title()));
        }
        return null;
    }

    public static MinecraftRoleType.Info<MinecraftScreen.ExtendedMenu<?>, MinecraftScreen.MenuParameters<?>> craftMenu(NexoMinecraft nexo, ScreenBase<?> base) {
        if (base.role() instanceof InventoryRole(@NotNull List<InventoryRole.Config> configs)) {
            Utils.Extender<MinecraftScreen.ExtendedMenu<?>> extender = Utils.extend(nexo, Nexo.type(MinecraftScreen.ExtendedMenu.class));
            extender.initialize(menu -> {
                addSlots(nexo, menu, configs);
                return null;
            });
            return new MinecraftRoleType.Info<>(extender, extender::instantiate);
        }
        return null;
    }

    private static void addSlots(@NotNull NexoMinecraft nexo, @NotNull MinecraftScreen.ExtendedMenu<?> menu, @NotNull List<InventoryRole.Config> configs) {
        for (InventoryRole.Config config : configs) {
            @Nullable Vault<ItemUnit> vault = vault(nexo, menu, config);
            if (!(vault instanceof Vault.Slotted<ItemUnit> slotted)) {
                continue;
            }
            Container container = new MinecraftVaultContainer(nexo, slotted);
            int slotCount = container.getContainerSize();
            for (int index = 0; index < slotCount; index++) {
                Vector2i position = config.position(index, slotCount);
                menu.addSlot(new Slot(container, index, position.x(), position.y()));
            }
        }
    }

    private static @Nullable Vault<ItemUnit> vault(@NotNull NexoMinecraft nexo, @NotNull MinecraftScreen.ExtendedMenu<?> menu, @NotNull InventoryRole.Config config) {
        Unit<?> target = switch (config.target()) {
            case ENTITY -> nexo.entityToUnit(menu.inventory().player);
            case OWNER -> menu.owner();
        };
        return target == null ? null : target.vault(ItemUnit.class, config.key());
    }

    public static InventoryRole uncraftScreen(NexoMinecraft nexo, MinecraftScreen.ScreenCrafter<?> screen) {
        //TODO
        return null;
    }

    public static InventoryRole uncraftMenu(NexoMinecraft nexo, MinecraftScreen.MenuCrafter<?> menu) {
        //TODO
        return null;
    }

}

package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricMinecraftScreenUnit<D> extends MinecraftScreenUnit<D> {

    public FabricMinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> feature, @Nullable Role role, @NotNull MinecraftScreen.ScreenCrafter<D> crafter) {
        super(nexo, feature, role, crafter);
    }

    public FabricMinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> feature, @Nullable Role role, @NotNull Screen screen) {
        super(nexo, feature, role, screen);
    }

    @Override
    public boolean open(@NotNull EntityUnit entity, @NotNull D data, @Nullable Unit<?> owner) {
        boolean isDynamic = MinecraftScreen.isDynamicScreen(feature);
        if(isDynamic) {
            if(entity.side().isServer()) {
                if(entity instanceof MinecraftEntityUnit<?, ?> minecraftEntity && minecraftEntity.get() instanceof ServerPlayer player) {
                    Class<MinecraftScreen.ExtendedMenuType<D>> type = Nexo.type(MinecraftScreen.ExtendedMenuType.class);
                    MinecraftScreen.ExtendedMenuType<D> menuType = type.cast(MinecraftScreen.MENU_HOLDER_MAP.get(feature.location()).value());
                    return player.openMenu(new ExtendedScreenHandlerFactory<D>() {
                        @Override
                        public @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                            return menuType.craftMenu(i, inventory, data, owner);
                        }

                        @Override
                        public @NotNull Component getDisplayName() {
                            //TODO
                            return Component.empty();
                        }

                        @Override
                        public D getScreenOpeningData(ServerPlayer player) {
                            return data;
                        }
                    }).isPresent();
                }else{
                    throw new IllegalArgumentException("Minecraft dynamic screens can only be opened by minecraft server players");
                }
            }else{
                //TODO: Send packet to server to open screen
                return false;
            }
        }else{
            return super.open(entity, data, owner);
        }
    }

}

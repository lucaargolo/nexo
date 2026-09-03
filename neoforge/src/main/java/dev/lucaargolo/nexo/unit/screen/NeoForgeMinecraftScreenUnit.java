package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftScreenUnit<D> extends MinecraftScreenUnit<D> {

    public NeoForgeMinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> feature, @Nullable Role role, @NotNull MinecraftScreen.ScreenCrafter crafter) {
        super(nexo, feature, role, crafter);
    }

    public NeoForgeMinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> feature, @Nullable Role role, @NotNull MinecraftScreen.ScreenCrafter crafter, @NotNull Screen screen) {
        super(nexo, feature, role, crafter, screen);
    }

    @Override
    public boolean open(@NotNull EntityUnit entity, @NotNull D data) {
        boolean isDynamic = MinecraftScreen.isDynamicScreen(feature);
        if(isDynamic) {
            if(entity.side().isServer()) {
                if(entity instanceof MinecraftEntityUnit<?, ?> minecraftEntity && minecraftEntity.get() instanceof ServerPlayer player) {
                    return player.openMenu(new MenuProvider() {
                        @Override
                        public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
                            return MinecraftScreen.CONVERT_MENU.forward(feature).value().create(i, inventory);
                        }

                        @Override
                        public @NotNull Component getDisplayName() {
                            //TODO
                            return Component.empty();
                        }
                    }, buf -> {
                        NexoMinecraft.packetCodec(feature.data()).encode(buf, data);
                    }).isPresent();
                }else{
                    throw new IllegalArgumentException("Minecraft dynamic screens can only be opened by minecraft server players");
                }
            }else{
                //TODO: Send packet to server to open screen
                return false;
            }
        }else{
            return super.open(entity, data);
        }
    }

}

package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftScreenUnit<C extends Role> extends MinecraftScreenUnit<C> {

    public NeoForgeMinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @Nullable C role, @NotNull Screen screen) {
        super(nexo, feature, role, screen);
    }

}

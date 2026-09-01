package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public abstract class MinecraftScreenUnit<C extends Role> extends ScreenUnit<C> implements MinecraftUnit<Screen> {

    private final @NotNull Screen screen;
    private final @NotNull Vector2f mouse = new Vector2f();

    public MinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @Nullable C role, @NotNull Screen screen) {
        super(nexo, feature, role);
        this.screen = screen;
    }

    @Override
    public @NotNull Screen get() {
        return screen;
    }

    @Override
    public @NotNull Vector2f mouse() {
        return mouse;
    }

    @Override
    public int width() {
        return screen.width;
    }

    @Override
    public int height() {
        return screen.height;
    }

    @Override
    public void open() {
        Minecraft.getInstance().setScreen(screen);
    }

    @Override
    public void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == screen) {
            minecraft.setScreen(null);
        }
    }



}

package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public final class MinecraftScreenUnit<C extends Role> extends ScreenUnit<C> implements MinecraftUnit<MinecraftScreen> {

    private final @NotNull MinecraftScreen view;
    private final @NotNull Vector2f mouse = new Vector2f();

    public MinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @Nullable C role) {
        super(nexo, feature, role);
        this.view = new MinecraftScreen(nexo, feature, this);
    }

    @Override
    public @NotNull Vector2f mouse() {
        return new Vector2f(mouse);
    }

    @Override
    public int width() {
        return view.width;
    }

    @Override
    public int height() {
        return view.height;
    }

    @Override
    public void open() {
        Minecraft.getInstance().setScreen(view);
    }

    @Override
    public void close() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public @NotNull MinecraftScreen get() {
        return view;
    }

    public void setMouse(float x, float y) {
        mouse.set(x, y);
    }
}

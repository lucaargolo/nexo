package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.render.font.MinecraftText;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public abstract class MinecraftScreenUnit<C extends Role, D> extends ScreenUnit<C, D> implements MinecraftUnit<MinecraftScreen.ScreenCrafter> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull MinecraftScreen.ScreenCrafter crafter;

    private final @NotNull Vector2f mouse = new Vector2f();
    private @Nullable Screen screen;

    public MinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> feature, @Nullable C role, @NotNull MinecraftScreen.ScreenCrafter crafter) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.crafter = crafter;
        this.screen = null;
    }

    public MinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> feature, @Nullable C role, @NotNull MinecraftScreen.ScreenCrafter crafter, @NotNull Screen screen) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.crafter = crafter;
        this.screen = screen;
    }

    @Override
    public @NotNull MinecraftScreen.ScreenCrafter get() {
        return crafter;
    }

    @Override
    public @NotNull Vector2f mouse() {
        return mouse;
    }

    @Override
    public int width() {
        return this.screen != null ? this.screen.width : 0;
    }

    @Override
    public int height() {
        return this.screen != null ? this.screen.height : 0;
    }

    @Override
    public boolean open(@NotNull EntityUnit<?> entity, @NotNull D data) {
        if(entity.side().isClient()) {
            this.screen = crafter.craft(new MinecraftScreen.ScreenParameters<>(null, null, Component.translatable(feature.languageKey())));
            Minecraft.getInstance().setScreen(this.screen);
            return true;
        }else{
            //TODO: Send packet to client to open screen
            return false;
        }
    }

}

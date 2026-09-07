package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public abstract class MinecraftScreenUnit<D> extends ScreenUnit<D> implements MinecraftUnit<MinecraftScreen.ScreenCrafter<D>> {

    private final @Nullable MinecraftScreen.ScreenCrafter<D> crafter;

    private final @NotNull Vector2f mouse = new Vector2f();
    private double previousMouseX = Double.NaN;
    private double previousMouseY = Double.NaN;
    private @Nullable Screen screen;

    public MinecraftScreenUnit(@NotNull NexoMinecraft nexo, @NotNull ScreenBase<D> feature, @Nullable Role role, @NotNull MinecraftScreen.ScreenCrafter<D> crafter) {
        super(nexo, feature, role);
        this.crafter = crafter;
        this.screen = null;
    }

    public MinecraftScreenUnit(@NotNull NexoMinecraft nexo, @NotNull ScreenBase<D> feature, @Nullable Role role, @NotNull Screen screen) {
        super(nexo, feature, role);
        this.crafter = null;
        this.screen = screen;
    }

    @Override
    public @Nullable MinecraftScreen.ScreenCrafter<D> get() {
        return crafter;
    }

    @Override
    public @NotNull Vector2f mouse() {
        return mouse;
    }

    @Override
    public void build() {
        previousMouseX = Double.NaN;
        previousMouseY = Double.NaN;
        super.build();
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
    public boolean open(@NotNull EntityUnit entity, @NotNull D data, @Nullable Unit<?> owner) {
        if(entity.side().isClient()) {
            if(this.screen == null) {
                if(this.crafter == null) {
                    throw new IllegalStateException("Screen and ScreenCrafter are both null");
                }
                this.screen = crafter.craft(new MinecraftScreen.ScreenParameters<>(null, null, Component.translatable(feature.languageKey()), data));
            }
            Minecraft.getInstance().setScreen(this.screen);
            return true;
        }else{
            //TODO: Send packet to client to open screen
            return false;
        }
    }

    public void handleMouseMoved(double mouseX, double mouseY) {
        double deltaX = Double.isNaN(previousMouseX) ? 0.0 : mouseX - previousMouseX;
        double deltaY = Double.isNaN(previousMouseY) ? 0.0 : mouseY - previousMouseY;
        this.handleMouseDragged(mouseX, mouseY, deltaX, deltaY);
    }

    public boolean handleMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.previousMouseX = mouseX;
        this.previousMouseY = mouseY;
        this.mouse.set((float) mouseX, (float) mouseY);
        boolean handledX = deltaX != 0.0 && this.inputMove(Input.Axis.MOUSE_X, (float) deltaX);
        boolean handledY = deltaY != 0.0 && this.inputMove(Input.Axis.MOUSE_Y, (float) deltaY);
        return handledX || handledY;
    }

}

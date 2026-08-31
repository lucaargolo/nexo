package dev.lucaargolo.nexo.unit.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.screen.ServerScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public final class MinecraftScreenUnit<C extends Role> extends ScreenUnit<C> implements MinecraftUnit<MinecraftScreen> {

    private final @Nullable MinecraftScreen view;
    private final @NotNull Vector2f mouse = new Vector2f();

    public MinecraftScreenUnit(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @Nullable C role) {
        super(nexo, feature, role);
        this.view = feature instanceof ServerScreenBase<?> ? null : MinecraftScreen.create(nexo, feature, this);
    }

    public MinecraftScreenUnit(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull ScreenBase feature,
            @Nullable C role,
            @NotNull AbstractContainerMenu menu
    ) {
        super(nexo, feature, role);
        this.view = MinecraftScreen.create(nexo, feature, this, menu);
    }

    @Override
    public @NotNull Vector2f mouse() {
        return new Vector2f(mouse);
    }

    @Override
    public int width() {
        return view().screen().width;
    }

    @Override
    public int height() {
        return view().screen().height;
    }

    @Override
    public void open() {
        if (feature instanceof ServerScreenBase<?> serverScreen
                && serverScreen.owner() instanceof dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit<?, ?, ?> entityUnit
                && entityUnit.get() instanceof ServerPlayer player) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> MinecraftScreen.menu(feature).create(containerId, inventory),
                    Component.translatable(feature.languageKey())
            ));
            return;
        }
        Minecraft.getInstance().setScreen(view().screen());
    }

    @Override
    public void close() {
        if (feature instanceof ServerScreenBase<?> serverScreen
                && serverScreen.owner() instanceof dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit<?, ?, ?> entityUnit
                && entityUnit.get() instanceof ServerPlayer player) {
            player.closeContainer();
            return;
        }
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public @NotNull MinecraftScreen get() {
        return view();
    }

    private @NotNull MinecraftScreen view() {
        if (view == null) {
            throw new IllegalStateException("Server screen units do not have a client view until their menu opens");
        }
        return view;
    }

    public void setMouse(float x, float y) {
        mouse.set(x, y);
    }
}

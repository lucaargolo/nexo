package dev.lucaargolo.nexo.api.unit.screen;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public abstract class ScreenUnit<C extends Role> extends Unit<ScreenBase, C> {

    protected ScreenUnit(@NotNull Nexo nexo, @NotNull ScreenBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }


    public abstract @NotNull Vector2f mouse();


    public abstract int width();


    public abstract int height();


    public abstract void open();


    public abstract void close();
}

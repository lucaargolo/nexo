package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SimpleScreen extends ScreenBase {

    public SimpleScreen(@NotNull Location location) {
        super(location);
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return Map.of();
    }

}

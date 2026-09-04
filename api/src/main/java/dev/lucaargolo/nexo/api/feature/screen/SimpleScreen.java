package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.data.TextData;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SimpleScreen extends ScreenBase<Text> {

    public SimpleScreen() {
        super(TextData.TEXT);
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return Map.of();
    }

}

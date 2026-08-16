package dev.lucaargolo.nexo.api.render.font.loader;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.font.Font;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OtfFontLoader implements FontLoader {

    @Override
    public boolean supports(@NotNull Location path) {
        return path.path().endsWith(".otf");
    }

    @Override
    public @NotNull List<@NotNull String> extensions() {
        return List.of(".otf");
    }

    @Override
    public @NotNull Font load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) throws Exception {
        return SfntFontParser.parse(data, false);
    }

}

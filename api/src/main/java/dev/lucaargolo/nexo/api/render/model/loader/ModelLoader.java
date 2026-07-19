package dev.lucaargolo.nexo.api.render.model.loader;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
public interface ModelLoader {

    boolean supports(@NotNull Location path);

    @NotNull Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) throws Exception;
}

package dev.lucaargolo.nexo.api.instance;

import dev.lucaargolo.nexo.api.util.Side;
import org.jetbrains.annotations.NotNull;

public interface SideProvider {

    @NotNull Side side();

}

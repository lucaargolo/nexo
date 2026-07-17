package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import org.jetbrains.annotations.Nullable;

public interface RendererProvider<U> {

    @Nullable Renderer<Graphics3D, U> renderer();
}

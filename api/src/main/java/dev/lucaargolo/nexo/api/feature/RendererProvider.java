package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Renderer;
import org.jetbrains.annotations.Nullable;

public interface RendererProvider<G extends Graphics2D, U> {

    @Nullable Renderer<G, U> renderer();
}

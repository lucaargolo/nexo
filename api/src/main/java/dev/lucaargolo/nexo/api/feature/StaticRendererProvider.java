package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import org.jetbrains.annotations.Nullable;

public interface StaticRendererProvider<G extends Graphics2D, U> extends RendererProvider<G, U> {

    @Override
    @Nullable StaticRenderer<G, U> renderer();
}

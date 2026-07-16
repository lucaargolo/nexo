package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import org.jetbrains.annotations.Nullable;

public interface StaticRendererProvider<U> {

    @Nullable StaticRenderer<Graphics3D, U> renderer();

}

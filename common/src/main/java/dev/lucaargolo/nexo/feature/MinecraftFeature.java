package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.util.NexoHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MinecraftFeature<F extends Feature<F>, R> {

    @NotNull NexoMinecraft nexo();

    @NotNull NexoHolder<R, ? extends R> holder();

    @Nullable F delegate();

}

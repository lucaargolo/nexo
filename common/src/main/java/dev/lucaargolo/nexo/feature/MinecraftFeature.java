package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MinecraftFeature<F extends Feature<F>, T> {

    @NotNull NexoMinecraft nexo();

    @NotNull Holder<T> holder();

    @Nullable F delegate();

}

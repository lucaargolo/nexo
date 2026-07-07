package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.api.feature.Feature;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface MinecraftFeature<F extends Feature<F>, T> {

    @NotNull Holder<T> holder();

    @Nullable F delegate();

}

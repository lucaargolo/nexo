package dev.lucaargolo.nexo.api.feature.provider;

import dev.lucaargolo.nexo.api.model.Model;
import org.jetbrains.annotations.Nullable;

public interface ModelProvider {

    @Nullable Model model();

}

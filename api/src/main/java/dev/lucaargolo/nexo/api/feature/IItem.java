package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.model.Model;
import org.jetbrains.annotations.Nullable;

public interface IItem extends IFeature {

    @Nullable Model model();

}

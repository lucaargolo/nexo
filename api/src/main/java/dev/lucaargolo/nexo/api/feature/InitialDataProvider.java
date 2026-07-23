package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface InitialDataProvider {

    @NotNull default List<@NotNull DataBase<?>> data() {
        return List.of();
    }

}

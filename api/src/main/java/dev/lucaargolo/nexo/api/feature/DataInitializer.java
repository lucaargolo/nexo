package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DataInitializer {

    @NotNull default List<@NotNull DataBase<?>> initialData() {
        return List.of();
    }

}

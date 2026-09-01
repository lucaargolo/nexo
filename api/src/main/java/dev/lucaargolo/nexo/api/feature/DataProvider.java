package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface DataProvider<P> {

    @NotNull List<@NotNull DataBase<?>> data();

    <D> @Nullable D getData(@NotNull DataBase<D> data);

    <D> @NotNull P setData(@NotNull DataBase<D> data, @Nullable D d);

    default @NotNull <D> P withData(@NotNull DataBase<D> data, @NotNull Function<D, D> function) {
        D d = this.getData(data);
        if (d == null) {
            d = data.initial();
        }
        return this.setData(data, function.apply(d));
    }}

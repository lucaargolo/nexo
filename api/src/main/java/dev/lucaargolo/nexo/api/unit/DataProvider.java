package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface DataProvider {

    <D> @Nullable D getData(@NotNull DataBase<D> data);

    <D> void setData(@NotNull DataBase<D> data, @Nullable D d);

    default <D> void setInitialData(@NotNull DataBase<D> data) {
        this.setData(data, data.initial());
    }

    default @NotNull <D, U extends Unit<?, ?>> U withData(@NotNull DataBase<D> data, @NotNull Function<D, D> function) {
        D d = this.getData(data);
        if (d == null) {
            d = data.initial();
        }
        this.setData(data, function.apply(d));
        Class<U> clazz = Nexo.type(this.getClass());
        return clazz.cast(this);
    }

}

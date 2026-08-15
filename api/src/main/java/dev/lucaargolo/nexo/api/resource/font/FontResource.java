package dev.lucaargolo.nexo.api.resource.font;

import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class FontResource<T extends FontResource<T>> extends Resource<T> {

    private final @NotNull Supplier<byte @Nullable []> supplier;
    protected byte @Nullable [] data;

    public FontResource(@NotNull Location location, @NotNull Supplier<byte @Nullable []> supplier) {
        super(location);
        this.supplier = supplier;
    }

    public byte @Nullable [] data() {
        if (data == null) {
            data = supplier.get();
        }
        return data;
    }

    @Override
    public boolean resolved() {
        return data != null;
    }

    public static abstract class TTF extends FontResource<TTF> {

        public TTF(@NotNull Location location, @NotNull Supplier<byte @Nullable []> supplier) {
            super(location, supplier);
        }

        @Override
        public final @NotNull Type<TTF> type() {
            return Type.FONT_TTF;
        }

    }

}

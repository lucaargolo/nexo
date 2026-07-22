package dev.lucaargolo.nexo.api.resource.image;

import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class ImageResource<T extends ImageResource<T>> extends Resource<T> {

    private final @NotNull Supplier<byte[]> supplier;
    protected byte @Nullable [] image;

    public ImageResource(@NotNull Location location, @NotNull Supplier<byte[]> supplier) {
        super(location);
        this.supplier = supplier;
    }

    public byte @NotNull [] data() {
        if (image == null) {
            image = Objects.requireNonNull(supplier.get());
        }
        return image;
    }

    public static abstract class PNG extends ImageResource<PNG> {

        public PNG(@NotNull Location location, @NotNull Supplier<byte[]> supplier) {
            super(location, supplier);
        }

        @Override
        public @NotNull Type<PNG> type() {
            return Type.PNG_IMAGE;
        }

    }

}

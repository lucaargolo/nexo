package dev.lucaargolo.nexo.api.resource.image;

import dev.lucaargolo.nexo.api.render.image.Image;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class ImageResource extends Resource<ImageResource> {

    private final @NotNull Supplier<Image> supplier;
    protected @Nullable Image image;

    public ImageResource(@NotNull Location location, @NotNull Supplier<Image> supplier) {
        super(location);
        this.supplier = supplier;
    }

    @Override
    public final @NotNull Type<ImageResource> type() {
        return Type.IMAGE;
    }

    @Override
    public byte @NotNull [] data() {
        return image().data();
    }

    public @NotNull Image image() {
        if (image == null) {
            image = Objects.requireNonNull(supplier.get());
        }
        return image;
    }

}

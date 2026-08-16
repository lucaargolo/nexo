package dev.lucaargolo.nexo.api.resource.font;

import dev.lucaargolo.nexo.api.render.font.Font;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class FontResource extends Resource<FontResource> {

    private final @NotNull Supplier<Font> supplier;
    protected @Nullable Font font;

    public FontResource(@NotNull Location location, @NotNull Supplier<Font> supplier) {
        super(location);
        this.supplier = supplier;
    }

    @Override
    public final @NotNull Type<FontResource> type() {
        return Type.FONT;
    }

    @Override
    public byte @Nullable [] data() {
        return null;
    }

    public @NotNull Font font() {
        if (font == null) {
            font = Objects.requireNonNull(supplier.get());
        }
        return font;
    }

    @Override
    public boolean resolved() {
        return font != null;
    }

}

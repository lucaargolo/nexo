package dev.lucaargolo.nexo.api.resource.shader;

import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class ShaderResource extends Resource<ShaderResource> {

    private final @NotNull Supplier<String> supplier;
    protected @Nullable String source;

    public ShaderResource(@NotNull Location location, @NotNull Supplier<String> supplier) {
        super(location);
        this.supplier = supplier;
    }

    @Override
    public final @NotNull Type<ShaderResource> type() {
        return Type.SHADER;
    }

    @Override
    public byte @Nullable [] data() {
        return this.source().getBytes(StandardCharsets.UTF_8);
    }

    public final @NotNull String source() {
        if (source == null) {
            source = Objects.requireNonNull(supplier.get());
        }
        return source;
    }

}

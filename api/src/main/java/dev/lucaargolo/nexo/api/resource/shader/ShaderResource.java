package dev.lucaargolo.nexo.api.resource.shader;

import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class ShaderResource<T extends ShaderResource<T>> extends Resource<T> {

    private final @NotNull Supplier<String> supplier;
    private @Nullable String source;

    public ShaderResource(@NotNull Location location, @NotNull Supplier<String> supplier) {
        super(location);
        this.supplier = supplier;
    }

    public final @NotNull String source() {
        if (source == null) {
            source = Objects.requireNonNull(supplier.get());
        }
        return source;
    }

    public static class VSH extends ShaderResource<VSH> {

        public VSH(@NotNull Location location, @NotNull Supplier<String> supplier) {
            super(location, supplier);
        }

        @Override
        public final @NotNull Type<VSH> type() {
            return Type.VSH_SHADER;
        }

    }

    public static class FSH extends ShaderResource<FSH> {

        public FSH(@NotNull Location location, @NotNull Supplier<String> supplier) {
            super(location, supplier);
        }

        @Override
        public final @NotNull Type<FSH> type() {
            return Type.FSH_SHADER;
        }

    }

}

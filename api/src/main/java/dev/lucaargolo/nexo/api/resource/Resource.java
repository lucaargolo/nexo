package dev.lucaargolo.nexo.api.resource;

import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Resource<T extends Resource<T>> {

    @NotNull
    private final Location location;

    public Resource(@NotNull Location location) {
        this.location = location;
    }

    public abstract @NotNull Type<T> type();

    public abstract byte @Nullable [] data();

    public abstract boolean resolved();

    public final @NotNull Location location() {
        return location;
    }

    public static final class Type<T extends Resource<T>> {

        public static final @NotNull Type<ModelResource> MODEL = new Type<>(ModelResource.class);
        public static final @NotNull Type<ImageResource> IMAGE = new Type<>(ImageResource.class);
        public static final @NotNull Type<ShaderResource> SHADER = new Type<>(ShaderResource.class);
        public static final @NotNull Type<FontResource> FONT = new Type<>(FontResource.class);

        private final Class<T> type;

        private Type(Class<T> type) {
            this.type = type;
        }

        public Class<T> type() {
            return type;
        }

        public boolean isInstance(Resource<?> resource) {
            return type.isInstance(resource);
        }

        public T cast(Resource<?> resource) {
            return type.cast(resource);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type<?> that)) return false;
            return type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

    }

}

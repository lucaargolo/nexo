package dev.lucaargolo.nexo.api.resource;

import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class Resource<T extends Resource<T>> {

    @NotNull
    private final Location location;

    public Resource(@NotNull Location location) {
        this.location = location;
    }

    public abstract @NotNull Type<T> type();

    public final @NotNull Location location() {
        return location;
    }

    public static final class Type<T extends Resource<T>> {

        private static final List<Type<?>> ALL = new ArrayList<>();

        public static final @NotNull Type<ModelResource.Minecraft> MINECRAFT_MODEL = new Type<>(ModelResource.Minecraft.class);
        public static final @NotNull Type<ModelResource.GLTF> GLTF_MODEL = new Type<>(ModelResource.GLTF.class);
        public static final @NotNull Type<ModelResource.OBJ> OBJ_MODEL = new Type<>(ModelResource.OBJ.class);
        public static final @NotNull Type<ImageResource.PNG> PNG_IMAGE = new Type<>(ImageResource.PNG.class);
        public static final @NotNull Type<ShaderResource.VSH> VSH_SHADER = new Type<>(ShaderResource.VSH.class);
        public static final @NotNull Type<ShaderResource.FSH> FSH_SHADER = new Type<>(ShaderResource.FSH.class);

        private final Class<T> type;

        private Type(Class<T> type) {
            this.type = type;
            ALL.add(this);
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

        public static Iterable<Type<?>> values() {
            return ALL;
        }

    }

}

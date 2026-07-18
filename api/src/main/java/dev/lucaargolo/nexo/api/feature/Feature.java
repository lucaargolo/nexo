package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class Feature<T extends Feature<T>> {

    @NotNull
    private final Location location;
    @NotNull
    private final Supplier<Role> role;

    public Feature(@NotNull Location location) {
        this(location, () -> null);
    }

    public Feature(@NotNull Location location, @NotNull Supplier<Role> role) {
        this.location = location;
        this.role = role;
    }

    public abstract @NotNull Type<T> type();

    public final @NotNull Location location() {
        return location;
    }

    public @Nullable Role role() {
        return role.get();
    }

    public <C extends Role> boolean has(@NotNull Class<C> type) {
        return type.isInstance(role());
    }

    public @NotNull <C extends Role> C get(@NotNull Class<C> type) {
        Role role = role();
        if (type.isInstance(role)) {
            return type.cast(role);
        }
        throw new IllegalArgumentException("Feature does not have role type " + type.getName());
    }

    public @NotNull List<@NotNull Tag> tags() {
        return List.of();
    }

    public boolean is(@NotNull Tag tag) {
        return tags().contains(tag);
    }

    public record Tag(Location location) {}

    public static final class Type<T extends Feature<T>> {

        private static final List<Type<?>> ALL = new ArrayList<>();

        public static final Type<?> DATA = new Type<>(DataBase.class);
        public static final Type<BlockBase> BLOCK = new Type<>(BlockBase.class);
        public static final Type<ItemBase> ITEM = new Type<>(ItemBase.class);
        public static final Type<ItemCategoryBase> ITEM_CATEGORY = new Type<>(ItemCategoryBase.class);
        public static final Type<EntityBase> ENTITY = new Type<>(EntityBase.class);
        public static final Type<WorldBase> WORLD = new Type<>(WorldBase.class);

        private final Class<T> type;

        private Type(Class<T> type) {
            this.type = type;
            ALL.add(this);
        }

        public Class<T> type() {
            return type;
        }

        public boolean isInstance(Feature<?> feature) {
            return type.isInstance(feature);
        }

        public T cast(Feature<?> feature) {
            return type.cast(feature);
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

        @SuppressWarnings("unchecked")
        public static @NotNull Feature.Type<DataBase<?>> data() {
            return (Type<DataBase<?>>) DATA;
        }

    }

}

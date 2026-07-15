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

public abstract class Feature<T extends Feature<T>> {

    @NotNull
    private final Location location;
    @Nullable
    private final Role role;

    public Feature(@NotNull Location location) {
        this(location, null);
    }

    public Feature(@NotNull Location location, @Nullable Role role) {
        this.location = location;
        this.role = role;
    }

    public abstract @NotNull Type<T> type();

    public final @NotNull Location location() {
        return location;
    }

    public @Nullable Role role() {
        return role;
    }

    public <C extends Role> boolean has(@NotNull Class<C> type) {
        return type.isInstance(role);
    }

    public @NotNull <C extends Role> C get(@NotNull Class<C> type) {
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

    public record Type<T extends Feature<T>>(Class<T> type) {

        private static final List<Type<?>> ALL = new ArrayList<>();

        public static final Type<BlockBase> BLOCK = new Type<>(BlockBase.class);
        public static final Type<?> DATA = new Type<>(DataBase.class);
        public static final Type<EntityBase> ENTITY = new Type<>(EntityBase.class);
        public static final Type<ItemBase> ITEM = new Type<>(ItemBase.class);
        public static final Type<ItemCategoryBase> ITEM_CATEGORY = new Type<>(ItemCategoryBase.class);
        public static final Type<WorldBase> WORLD = new Type<>(WorldBase.class);

        public Type(Class<T> type) {
            this.type = type;
            ALL.add(this);
        }

        public T cast(Feature<?> feature) {
            return type.cast(feature);
        }

        public static Iterable<Type<?>> values() {
            return ALL;
        }

        @SuppressWarnings("unchecked")
        public static @NotNull <D> Feature.Type<DataBase<D>> data() {
            return (Type<DataBase<D>>) DATA;
        }

    }

}

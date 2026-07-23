package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class Feature<T extends Feature<T, U>, U extends Unit<T, ?>> {

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

    public abstract @NotNull Type<T, U> type();

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
        if (role == null) {
            throw new IllegalArgumentException("Feature " + location() + " has no role configured");
        }
        throw new IllegalArgumentException("Feature " + location() + " does not have role type " + type.getName() + ", found " + role.getClass().getName());
    }

    public @NotNull List<@NotNull Tag> tags() {
        return List.of();
    }

    public boolean is(@NotNull Tag tag) {
        return tags().contains(tag);
    }

    public record Tag(Location location) {}

    public static final class Type<T extends Feature<T, U>, U extends Unit<T, ?>> {

        private static final @NotNull List<Type<?, ?>> ALL = new ArrayList<>();

        public static final @NotNull Type<?, ?> DATA = new Type<>(DataBase.class);
        public static final @NotNull Type<BlockBase, BlockUnit<?>> BLOCK = new Type<>(BlockBase.class, Nexo.type(BlockUnit.class));
        public static final @NotNull Type<ItemBase, ItemUnit<?>> ITEM = new Type<>(ItemBase.class, Nexo.type(ItemUnit.class));
        public static final @NotNull Type<ItemCategoryBase, ItemCategoryUnit<?>> ITEM_CATEGORY = new Type<>(ItemCategoryBase.class, Nexo.type(ItemCategoryUnit.class));
        public static final @NotNull Type<EntityBase, EntityUnit<?>> ENTITY = new Type<>(EntityBase.class, Nexo.type(EntityUnit.class));
        public static final @NotNull Type<WorldBase, WorldUnit<?>> WORLD = new Type<>(WorldBase.class, Nexo.type(WorldUnit.class));
        public static final @NotNull Type<BiomeBase, Unit<BiomeBase, ?>> BIOME = new Type<>(BiomeBase.class);

        private final @NotNull Class<T> featureType;
        private final @Nullable Class<U> unitType;

        private Type(@NotNull Class<T> featureType, @Nullable Class<U> unitType) {
            this.featureType = featureType;
            this.unitType = unitType;
            ALL.add(this);
        }

        private Type(@NotNull Class<T> featureType) {
            this(featureType, null);
        }

        public boolean isInstance(Feature<?, ?> feature) {
            return featureType.isInstance(feature);
        }

        public boolean isInstance(Unit<?, ?> unit) {
            return unitType != null && unitType.isInstance(unit);
        }

        public @NotNull T cast(Feature<?, ?> feature) {
            return featureType.cast(feature);
        }

        public @NotNull U cast(Unit<?, ?> feature) {
            return Objects.requireNonNull(unitType).cast(feature);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type<?, ?> that)) return false;
            return featureType.equals(that.featureType);
        }

        @Override
        public int hashCode() {
            return featureType.hashCode();
        }

        public static @NotNull Iterable<Type<?, ?>> values() {
            return ALL;
        }

        public static @NotNull Feature.Type<DataBase<?>, Unit<DataBase<?>, ?>> data() {
            Class<Type<DataBase<?>, Unit<DataBase<?>, ?>>> clazz = Nexo.type(Type.class);
            return clazz.cast(DATA);
        }

    }

}

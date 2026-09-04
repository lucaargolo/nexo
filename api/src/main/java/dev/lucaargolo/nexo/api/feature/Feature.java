package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public abstract class Feature<T extends Feature<T, U>, U extends Unit<T>> {

    private static final @NotNull List<@NotNull Feature<?, ?>> INVALID = new CopyOnWriteArrayList<>();

    @NotNull
    private final Supplier<Role> role;

    @Nullable
    private Feature.Identity<?> identity = null;

    public Feature(@NotNull Supplier<Role> role) {
        this.role = role;
        INVALID.add(this);
    }

    public Feature() {
        this(() -> null);
    }

    public void identify(@NotNull Nexo nexo, @NotNull Feature.Identity<?> identity) {
        if(this.identity == null) {
            if(nexo.validateAuthority(identity.authority())) {
                INVALID.remove(this);
                this.identity = identity;
            }else{
                throw new IllegalStateException("Feature authority is not valid for current platform");
            }
        }else{
            throw new IllegalStateException("Feature already has an identity");
        }
    }

    public abstract @NotNull Type<T, U> type();

    public final @NotNull Location location() {
        if(identity == null) {
            throw new IllegalStateException("Feature has not been registered");
        }
        return identity.location();
    }

    public final @NotNull String languageKey() {
        return type().identifier + "." + location().namespace() + "." + location().path().replace("/", ".");
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

    public record Tag(@NotNull Location location) {}

    public static final class Type<T extends Feature<?, ?>, U extends Unit<?>> {

        private static final @NotNull List<Type<?, ?>> ALL = new ArrayList<>();

        public static final @NotNull Type<?, ?> DATA = new Type<>("data", DataBase.class);
        public static final @NotNull Type<?, ?> PACKET = new Type<>("packet", Packet.class);
        public static final @NotNull Type<BlockBase, BlockUnit> BLOCK = new Type<>("block", BlockBase.class, Nexo.type(BlockUnit.class));
        public static final @NotNull Type<ItemBase, ItemUnit> ITEM = new Type<>("item", ItemBase.class, Nexo.type(ItemUnit.class));
        public static final @NotNull Type<ItemCategoryBase, ItemCategoryUnit> ITEM_CATEGORY = new Type<>("item_category", ItemCategoryBase.class, Nexo.type(ItemCategoryUnit.class));
        public static final @NotNull Type<EntityBase, EntityUnit> ENTITY = new Type<>("entity", EntityBase.class, Nexo.type(EntityUnit.class));
        public static final @NotNull Type<WorldBase, WorldUnit> WORLD = new Type<>("world", WorldBase.class, Nexo.type(WorldUnit.class));
        public static final @NotNull Type<BiomeBase, Unit<BiomeBase>> BIOME = new Type<>("biome", BiomeBase.class);
        public static final @NotNull Type<ScreenBase<?>, ScreenUnit<?>> SCREEN = new Type<>("screen", Nexo.type(ScreenBase.class), Nexo.type(ScreenUnit.class));

        private final @NotNull String identifier;
        private final @NotNull Class<T> featureType;
        private final @Nullable Class<U> unitType;

        private Type(@NotNull String identifier, @NotNull Class<T> featureType, @Nullable Class<U> unitType) {
            this.identifier = identifier;
            this.featureType = featureType;
            this.unitType = unitType;
            ALL.add(this);
        }

        private Type(@NotNull String identifier, @NotNull Class<T> featureType) {
            this(identifier, featureType, null);
        }

        public @NotNull String identifier() {
            return identifier;
        }

        public boolean isInstance(@NotNull Feature<?, ?> feature) {
            return featureType.isInstance(feature);
        }

        // Returns false for every unit when this feature type does not support units.
        public boolean isInstance(@Nullable Unit<?> unit) {
            return unitType != null && unitType.isInstance(unit);
        }

        public @NotNull T cast(@NotNull Feature<?, ?> feature) {
            return featureType.cast(feature);
        }

        public @NotNull U cast(@NotNull Unit<?> feature) {
            if (unitType == null) {
                throw new IllegalStateException("Feature type " + this + " does not support units");
            }
            return unitType.cast(feature);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Type<?, ?> type = (Type<?, ?>) o;
            return Objects.equals(identifier, type.identifier) && Objects.equals(featureType, type.featureType) && Objects.equals(unitType, type.unitType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, featureType, unitType);
        }

        public static @NotNull Iterable<Type<?, ?>> values() {
            return ALL;
        }

        public static @NotNull Feature.Type<DataBase<?>, Unit<DataBase<?>>> data() {
            Class<Type<DataBase<?>, Unit<DataBase<?>>>> clazz = Nexo.type(Type.class);
            return clazz.cast(DATA);
        }

        public static @NotNull Feature.Type<Packet<?, ?>, Unit<Packet<?, ?>>> packet() {
            Class<Type<Packet<?, ?>, Unit<Packet<?, ?>>>> clazz = Nexo.type(Type.class);
            return clazz.cast(PACKET);
        }

        public static <D> @NotNull Feature.Type<ScreenBase<D>, ScreenUnit<D>> screen() {
            Class<Feature.Type<ScreenBase<D>, ScreenUnit<D>>> clazz = Nexo.type(Type.class);
            return clazz.cast(SCREEN);
        }

    }

    public interface Identity<T> {

        @NotNull Location location();

        @NotNull T authority();

    }

    private static void validateAll() {
        if(!INVALID.isEmpty()) {
            throw new IllegalStateException("Some features were not properly identified");
        }
    }

}

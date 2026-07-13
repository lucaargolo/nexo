package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.component.Component;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature<T extends Feature<T>> {

    @NotNull
    private final Location location;

    public Feature(@NotNull Location location) {
        this.location = location;
    }

    @NotNull
    public abstract Type<T> type();

    @NotNull
    public final Location location() {
        return location;
    }

    @NotNull
    public List<@NotNull Component> components() {
        return List.of();
    }

    public <C extends Component> boolean hasComponent(@NotNull Class<C> componentType) {
        return getComponent(componentType) != null;
    }

    @Nullable
    public <C extends Component> C getComponent(@NotNull Class<C> componentType) {
        for (Component component : components()) {
            if (componentType.isInstance(component)) {
                return componentType.cast(component);
            }
        }
        return null;
    }

    @NotNull
    public List<@NotNull Tag> tags() {
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
        public static @NotNull <T> Feature.Type<DataBase<T>> data() {
            return (Type<DataBase<T>>) DATA;
        }

    }

}

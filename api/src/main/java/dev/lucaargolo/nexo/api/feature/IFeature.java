package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.component.IComponent;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IFeature {

    @NotNull Type type();

    @NotNull Location location();

    default @NotNull List<@NotNull IComponent> components() {
        return List.of();
    }

    default <C extends IComponent> boolean hasComponent(@NotNull Class<C> componentType) {
        return getComponent(componentType) != null;
    }

    @SuppressWarnings("unchecked")
    default <C extends IComponent> @Nullable C getComponent(@NotNull Class<C> componentType) {
        for (IComponent component : components()) {
            if (componentType.isInstance(component)) {
                return (C) component;
            }
        }
        return null;
    }

    default @NotNull List<@NotNull Tag> tags() {
        return List.of();
    }

    default boolean is(@NotNull Tag tag) {
        return tags().contains(tag);
    }

    record Tag(Location location) {}

    enum Type {
        DATA,
        BLOCK,
        ITEM,
        ITEM_CATEGORY
    }

}

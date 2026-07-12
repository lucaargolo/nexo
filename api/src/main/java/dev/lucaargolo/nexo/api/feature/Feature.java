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

import java.util.List;

public abstract class Feature<T extends Feature<T>> {

    @NotNull
    private final Location location;

    public Feature(@NotNull Location location) {
        this.location = location;
    }

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

}

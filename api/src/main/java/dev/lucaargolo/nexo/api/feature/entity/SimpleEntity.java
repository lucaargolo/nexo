package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SimpleEntity extends EntityBase {

    public SimpleEntity(@NotNull Location location) {
        super(location);
    }

    public SimpleEntity(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }
}

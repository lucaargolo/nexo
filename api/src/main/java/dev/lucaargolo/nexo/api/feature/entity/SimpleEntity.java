package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleEntity extends EntityBase {

    public SimpleEntity(@NotNull Location location) {
        this(location, null);
    }

    public SimpleEntity(@NotNull Location location, @Nullable Role role) {
        super(location, role);
    }
}

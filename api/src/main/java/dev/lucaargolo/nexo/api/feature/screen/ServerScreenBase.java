package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.DataProvider;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class ServerScreenBase<P extends Unit<?, ?> & DataProvider> extends ScreenBase {

    private final @NotNull PacketReceiver owner;
    private final @NotNull P provider;

    public ServerScreenBase(@NotNull Location location, @NotNull PacketReceiver owner, @NotNull P provider) {
        super(location);
        this.owner = owner;
        this.provider = provider;
    }

    public ServerScreenBase(@NotNull Location location, @NotNull Supplier<Role> role, @NotNull PacketReceiver owner, @NotNull P provider) {
        super(location, role);
        this.owner = owner;
        this.provider = provider;
    }

    public final @NotNull PacketReceiver owner() {
        return this.owner;
    }

    public final @NotNull P provider() {
        return this.provider;
    }

}

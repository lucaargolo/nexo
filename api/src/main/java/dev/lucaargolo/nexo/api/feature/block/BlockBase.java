package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.InitialDataProvider;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.StaticRendererProvider;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.function.Supplier;

public abstract class BlockBase extends Feature<BlockBase, BlockUnit<?>> implements StaticRendererProvider<BlockUnit<?>>, ItemProvider, InitialDataProvider {

    public BlockBase(@NotNull Location location) {
        super(location);
    }

    public BlockBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<BlockBase, BlockUnit<?>> type() {
        return Type.BLOCK;
    }

    public abstract @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos);

}

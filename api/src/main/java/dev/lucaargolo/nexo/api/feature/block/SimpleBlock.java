package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.function.Supplier;

public class SimpleBlock extends BlockBase {

    private final @Nullable BlockItem item;
    private final @Nullable StaticRenderer<Graphics3D, BlockUnit<?>> renderer;

    public SimpleBlock(@NotNull Location location, @Nullable ModelResource resource, @Nullable BlockItem item) {
        super(location);
        this.item = item;
        this.renderer = resource != null ? new ModelRenderer<>(resource) : null;
    }

    public SimpleBlock(@NotNull Location location, @Nullable ModelResource resource) {
        this(location, resource, null);
    }

    @Override
    public @Nullable StaticRenderer<Graphics3D, BlockUnit<?>> renderer() {
        return this.renderer;
    }

    @Override
    public @Nullable BlockItem item() {
        return item;
    }

    @Override
    public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
        return Interaction.PASS;
    }

}

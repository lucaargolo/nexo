package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.Set;
import java.util.function.Function;

public class SimpleBlock extends BlockBase {

    private final @Nullable StaticRenderer<Graphics3D, BlockUnit> renderer;

    private boolean computedItem = false;
    private @Nullable ItemBase item = null;

    public SimpleBlock(@Nullable ModelResource resource) {
        this.renderer = resource != null ? new ModelRenderer<>(resource) : null;
    }

    @Override
    public @Nullable ItemBase item() {
        if(!this.computedItem) {
            this.computedItem = true;
            this.item = this.nexo().getFeature(Type.ITEM, this.location());
        }
        return this.item;
    }

    @Override
    public @Nullable StaticRenderer<Graphics3D, BlockUnit> renderer() {
        return this.renderer;
    }

    @Override
    public @NotNull Interaction onInteract(@NotNull BlockUnit block, @NotNull WorldUnit world, @NotNull EntityUnit entity, @NotNull Vector3i pos) {
        return Interaction.PASS;
    }

}

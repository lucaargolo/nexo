package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BlockItem extends ItemBase {

    private final @Nullable ItemCategoryBase category;
    private final @Nullable Renderer<Graphics3D, ItemUnit<?>> renderer;

    public BlockItem(
            @NotNull BlockBase block,
            @Nullable ItemCategoryBase category
    ) {
        super(block.location(), () -> new BlockItemRole(block));
        this.renderer = renderer(block.renderer());
        this.category = category;
    }

    @Override
    public @Nullable Renderer<Graphics3D, ItemUnit<?>> renderer() {
        return renderer;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        return category;
    }

    public static @Nullable Renderer<Graphics3D, ItemUnit<?>> renderer(@Nullable Renderer<Graphics3D, BlockUnit<?>> renderer) {
        return renderer != null ? new Renderer<>() {
            @Override
            public void render(@NotNull Graphics3D graphics, @NotNull ItemUnit<?> unit) {
                Role role = unit.role();
                if(role instanceof BlockItemRole(BlockBase base)) {
                    renderer.render(graphics, BlockUnit.of(base));
                }
            }

            @Override
            public @NotNull Map<String, Material<?>> materials() {
                return renderer.materials();
            }

            @Override
            public @NotNull Transform transform(@NotNull Location location) {
                return renderer.transform(location);
            }
        } : null;
    }


}

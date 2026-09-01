package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.render.*;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
        if (renderer == null) {
            return null;
        }
        if (renderer instanceof StaticRenderer<?, ?>) {
            StaticRenderer<Graphics3D, BlockUnit<?>> staticRenderer = (StaticRenderer<Graphics3D, BlockUnit<?>>) renderer;
            return new StaticBlockItemRenderer(staticRenderer);
        }
        return new BlockItemRenderer(renderer);
    }

    private static class BlockItemRenderer implements Renderer<Graphics3D, ItemUnit<?>> {

        protected final @NotNull Renderer<Graphics3D, BlockUnit<?>> delegate;

        private BlockItemRenderer(@NotNull Renderer<Graphics3D, BlockUnit<?>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void render(@NotNull ItemUnit<?> unit, @NotNull Graphics3D graphics) {
            BlockUnit<?> block = block(unit);
            if (block != null) {
                delegate.render(block, graphics);
            }
        }

        @Override
        public @NotNull Map<String, Material<?>> materials() {
            return delegate.materials();
        }

        @Override
        public @NotNull Transform transform(@NotNull Location location) {
            return delegate.transform(location);
        }

        @Override
        public boolean resolved() {
            return delegate.resolved();
        }

        @Override
        public boolean shaded() {
            return delegate.shaded();
        }

        protected static @Nullable BlockUnit<?> block(@NotNull ItemUnit<?> unit) {
            Role role = unit.role();
            return role instanceof BlockItemRole(BlockBase block) ? unit.nexo().unit(block) : null;
        }
    }

    private static final class StaticBlockItemRenderer extends BlockItemRenderer implements StaticRenderer<Graphics3D, ItemUnit<?>> {

        private final @NotNull StaticRenderer<Graphics3D, BlockUnit<?>> staticDelegate;

        private StaticBlockItemRenderer(@NotNull StaticRenderer<Graphics3D, BlockUnit<?>> delegate) {
            super(delegate);
            this.staticDelegate = delegate;
        }

        @Override
        public @NotNull List<@NotNull DrawCall<Graphics3D>> calls(@NotNull ItemUnit<?> unit) {
            BlockUnit<?> block = block(unit);
            return block != null ? staticDelegate.calls(block) : List.of();
        }
    }

}

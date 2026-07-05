package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.component.Component;
import dev.lucaargolo.nexo.api.feature.block.BaseBlock;
import dev.lucaargolo.nexo.api.feature.item.BaseItem;
import dev.lucaargolo.nexo.api.feature.item.BaseItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        BaseItemCategory category = nexo.registerFeature(new SimpleItemCategory(
                NexoTestMod.id("test")
        ));

        BaseBlock testBlock = nexo.registerFeature(new SimpleBlock(
            NexoTestMod.id("test_block"),
            Model.full(nexo, NexoTestMod.id("test_block.png"))
        ));

        nexo.registerFeature(new SimpleBlockItem(
            NexoTestMod.id("test_block"),
            Model.full(nexo, NexoTestMod.id("test_block.png")),
            category,
            testBlock
        ));

        nexo.registerFeature(new SimpleBlock(
                id("test_block_2"),
                Model.full(nexo, Location.of("minecraft", "block/yellow_wool.png"))
        ));
        nexo.registerFeature(new SimpleBlock(
            id("test_block_3"),
            Model.load(nexo, NexoTestMod.id("test_block.json"))
        ));
    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }

    private static class SimpleBlock extends BaseBlock {

        @NotNull
        private final Location location;
        @Nullable
        private final Model model;
        @Nullable
        private final SimpleBlockItem item;

        SimpleBlock(@NotNull Location location, @Nullable Model model, @Nullable SimpleBlockItem item) {
            this.location = location;
            this.model = model;
            this.item = item;
        }

        SimpleBlock(@NotNull Location location, @Nullable Model model) {
            this(location, model, null);
        }

        @Override
        public @NotNull Location location() {
            return location;
        }

        @Override
        public @Nullable Model model() {
            return model;
        }

        @Nullable
        public SimpleBlockItem item() {
            return item;
        }

    }

    private static class SimpleItem extends BaseItem {

        @NotNull
        private final Location location;
        @Nullable
        private final Model model;
        @Nullable
        private final BaseItemCategory category;

        SimpleItem(@NotNull Location location, @Nullable Model model, @Nullable BaseItemCategory category) {
            this.location = location;
            this.model = model;
            this.category = category;
        }

        SimpleItem(@NotNull Location location, @Nullable Model model) {
            this(location, model, null);
        }

        @Override
        public @NotNull Location location() {
            return location;
        }

        @Override
        public @Nullable Model model() {
            return model;
        }

        @Override
        public @Nullable BaseItemCategory category() {
            return category;
        }

    }

    private static class SimpleBlockItem extends BaseItem {

        @NotNull
        private final Location location;
        @Nullable
        private final Model model;
        @Nullable
        private final BaseItemCategory category;
        @NotNull
        private final BaseBlock block;

        SimpleBlockItem(
                @NotNull Location location,
                @Nullable Model model,
                @Nullable BaseItemCategory category,
                @NotNull BaseBlock block
        ) {
            this.location = location;
            this.model = model;
            this.category = category;
            this.block = block;
        }

        @Override
        public @NotNull Location location() {
            return location;
        }

        @Override
        public @Nullable Model model() {
            return model;
        }

        @Override
        public @Nullable BaseItemCategory category() {
            return category;
        }

        @Override
        public @NotNull List<@NotNull Component> components() {
            return List.of(new BlockItemComponent(block));
        }

    }

    private static class SimpleItemCategory extends BaseItemCategory {

        @NotNull
        private final Location location;

        SimpleItemCategory(@NotNull Location location) {
            this.location = location;
        }

        @Override
        public @NotNull Location location() {
            return location;
        }

    }

}

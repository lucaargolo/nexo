package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.feature.IItem;
import dev.lucaargolo.nexo.api.feature.IItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        IItemCategory category = nexo.registerFeature(IItemCategory.class, new SimpleItemCategory(
                NexoTestMod.id("test")
        ));
        IItem item = nexo.registerFeature(IItem.class, new SimpleItem(
            NexoTestMod.id("test_block"),
            Model.full(nexo, NexoTestMod.id("test_block.png")),
            category
        ));
        nexo.registerFeature(IBlock.class, new SimpleBlock(
            NexoTestMod.id("test_block"),
            Model.full(nexo, NexoTestMod.id("test_block.png")),
            item
        ));

        nexo.registerFeature(IBlock.class, new SimpleBlock(
                id("test_block_2"),
                Model.full(nexo, Location.of("minecraft", "block/yellow_wool.png"))
        ));
        nexo.registerFeature(IBlock.class, new SimpleBlock(
            id("test_block_3"),
            Model.load(nexo, NexoTestMod.id("test_block.json"))
        ));
    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }

    private record SimpleBlock(@NotNull Location location, @Nullable Model model, @Nullable IItem item) implements IBlock {

        public SimpleBlock(@NotNull Location location, @Nullable Model model) {
            this(location, model, null);
        }

    }

    private record SimpleItem(@NotNull Location location, @Nullable Model model, @Nullable IItemCategory category) implements IItem {

        public SimpleItem(@NotNull Location location, @Nullable Model model) {
            this(location, model, null);
        }

    }

    private record SimpleItemCategory(@NotNull Location location) implements IItemCategory {


    }


}

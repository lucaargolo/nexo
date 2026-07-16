package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.entity.SimpleEntity;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.world.SimpleWorld;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.util.Location;

import java.util.UUID;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(
                NexoTestMod.id("test")
        ));

        BlockBase testBlock = nexo.registerFeature(new SimpleBlock(
            NexoTestMod.id("test_block"),
            Model.full(nexo, NexoTestMod.id("test_block.png"))
        ));

        nexo.registerFeature(new BlockItem(
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

        nexo.registerFeature(new SimpleWorld(
                id("test")
        ));

        nexo.registerFeature(new SimpleEntity(id("test_entity")));

        nexo.registerFeature(new SimpleEntity(
                id("test_player"),
                () -> new PlayerRole(UUID.fromString("00000000-0000-0000-0000-000000000001"), "test_player")
        ));

    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }

}

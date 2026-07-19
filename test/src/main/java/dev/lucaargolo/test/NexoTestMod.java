package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.entity.SimpleEntity;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.world.SimpleWorld;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Map;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(
                NexoTestMod.id("test")
        ));

        BlockBase testBlock = nexo.registerFeature(new SimpleBlock(
            NexoTestMod.id("test_block"),
            Model.full(NexoTestMod.id("test_block.png"))
        ));
        nexo.registerFeature(new BlockItem(
            testBlock,
            category
        ));

        BlockBase testBlock2 = nexo.registerFeature(new SimpleBlock(
                id("test_block_2"),
                Model.full(Location.of("minecraft", "block/yellow_wool.png"))
        ));
        nexo.registerFeature(new BlockItem(
                testBlock2,
                category
        ));

        BlockBase testBlock3 = nexo.registerFeature(new SimpleBlock(
            id("test_block_3"),
            Model.load(nexo, NexoTestMod.id("test_block.json"))
        ));
        nexo.registerFeature(new BlockItem(
                testBlock3,
                category
        ));

        BlockBase testGltf = nexo.registerFeature(new SimpleBlock(
                id("test_gltf"),
                Model.load(nexo, id("test_model.gltf"))
        ));
        nexo.registerFeature(new BlockItem(
                testGltf,
                category
        ));

        BlockBase testObj = nexo.registerFeature(new SimpleBlock(
                id("test_obj"),
                Model.load(nexo, id("test_model.obj"))
        ));
        nexo.registerFeature(new BlockItem(
                testObj,
                category
        ));

        nexo.registerFeature(new SimpleWorld(
                id("test")
        ));
        nexo.registerFeature(new SimpleEntity(
                id("test_entity"),
                dynamicRenderer(NexoTestMod.id("test_block.png"))
        ));

//        nexo.registerFeature(new SimpleEntity(
//                id("test_player"),
//                () -> new PlayerRole(UUID.fromString("00000000-0000-0000-0000-000000000001"), "test_player")
//        ));

    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }


    private static <U extends Unit<?>> @NotNull Renderer<Graphics3D, U> dynamicRenderer(
            @NotNull Location texture
    ) {
        return new Renderer<>() {
            @Override
            public void render(@NotNull Graphics3D graphics, @NotNull U unit) {
                graphics.pushState();
                graphics.pushMatrix();
                graphics.translate(-0.5F, -0.5F, -0.5F);
                graphics.color(0.15F, 0.45F, 1.0F, 1.0F);
                graphics.cullMode(CullMode.DISABLED);
                graphics.drawCube(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                graphics.popMatrix();
                graphics.popState();

                graphics.pushState();
                graphics.pushMatrix();
                graphics.translate(-0.4F, -0.4F, -0.4F);
                graphics.scale(0.8F, 0.8F, 0.8F);
                graphics.rotate(22.5F, 0.0F, 1.0F, 0.0F);
                graphics.blendMode(BlendMode.ALPHA);
                graphics.bindTexture(texture);
                graphics.drawCube(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                graphics.popMatrix();
                graphics.popState();
            }

            @Override
            public @NotNull Map<String, Location> textures() {
                return Map.of("main", texture);
            }

            @Override
            public @NotNull Transform transform(@NotNull Location location) {
                return new Transform(
                        new Vector3f(),
                        new Vector3f(),
                        new Vector3f(1.0F, 1.0F, 1.0F)
                );
            }
        };
    }

}

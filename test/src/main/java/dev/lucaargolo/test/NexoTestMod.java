package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.data.BooleanData;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.SimpleEntity;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.world.SimpleWorld;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.shader.ShaderBuiltins;
import dev.lucaargolo.nexo.api.render.shader.ShaderSource;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;
import java.util.Map;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(
                NexoTestMod.id("test")
        ));

        ModelResource.Minecraft testModel = ModelResource.Minecraft.full(NexoTestMod.id("test_block"));
        BlockBase testBlock = nexo.registerFeature(new SimpleBlock(
            NexoTestMod.id("test_block"),
            testModel
        ));
        nexo.registerFeature(new BlockItem(
            testBlock,
            category
        ));

        ModelResource.Minecraft testModel2 = ModelResource.Minecraft.full(Location.of("minecraft", "block/yellow_wool"));
        BlockBase testBlock2 = nexo.registerFeature(new SimpleBlock(
                id("test_block_2"),
                testModel2
        ));
        nexo.registerFeature(new BlockItem(
                testBlock2,
                category
        ));

        ModelResource.Minecraft testModel3 = nexo.getResource(Resource.Type.MINECRAFT_MODEL, NexoTestMod.id("test_block"));
        BlockBase testBlock3 = nexo.registerFeature(new SimpleBlock(
            id("test_block_3"),
            testModel3
        ));
        nexo.registerFeature(new BlockItem(
                testBlock3,
                category
        ));

        ModelResource.Minecraft testModel4 = nexo.getResource(Resource.Type.MINECRAFT_MODEL, Location.of("minecraft", "block/red_wool"));
        BlockBase testBlock4 = nexo.registerFeature(new SimpleBlock(
                id("test_block_4"),
                testModel4
        ));
        nexo.registerFeature(new BlockItem(
                testBlock4,
                category
        ));

        ModelResource.GLTF testModel5 = nexo.getResource(Resource.Type.GLTF_MODEL, NexoTestMod.id("test_model"));
        BlockBase testGltf = nexo.registerFeature(new SimpleBlock(
                id("test_gltf"),
                testModel5
        ));
        nexo.registerFeature(new BlockItem(
                testGltf,
                category
        ));

        ModelResource.OBJ testModel6 = nexo.getResource(Resource.Type.OBJ_MODEL, NexoTestMod.id("test_model"));
        BlockBase testObj = nexo.registerFeature(new SimpleBlock(
                id("test_obj"),
                testModel6
        ));
        nexo.registerFeature(new BlockItem(
                testObj,
                category
        ));

        ModelResource.Minecraft testModel7 = nexo.getResource(Resource.Type.MINECRAFT_MODEL, Location.of("minecraft", "block/redstone_lamp"));
        BlockBase testState = nexo.registerFeature(new SimpleBlock(
                id("test_state"),
                testModel7
        ) {
            private static final BooleanData TEST = new BooleanData(id("test_state"), false);

            @Override
            public @NotNull List<@NotNull DataBase<?>> data() {
                return List.of(TEST);
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                world.setBlock(pos, block.withData(TEST, d -> !d));
                return Interaction.SUCCESS;
            }
        });
        nexo.registerFeature(new BlockItem(
                testState,
                category
        ));

        nexo.registerFeature(new SimpleWorld(
                id("test")
        ));

        ShaderResource.VSH vertexShader = nexo.getResource(Resource.Type.VSH_SHADER, NexoTestMod.id("blackhole"));
        ShaderResource.FSH fragmentShader = nexo.getResource(Resource.Type.FSH_SHADER, NexoTestMod.id("blackhole"));
        ShaderSource blackHoleSource = new ShaderSource(vertexShader.source(), fragmentShader.source());
        nexo.registerFeature(new SimpleEntity(
                id("test_entity"),
                blackHoleRenderer(blackHoleSource)
        ));

//        nexo.registerFeature(new SimpleEntity(
//                id("test_player"),
//                () -> new PlayerRole(UUID.fromString("00000000-0000-0000-0000-000000000001"), "test_player")
//        ));

    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }


    private static <U extends Unit<?, ?>> @NotNull Renderer<Graphics3D, U> blackHoleRenderer(ShaderSource source) {
        return new Renderer<>() {

            private Shader shader;

            @Override
            public void render(@NotNull Graphics3D graphics, @NotNull U unit) {
                if (shader == null) {
                    shader = graphics.createShader(source);
                    shader.uniform(ShaderBuiltins.CHANNEL_0, graphics.sceneTexture());
                }

                shader.uniform("iEventHorizon", 0.235F);
                shader.uniform("iDiskColor", 1.0F, 0.22F, 0.025F);

                graphics.pushState();
                graphics.pushMatrix();
                graphics.translate(0.0F, 1.8F, 0.0F);
                Vector3f camera = graphics.cameraPosition();
                if (camera.lengthSquared() > 1.0E-6F) camera.normalize();
                else camera.set(0.0F, 0.0F, 1.0F);
                shader.uniform("iCameraDirection", camera.x(), camera.y(), camera.z());
                float horizontalDistance = (float) Math.hypot(camera.x(), camera.z());
                float yaw = (float) Math.toDegrees(Math.atan2(camera.x(), camera.z()));
                float pitch = (float) -Math.toDegrees(Math.atan2(camera.y(), horizontalDistance));
                graphics.rotate(yaw, 0.0F, 1.0F, 0.0F);
                graphics.rotate(pitch, 1.0F, 0.0F, 0.0F);
                graphics.scale(4.95F, 4.95F, 4.95F);
                graphics.bindShader(shader);
                graphics.blendMode(BlendMode.ALPHA);
                graphics.depthMode(DepthMode.ENABLED);
                graphics.cullMode(CullMode.DISABLED);
                drawBlackHoleQuad(graphics);
                graphics.popMatrix();
                graphics.popState();
            }

            private void drawBlackHoleQuad(@NotNull Graphics3D graphics) {
                graphics.begin(PrimitiveType.QUADS, VertexFormat.POSITION_COLOR_TEX);
                graphics.vertex(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F);
                graphics.vertex(1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
                graphics.vertex(1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                graphics.vertex(-1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
                graphics.end();
            }

            @Override
            public @NotNull Map<String, Material<?>> materials() {
                return Map.of();
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

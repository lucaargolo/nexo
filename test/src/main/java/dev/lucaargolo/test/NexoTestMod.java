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
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.shader.ShaderBuiltins;
import dev.lucaargolo.nexo.api.render.shader.ShaderSource;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.resource.Resource;
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

        BlockBase testBlock4 = nexo.registerFeature(new SimpleBlock(
                id("test_block_4"),
                nexo.getResource(Resource.Type.MINECRAFT_MODEL, Location.of("minecraft", "block/red_wool.json"))
        ));
        nexo.registerFeature(new BlockItem(
                testBlock4,
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

        byte[] vertexShader = nexo.loadResource(id("blackhole.vsh"));
        byte[] fragmentShader = nexo.loadResource(id("blackhole.fsh"));
        assert vertexShader != null && fragmentShader != null;
        ShaderSource blackHoleSource = new ShaderSource(new String(vertexShader), new String(fragmentShader));
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


    private static <U extends Unit<?>> @NotNull Renderer<Graphics3D, U> blackHoleRenderer(ShaderSource source) {
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
            public @NotNull Map<String, Location> textures() {
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

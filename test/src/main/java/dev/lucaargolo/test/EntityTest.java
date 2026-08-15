package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.entity.SimpleEntity;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.shader.ShaderBuiltins;
import dev.lucaargolo.nexo.api.render.shader.ShaderSource;
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.DepthMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Map;

public final class EntityTest {

    private EntityTest() {
    }

    public static void register(@NotNull Nexo nexo) {
        ShaderResource.VSH vertexShader = nexo.getResource(Resource.Type.VSH_SHADER, NexoTestMod.id("blackhole"));
        ShaderResource.FSH fragmentShader = nexo.getResource(Resource.Type.FSH_SHADER, NexoTestMod.id("blackhole"));
        ShaderSource blackHoleSource = new ShaderSource(vertexShader.source(), fragmentShader.source());
        nexo.registerFeature(new SimpleEntity(
                NexoTestMod.id("test_entity"),
                blackHoleRenderer(blackHoleSource)
        ) {
            @Override
            public Ticker<EntityUnit<?>> ticker() {
                return unit -> { };
            }
        });
    }

    private static @NotNull Renderer<Graphics3D, EntityUnit<?>> blackHoleRenderer(ShaderSource source) {
        return new Renderer<>() {

            private Shader shader;

            @Override
            public void render(@NotNull Graphics3D graphics, @NotNull EntityUnit<?> unit) {
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

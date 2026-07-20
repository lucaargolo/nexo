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
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.DepthMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix2f;
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
                blackHoleRenderer()
        ));

//        nexo.registerFeature(new SimpleEntity(
//                id("test_player"),
//                () -> new PlayerRole(UUID.fromString("00000000-0000-0000-0000-000000000001"), "test_player")
//        ));

    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }


    private static <U extends Unit<?>> @NotNull Renderer<Graphics3D, U> blackHoleRenderer() {
        return new Renderer<>() {

            private static final ShaderSource SOURCE = new ShaderSource(
                    """
                    in vec3 aPosition;
                    in vec4 aColor;
                    in vec2 aTexCoord0;

                    uniform mat4 iModelView;
                    uniform mat4 iProjection;

                    out vec2 vTexCoord;
                    out vec4 vColor;

                    void main() {
                        gl_Position = iProjection * iModelView * vec4(aPosition, 1.0);
                        vTexCoord = aTexCoord0;
                        vColor = aColor;
                    }
                    """,
                    """
                    uniform float iTime;
                    uniform float iEventHorizon;
                    uniform vec3 iDiskColor;
                    uniform mat2 iWarp;
                    uniform sampler2D iChannel0;
                    uniform vec3 iResolution;

                    in vec2 vTexCoord;
                    in vec4 vColor;
                    out vec4 fragColor;

                    float hash(vec2 p) {
                        p = fract(p * vec2(123.34, 456.21));
                        p += dot(p, p + 45.32);
                        return fract(p.x * p.y);
                    }

                    void main() {
                        float tilt = sin(iTime * 0.25) * 0.12;
                        mat2 animation = mat2(cos(tilt), -sin(tilt), sin(tilt), cos(tilt));
                        vec2 p = animation * iWarp * (vTexCoord * 2.0 - 1.0);
                        float radius = length(p);
                        float angle = atan(p.y, p.x);
                        if (radius > 0.98) discard;

                        vec2 screenUv = gl_FragCoord.xy / iResolution.xy;
                        vec3 originalScene = texture(iChannel0, screenUv).rgb;

                        vec2 radial = p / max(radius, 0.0001);
                        vec2 screenPerLocal = vec2(
                            length(dFdx(screenUv)) / max(length(dFdx(p)), 0.000001),
                            length(dFdy(screenUv)) / max(length(dFdy(p)), 0.000001)
                        );
                        float gravity = 0.020 / max(radius * radius - iEventHorizon * iEventHorizon, 0.012);
                        gravity *= 1.0 - smoothstep(0.58, 0.95, radius);
                        vec2 lensedUv = clamp(screenUv + radial * gravity * screenPerLocal, vec2(0.001), vec2(0.999));
                        vec3 lensedScene = texture(iChannel0, lensedUv).rgb;
                        float lensMask = 1.0 - smoothstep(0.82, 0.98, radius);
                        vec3 color = mix(originalScene, lensedScene, lensMask);

                        float lens = 0.045 / max(abs(radius - iEventHorizon), 0.018);
                        float swirl = angle + iTime * 0.9 + lens;
                        float turbulence = hash(vec2(floor(swirl * 18.0), floor(radius * 55.0)));

                        float diskPlane = exp(-abs(p.y) * 32.0);
                        float diskBounds = smoothstep(0.88, 0.34, radius) * smoothstep(iEventHorizon + 0.035, iEventHorizon + 0.13, radius);
                        float bands = 0.52 + 0.48 * sin(swirl * 11.0 - radius * 92.0 + turbulence * 2.8);
                        float doppler = 0.58 + 0.42 * smoothstep(-1.0, 1.0, radial.x);
                        float disk = diskPlane * diskBounds * (0.35 + 0.65 * bands) * doppler;

                        float farDiskPlane = exp(-abs(abs(p.y) - 0.155) * 42.0);
                        float farDisk = farDiskPlane * smoothstep(iEventHorizon + 0.02, iEventHorizon + 0.09, radius)
                                * smoothstep(0.62, 0.30, radius) * 0.42;

                        float photonRing = exp(-pow((radius - iEventHorizon - 0.030) * 105.0, 2.0));
                        float blueLens = exp(-pow((radius - iEventHorizon - 0.072) * 48.0, 2.0));
                        float horizon = 1.0 - smoothstep(iEventHorizon - 0.008, iEventHorizon + 0.008, radius);

                        vec3 hotDisk = mix(iDiskColor * 0.28, vec3(1.0, 0.95, 0.78), bands);
                        color += hotDisk * disk * 2.6;
                        color += mix(iDiskColor, vec3(1.0, 0.82, 0.48), bands) * farDisk * 1.3;
                        color += vec3(1.0, 0.72, 0.28) * photonRing * 2.8;
                        color += lensedScene * blueLens * 0.48;
                        color = mix(color, vec3(0.0), horizon);

                        fragColor = vec4(color, 1.0) * vec4(vColor.rgb, 1.0);
                    }
                    """
            );

            private Shader shader;

            @Override
            public void render(@NotNull Graphics3D graphics, @NotNull U unit) {
                if (shader == null) {
                    shader = graphics.createShader(SOURCE);
                    shader.uniform(ShaderBuiltins.CHANNEL_0, graphics.sceneTexture());
                }

                shader.uniform("iEventHorizon", 0.235F);
                shader.uniform("iDiskColor", 1.0F, 0.22F, 0.025F);
                shader.uniform("iWarp", new Matrix2f().scale(1.0F, 1.18F));

                graphics.pushState();
                graphics.pushMatrix();
                graphics.translate(0.0F, 1.8F, 0.0F);
                Vector3f camera = graphics.cameraPosition();
                float horizontalDistance = (float) Math.hypot(camera.x(), camera.z());
                float yaw = (float) Math.toDegrees(Math.atan2(camera.x(), camera.z()));
                float pitch = (float) -Math.toDegrees(Math.atan2(camera.y(), horizontalDistance));
                graphics.rotate(yaw, 0.0F, 1.0F, 0.0F);
                graphics.rotate(pitch, 1.0F, 0.0F, 0.0F);
                graphics.scale(1.65F, 1.65F, 1.65F);
                graphics.bindShader(shader);
                graphics.blendMode(BlendMode.ALPHA);
                graphics.depthMode(DepthMode.READ_ONLY);
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

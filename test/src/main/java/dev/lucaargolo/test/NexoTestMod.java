package dev.lucaargolo.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.data.BooleanData;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.data.StringData;
import dev.lucaargolo.nexo.api.feature.entity.SimpleEntity;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.screen.SimpleScreen;
import dev.lucaargolo.nexo.api.feature.world.SimpleWorld;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.*;
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
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(
                NexoTestMod.id("test")
        ));

        BlockPositionData blockPositionData = nexo.registerFeature(new BlockPositionData());

        BlockBase air = nexo.getFeature(Feature.Type.BLOCK, Location.of("minecraft", "air"));
        assert air != null;
        BlockUnit<?> airUnit = nexo.unit(air);
        assert airUnit != null;

        Class<EntityUnit<PlayerRole>> receiverType = Nexo.type(EntityUnit.class);
        Packet<Vector3i, EntityUnit<PlayerRole>> packet = nexo.registerFeature(new Packet<>(id("break_block"), blockPositionData, receiverType) {
            @Override
            public void handle(@NotNull EntityUnit<PlayerRole> receiver) {
                WorldUnit<?> world = receiver.world();
                BlockUnit<?> block = world.getBlock(value());
                if (block != null && block.feature().location().equals(id("packet_test_block"))) {
                    world.setBlock(value(), airUnit);
                }
            }
        });

        ScreenBase testScreen = nexo.registerFeature(new SimpleScreen(
                id("test_screen")
        ) {
            @Override
            public void render(@NotNull Graphics2D graphics, @NotNull ScreenUnit<?> unit) {
                graphics.color(0.1F, 0.1F, 0.2F, 1.0F);
                graphics.fillRect(0.0F, 0.0F, unit.width(), unit.height());
                graphics.blendMode(BlendMode.ALPHA);

                graphics.pushState();
                graphics.pushMatrix();
                graphics.translate(20.0F, 15.0F);
                graphics.color(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.drawText("Nexo Screen - Shape Tests", 0.0F, 0.0F);
                graphics.drawText("Mouse: " + unit.mouse().x() + ", " + unit.mouse().y(), 0.0F, 10.0F);
                graphics.popMatrix();
                graphics.popState();

                float margin = 10.0F;
                float headerHeight = 32.0F;
                float gap = 8.0F;
                int columns = 2;
                int rows = 4;
                float designWidth = 175.0F;
                float designHeight = 95.0F;

                // Fit a 2x4 grid of designWidth/designHeight cells into the screen, shrinking if needed
                float gridWidth = unit.width() - 2.0F * margin;
                float gridHeight = unit.height() - margin - headerHeight - margin;
                float cellWidth = (gridWidth - gap * (columns - 1)) / columns;
                float cellHeight = (gridHeight - gap * (rows - 1)) / rows;
                float contentScale = Math.min(1.0F, Math.min(cellWidth / designWidth, cellHeight / designHeight));
                float startX = (unit.width() - (columns * cellWidth + gap * (columns - 1))) * 0.5F;
                float startY = margin + headerHeight;

                float[] polygonX = {32.0F, 56.0F, 56.0F, 32.0F, 8.0F, 8.0F};
                float[] polygonY = {4.0F, 18.0F, 46.0F, 60.0F, 46.0F, 18.0F};

                shapeCell(graphics, startX, startY, contentScale, "Line",
                        g -> g.drawLine(2.0F, 12.0F, 70.0F, 62.0F),
                        g -> {
                            g.color(0.5F, 0.8F, 0.4F, 1.0F);
                            g.lineWidth(3.0F);
                            g.drawLine(2.0F, 12.0F, 70.0F, 62.0F);
                            g.lineWidth(1.0F);
                        }
                );

                shapeCell(graphics, startX + cellWidth + gap, startY, contentScale, "Rect",
                        g -> g.drawRect(5.0F, 5.0F, 60.0F, 60.0F),
                        g -> {
                            g.color(0.25F, 0.65F, 0.95F, 0.75F);
                            g.fillRect(5.0F, 5.0F, 60.0F, 60.0F);
                        }
                );

                shapeCell(graphics, startX, startY + (cellHeight + gap) * 1, contentScale, "Circle",
                        g -> g.drawCircle(35.0F, 35.0F, 28.0F),
                        g -> {
                            g.color(0.35F, 0.85F, 0.45F, 0.75F);
                            g.fillCircle(35.0F, 35.0F, 28.0F);
                        }
                );

                shapeCell(graphics, startX + cellWidth + gap, startY + (cellHeight + gap) * 1, contentScale, "Ellipse",
                        g -> g.drawEllipse(5.0F, 15.0F, 60.0F, 45.0F),
                        g -> {
                            g.color(0.95F, 0.75F, 0.25F, 0.75F);
                            g.fillEllipse(5.0F, 15.0F, 60.0F, 45.0F);
                        }
                );

                shapeCell(graphics, startX, startY + (cellHeight + gap) * 2, contentScale, "RoundedRect",
                        g -> g.drawRoundedRect(5.0F, 5.0F, 60.0F, 60.0F, 12.0F),
                        g -> {
                            g.color(0.95F, 0.35F, 0.75F, 0.75F);
                            g.fillRoundedRect(5.0F, 5.0F, 60.0F, 60.0F, 12.0F);
                        }
                );

                shapeCell(graphics, startX + cellWidth + gap, startY + (cellHeight + gap) * 2, contentScale, "Polygon",
                        g -> g.drawPolygon(polygonX, polygonY),
                        g -> {
                            g.color(0.65F, 0.45F, 0.95F, 0.75F);
                            g.fillPolygon(polygonX, polygonY);
                        }
                );

                shapeCell(graphics, startX, startY + (cellHeight + gap) * 3, contentScale, "Arc",
                        g -> g.drawArc(35.0F, 35.0F, 28.0F, 30.0F, 300.0F),
                        g -> {
                            g.color(0.30F, 0.55F, 0.95F, 0.5F);
                            g.fillArc(35.0F, 35.0F, 28.0F, 0.0F, 360.0F);
                            g.color(0.95F, 0.55F, 0.25F, 0.85F);
                            g.fillArc(35.0F, 35.0F, 28.0F, 30.0F, 300.0F);
                        }
                );

                // Cursor rectangle, drawn before textures so it stays untextured
                graphics.color(0.95F, 0.35F, 0.35F, 1.0F);
                graphics.fillRoundedRect(
                        unit.mouse().x() - 4.0F,
                        unit.mouse().y() - 4.0F,
                        8.0F,
                        8.0F,
                        2.0F
                );

                // Textures last: binding a texture cannot be undone, so it would tint later shapes
                shapeCell(graphics, startX + cellWidth + gap, startY + (cellHeight + gap) * 3, contentScale, "Texture",
                        g -> {
                            g.bindTexture(Location.of("minecraft", "block/bedrock"));
                            g.drawTexture(5.0F, 5.0F, 60.0F, 60.0F);
                        },
                        g -> {
                            g.bindTexture(Location.of("minecraft", "block/bedrock"));
                            g.textureWrap(TextureWrap.CLAMP, TextureWrap.CLAMP);
                            g.drawTextureRegion(5.0F, 5.0F, 60.0F, 60.0F, 0.25F, 0.25F, 0.75F, 0.75F);
                        }
                );
            }

            private void shapeCell(
                    @NotNull Graphics2D graphics,
                    float x,
                    float y,
                    float scale,
                    @NotNull String label,
                    @NotNull Consumer<@NotNull Graphics2D> outline,
                    @NotNull Consumer<@NotNull Graphics2D> fill
            ) {
                graphics.pushState();
                graphics.pushMatrix();
                graphics.translate(x, y);
                graphics.scale(scale, scale);
                graphics.color(0.55F, 0.55F, 0.65F, 1.0F);
                graphics.drawText(label, 0.0F, 0.0F);
                graphics.translate(0.0F, 18.0F);
                graphics.color(1.0F, 1.0F, 1.0F, 1.0F);
                outline.accept(graphics);
                graphics.translate(90.0F, 0.0F);
                fill.accept(graphics);
                graphics.popMatrix();
                graphics.popState();
            }

            @Override
            public boolean onInputPressed(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
                nexo.getLogger().info("Screen input pressed: {}", input);
                return false;
            }

            @Override
            public boolean onInputReleased(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
                nexo.getLogger().info("Screen input released: {}", input);
                return false;
            }

            @Override
            public void onInputMove(@NotNull ScreenUnit<?> screen, @NotNull Input.Axis axis, float delta) {
                nexo.getLogger().info("Screen input move: {} {}", axis, delta);
            }
        });

        // TEMPORARY DEBUG: auto-open the test screen and capture screenshots while it is open (reflection: test mod has no Minecraft on its classpath)
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object client = minecraftClass.getMethod("getInstance").invoke(null);
            if (client != null) {
                executeAfterFrames(20, () -> {
                    ScreenUnit<?> debugUnit = nexo.unit(testScreen);
                    if (debugUnit != null) {
                        nexo.getLogger().info("DEBUG opening test screen {}x{}", debugUnit.width(), debugUnit.height());
                        debugUnit.open();
                    }
                });
                executeAfterFrames(40, () -> {
                    try {
                        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                        Object window = minecraftClass.getMethod("getWindow").invoke(minecraft);
                        int width = (int) window.getClass().getMethod("getScreenWidth").invoke(window);
                        int height = (int) window.getClass().getMethod("getScreenHeight").invoke(window);
                        double scale = (double) window.getClass().getMethod("getGuiScale").invoke(window);
                        int guiWidth = (int) window.getClass().getMethod("getGuiScaledWidth").invoke(window);
                        int guiHeight = (int) window.getClass().getMethod("getGuiScaledHeight").invoke(window);
                        nexo.getLogger().info("DEBUG window {}x{} guiScale {} guiSpace {}x{}", width, height, scale, guiWidth, guiHeight);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
                for (int i = 0; i < 400; i++) {
                    int index = i;
                    executeAfterFrames(60 + i * 40, () -> {
                        try {
                            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                            if (minecraft == null) return;
                            Object current = minecraftClass.getField("screen").get(minecraft);
                            if (current == null || !current.getClass().getName().equals("dev.lucaargolo.nexo.feature.screen.MinecraftScreen")) return;
                            File gameDir = (File) minecraftClass.getField("gameDirectory").get(minecraft);
                            Object renderTarget = minecraftClass.getMethod("getMainRenderTarget").invoke(minecraft);
                            Class<?> screenshotClass = Class.forName("net.minecraft.client.Screenshot");
                            Method grab = Arrays.stream(screenshotClass.getMethods())
                                    .filter(method -> method.getName().equals("grab"))
                                    .filter(method -> method.getParameterCount() == 4)
                                    .filter(method -> method.getParameterTypes()[0] == File.class)
                                    .findFirst()
                                    .orElseThrow();
                            grab.invoke(null, gameDir, "nexo_screen_debug_" + index, renderTarget, (Consumer<Object>) ignored -> { });
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        ModelResource.Minecraft packetTestModel = ModelResource.Minecraft.full(Location.of("minecraft", "block/bedrock"));
        BlockBase packetTestBlock = nexo.registerFeature(new SimpleBlock(
                id("packet_test_block"),
                packetTestModel
        ) {
            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                if (world.side().isClient()) {
                    ScreenUnit<?> unit = nexo.unit(testScreen);
                    if (unit != null) {
                        unit.open();
                    }
                    nexo.sendPacket(PacketReceiver.server(), packet);
                }
                return Interaction.SUCCESS;
            }
        });
        nexo.registerFeature(new BlockItem(packetTestBlock, category));

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

        StringData dynamicData = new StringData(id("dynamic_block_data"), "initial");
        nexo.registerFeature(dynamicData);
        BlockBase dynamicBlock = nexo.registerFeature(new BlockBase(id("dynamic_block")) {
            private final Renderer<Graphics3D, BlockUnit<?>> renderer = dynamicRenderer();

            @Override
            public Renderer<Graphics3D, BlockUnit<?>> renderer() {
                return this.renderer;
            }

            @Override
            public BlockItem item() {
                return null;
            }

            @Override
            public @NotNull List<@NotNull DataBase<?>> data() {
                return List.of(dynamicData);
            }

            @Override
            public Ticker<BlockUnit<?>> ticker() {
                return unit -> { };
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                block.withData(dynamicData, value -> value + "!");
                return Interaction.SUCCESS;
            }
        });
        nexo.registerFeature(new BlockItem(dynamicBlock, category) {
            @Override
            public Ticker<ItemUnit<?>> ticker() {
                return unit -> { };
            }
        });

        nexo.registerFeature(new SimpleWorld(
                id("test")
        ) {
            @Override
            public Ticker<WorldUnit<?>> ticker() {
                return unit -> { };
            }
        });

        ShaderResource.VSH vertexShader = nexo.getResource(Resource.Type.VSH_SHADER, NexoTestMod.id("blackhole"));
        ShaderResource.FSH fragmentShader = nexo.getResource(Resource.Type.FSH_SHADER, NexoTestMod.id("blackhole"));
        ShaderSource blackHoleSource = new ShaderSource(vertexShader.source(), fragmentShader.source());
        nexo.registerFeature(new SimpleEntity(
                id("test_entity"),
                blackHoleRenderer(blackHoleSource)
        ) {
            @Override
            public Ticker<EntityUnit<?>> ticker() {
                return unit -> { };
            }
        });

//        nexo.registerFeature(new SimpleEntity(
//                id("test_player"),
//                () -> new PlayerRole(UUID.fromString("00000000-0000-0000-0000-000000000001"), "test_player")
//        ));

    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }

    private static void executeAfterFrames(int frames, @NotNull Runnable task) {
        Runnable scheduled = task;
        for (int i = 0; i < frames; i++) {
            Runnable next = scheduled;
            scheduled = () -> {
                try {
                    Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                    Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                    minecraftClass.getMethod("execute", Runnable.class).invoke(minecraft, next);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            };
        }
        scheduled.run();
    }

    private static final class BlockPositionData extends DataBase<Vector3i> {

        @NotNull
        private final Vector3i initial;

        private BlockPositionData() {
            super(id("block_position"));
            this.initial = new Vector3i();
        }

        @Override
        public @NotNull Vector3i initial() {
            return initial;
        }

        @Override
        public @NotNull ByteBuffer write(@NotNull Vector3i value) {
            return ByteBuffer.allocate(3 * Integer.BYTES)
                    .putInt(value.x)
                    .putInt(value.y)
                    .putInt(value.z)
                    .flip();
        }

        @Override
        public @NotNull Vector3i read(@NotNull ByteBuffer buffer) {
            return new Vector3i(buffer.getInt(), buffer.getInt(), buffer.getInt());
        }

        @Override
        public @NotNull JsonElement serialize(@NotNull Vector3i value) {
            JsonArray array = new JsonArray(3);
            array.add(value.x);
            array.add(value.y);
            array.add(value.z);
            return array;
        }

        @Override
        public @NotNull Vector3i deserialize(@NotNull JsonElement element) {
            JsonArray array = element.getAsJsonArray();
            return new Vector3i(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
        }
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

    private static <U extends Unit<?, ?>> @NotNull Renderer<Graphics3D, U> dynamicRenderer() {
        return new Renderer<>() {
            @Override
            public void render(@NotNull Graphics3D graphics, @NotNull U unit) {
            }

            @Override
            public @NotNull Map<String, Material<?>> materials() {
                return Map.of();
            }

            @Override
            public @NotNull Transform transform(@NotNull Location location) {
                return new Transform(new Vector3f(), new Vector3f(), new Vector3f(1.0F));
            }
        };
    }

}

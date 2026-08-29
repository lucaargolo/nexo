package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.screen.SimpleScreen;
import dev.lucaargolo.nexo.api.feature.screen.widget.Button;
import dev.lucaargolo.nexo.api.feature.screen.widget.Label;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexLayout;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ScreenTest extends SimpleScreen {

    private static final Material<Void> UI_MATERIAL = Material.untextured()
            .withBlendMode(BlendMode.ALPHA)
            .withCullMode(CullMode.DISABLED);

    private static final float[] POLYGON_X = {32.0F, 56.0F, 56.0F, 32.0F, 8.0F, 8.0F};
    private static final float[] POLYGON_Y = {4.0F, 18.0F, 46.0F, 60.0F, 46.0F, 18.0F};
    private static final float[] TRIANGLE_X = {30.0F, 58.0F, 2.0F};
    private static final float[] TRIANGLE_Y = {2.0F, 50.0F, 50.0F};
    private static final float[] DIAMOND_X = {30.0F, 60.0F, 30.0F, 0.0F};
    private static final float[] DIAMOND_Y = {2.0F, 32.0F, 62.0F, 32.0F};
    private static final float[] PENTAGON_X = {30.0F, 4.3F, 14.1F, 45.9F, 55.7F};
    private static final float[] PENTAGON_Y = {57.0F, 38.3F, 8.1F, 8.1F, 38.3F};
    private static final float[] HEXAGON_X = {30.0F, 54.2F, 54.2F, 30.0F, 5.8F, 5.8F};
    private static final float[] HEXAGON_Y = {58.0F, 44.0F, 16.0F, 2.0F, 16.0F, 44.0F};
    private static final float[] STAR_X = {30.0F, 23.0F, 1.5F, 18.6F, 12.4F, 30.0F, 47.6F, 41.4F, 58.5F, 37.1F};
    private static final float[] STAR_Y = {60.0F, 39.7F, 39.3F, 26.3F, 5.7F, 18.0F, 5.7F, 26.3F, 39.3F, 39.7F};
    private static final float[] STAR_CORE_X = {23.0F, 18.6F, 30.0F, 41.4F, 37.1F};
    private static final float[] STAR_CORE_Y = {39.7F, 26.3F, 18.0F, 26.3F, 39.7F};
    private static final float[] HEXAGRAM_CENTER_X = {10.0F, 20.0F, 40.0F, 50.0F, 40.0F, 20.0F};
    private static final float[] HEXAGRAM_CENTER_Y = {30.0F, 12.68F, 12.68F, 30.0F, 47.32F, 47.32F};
    private static final float[] TRAPEZOID_X = {15.0F, 45.0F, 60.0F, 0.0F};
    private static final float[] TRAPEZOID_Y = {5.0F, 5.0F, 60.0F, 60.0F};
    private static final float[] ARROW_HEAD_X = {35.0F, 55.0F, 35.0F};
    private static final float[] ARROW_HEAD_Y = {5.0F, 25.0F, 45.0F};
    private static final float[] HEART_X = {30.00F, 30.02F, 30.19F, 30.61F, 31.40F, 32.62F, 34.29F, 36.38F, 38.84F, 41.55F, 44.37F, 47.15F, 49.71F, 51.91F, 53.59F, 54.64F, 55.00F, 54.64F, 53.59F, 51.91F, 49.71F, 47.15F, 44.37F, 41.55F, 38.84F, 36.38F, 34.29F, 32.62F, 31.40F, 30.61F, 30.19F, 30.02F, 30.00F, 29.98F, 29.81F, 29.39F, 28.60F, 27.38F, 25.71F, 23.62F, 21.16F, 18.45F, 15.63F, 12.85F, 10.29F, 8.09F, 6.41F, 5.36F, 5.00F, 5.36F, 6.41F, 8.09F, 10.29F, 12.85F, 15.63F, 18.45F, 21.16F, 23.62F, 25.71F, 27.38F, 28.60F, 29.39F, 29.81F, 29.98F};
    private static final float[] HEART_Y = {13.04F, 12.75F, 11.93F, 10.67F, 9.10F, 7.41F, 5.79F, 4.41F, 3.44F, 3.00F, 3.15F, 3.92F, 5.25F, 7.09F, 9.32F, 11.83F, 14.49F, 17.22F, 19.92F, 22.55F, 25.08F, 27.51F, 29.84F, 32.09F, 34.26F, 36.35F, 38.33F, 40.15F, 41.78F, 43.13F, 44.15F, 44.78F, 45.00F, 44.78F, 44.15F, 43.13F, 41.78F, 40.15F, 38.33F, 36.35F, 34.26F, 32.09F, 29.84F, 27.51F, 25.08F, 22.55F, 19.92F, 17.22F, 14.49F, 11.83F, 9.32F, 7.09F, 5.25F, 3.92F, 3.15F, 3.00F, 3.44F, 4.41F, 5.79F, 7.41F, 9.10F, 10.67F, 11.93F, 12.75F};

    private static final List<ShapeTest> SHAPE_TESTS = List.of(
            new ShapeTest("Line", 36.0F, 37.0F,
                    g -> g.drawLine(2.0F, 12.0F, 70.0F, 62.0F),
                    g -> {
                        shapeColor(g, 0.5F, 0.8F, 0.4F, 1.0F);
                        g.lineWidth(3.0F);
                        g.drawLine(2.0F, 12.0F, 70.0F, 62.0F);
                        g.lineWidth(1.0F);
                    }
            ),
            new ShapeTest("Rect", 35.0F, 35.0F,
                    g -> g.drawRect(5.0F, 5.0F, 60.0F, 60.0F),
                    g -> {
                        shapeColor(g, 0.25F, 0.65F, 0.95F, 0.75F);
                        g.fillRect(5.0F, 5.0F, 60.0F, 60.0F);
                    }
            ),
            new ShapeTest("Circle", 35.0F, 35.0F,
                    g -> g.drawCircle(35.0F, 35.0F, 28.0F),
                    g -> {
                        shapeColor(g, 0.35F, 0.85F, 0.45F, 0.75F);
                        g.fillCircle(35.0F, 35.0F, 28.0F);
                    }
            ),
            new ShapeTest("Ellipse", 35.0F, 37.5F,
                    g -> g.drawEllipse(35.0F, 37.5F, 60.0F, 45.0F),
                    g -> {
                        shapeColor(g, 0.95F, 0.75F, 0.25F, 0.75F);
                        g.fillEllipse(35.0F, 37.5F, 60.0F, 45.0F);
                    }
            ),
            new ShapeTest("RoundedRect", 35.0F, 35.0F,
                    g -> g.drawRoundedRect(5.0F, 5.0F, 60.0F, 60.0F, 12.0F),
                    g -> {
                        shapeColor(g, 0.95F, 0.35F, 0.75F, 0.75F);
                        g.fillRoundedRect(5.0F, 5.0F, 60.0F, 60.0F, 12.0F);
                    }
            ),
            new ShapeTest("Polygon", 32.0F, 32.0F,
                    g -> g.drawPolygon(POLYGON_X, POLYGON_Y),
                    g -> {
                        shapeColor(g, 0.65F, 0.45F, 0.95F, 0.75F);
                        g.fillPolygon(POLYGON_X, POLYGON_Y);
                    }
            ),
            new ShapeTest("Triangle", 30.0F, 34.0F,
                    g -> g.drawPolygon(TRIANGLE_X, TRIANGLE_Y),
                    g -> {
                        shapeColor(g, 0.95F, 0.55F, 0.2F, 0.75F);
                        g.fillPolygon(TRIANGLE_X, TRIANGLE_Y);
                    }
            ),
            new ShapeTest("Diamond", 30.0F, 32.0F,
                    g -> g.drawPolygon(DIAMOND_X, DIAMOND_Y),
                    g -> {
                        shapeColor(g, 0.2F, 0.8F, 0.8F, 0.75F);
                        g.fillPolygon(DIAMOND_X, DIAMOND_Y);
                    }
            ),
            new ShapeTest("Pentagon", 30.0F, 29.96F,
                    g -> g.drawPolygon(PENTAGON_X, PENTAGON_Y),
                    g -> {
                        shapeColor(g, 0.6F, 0.4F, 0.9F, 0.75F);
                        g.fillPolygon(PENTAGON_X, PENTAGON_Y);
                    }
            ),
            new ShapeTest("Hexagon", 30.0F, 30.0F,
                    g -> g.drawPolygon(HEXAGON_X, HEXAGON_Y),
                    g -> {
                        shapeColor(g, 0.6F, 0.85F, 0.2F, 0.75F);
                        g.fillPolygon(HEXAGON_X, HEXAGON_Y);
                    }
            ),
            new ShapeTest("Star", 30.0F, 30.0F,
                    g -> g.drawPolygon(STAR_X, STAR_Y),
                    g -> {
                        shapeColor(g, 1.0F, 0.8F, 0.15F, 0.75F);
                        fillTriangle(g, 30.0F, 60.0F, 23.0F, 39.7F, 37.1F, 39.7F, 1.5F, 5.7F, 58.5F, 60.0F);
                        fillTriangle(g, 1.5F, 39.3F, 23.0F, 39.7F, 18.6F, 26.3F, 1.5F, 5.7F, 58.5F, 60.0F);
                        fillTriangle(g, 12.4F, 5.7F, 18.6F, 26.3F, 30.0F, 18.0F, 1.5F, 5.7F, 58.5F, 60.0F);
                        fillTriangle(g, 47.6F, 5.7F, 30.0F, 18.0F, 41.4F, 26.3F, 1.5F, 5.7F, 58.5F, 60.0F);
                        fillTriangle(g, 58.5F, 39.3F, 41.4F, 26.3F, 37.1F, 39.7F, 1.5F, 5.7F, 58.5F, 60.0F);
                        fillPolygon(g, STAR_CORE_X, STAR_CORE_Y, 1.5F, 5.7F, 58.5F, 60.0F);
                    }
            ),
            new ShapeTest("Hexagram", 30.0F, 30.0F,
                    g -> g.drawPolygon(
                            new float[]{30.0F, 40.0F, 60.0F, 50.0F, 60.0F, 40.0F, 30.0F, 20.0F, 0.0F, 10.0F, 0.0F, 20.0F},
                            new float[]{-4.64F, 12.68F, 12.68F, 30.0F, 47.32F, 47.32F, 64.64F, 47.32F, 47.32F, 30.0F, 12.68F, 12.68F}
                    ),
                    g -> {
                        shapeColor(g, 0.75F, 0.45F, 0.85F, 0.75F);
                        fillTriangle(g, 30.0F, -4.64F, 20.0F, 12.68F, 40.0F, 12.68F, 0.0F, -4.64F, 60.0F, 64.64F);
                        fillTriangle(g, 0.0F, 12.68F, 10.0F, 30.0F, 20.0F, 12.68F, 0.0F, -4.64F, 60.0F, 64.64F);
                        fillTriangle(g, 60.0F, 12.68F, 40.0F, 12.68F, 50.0F, 30.0F, 0.0F, -4.64F, 60.0F, 64.64F);
                        fillTriangle(g, 0.0F, 47.32F, 20.0F, 47.32F, 10.0F, 30.0F, 0.0F, -4.64F, 60.0F, 64.64F);
                        fillTriangle(g, 60.0F, 47.32F, 50.0F, 30.0F, 40.0F, 47.32F, 0.0F, -4.64F, 60.0F, 64.64F);
                        fillTriangle(g, 30.0F, 64.64F, 20.0F, 47.32F, 40.0F, 47.32F, 0.0F, -4.64F, 60.0F, 64.64F);
                        fillPolygon(g, HEXAGRAM_CENTER_X, HEXAGRAM_CENTER_Y, 0.0F, -4.64F, 60.0F, 64.64F);
                    }
            ),
            new ShapeTest("Trapezoid", 30.0F, 35.56F,
                    g -> g.drawPolygon(TRAPEZOID_X, TRAPEZOID_Y),
                    g -> {
                        shapeColor(g, 0.25F, 0.7F, 0.85F, 0.75F);
                        g.fillPolygon(TRAPEZOID_X, TRAPEZOID_Y);
                    }
            ),
            new ShapeTest("Arrow", 28.67F, 25.0F,
                    g -> g.drawPolygon(
                            new float[]{5.0F, 35.0F, 35.0F, 55.0F, 35.0F, 35.0F, 5.0F},
                            new float[]{15.0F, 15.0F, 5.0F, 25.0F, 45.0F, 35.0F, 35.0F}
                    ),
                    g -> {
                        shapeColor(g, 0.35F, 0.75F, 0.4F, 0.75F);
                        fillRect(g, 5.0F, 15.0F, 30.0F, 20.0F, 5.0F, 5.0F, 55.0F, 45.0F);
                        fillPolygon(g, ARROW_HEAD_X, ARROW_HEAD_Y, 5.0F, 5.0F, 55.0F, 45.0F);
                    }
            ),
            new ShapeTest("Plus", 30.0F, 30.0F,
                    g -> {
                        g.drawPolygon(
                                new float[]{20.0F, 40.0F, 40.0F, 55.0F, 55.0F, 40.0F, 40.0F, 20.0F, 20.0F, 5.0F, 5.0F, 20.0F},
                                new float[]{5.0F, 5.0F, 20.0F, 20.0F, 40.0F, 40.0F, 55.0F, 55.0F, 40.0F, 40.0F, 20.0F, 20.0F}
                        );
                    },
                    g -> {
                        shapeColor(g, 0.3F, 0.5F, 0.95F, 0.75F);
                        fillRect(g, 20.0F, 20.0F, 20.0F, 20.0F, 5.0F, 5.0F, 55.0F, 55.0F);
                        fillRect(g, 20.0F, 5.0F, 20.0F, 15.0F, 5.0F, 5.0F, 55.0F, 55.0F);
                        fillRect(g, 20.0F, 40.0F, 20.0F, 15.0F, 5.0F, 5.0F, 55.0F, 55.0F);
                        fillRect(g, 5.0F, 20.0F, 15.0F, 20.0F, 5.0F, 5.0F, 55.0F, 55.0F);
                        fillRect(g, 40.0F, 20.0F, 15.0F, 20.0F, 5.0F, 5.0F, 55.0F, 55.0F);
                    }
            ),
            new ShapeTest("Heart", 30.0F, 19.13F,
                    g -> g.drawPolygon(HEART_X, HEART_Y),
                    g -> {
                        shapeColor(g, 0.9F, 0.3F, 0.4F, 0.75F);
                        g.fillPolygon(HEART_X, HEART_Y);
                    }
            ),
            new ShapeTest("Arc", 35.0F, 35.0F,
                    g -> {
                        g.drawLine(35.0F, 35.0F, 59.25F, 49.0F);
                        g.drawArc(35.0F, 35.0F, 28.0F, 30.0F, 300.0F);
                        g.drawLine(49.0F, 10.75F, 35.0F, 35.0F);
                    },
                    g -> {
                        shapeColor(g, 0.30F, 0.55F, 0.95F, 0.5F);
                        g.fillArc(35.0F, 35.0F, 28.0F, 0.0F, 360.0F);
                        shapeColor(g, 0.95F, 0.55F, 0.25F, 0.85F);
                        g.fillArc(35.0F, 35.0F, 28.0F, 30.0F, 300.0F);
                    }
            )
    );

    private static final List<TextureOption> TEXTURES = textureOptions();

    private static List<TextureOption> textureOptions() {
        List<TextureOption> options = new ArrayList<>();
        for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            options.add(new TextureOption(color + "_wool", material(Location.of("minecraft", "block/" + color + "_wool"))));
        }
        options.add(new TextureOption("bedrock", material(Location.of("minecraft", "block/bedrock"))));
        options.add(new TextureOption("orange_stained_glass", material(Location.of("minecraft", "block/orange_stained_glass"))));
        options.add(new TextureOption("Minecraft block atlas", material(Location.of("minecraft", "block/white_wool"))));
        options.add(new TextureOption("Nexo block atlas", material(NexoTestMod.id("test_block"))));
        options.add(new TextureOption("Minecraft GUI atlas", material(Location.of("minecraft", "widget/button"))));
        options.add(new TextureOption("Nexo GUI atlas", material(NexoTestMod.id("test_gui_texture.png"))));
        options.add(new TextureOption("Nexo entity atlas", material(NexoTestMod.id("test_entity_texture.png"))));
        options.add(new TextureOption("Minecraft non-atlas", material(Location.of("minecraft", "textures/gui/title/minecraft.png"))));
        options.add(new TextureOption("Nexo non-atlas", material(NexoTestMod.id("test_block_jpeg"))));
        options.add(new TextureOption("JPEG loader", material(NexoTestMod.id("test_block_jpeg"))));
        options.add(new TextureOption("WebP loader", material(NexoTestMod.id("test_block_webp"))));
        options.add(new TextureOption("Translucent", material(NexoTestMod.id("test_block_translucent"))));
        options.add(new TextureOption("None", null));
        return options;
    }

    private static @NotNull Material<Location> material(@NotNull Location location) {
        return new Material<>(
                location,
                location,
                new float[]{1.0F, 1.0F, 1.0F, 1.0F},
                CullMode.DISABLED,
                BlendMode.ALPHA
        );
    }

    private static void shapeColor(
            @NotNull Graphics2D graphics,
            float r, float g, float b, float a
    ) {
        Material<?> material = graphics.material();
        if (material == null || material.texture() == null) {
            graphics.color(r, g, b, a);
        }
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return Map.of(
                "test_gui_texture", material(NexoTestMod.id("test_gui_texture.png"))
        );
    }

    private static final int DESIGN_WIDTH = 175;
    private static final int DESIGN_HEIGHT = 120;

    private static final List<FontOption> FONTS = List.of(
            new FontOption("Default", null),
            new FontOption("Fira Mono", NexoTestMod.id("fonts/fira_mono")),
            new FontOption("Source Sans 3", NexoTestMod.id("fonts/source_sans_3_regular")),
            new FontOption("Source Serif 4", NexoTestMod.id("fonts/source_serif_4_regular")),
            new FontOption("Minecraft", Location.of("minecraft", "default")),
            new FontOption("Uniform", Location.of("minecraft", "uniform")),
            new FontOption("Alt", Location.of("minecraft", "alt")),
            new FontOption("Illageralt", Location.of("minecraft", "illageralt")),
            new FontOption("Unknown", Location.of("unknown", "unknown"))
        );

    private static final Text RICH_TEXT = Text.parse(
            "[b]Bold[/b] [i]Italic[/i] [u]Under[/u] [s]Strike[/s]\n"
                    + "[color=#55ffff]Color[/color] [size=12]Size[/size] "
                    + "[o]Obf[/o] [font=" + NexoTestMod.id("fonts/fira_mono") + "]Font[/font]"
    );

    private static final Text LOCALIZED_TEXT = Text.translatable(
            "text.nexo_test.rich",
            Text.literal("Nexo")
    );

    private int currentIndex;
    private int textureIndex;
    private int fontIndex;

    private @NotNull Label mouseLabel;
    private @NotNull Label textureLabel;
    private @NotNull Button fontButton;
    private int cellX;
    private int cellY;
    private int contentScale;

    private ScreenTest() {
        super(NexoTestMod.id("test_screen"));
    }

    @Override
    public void onBuild(@NotNull ScreenUnit<?> unit) {
        contentScale = Math.max(0, Math.min(
                1,
                Math.min((unit.width() - 40) / DESIGN_WIDTH, (unit.height() - 95) / DESIGN_HEIGHT)
        ));
        cellX = (unit.width() - DESIGN_WIDTH * contentScale) / 2;
        int headerHeight = 40;
        int blockHeight = 35 + DESIGN_HEIGHT * contentScale + 10 + 20 + 84;
        cellY = headerHeight + Math.max(0, (unit.height() - headerHeight - blockHeight) / 2);
        int cellWidth = DESIGN_WIDTH * contentScale;

        addWidget(new Label(10.0F, 10.0F, Text.translatable("screen.nexo_test.test_screen")));
        mouseLabel = new Label(10.0F, 19.0F, Text.literal(""));
        addWidget(mouseLabel);
        int buttonY = cellY + 35 + DESIGN_HEIGHT * contentScale + 10;
        addWidget(new Button(cellX, buttonY, 80.0F, 20.0F, Text.translatable("screen.nexo_test.previous"), this::previous));
        addWidget(new Button(cellX + Math.max(85, cellWidth - 80), buttonY, 80.0F, 20.0F, Text.translatable("screen.nexo_test.next"), this::next));
        int textureButtonY = buttonY + 42;
        textureLabel = new Label(cellX, textureButtonY - 14.0F, Text.literal(""));
        addWidget(textureLabel);
        addWidget(new Button(cellX, textureButtonY, 80.0F, 20.0F, Text.translatable("screen.nexo_test.previous"), this::previousTexture));
        addWidget(new Button(cellX + Math.max(85, cellWidth - 80), textureButtonY, 80.0F, 20.0F, Text.translatable("screen.nexo_test.next"), this::nextTexture));
        int fontButtonY = textureButtonY + 42;
        fontButton = new Button(cellX, fontButtonY, 130.0F, 20.0F, Text.translatable("screen.nexo_test.font", Text.literal(FONTS.get(fontIndex).name())), this::toggleFont);
        addWidget(fontButton);
        updateTextureLabel();
    }

    public static @NotNull ScreenBase register(@NotNull Nexo nexo) {
        return nexo.registerFeature(new ScreenTest());
    }

    private void previous() {
        currentIndex = Math.floorMod(currentIndex - 1, SHAPE_TESTS.size());
    }

    private void next() {
        currentIndex = Math.floorMod(currentIndex + 1, SHAPE_TESTS.size());
    }

    private void previousTexture() {
        textureIndex = Math.floorMod(textureIndex - 1, TEXTURES.size());
        updateTextureLabel();
    }

    private void nextTexture() {
        textureIndex = Math.floorMod(textureIndex + 1, TEXTURES.size());
        updateTextureLabel();
    }

    private void updateTextureLabel() {
        textureLabel.text(Text.translatable(
                "screen.nexo_test.texture",
                Text.literal(TEXTURES.get(textureIndex).name()),
                Text.literal(Integer.toString(textureIndex + 1)),
                Text.literal(Integer.toString(TEXTURES.size()))
        ));
    }

    private void toggleFont() {
        fontIndex = Math.floorMod(fontIndex + 1, FONTS.size());
        fontButton.text(Text.translatable("screen.nexo_test.font", Text.literal(FONTS.get(fontIndex).name())));
    }

    @Override
    public void render(@NotNull Graphics2D graphics, @NotNull ScreenUnit<?> unit) {
        graphics.bindMaterial(UI_MATERIAL);
        graphics.color(0.1F, 0.1F, 0.2F, 1.0F);
        graphics.fillRect(0.0F, 0.0F, unit.width(), unit.height());

        graphics.color(1F, 1F, 1F, 1.0F);
        graphics.drawText(RICH_TEXT, 10, 50);
        graphics.drawText(LOCALIZED_TEXT, 10, 70);

        graphics.color(0.95F, 0.35F, 0.35F, 1.0F);
        graphics.fillRoundedRect(
                unit.mouse().x() - 4.0F,
                unit.mouse().y() - 4.0F,
                8.0F,
                8.0F,
                2.0F
        );

        ShapeTest shapeTest = SHAPE_TESTS.get(currentIndex);
        TextureOption texture = TEXTURES.get(textureIndex);

        mouseLabel.text(Text.translatable(
                "screen.nexo_test.mouse",
                Text.literal(Float.toString(unit.mouse().x())),
                Text.literal(Float.toString(unit.mouse().y()))
        ));

        super.render(graphics, unit);

        graphics.pushState();
        graphics.pushMatrix();
        graphics.translate(cellX, cellY);
        if (texture.material() != null) {
            graphics.bindMaterial(texture.material());
        }
        graphics.color(1.0F, 1.0F, 1.0F, 1.0F);
        String name = shapeTest.name() + " (" + (currentIndex + 1) + "/" + SHAPE_TESTS.size() + ")";
        Location selectedFont = FONTS.get(fontIndex).location();
        Text text = Text.parse((selectedFont == null ? "" : "[font=" + selectedFont + "]") + name);
        float nameWidth = graphics.textWidth(text);
        graphics.drawText(text, Math.round((DESIGN_WIDTH - nameWidth) * 0.5F), 0);
        graphics.translate(0, 16);
        graphics.scale(contentScale, contentScale);
        float contentCenterX = shapeTest.centerX();
        float contentCenterY = shapeTest.centerY();
        int halfWidth = DESIGN_WIDTH / 2;
        int halfHeight = (DESIGN_HEIGHT - 16) / 2;
        graphics.pushMatrix();
        graphics.translate(Math.round(halfWidth * 0.5F - contentCenterX), Math.round(halfHeight - contentCenterY));
        shapeTest.outline().accept(graphics);
        graphics.popMatrix();
        graphics.pushMatrix();
        graphics.translate(Math.round(halfWidth * 1.5F - contentCenterX), Math.round(halfHeight - contentCenterY));
        shapeTest.fill().accept(graphics);
        graphics.popMatrix();
        graphics.popMatrix();
        graphics.popState();
    }

    @Override
    public boolean onInputPressed(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        if (input.type() == Input.Type.KEYBOARD) {
            if (input.key() == Input.Key.LEFT) {
                previous();
                return true;
            }
            if (input.key() == Input.Key.RIGHT) {
                next();
                return true;
            }
        }
        return super.onInputPressed(screen, input);
    }

    @Override
    public void onInputMove(@NotNull ScreenUnit<?> screen, @NotNull Input.Axis axis, float delta) {
        if (axis == Input.Axis.SCROLL) {
            if (delta > 0.0F) {
                previous();
            } else if (delta < 0.0F) {
                next();
            }
        }
        super.onInputMove(screen, axis, delta);
    }

    private static void fillTriangle(
            @NotNull Graphics2D graphics,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float minX, float minY,
            float maxX, float maxY
    ) {
        boolean textured = begin(graphics, PrimitiveType.TRIANGLES);
        vertex(graphics, textured, x1, y1, minX, minY, maxX, maxY);
        vertex(graphics, textured, x2, y2, minX, minY, maxX, maxY);
        vertex(graphics, textured, x3, y3, minX, minY, maxX, maxY);
        graphics.end();
    }

    private static void fillRect(
            @NotNull Graphics2D graphics,
            float x, float y,
            float width, float height,
            float minX, float minY,
            float maxX, float maxY
    ) {
        boolean textured = begin(graphics, PrimitiveType.QUADS);
        vertex(graphics, textured, x, y + height, minX, minY, maxX, maxY);
        vertex(graphics, textured, x + width, y + height, minX, minY, maxX, maxY);
        vertex(graphics, textured, x + width, y, minX, minY, maxX, maxY);
        vertex(graphics, textured, x, y, minX, minY, maxX, maxY);
        graphics.end();
    }

    private static void fillPolygon(
            @NotNull Graphics2D graphics,
            float @NotNull [] x,
            float @NotNull [] y,
            float minX, float minY,
            float maxX, float maxY
    ) {
        boolean textured = begin(graphics, PrimitiveType.TRIANGLE_FAN);
        for (int i = 0; i < x.length; i++) {
            vertex(graphics, textured, x[i], y[i], minX, minY, maxX, maxY);
        }
        graphics.end();
    }

    private static boolean begin(@NotNull Graphics2D graphics, @NotNull PrimitiveType primitive) {
        Material<?> material = graphics.material();
        boolean textured = material != null && material.texture() != null;
        graphics.begin(primitive, textured ? VertexLayout.POSITION_TEX : VertexLayout.POSITION);
        return textured;
    }

    private static void vertex(
            @NotNull Graphics2D graphics,
            boolean textured,
            float x, float y,
            float minX, float minY,
            float maxX, float maxY
    ) {
        if (textured) {
            float u = maxX > minX ? (x - minX) / (maxX - minX) : 0.0F;
            float v = maxY > minY ? (y - minY) / (maxY - minY) : 0.0F;
            graphics.vertex(x, y, 0.0F, u, v);
        } else {
            graphics.vertex(x, y, 0.0F);
        }
    }

    private record ShapeTest(
            @NotNull String name,
            float centerX,
            float centerY,
            @NotNull Consumer<@NotNull Graphics2D> outline,
            @NotNull Consumer<@NotNull Graphics2D> fill
    ) {
    }

    private record TextureOption(@NotNull String name, @Nullable Material<Location> material) {
    }

    private record FontOption(@NotNull String name, @Nullable Location location) {
    }
}

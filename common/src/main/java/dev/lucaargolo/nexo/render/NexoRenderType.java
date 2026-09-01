package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.shader.MinecraftShader;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

final class NexoRenderType extends RenderType {

    private static final AtomicLong NEXT_ID = new AtomicLong();

    private NexoRenderType(
            @NotNull Mode mode,
            boolean textured,
            @NotNull BlendMode blendMode,
            @Nullable MinecraftShader shader,
            @NotNull VertexLayout layout,
            @NotNull List<RenderStateShard> states
    ) {
        super(
                "nexo_dynamic_" + NEXT_ID.incrementAndGet(),
                minecraftFormat(textured, shader, layout),
                mode,
                SMALL_BUFFER_SIZE,
                false,
                blendMode != BlendMode.DISABLED,
                () -> states.forEach(RenderStateShard::setupRenderState),
                () -> states.forEach(RenderStateShard::clearRenderState)
        );
    }

    static @NotNull RenderType create(
            @NotNull Mode mode,
            boolean textured,
            @Nullable Location texture,
            @Nullable Material<?> material,
            @NotNull DepthMode depthMode,
            @NotNull CullMode cullMode,
            @NotNull VertexLayout layout
    ) {
        BlendMode blendMode = material == null ? BlendMode.DISABLED : material.blendMode();
        TextureFilter minFilter = material == null ? TextureFilter.NEAREST : material.minFilter();
        TextureFilter magFilter = material == null ? TextureFilter.NEAREST : material.magFilter();
        MinecraftShader shader = material == null ? null : (MinecraftShader) material.shader();
        Map<String, MinecraftShader.UniformValue> uniforms = shader == null ? Map.of() : shader.uniforms();

        List<RenderStateShard> states = new ArrayList<>();
        if (shader == null) {
            states.add(textured ? new ShaderStateShard(GameRenderer::getRendertypeTextShader) : POSITION_COLOR_SHADER);
        } else {
            states.add(new ShaderStateShard(() -> {
                ShaderInstance instance = shader.instance(minecraftFormat(textured, shader, layout));
                shader.apply(instance, uniforms);
                return instance;
            }));
        }
        states.add(transparency(blendMode));
        states.add(depthTest(depthMode));
        states.add(cullState(cullMode));
        states.add(writeMask(depthMode));
        if (textured) {
            states.add(new TextureStateShard(
                    NexoMinecraft.rl(java.util.Objects.requireNonNull(texture)),
                    blurred(minFilter, magFilter),
                    mipmapped(minFilter)
            ));
            states.add(LIGHTMAP);
        }
        return new NexoRenderType(
                mode,
                textured,
                blendMode,
                shader,
                layout,
                List.copyOf(states)
        );
    }

    static com.mojang.blaze3d.vertex.@NotNull VertexFormat customVertexFormat(@NotNull VertexLayout layout) {
        return switch (layout) {
            case POSITION, POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_TEX, POSITION_COLOR_TEX -> DefaultVertexFormat.POSITION_TEX_COLOR;
            case POSITION_TEX_NORMAL, POSITION_COLOR_TEX_NORMAL -> DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        };
    }

    private static com.mojang.blaze3d.vertex.@NotNull VertexFormat minecraftFormat(
            boolean textured,
            @Nullable MinecraftShader shader,
            @NotNull VertexLayout layout
    ) {
        if (shader == null) {
            return textured ? DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP : DefaultVertexFormat.POSITION_COLOR;
        }
        return customVertexFormat(layout);
    }

    private static boolean blurred(@NotNull TextureFilter min, @NotNull TextureFilter mag) {
        return min == TextureFilter.LINEAR
                || min == TextureFilter.LINEAR_MIPMAP_NEAREST
                || min == TextureFilter.LINEAR_MIPMAP_LINEAR
                || mag == TextureFilter.LINEAR;
    }

    private static boolean mipmapped(@NotNull TextureFilter min) {
        return min != TextureFilter.NEAREST && min != TextureFilter.LINEAR;
    }

    private static @NotNull TransparencyStateShard transparency(@NotNull BlendMode mode) {
        return switch (mode) {
            case DISABLED -> NO_TRANSPARENCY;
            case ALPHA -> TRANSLUCENT_TRANSPARENCY;
            case ADD -> ADDITIVE_TRANSPARENCY;
            case MULTIPLY -> customTransparency(mode, GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO, GL14.GL_FUNC_ADD);
            case SUBTRACT -> customTransparency(mode, GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GL14.GL_FUNC_REVERSE_SUBTRACT);
            case PREMUL_ALPHA -> customTransparency(mode, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
            case SCREEN -> customTransparency(mode, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GL14.GL_FUNC_ADD);
        };
    }

    private static @NotNull TransparencyStateShard customTransparency(
            @NotNull BlendMode mode,
            @NotNull GlStateManager.SourceFactor source,
            @NotNull GlStateManager.DestFactor destination,
            int equation
    ) {
        return new TransparencyStateShard(
                "nexo_" + mode.name().toLowerCase(),
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendEquation(equation);
                    RenderSystem.blendFunc(source, destination);
                },
                () -> {
                    RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableBlend();
                }
        );
    }

    private static @NotNull DepthTestStateShard depthTest(@NotNull DepthMode mode) {
        return switch (mode) {
            case DISABLED -> NO_DEPTH_TEST;
            case READ_ONLY, ENABLED -> LEQUAL_DEPTH_TEST;
            case REVERSED_READ_ONLY, REVERSED -> GREATER_DEPTH_TEST;
        };
    }

    private static @NotNull RenderStateShard cullState(@NotNull CullMode mode) {
        return switch (mode) {
            case DISABLED -> NO_CULL;
            case BACK -> CULL;
            case FRONT -> new RenderStateShard(
                    "nexo_front_cull",
                    () -> {
                        RenderSystem.enableCull();
                        GL11.glCullFace(GL11.GL_FRONT);
                    },
                    () -> {
                        GL11.glCullFace(GL11.GL_BACK);
                        RenderSystem.disableCull();
                    }
            ) {
            };
        };
    }

    private static @NotNull WriteMaskStateShard writeMask(@NotNull DepthMode mode) {
        return mode == DepthMode.ENABLED || mode == DepthMode.REVERSED
                ? COLOR_DEPTH_WRITE
                : COLOR_WRITE;
    }
}

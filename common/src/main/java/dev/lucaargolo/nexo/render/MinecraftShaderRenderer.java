package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;

final class MinecraftShaderRenderer {

    static final @NotNull Location SCENE_TEXTURE = Location.of(NexoMinecraft.MOD_ID, "scene_texture");

    private final @NotNull Deque<Draw> draws = new ArrayDeque<>();
    private final long startNanos = System.nanoTime();
    private @Nullable TextureTarget sceneTexture;
    private long previousFrameNanos;
    private float time;
    private float timeDelta;
    private int frame = -1;
    private boolean worldRendering;

    void beginFrame() {
        long now = System.nanoTime();
        time = (now - startNanos) / 1_000_000_000.0F;
        timeDelta = previousFrameNanos == 0L ? 0.0F : (now - previousFrameNanos) / 1_000_000_000.0F;
        previousFrameNanos = now;
        frame++;
        worldRendering = true;
    }

    float time() {
        return time;
    }

    float timeDelta() {
        return timeDelta;
    }

    int frame() {
        return frame;
    }

    boolean deferred() {
        return worldRendering;
    }

    void enqueue(
            @NotNull RenderType renderType,
            @NotNull MeshData mesh,
            @NotNull ByteBufferBuilder allocation,
            @NotNull Matrix4f modelView,
            @NotNull Matrix4f projection,
            @NotNull VertexSorting sorting
    ) {
        draws.addLast(new Draw(renderType, mesh, allocation, modelView, projection, sorting));
    }

    @NotNull RenderTarget sceneTexture() {
        TextureTarget texture = sceneTexture;
        if (texture == null) throw new IllegalStateException("Scene texture is unavailable outside deferred world rendering");
        return texture;
    }

    void endFrame() {
        worldRendering = false;
        if (draws.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        TextureTarget scene = prepareSceneTexture(main);
        copyColor(main, scene);
        main.bindWrite(true);

        while (true) {
            Draw draw = draws.pollFirst();
            if (draw == null) break;
            try (draw) {
                Matrix4f previousModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
                Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
                VertexSorting previousSorting = RenderSystem.getVertexSorting();
                RenderSystem.getModelViewStack().set(draw.modelView);
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(draw.projection, draw.sorting);
                draw.renderType.setupRenderState();
                try {
                    BufferUploader.drawWithShader(draw.mesh);
                } finally {
                    draw.renderType.clearRenderState();
                    RenderSystem.getModelViewStack().set(previousModelView);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
                }
            }
        }
        main.bindWrite(true);
    }

    void close() {
        worldRendering = false;
        Draw draw;
        while ((draw = draws.pollFirst()) != null) draw.close();
        TextureTarget texture = sceneTexture;
        if (texture != null) {
            texture.destroyBuffers();
            sceneTexture = null;
        }
    }

    private @NotNull TextureTarget prepareSceneTexture(@NotNull RenderTarget main) {
        TextureTarget texture = sceneTexture;
        if (texture == null) {
            texture = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            texture.setFilterMode(GlConst.GL_LINEAR);
            sceneTexture = texture;
        } else if (texture.width != main.width || texture.height != main.height) {
            texture.resize(main.width, main.height, Minecraft.ON_OSX);
            texture.setFilterMode(GlConst.GL_LINEAR);
        }
        return texture;
    }

    private static void copyColor(@NotNull RenderTarget source, @NotNull RenderTarget destination) {
        GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
                0, 0, source.width, source.height,
                0, 0, destination.width, destination.height,
                GlConst.GL_COLOR_BUFFER_BIT,
                GlConst.GL_NEAREST
        );
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, source.frameBufferId);
    }

    private record Draw(
            @NotNull RenderType renderType,
            @NotNull MeshData mesh,
            @NotNull ByteBufferBuilder allocation,
            @NotNull Matrix4f modelView,
            @NotNull Matrix4f projection,
            @NotNull VertexSorting sorting
    ) implements AutoCloseable {
        @Override
        public void close() {
            mesh.close();
            allocation.close();
        }
    }
}

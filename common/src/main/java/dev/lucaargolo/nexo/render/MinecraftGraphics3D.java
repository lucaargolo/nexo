package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public final class MinecraftGraphics3D extends MinecraftGraphics2D implements AbstractMinecraftGraphics3D {

    public MinecraftGraphics3D(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull PoseStack poses, @NotNull MultiBufferSource buffers, int packedLight, int packedOverlay) {
        super(nexo, poses, buffers, packedLight, packedOverlay);
    }

    public MinecraftGraphics3D(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull PoseStack poses, @NotNull MultiBufferSource buffers, @NotNull Location atlas, int packedLight, int packedOverlay) {
        super(nexo, poses, buffers, atlas, packedLight, packedOverlay);
    }

    @Override
    public void drawLine(float x1, float y1, float z1, float x2, float y2, float z2) {
        begin(PrimitiveType.LINES, VertexFormat.POSITION);
        vertex(x1, y1, z1);
        vertex(x2, y2, z2);
        end();
    }

    @Override
    public @NotNull CullMode defaultCullMode() {
        return CullMode.BACK;
    }

    @Override
    public @NotNull Vector3f cameraPosition() {
        return new Matrix4f(poses.last().pose()).invert().transformPosition(new Vector3f());
    }

}

package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.DepthMode;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public final class MinecraftGraphics3D extends MinecraftGraphics2D implements Graphics3D {

    public MinecraftGraphics3D(
            @NotNull PoseStack poses,
            @NotNull MultiBufferSource buffers,
            @NotNull MinecraftShaderRenderer shaderRenderer,
            int packedLight,
            int packedOverlay
    ) {
        super(poses, buffers, shaderRenderer, packedLight, packedOverlay);
        state.depthMode = DepthMode.ENABLED;
    }

    @Override
    protected @NotNull CullMode defaultCullMode() {
        return CullMode.BACK;
    }

    @Override
    public @NotNull Vector3f cameraPosition() {
        return new Matrix4f(poses.last().pose()).invert().transformPosition(new Vector3f());
    }
}

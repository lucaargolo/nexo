package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.BlockElementRotationMixed;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

    @Unique
    private static final float RESCALE_22_5 = 1.0F / (float) Math.cos(Math.PI / 8.0) - 1.0F;
    @Unique
    private static final float RESCALE_45 = 1.0F / (float) Math.cos(Math.PI / 4.0) - 1.0F;

    private static final ThreadLocal<Vector3f> SAVED_VERTEX = new ThreadLocal<>();

    @Inject(method = "applyElementRotation", at = @At("HEAD"))
    private void nexo$onApplyElementRotationHead(
            Vector3f vertex,
            BlockElementRotation rotation,
            CallbackInfo ci
    ) {
        if (rotation == null) {
            SAVED_VERTEX.remove();
            return;
        }
        Vector3f euler = ((BlockElementRotationMixed) (Object) rotation).nexo$getEulerRotation();
        if (euler != null) {
            SAVED_VERTEX.set(new Vector3f(vertex));
        } else {
            SAVED_VERTEX.remove();
        }
    }

    @Inject(method = "applyElementRotation", at = @At("RETURN"))
    private void nexo$onApplyElementRotationReturn(
            Vector3f vertex,
            BlockElementRotation rotation,
            CallbackInfo ci
    ) {
        Vector3f original = SAVED_VERTEX.get();
        if (original == null) return;

        vertex.set(original);
        Vector3f euler = ((BlockElementRotationMixed) (Object) rotation).nexo$getEulerRotation();

        Vector3f origin = rotation.origin();
        float toRad = (float) Math.PI / 180.0F;
        Quaternionf quat = new Quaternionf()
                .rotationZYX(euler.z() * toRad, euler.y() * toRad, euler.x() * toRad);
        Matrix4f matrix = new Matrix4f().rotation(quat);

        Vector3f rescaleVec;
        if (rotation.rescale()) {
            rescaleVec = nexo$computeEulerRescale(euler);
        } else {
            rescaleVec = new Vector3f(1.0F, 1.0F, 1.0F);
        }

        Vector4f v = matrix.transform(new Vector4f(
                vertex.x() - origin.x(),
                vertex.y() - origin.y(),
                vertex.z() - origin.z(),
                1.0F
        ));
        v.mul(new Vector4f(rescaleVec, 1.0F));
        vertex.set(v.x() + origin.x(), v.y() + origin.y(), v.z() + origin.z());
    }

    @Unique
    private static Vector3f nexo$computeEulerRescale(Vector3f euler) {
        float rx = 1.0F, ry = 1.0F, rz = 1.0F;
        final float absX = Math.abs(euler.x());
        final float absY = Math.abs(euler.y());
        final float absZ = Math.abs(euler.z());

        if (absX > 0.001F) {
            float fac = rescaleFactor(absX);
            ry = 1.0F + fac;
            rz = 1.0F + fac;
        }
        if (absY > 0.001F) {
            float fac = rescaleFactor(absY);
            rx *= (1.0F + fac);
            rz *= (1.0F + fac);
        }
        if (absZ > 0.001F) {
            float fac = rescaleFactor(absZ);
            rx *= (1.0F + fac);
            ry *= (1.0F + fac);
        }
        return new Vector3f(rx, ry, rz);
    }

    private static float rescaleFactor(float absDegrees) {
        if (Math.abs(absDegrees - 22.5F) < 0.01F) return RESCALE_22_5;
        if (Math.abs(absDegrees - 45.0F) < 0.01F) return RESCALE_45;
        return 1.0F / (float) Math.cos(absDegrees * Math.PI / 180.0) - 1.0F;
    }
}

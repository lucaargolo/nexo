package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.BlockElementRotationMixed;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockElementRotation.class)
public class BlockElementRotationMixin implements BlockElementRotationMixed {

    @Unique
    private Vector3f nexo$eulerRotation;

    @Override
    public Vector3f nexo$getEulerRotation() {
        return this.nexo$eulerRotation;
    }

    @Override
    public void nexo$setEulerRotation(Vector3f euler) {
        this.nexo$eulerRotation = euler;
    }
}

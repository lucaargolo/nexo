package dev.lucaargolo.nexo.mixed;

import org.joml.Vector3f;

/**
 * Accessor interface for the euler-angle rotation data that
 * {@code BlockElementRotationMixin} injects into {@code BlockElementRotation}.
 * <p>
 * Cast a {@code BlockElementRotation} instance to this interface to read or
 * write the euler rotation vector stored by the mixin.
 */
public interface BlockElementRotationMixed {

    Vector3f nexo$getEulerRotation();

    void nexo$setEulerRotation(Vector3f euler);
}

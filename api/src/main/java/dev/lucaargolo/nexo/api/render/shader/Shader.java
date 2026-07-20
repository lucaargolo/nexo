package dev.lucaargolo.nexo.api.render.shader;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix2fc;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;

public interface Shader extends AutoCloseable {

    void uniform(@NotNull String name, boolean value);
    void uniform(@NotNull String name, boolean x, boolean y);
    void uniform(@NotNull String name, boolean x, boolean y, boolean z);
    void uniform(@NotNull String name, boolean x, boolean y, boolean z, boolean w);

    void uniform(@NotNull String name, int value);
    void uniform(@NotNull String name, int x, int y);
    void uniform(@NotNull String name, int x, int y, int z);
    void uniform(@NotNull String name, int x, int y, int z, int w);

    void uniform(@NotNull String name, float value);
    void uniform(@NotNull String name, float x, float y);
    void uniform(@NotNull String name, float x, float y, float z);
    void uniform(@NotNull String name, float x, float y, float z, float w);

    void uniform(@NotNull String name, @NotNull Matrix2fc value);
    void uniform(@NotNull String name, @NotNull Matrix3fc value);
    void uniform(@NotNull String name, @NotNull Matrix4fc value);

    void uniform(@NotNull String name, @NotNull Location texture);

    @Override
    void close();

}

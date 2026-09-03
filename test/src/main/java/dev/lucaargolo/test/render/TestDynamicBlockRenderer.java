package dev.lucaargolo.test.render;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.awt.*;
import java.util.Map;

public class TestDynamicBlockRenderer implements Renderer<Graphics3D, BlockUnit<?>> {

    @Override
    public void render(@NotNull BlockUnit<?> unit, @NotNull Graphics3D graphics) {
        float value = (System.currentTimeMillis() % 10000) / 10000.0f;
        int color = Color.HSBtoRGB(value, 1.0F, 1.0F);
        graphics.pushState();
        graphics.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        graphics.drawCube(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        graphics.popState();
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return Map.of();
    }

    @Override
    public @NotNull Transform transform(@NotNull Location location) {
        return Map.of(
                Location.of("minecraft", "gui"), new Transform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
                Location.of("minecraft", "ground"), new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
                Location.of("minecraft", "fixed"), new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f)),
                Location.of("minecraft", "thirdperson_righthand"), new Transform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
                Location.of("minecraft", "firstperson_righthand"), new Transform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
                Location.of("minecraft", "firstperson_lefthand"), new Transform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f))
        ).getOrDefault(location, new Transform(new Vector3f(), new Vector3f(), new Vector3f(1.0F)));
    }

}
package dev.lucaargolo.nexo.api.input;

import org.jetbrains.annotations.NotNull;

/**
 * A discrete input event: a keyboard key, a mouse button, or a joypad button.
 * The {@code code} mirrors the underlying GLFW key code, mouse button code, or
 * gamepad button index respectively.
 */
public record Input(@NotNull Type type, int code) {

    public static @NotNull Input keyboard(int code) {
        return new Input(Type.KEYBOARD, code);
    }

    public static @NotNull Input mouse(int code) {
        return new Input(Type.MOUSE, code);
    }

    public static @NotNull Input gamepad(int code) {
        return new Input(Type.GAMEPAD, code);
    }

    public enum Type {
        KEYBOARD,
        MOUSE,
        GAMEPAD
    }

    @Override
    public @NotNull String toString() {
        return type + "(" + code + ")";
    }
}

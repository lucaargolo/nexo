package dev.lucaargolo.nexo.api.input;

import org.jetbrains.annotations.NotNull;


public record Input(@NotNull Type type, @NotNull Key key) {

    public static @NotNull Input keyboard(@NotNull Key key) {
        return new Input(Type.KEYBOARD, key);
    }

    public static @NotNull Input mouse(@NotNull Key key) {
        return new Input(Type.MOUSE, key);
    }

    public static @NotNull Input gamepad(@NotNull Key key) {
        return new Input(Type.GAMEPAD, key);
    }

    public enum Type {
        KEYBOARD,
        MOUSE,
        GAMEPAD
    }

    public enum Key {
        UNKNOWN,
        SPACE,
        APOSTROPHE,
        COMMA,
        MINUS,
        PERIOD,
        SLASH,
        ZERO,
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        SEMICOLON,
        EQUAL,
        A,
        B,
        C,
        D,
        E,
        F,
        G,
        H,
        I,
        J,
        K,
        L,
        M,
        N,
        O,
        P,
        Q,
        R,
        S,
        T,
        U,
        V,
        W,
        X,
        Y,
        Z,
        LEFT_BRACKET,
        BACKSLASH,
        RIGHT_BRACKET,
        GRAVE_ACCENT,
        WORLD_1,
        WORLD_2,


        ESCAPE,
        ENTER,
        TAB,
        BACKSPACE,
        INSERT,
        DELETE,
        RIGHT,
        LEFT,
        DOWN,
        UP,
        PAGE_UP,
        PAGE_DOWN,
        HOME,
        END,
        CAPS_LOCK,
        SCROLL_LOCK,
        NUM_LOCK,
        PRINT_SCREEN,
        PAUSE,
        F1,
        F2,
        F3,
        F4,
        F5,
        F6,
        F7,
        F8,
        F9,
        F10,
        F11,
        F12,
        F13,
        F14,
        F15,
        F16,
        F17,
        F18,
        F19,
        F20,
        F21,
        F22,
        F23,
        F24,
        F25,


        KP_0,
        KP_1,
        KP_2,
        KP_3,
        KP_4,
        KP_5,
        KP_6,
        KP_7,
        KP_8,
        KP_9,
        KP_DECIMAL,
        KP_DIVIDE,
        KP_MULTIPLY,
        KP_SUBTRACT,
        KP_ADD,
        KP_ENTER,
        KP_EQUAL,


        LEFT_SHIFT,
        LEFT_CONTROL,
        LEFT_ALT,
        LEFT_SUPER,
        RIGHT_SHIFT,
        RIGHT_CONTROL,
        RIGHT_ALT,
        RIGHT_SUPER,
        MENU,


        MOUSE_LEFT,
        MOUSE_RIGHT,
        MOUSE_MIDDLE,
        MOUSE_BUTTON_4,
        MOUSE_BUTTON_5,
        MOUSE_BUTTON_6,
        MOUSE_BUTTON_7,
        MOUSE_BUTTON_8,


        GAMEPAD_A,
        GAMEPAD_B,
        GAMEPAD_X,
        GAMEPAD_Y,
        GAMEPAD_LEFT_BUMPER,
        GAMEPAD_RIGHT_BUMPER,
        GAMEPAD_BACK,
        GAMEPAD_START,
        GAMEPAD_GUIDE,
        GAMEPAD_LEFT_THUMB,
        GAMEPAD_RIGHT_THUMB,
        GAMEPAD_DPAD_UP,
        GAMEPAD_DPAD_RIGHT,
        GAMEPAD_DPAD_DOWN,
        GAMEPAD_DPAD_LEFT
    }


    public enum Axis {
        MOUSE_X,
        MOUSE_Y,
        SCROLL,
        GAMEPAD_LEFT_X,
        GAMEPAD_LEFT_Y,
        GAMEPAD_RIGHT_X,
        GAMEPAD_RIGHT_Y,
        GAMEPAD_LEFT_TRIGGER,
        GAMEPAD_RIGHT_TRIGGER
    }

    @Override
    public @NotNull String toString() {
        return type + "(" + key + ")";
    }
}

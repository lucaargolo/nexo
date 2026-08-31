package dev.lucaargolo.nexo.input;

import dev.lucaargolo.nexo.api.input.Input;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class GlfwKeyConversions {

    private GlfwKeyConversions() {
    }

    public static @NotNull Input.Key key(int code) {
        return switch (code) {
            case GLFW.GLFW_KEY_SPACE -> Input.Key.SPACE;
            case GLFW.GLFW_KEY_APOSTROPHE -> Input.Key.APOSTROPHE;
            case GLFW.GLFW_KEY_COMMA -> Input.Key.COMMA;
            case GLFW.GLFW_KEY_MINUS -> Input.Key.MINUS;
            case GLFW.GLFW_KEY_PERIOD -> Input.Key.PERIOD;
            case GLFW.GLFW_KEY_SLASH -> Input.Key.SLASH;
            case GLFW.GLFW_KEY_0 -> Input.Key.ZERO;
            case GLFW.GLFW_KEY_1 -> Input.Key.ONE;
            case GLFW.GLFW_KEY_2 -> Input.Key.TWO;
            case GLFW.GLFW_KEY_3 -> Input.Key.THREE;
            case GLFW.GLFW_KEY_4 -> Input.Key.FOUR;
            case GLFW.GLFW_KEY_5 -> Input.Key.FIVE;
            case GLFW.GLFW_KEY_6 -> Input.Key.SIX;
            case GLFW.GLFW_KEY_7 -> Input.Key.SEVEN;
            case GLFW.GLFW_KEY_8 -> Input.Key.EIGHT;
            case GLFW.GLFW_KEY_9 -> Input.Key.NINE;
            case GLFW.GLFW_KEY_SEMICOLON -> Input.Key.SEMICOLON;
            case GLFW.GLFW_KEY_EQUAL -> Input.Key.EQUAL;
            case GLFW.GLFW_KEY_A -> Input.Key.A;
            case GLFW.GLFW_KEY_B -> Input.Key.B;
            case GLFW.GLFW_KEY_C -> Input.Key.C;
            case GLFW.GLFW_KEY_D -> Input.Key.D;
            case GLFW.GLFW_KEY_E -> Input.Key.E;
            case GLFW.GLFW_KEY_F -> Input.Key.F;
            case GLFW.GLFW_KEY_G -> Input.Key.G;
            case GLFW.GLFW_KEY_H -> Input.Key.H;
            case GLFW.GLFW_KEY_I -> Input.Key.I;
            case GLFW.GLFW_KEY_J -> Input.Key.J;
            case GLFW.GLFW_KEY_K -> Input.Key.K;
            case GLFW.GLFW_KEY_L -> Input.Key.L;
            case GLFW.GLFW_KEY_M -> Input.Key.M;
            case GLFW.GLFW_KEY_N -> Input.Key.N;
            case GLFW.GLFW_KEY_O -> Input.Key.O;
            case GLFW.GLFW_KEY_P -> Input.Key.P;
            case GLFW.GLFW_KEY_Q -> Input.Key.Q;
            case GLFW.GLFW_KEY_R -> Input.Key.R;
            case GLFW.GLFW_KEY_S -> Input.Key.S;
            case GLFW.GLFW_KEY_T -> Input.Key.T;
            case GLFW.GLFW_KEY_U -> Input.Key.U;
            case GLFW.GLFW_KEY_V -> Input.Key.V;
            case GLFW.GLFW_KEY_W -> Input.Key.W;
            case GLFW.GLFW_KEY_X -> Input.Key.X;
            case GLFW.GLFW_KEY_Y -> Input.Key.Y;
            case GLFW.GLFW_KEY_Z -> Input.Key.Z;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> Input.Key.LEFT_BRACKET;
            case GLFW.GLFW_KEY_BACKSLASH -> Input.Key.BACKSLASH;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> Input.Key.RIGHT_BRACKET;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> Input.Key.GRAVE_ACCENT;
            case GLFW.GLFW_KEY_WORLD_1 -> Input.Key.WORLD_1;
            case GLFW.GLFW_KEY_WORLD_2 -> Input.Key.WORLD_2;
            case GLFW.GLFW_KEY_ESCAPE -> Input.Key.ESCAPE;
            case GLFW.GLFW_KEY_ENTER -> Input.Key.ENTER;
            case GLFW.GLFW_KEY_TAB -> Input.Key.TAB;
            case GLFW.GLFW_KEY_BACKSPACE -> Input.Key.BACKSPACE;
            case GLFW.GLFW_KEY_INSERT -> Input.Key.INSERT;
            case GLFW.GLFW_KEY_DELETE -> Input.Key.DELETE;
            case GLFW.GLFW_KEY_RIGHT -> Input.Key.RIGHT;
            case GLFW.GLFW_KEY_LEFT -> Input.Key.LEFT;
            case GLFW.GLFW_KEY_DOWN -> Input.Key.DOWN;
            case GLFW.GLFW_KEY_UP -> Input.Key.UP;
            case GLFW.GLFW_KEY_PAGE_UP -> Input.Key.PAGE_UP;
            case GLFW.GLFW_KEY_PAGE_DOWN -> Input.Key.PAGE_DOWN;
            case GLFW.GLFW_KEY_HOME -> Input.Key.HOME;
            case GLFW.GLFW_KEY_END -> Input.Key.END;
            case GLFW.GLFW_KEY_CAPS_LOCK -> Input.Key.CAPS_LOCK;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> Input.Key.SCROLL_LOCK;
            case GLFW.GLFW_KEY_NUM_LOCK -> Input.Key.NUM_LOCK;
            case GLFW.GLFW_KEY_PRINT_SCREEN -> Input.Key.PRINT_SCREEN;
            case GLFW.GLFW_KEY_PAUSE -> Input.Key.PAUSE;
            case GLFW.GLFW_KEY_F1 -> Input.Key.F1;
            case GLFW.GLFW_KEY_F2 -> Input.Key.F2;
            case GLFW.GLFW_KEY_F3 -> Input.Key.F3;
            case GLFW.GLFW_KEY_F4 -> Input.Key.F4;
            case GLFW.GLFW_KEY_F5 -> Input.Key.F5;
            case GLFW.GLFW_KEY_F6 -> Input.Key.F6;
            case GLFW.GLFW_KEY_F7 -> Input.Key.F7;
            case GLFW.GLFW_KEY_F8 -> Input.Key.F8;
            case GLFW.GLFW_KEY_F9 -> Input.Key.F9;
            case GLFW.GLFW_KEY_F10 -> Input.Key.F10;
            case GLFW.GLFW_KEY_F11 -> Input.Key.F11;
            case GLFW.GLFW_KEY_F12 -> Input.Key.F12;
            case GLFW.GLFW_KEY_F13 -> Input.Key.F13;
            case GLFW.GLFW_KEY_F14 -> Input.Key.F14;
            case GLFW.GLFW_KEY_F15 -> Input.Key.F15;
            case GLFW.GLFW_KEY_F16 -> Input.Key.F16;
            case GLFW.GLFW_KEY_F17 -> Input.Key.F17;
            case GLFW.GLFW_KEY_F18 -> Input.Key.F18;
            case GLFW.GLFW_KEY_F19 -> Input.Key.F19;
            case GLFW.GLFW_KEY_F20 -> Input.Key.F20;
            case GLFW.GLFW_KEY_F21 -> Input.Key.F21;
            case GLFW.GLFW_KEY_F22 -> Input.Key.F22;
            case GLFW.GLFW_KEY_F23 -> Input.Key.F23;
            case GLFW.GLFW_KEY_F24 -> Input.Key.F24;
            case GLFW.GLFW_KEY_F25 -> Input.Key.F25;
            case GLFW.GLFW_KEY_KP_0 -> Input.Key.KP_0;
            case GLFW.GLFW_KEY_KP_1 -> Input.Key.KP_1;
            case GLFW.GLFW_KEY_KP_2 -> Input.Key.KP_2;
            case GLFW.GLFW_KEY_KP_3 -> Input.Key.KP_3;
            case GLFW.GLFW_KEY_KP_4 -> Input.Key.KP_4;
            case GLFW.GLFW_KEY_KP_5 -> Input.Key.KP_5;
            case GLFW.GLFW_KEY_KP_6 -> Input.Key.KP_6;
            case GLFW.GLFW_KEY_KP_7 -> Input.Key.KP_7;
            case GLFW.GLFW_KEY_KP_8 -> Input.Key.KP_8;
            case GLFW.GLFW_KEY_KP_9 -> Input.Key.KP_9;
            case GLFW.GLFW_KEY_KP_DECIMAL -> Input.Key.KP_DECIMAL;
            case GLFW.GLFW_KEY_KP_DIVIDE -> Input.Key.KP_DIVIDE;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> Input.Key.KP_MULTIPLY;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> Input.Key.KP_SUBTRACT;
            case GLFW.GLFW_KEY_KP_ADD -> Input.Key.KP_ADD;
            case GLFW.GLFW_KEY_KP_ENTER -> Input.Key.KP_ENTER;
            case GLFW.GLFW_KEY_KP_EQUAL -> Input.Key.KP_EQUAL;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> Input.Key.LEFT_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> Input.Key.LEFT_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT -> Input.Key.LEFT_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER -> Input.Key.LEFT_SUPER;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> Input.Key.RIGHT_SHIFT;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> Input.Key.RIGHT_CONTROL;
            case GLFW.GLFW_KEY_RIGHT_ALT -> Input.Key.RIGHT_ALT;
            case GLFW.GLFW_KEY_RIGHT_SUPER -> Input.Key.RIGHT_SUPER;
            case GLFW.GLFW_KEY_MENU -> Input.Key.MENU;
            default -> Input.Key.UNKNOWN;
        };
    }

    public static @NotNull Input.Key mouse(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> Input.Key.MOUSE_LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> Input.Key.MOUSE_RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> Input.Key.MOUSE_MIDDLE;
            case GLFW.GLFW_MOUSE_BUTTON_4 -> Input.Key.MOUSE_BUTTON_4;
            case GLFW.GLFW_MOUSE_BUTTON_5 -> Input.Key.MOUSE_BUTTON_5;
            case GLFW.GLFW_MOUSE_BUTTON_6 -> Input.Key.MOUSE_BUTTON_6;
            case GLFW.GLFW_MOUSE_BUTTON_7 -> Input.Key.MOUSE_BUTTON_7;
            case GLFW.GLFW_MOUSE_BUTTON_8 -> Input.Key.MOUSE_BUTTON_8;
            default -> Input.Key.UNKNOWN;
        };
    }

    public static @NotNull Input.Key gamepad(int button) {
        return switch (button) {
            case GLFW.GLFW_GAMEPAD_BUTTON_A -> Input.Key.GAMEPAD_A;
            case GLFW.GLFW_GAMEPAD_BUTTON_B -> Input.Key.GAMEPAD_B;
            case GLFW.GLFW_GAMEPAD_BUTTON_X -> Input.Key.GAMEPAD_X;
            case GLFW.GLFW_GAMEPAD_BUTTON_Y -> Input.Key.GAMEPAD_Y;
            case GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER -> Input.Key.GAMEPAD_LEFT_BUMPER;
            case GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER -> Input.Key.GAMEPAD_RIGHT_BUMPER;
            case GLFW.GLFW_GAMEPAD_BUTTON_BACK -> Input.Key.GAMEPAD_BACK;
            case GLFW.GLFW_GAMEPAD_BUTTON_START -> Input.Key.GAMEPAD_START;
            case GLFW.GLFW_GAMEPAD_BUTTON_GUIDE -> Input.Key.GAMEPAD_GUIDE;
            case GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB -> Input.Key.GAMEPAD_LEFT_THUMB;
            case GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB -> Input.Key.GAMEPAD_RIGHT_THUMB;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP -> Input.Key.GAMEPAD_DPAD_UP;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT -> Input.Key.GAMEPAD_DPAD_RIGHT;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN -> Input.Key.GAMEPAD_DPAD_DOWN;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT -> Input.Key.GAMEPAD_DPAD_LEFT;
            default -> Input.Key.UNKNOWN;
        };
    }
}

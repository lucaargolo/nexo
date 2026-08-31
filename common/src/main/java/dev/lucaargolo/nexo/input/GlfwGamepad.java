package dev.lucaargolo.nexo.input;

import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class GlfwGamepad {

    private static final Input.Axis[] AXIS_BY_INDEX = {
            Input.Axis.GAMEPAD_LEFT_X,
            Input.Axis.GAMEPAD_LEFT_Y,
            Input.Axis.GAMEPAD_RIGHT_X,
            Input.Axis.GAMEPAD_RIGHT_Y,
            Input.Axis.GAMEPAD_LEFT_TRIGGER,
            Input.Axis.GAMEPAD_RIGHT_TRIGGER
    };

    private final @NotNull ScreenBase feature;
    private final @NotNull MinecraftScreenUnit<?> unit;
    private final @NotNull GLFWGamepadState state = GLFWGamepadState.create();
    private final byte[] previousButtons = new byte[GLFW.GLFW_GAMEPAD_BUTTON_LAST + 1];
    private final float[] previousAxes = new float[GLFW.GLFW_GAMEPAD_AXIS_LAST + 1];
    private int joystick = -1;
    private boolean polledOnce;

    public GlfwGamepad(@NotNull ScreenBase feature, @NotNull MinecraftScreenUnit<?> unit) {
        this.feature = feature;
        this.unit = unit;
    }

    public void poll() {
        int id = joystick;
        if (id < 0 || !GLFW.glfwJoystickPresent(id)) {
            id = findJoystick();
            joystick = id;
            if (id < 0) {
                return;
            }
        }
        boolean mapped = GLFW.glfwGetGamepadState(id, state);
        ByteBuffer buttons = mapped ? state.buttons() : GLFW.glfwGetJoystickButtons(id);
        FloatBuffer axes = mapped ? state.axes() : GLFW.glfwGetJoystickAxes(id);
        if (buttons == null || axes == null) {
            return;
        }
        int buttonCount = Math.min(buttons.remaining(), previousButtons.length);
        int axisCount = Math.min(axes.remaining(), previousAxes.length);
        if (polledOnce) {
            for (int i = 0; i < buttonCount; i++) {
                boolean pressed = buttons.get(i) == GLFW.GLFW_PRESS;
                boolean previous = previousButtons[i] == GLFW.GLFW_PRESS;
                if (pressed && !previous) {
                    feature.onInputPressed(unit, Input.gamepad(GlfwKeyConversions.gamepad(i)));
                } else if (!pressed && previous) {
                    feature.onInputReleased(unit, Input.gamepad(GlfwKeyConversions.gamepad(i)));
                }
            }
            int mappedAxisCount = Math.min(axisCount, AXIS_BY_INDEX.length);
            for (int i = 0; i < mappedAxisCount; i++) {
                float value = axes.get(i);
                float delta = value - previousAxes[i];
                if (delta != 0.0F) {
                    feature.onInputMove(unit, AXIS_BY_INDEX[i], delta);
                }
            }
        }
        for (int i = 0; i < buttonCount; i++) {
            previousButtons[i] = buttons.get(i);
        }
        for (int i = 0; i < axisCount; i++) {
            previousAxes[i] = axes.get(i);
        }
        polledOnce = true;
    }

    private static int findJoystick() {
        if (GLFW.glfwJoystickPresent(GLFW.GLFW_JOYSTICK_1)) {
            return GLFW.GLFW_JOYSTICK_1;
        }
        for (int i = GLFW.GLFW_JOYSTICK_1 + 1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
                return i;
            }
        }
        return -1;
    }
}

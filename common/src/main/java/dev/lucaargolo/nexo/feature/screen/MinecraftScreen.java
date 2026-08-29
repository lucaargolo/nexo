package dev.lucaargolo.nexo.feature.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.input.GlfwKeyMapper;
import dev.lucaargolo.nexo.render.DynamicMinecraftGraphics2D;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;


public class MinecraftScreen extends Screen {

    private static final Input.Axis[] AXIS_BY_INDEX = {
            Input.Axis.GAMEPAD_LEFT_X,
            Input.Axis.GAMEPAD_LEFT_Y,
            Input.Axis.GAMEPAD_RIGHT_X,
            Input.Axis.GAMEPAD_RIGHT_Y,
            Input.Axis.GAMEPAD_LEFT_TRIGGER,
            Input.Axis.GAMEPAD_RIGHT_TRIGGER
    };

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull ScreenBase feature;
    private final @NotNull MinecraftScreenUnit<?> unit;

    private final @NotNull GLFWGamepadState gamepadState = GLFWGamepadState.create();
    private final byte[] previousButtons = new byte[GLFW.GLFW_GAMEPAD_BUTTON_LAST + 1];
    private final float[] previousAxes = new float[GLFW.GLFW_GAMEPAD_AXIS_LAST + 1];
    private int joystick = -1;
    private boolean polledOnce;

    private double lastMouseX = -1.0;
    private double lastMouseY = -1.0;

    public MinecraftScreen(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @NotNull MinecraftScreenUnit<?> unit) {
        super(Component.translatable(feature.languageKey()));
        this.nexo = nexo;
        this.feature = feature;
        this.unit = unit;
    }

    @Override
    protected void init() {
        super.init();
        feature.build(unit);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        unit.setMouse(mouseX, mouseY);
        DynamicMinecraftGraphics2D g = new DynamicMinecraftGraphics2D(
                nexo,
                graphics.pose(),
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );
        try {
            feature.render(g, unit);
        } catch (Throwable t) {
            NexoMinecraft.LOGGER.error("Failed to render Nexo screen {}", feature.location(), t);
            throw t;
        } finally {
            g.finish();
        }
    }

    @Override
    public void tick() {
        super.tick();
        pollGamepad();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (feature.onInputPressed(unit, Input.keyboard(GlfwKeyMapper.key(keyCode)))) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (feature.onInputReleased(unit, Input.keyboard(GlfwKeyMapper.key(keyCode)))) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (feature.onInputPressed(unit, Input.mouse(GlfwKeyMapper.mouse(button)))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (feature.onInputReleased(unit, Input.mouse(GlfwKeyMapper.mouse(button)))) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        feature.onInputMove(unit, Input.Axis.MOUSE_X, (float) dragX);
        feature.onInputMove(unit, Input.Axis.MOUSE_Y, (float) dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (lastMouseX >= 0.0 && lastMouseY >= 0.0) {
            feature.onInputMove(unit, Input.Axis.MOUSE_X, (float) (mouseX - lastMouseX));
            feature.onInputMove(unit, Input.Axis.MOUSE_Y, (float) (mouseY - lastMouseY));
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        feature.onInputMove(unit, Input.Axis.SCROLL, (float) verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void pollGamepad() {
        int id = joystick;
        if (id < 0 || !GLFW.glfwJoystickPresent(id)) {
            id = findJoystick();
            joystick = id;
            if (id < 0) {
                return;
            }
        }
        boolean mapped = GLFW.glfwGetGamepadState(id, gamepadState);
        ByteBuffer buttons = mapped ? gamepadState.buttons() : GLFW.glfwGetJoystickButtons(id);
        FloatBuffer axes = mapped ? gamepadState.axes() : GLFW.glfwGetJoystickAxes(id);
        if (buttons == null || axes == null) {
            return;
        }
        if (polledOnce) {
            int buttonCount = Math.min(buttons.remaining(), previousButtons.length);
            for (int i = 0; i < buttonCount; i++) {
                boolean pressed = buttons.get(i) == GLFW.GLFW_PRESS;
                boolean previous = previousButtons[i] == GLFW.GLFW_PRESS;
                if (pressed && !previous) {
                    feature.onInputPressed(unit, Input.gamepad(GlfwKeyMapper.gamepad(i)));
                } else if (!pressed && previous) {
                    feature.onInputReleased(unit, Input.gamepad(GlfwKeyMapper.gamepad(i)));
                }
            }
            int axisCount = Math.min(axes.remaining(), Math.min(previousAxes.length, AXIS_BY_INDEX.length));
            for (int i = 0; i < axisCount; i++) {
                float value = axes.get(i);
                float delta = value - previousAxes[i];
                if (delta != 0.0F) {
                    feature.onInputMove(unit, AXIS_BY_INDEX[i], delta);
                }
            }
        }
        for (int i = 0; i < buttons.remaining() && i < previousButtons.length; i++) {
            previousButtons[i] = buttons.get(i);
        }
        for (int i = 0; i < axes.remaining() && i < previousAxes.length; i++) {
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

package dev.lucaargolo.nexo.api.input;

/**
 * An analogic input axis: mouse motion, mouse scrolling, joypad sticks, or
 * joypad triggers. Deltas are reported per event (e.g. pixels moved, scroll
 * steps, or normalized stick/trigger change).
 */
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

package dev.lucaargolo.nexo.language;

import dev.lucaargolo.nexo.api.render.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class MinecraftLanguageConversions {

    private MinecraftLanguageConversions() {
    }

    public static @NotNull String minecraftToNexo(@NotNull String value) {
        return minecraftParametersToNexo(minecraftFormattingToNexo(value));
    }

    public static @NotNull String nexoToMinecraft(@NotNull String value) {
        String sanitized = stripUnsupportedTags(nexoParametersToMinecraft(value));
        StringBuilder result = new StringBuilder(sanitized.length());
        Text.Style previous = Text.Style.DEFAULT;
        for (Text.Run run : Text.parse(sanitized).runs()) {
            Text.Style style = run.style();
            if (!style.equals(previous)) {
                result.append('§').append('r');
                appendMinecraftStyle(result, style);
                previous = style;
            }
            result.append(run.text());
        }
        return result.toString();
    }

    private static @NotNull String minecraftFormattingToNexo(@NotNull String value) {
        StringBuilder result = new StringBuilder(value.length());
        FormattingState state = new FormattingState();
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (character != '§' || index + 1 >= value.length()) {
                result.append(character);
                index++;
                continue;
            }

            char code = Character.toLowerCase(value.charAt(index + 1));
            if (code == 'x' && index + 13 < value.length()) {
                StringBuilder color = new StringBuilder(6);
                boolean valid = true;
                for (int part = 0; part < 6; part++) {
                    int marker = index + 2 + part * 2;
                    char digit = value.charAt(marker + 1);
                    if (value.charAt(marker) != '§' || !(digit >= '0' && digit <= '9'
                            || digit >= 'a' && digit <= 'f'
                            || digit >= 'A' && digit <= 'F')) {
                        valid = false;
                        break;
                    }
                    color.append(value.charAt(marker + 1));
                }
                if (valid) {
                    state.reset();
                    state.color = "#" + color;
                    appendState(result, state);
                    index += 14;
                    continue;
                }
            }

            String color = minecraftColor(code);
            if (color != null) {
                state.reset();
                state.color = color;
                appendState(result, state);
            } else {
                switch (code) {
                    case 'k' -> state.obfuscated = true;
                    case 'l' -> state.bold = true;
                    case 'm' -> state.strikethrough = true;
                    case 'n' -> state.underlined = true;
                    case 'o' -> state.italic = true;
                    case 'r' -> state.reset();
                    default -> {
                        index += 2;
                        continue;
                    }
                }
                appendState(result, state);
            }
            index += 2;
        }
        return result.toString();
    }

    private static void appendState(@NotNull StringBuilder result, @NotNull FormattingState state) {
        result.append("[reset]");
        if (state.color != null) {
            result.append("[color=").append(state.color).append(']');
        }
        if (state.bold) {
            result.append("[b]");
        }
        if (state.italic) {
            result.append("[i]");
        }
        if (state.underlined) {
            result.append("[u]");
        }
        if (state.strikethrough) {
            result.append("[s]");
        }
        if (state.obfuscated) {
            result.append("[o]");
        }
    }

    private static @NotNull String minecraftParametersToNexo(@NotNull String value) {
        StringBuilder result = new StringBuilder(value.length());
        int nextIndex = 0;
        int index = 0;
        while (index < value.length()) {
            if (value.charAt(index) != '%' || index + 1 >= value.length()) {
                result.append(value.charAt(index++));
                continue;
            }
            if (value.charAt(index + 1) == '%') {
                result.append('%');
                index += 2;
                continue;
            }

            int cursor = index + 1;
            int explicitIndex = -1;
            int digitsStart = cursor;
            while (cursor < value.length() && Character.isDigit(value.charAt(cursor))) {
                cursor++;
            }
            if (cursor > digitsStart && cursor < value.length() && value.charAt(cursor) == '$') {
                explicitIndex = Integer.parseInt(value.substring(digitsStart, cursor)) - 1;
                cursor++;
            }
            if (cursor < value.length() && value.charAt(cursor) == 's') {
                int argumentIndex = explicitIndex >= 0 ? explicitIndex : nextIndex++;
                result.append('{').append(argumentIndex).append('}');
                index = cursor + 1;
            } else {
                result.append(value.charAt(index++));
            }
        }
        return result.toString();
    }

    private static @NotNull String nexoParametersToMinecraft(@NotNull String value) {
        StringBuilder result = new StringBuilder(value.length());
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (character == '{' && index + 1 < value.length() && value.charAt(index + 1) == '{') {
                result.append('{');
                index += 2;
                continue;
            }
            if (character == '}' && index + 1 < value.length() && value.charAt(index + 1) == '}') {
                result.append('}');
                index += 2;
                continue;
            }
            if (character != '{') {
                result.append(character);
                index++;
                continue;
            }
            int close = value.indexOf('}', index + 1);
            boolean number = close >= 0 && close > index + 1;
            for (int digit = index + 1; number && digit < close; digit++) {
                number = Character.isDigit(value.charAt(digit));
            }
            if (!number) {
                result.append(character);
                index++;
                continue;
            }
            // Minecraft consumes arguments sequentially; the Nexo index is intentionally discarded.
            result.append("%s");
            index = close + 1;
        }
        return result.toString();
    }

    private static @NotNull String stripUnsupportedTags(@NotNull String value) {
        StringBuilder result = new StringBuilder(value.length());
        int index = 0;
        while (index < value.length()) {
            if (value.charAt(index) == '\\' && index + 1 < value.length()
                    && (value.charAt(index + 1) == '[' || value.charAt(index + 1) == ']' || value.charAt(index + 1) == '\\')) {
                result.append(value, index, index + 2);
                index += 2;
                continue;
            }
            if (value.charAt(index) != '[') {
                result.append(value.charAt(index++));
                continue;
            }
            int close = value.indexOf(']', index + 1);
            if (close < 0) {
                result.append(value.substring(index));
                break;
            }
            String rawTag = value.substring(index + 1, close).trim();
            String tagName = rawTag.startsWith("/") ? rawTag.substring(1).trim() : rawTag;
            if (tagName.endsWith("/")) {
                tagName = tagName.substring(0, tagName.length() - 1).trim();
            }
            int separator = tagName.indexOf('=');
            if (separator >= 0) {
                tagName = tagName.substring(0, separator).trim();
            }
            tagName = tagName.toLowerCase(Locale.ROOT);
            if (switch (tagName) {
                case "b", "bold", "i", "italic", "u", "underline", "underlined",
                     "s", "strike", "strikethrough", "o", "obfuscated", "color",
                     "plain", "br", "newline", "reset" -> true;
                default -> false;
            }) {
                result.append(value, index, close + 1);
            }
            index = close + 1;
        }
        return result.toString();
    }

    private static void appendMinecraftStyle(@NotNull StringBuilder result, @NotNull Text.Style style) {
        if (style.color() != null) {
            String color = String.format(Locale.ROOT, "%06x", style.color() & 0xFFFFFF);
            result.append('§').append('x');
            for (int index = 0; index < color.length(); index++) {
                result.append('§').append(color.charAt(index));
            }
        }
        if (style.bold()) {
            result.append('§').append('l');
        }
        if (style.italic()) {
            result.append('§').append('o');
        }
        if (style.underlined()) {
            result.append('§').append('n');
        }
        if (style.strikethrough()) {
            result.append('§').append('m');
        }
        if (style.obfuscated()) {
            result.append('§').append('k');
        }
    }

    private static @Nullable String minecraftColor(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            default -> null;
        };
    }

    private static final class FormattingState {
        private String color;
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean strikethrough;
        private boolean obfuscated;

        private void reset() {
            color = null;
            bold = false;
            italic = false;
            underlined = false;
            strikethrough = false;
            obfuscated = false;
        }
    }

}

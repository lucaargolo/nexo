package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Text {

    public static final float DEFAULT_SIZE = 9.0F;

    private static final @NotNull Map<String, Integer> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", 0x000000),
            Map.entry("dark_blue", 0x0000AA),
            Map.entry("dark_green", 0x00AA00),
            Map.entry("dark_aqua", 0x00AAAA),
            Map.entry("dark_red", 0xAA0000),
            Map.entry("dark_purple", 0xAA00AA),
            Map.entry("gold", 0xFFAA00),
            Map.entry("gray", 0xAAAAAA),
            Map.entry("dark_gray", 0x555555),
            Map.entry("blue", 0x5555FF),
            Map.entry("green", 0x55FF55),
            Map.entry("aqua", 0x55FFFF),
            Map.entry("red", 0xFF5555),
            Map.entry("light_purple", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55),
            Map.entry("white", 0xFFFFFF)
    );

    private final @NotNull String source;
    private final @NotNull List<Run> runs;
    private final @Nullable Translation translation;

    private Text(@NotNull String source, @NotNull List<Run> runs) {
        this(source, runs, null);
    }

    private Text(@NotNull String source, @NotNull List<Run> runs, @Nullable Translation translation) {
        this.source = source;
        this.runs = List.copyOf(runs);
        this.translation = translation;
    }

    public Text(@NotNull String text) {
        this(Objects.requireNonNull(text), List.of(new Run(text, Style.DEFAULT)));
    }

    public static @NotNull Text literal(@NotNull String text) {
        return new Text(text);
    }

    public static @NotNull Text translatable(@NotNull String key, Text @NotNull ... arguments) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(arguments, "arguments");
        return new Text(key, List.of(), new Translation(key, List.of(arguments)));
    }

    public @NotNull String source() {
        return source;
    }

    public @NotNull List<Run> runs() {
        return runs;
    }

    public boolean isTranslatable() {
        return translation != null;
    }

    public @NotNull Translation translation() {
        return Objects.requireNonNull(translation, "Text is not translatable");
    }

    public @NotNull String plainText() {
        if (translation != null) {
            return source;
        }
        StringBuilder result = new StringBuilder();
        for (Run run : runs) {
            result.append(run.text());
        }
        return result.toString();
    }

    public float maxSize() {
        float size = DEFAULT_SIZE;
        for (Run run : runs) {
            size = Math.max(size, run.style().size());
        }
        return size;
    }

    public boolean isEmpty() {
        return plainText().isEmpty();
    }

    public static @NotNull Text parse(@NotNull String bbcode) {
        Objects.requireNonNull(bbcode, "bbcode");
        List<Run> runs = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame("", Style.DEFAULT));

        int index = 0;
        while (index < bbcode.length()) {
            if (bbcode.charAt(index) == '\\' && index + 1 < bbcode.length()) {
                char escaped = bbcode.charAt(index + 1);
                if (escaped == '[' || escaped == ']' || escaped == '\\') {
                    content.append(escaped);
                    index += 2;
                    continue;
                }
            }
            if (bbcode.charAt(index) != '[') {
                content.append(bbcode.charAt(index++));
                continue;
            }
            int close = bbcode.indexOf(']', index + 1);
            if (close < 0) {
                content.append(bbcode.substring(index));
                break;
            }

            String rawTag = bbcode.substring(index + 1, close);
            Tag tag = Tag.parse(rawTag);
            if (tag == null) {
                content.append(bbcode, index, close + 1);
                index = close + 1;
                continue;
            }
            flush(runs, content, stack.peek().style());
            if (tag.name().equals("br") || tag.name().equals("newline")) {
                content.append('\n');
                index = close + 1;
                continue;
            }
            if (tag.name().equals("reset")) {
                stack.clear();
                stack.push(new Frame("", Style.DEFAULT));
                index = close + 1;
                continue;
            }
            if (tag.closing()) {
                if (stack.size() > 1 && stack.peek().name().equals(tag.name())) {
                    stack.pop();
                } else {
                    content.append(bbcode, index, close + 1);
                }
                index = close + 1;
                continue;
            }

            Style style = styleFor(tag, stack.peek().style());
            if (style == null) {
                content.append(bbcode, index, close + 1);
            } else {
                stack.push(new Frame(tag.name(), style));
            }
            index = close + 1;
        }
        flush(runs, content, stack.peek().style());
        return new Text(bbcode, runs);
    }

    private static void flush(@NotNull List<Run> runs, @NotNull StringBuilder content, @NotNull Style style) {
        if (content.length() > 0) {
            runs.add(new Run(content.toString(), style));
            content.setLength(0);
        }
    }

    private static @Nullable Style styleFor(@NotNull Tag tag, @NotNull Style parent) {
        return switch (tag.name()) {
            case "b", "bold" -> parent.withBold(true);
            case "i", "italic" -> parent.withItalic(true);
            case "u", "underline", "underlined" -> parent.withUnderlined(true);
            case "s", "strike", "strikethrough" -> parent.withStrikethrough(true);
            case "o", "obfuscated" -> parent.withObfuscated(true);
            case "color" -> {
                if (tag.value() == null) {
                    yield null;
                }
                Integer color = parseColor(tag.value());
                yield color == null ? null : parent.withColor(color);
            }
            case "font" -> {
                if (tag.value() == null) {
                    yield null;
                }
                String value = tag.value().trim();
                int separator = value.indexOf(':');
                yield separator <= 0 || separator == value.length() - 1
                        ? null
                        : parent.withFont(Location.of(value.substring(0, separator), value.substring(separator + 1)));
            }
            case "size" -> {
                if (tag.value() == null) {
                    yield null;
                }
                Float size = parseSize(tag.value());
                yield size == null ? null : parent.withSize(size);
            }
            case "plain" -> Style.DEFAULT;
            default -> null;
        };
    }

    private static @Nullable Integer parseColor(@NotNull String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        Integer named = NAMED_COLORS.get(normalized);
        if (named != null) {
            return named;
        }
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }
        if (normalized.length() == 3) {
            normalized = "" + normalized.charAt(0) + normalized.charAt(0)
                    + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2);
        }
        if (normalized.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @Nullable Float parseSize(@NotNull String value) {
        try {
            float size = Float.parseFloat(value.trim());
            return Float.isFinite(size) && size > 0.0F ? size : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record Translation(@NotNull String key, @NotNull List<@NotNull Text> arguments) {
        public Translation {
            Objects.requireNonNull(key, "key");
            arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        }
    }

    public record Run(@NotNull String text, @NotNull Style style) {
        public Run {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(style, "style");
        }
    }

    public record Style(
            @Nullable Location font,
            float size,
            @Nullable Integer color,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated
    ) {
        public static final @NotNull Style DEFAULT = new Style(null, DEFAULT_SIZE, null, false, false, false, false, false);

        public Style {
            if (!Float.isFinite(size) || size <= 0.0F) {
                throw new IllegalArgumentException("Font size must be positive and finite");
            }
        }

        private @NotNull Style withFont(@Nullable Location value) {
            return new Style(value, size, color, bold, italic, underlined, strikethrough, obfuscated);
        }

        private @NotNull Style withSize(@NotNull Float value) {
            return new Style(font, value, color, bold, italic, underlined, strikethrough, obfuscated);
        }

        private @NotNull Style withColor(@Nullable Integer value) {
            return new Style(font, size, value, bold, italic, underlined, strikethrough, obfuscated);
        }

        private @NotNull Style withBold(boolean value) {
            return new Style(font, size, color, value, italic, underlined, strikethrough, obfuscated);
        }

        private @NotNull Style withItalic(boolean value) {
            return new Style(font, size, color, bold, value, underlined, strikethrough, obfuscated);
        }

        private @NotNull Style withUnderlined(boolean value) {
            return new Style(font, size, color, bold, italic, value, strikethrough, obfuscated);
        }

        private @NotNull Style withStrikethrough(boolean value) {
            return new Style(font, size, color, bold, italic, underlined, value, obfuscated);
        }

        private @NotNull Style withObfuscated(boolean value) {
            return new Style(font, size, color, bold, italic, underlined, strikethrough, value);
        }
    }

    private record Frame(@NotNull String name, @NotNull Style style) {
    }

    private record Tag(@NotNull String name, boolean closing, @Nullable String value) {
        private static @Nullable Tag parse(@NotNull String source) {
            String value = source.trim();
            if (value.isEmpty()) {
                return null;
            }
            boolean closing = value.startsWith("/");
            if (closing) {
                value = value.substring(1).trim();
            }
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1).trim();
            }
            int separator = value.indexOf('=');
            String name = (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
            if (!isTagName(name)) {
                return null;
            }
            boolean acceptsValue = name.equals("color") || name.equals("font") || name.equals("size");
            if (closing) {
                return separator >= 0 ? null : new Tag(canonicalName(name), true, null);
            }
            if ((separator >= 0 && !acceptsValue) || (separator < 0 && acceptsValue)) {
                return null;
            }
            String argument = separator < 0 ? null : value.substring(separator + 1).trim();
            return new Tag(canonicalName(name), false, argument);
        }

        private static @NotNull String canonicalName(@NotNull String name) {
            return switch (name) {
                case "bold" -> "b";
                case "italic" -> "i";
                case "underline", "underlined" -> "u";
                case "strike", "strikethrough" -> "s";
                case "obfuscated" -> "o";
                default -> name;
            };
        }

        private static boolean isTagName(@NotNull String name) {
            return switch (name) {
                case "b", "bold", "i", "italic", "u", "underline", "underlined",
                     "s", "strike", "strikethrough", "o", "obfuscated", "color", "font",
                     "size", "plain", "br", "newline", "reset" -> true;
                default -> false;
            };
        }
    }

    @Override
    public @NotNull String toString() {
        return source;
    }
}

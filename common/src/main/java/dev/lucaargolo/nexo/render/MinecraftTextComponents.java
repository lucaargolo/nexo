package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftTextComponents {

    private static final @NotNull Location DEFAULT_NEXO_FONT = Location.of("nexo", "fonts/inter");
    private static final @NotNull Location DEFAULT_MINECRAFT_FONT = Location.of("minecraft", "default");

    private MinecraftTextComponents() {
    }

    public static @NotNull List<Text.Run> runs(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Text text) {
        if (!text.isTranslatable()) {
            return text.runs();
        }
        String translated = nexo.language().translate(text.translation().key());
        if (translated == null) {
            return List.of(new Text.Run(text.translation().key(), Text.Style.DEFAULT));
        }
        List<Text.Run> result = new ArrayList<>();
        appendTemplate(nexo, result, Text.parse(translated).runs(), text.translation().arguments());
        return List.copyOf(result);
    }

    public static @NotNull MutableComponent component(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Text text) {
        MutableComponent result = Component.empty();
        appendRuns(result, runs(nexo, text));
        return result;
    }

    public static @NotNull MutableComponent component(@NotNull String text, @NotNull Text.Style style) {
        return component(text, style, DEFAULT_NEXO_FONT);
    }

    public static @NotNull MutableComponent translated(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull String key,
            Object @NotNull [] arguments
    ) {
        String translated = nexo.language().translate(key);
        if (translated == null) {
            return Component.literal(key);
        }
        MutableComponent result = Component.empty();
        appendComponents(nexo, result, Text.parse(translated).runs(), arguments, DEFAULT_MINECRAFT_FONT);
        return result;
    }

    private static void appendTemplate(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull List<Text.Run> output,
            @NotNull List<Text.Run> template,
            @NotNull List<Text> arguments
    ) {
        for (Text.Run run : template) {
            appendRunsWithTextArguments(nexo, output, run.text(), run.style(), arguments);
        }
    }

    private static void appendRunsWithTextArguments(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull List<Text.Run> output,
            @NotNull String source,
            @NotNull Text.Style style,
            @NotNull List<Text> arguments
    ) {
        int cursor = 0;
        int literalStart = 0;
        while (cursor < source.length()) {
            if (source.charAt(cursor) != '{') {
                cursor++;
                continue;
            }
            if (cursor + 1 < source.length() && source.charAt(cursor + 1) == '{') {
                cursor += 2;
                continue;
            }
            int close = source.indexOf('}', cursor + 1);
            if (close < 0) {
                break;
            }
            int index = parseArgumentIndex(source, cursor + 1, close);
            if (index < 0) {
                cursor = close + 1;
                continue;
            }
            appendLiteral(output, source.substring(literalStart, cursor), style);
            if (index < arguments.size()) {
                output.addAll(withParentStyle(runs(nexo, arguments.get(index)), style));
            } else {
                appendLiteral(output, source.substring(cursor, close + 1), style);
            }
            cursor = close + 1;
            literalStart = cursor;
        }
        appendLiteral(output, unescapeBraces(source.substring(literalStart)), style);
    }

    private static void appendComponents(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull MutableComponent output,
            @NotNull List<Text.Run> template,
            Object @NotNull [] arguments,
            @NotNull Location defaultFont
    ) {
        for (Text.Run run : template) {
            appendComponents(nexo, output, run.text(), run.style(), arguments, defaultFont);
        }
    }

    private static void appendComponents(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull MutableComponent output,
            @NotNull String source,
            @NotNull Text.Style style,
            Object @NotNull [] arguments,
            @NotNull Location defaultFont
    ) {
        int cursor = 0;
        int literalStart = 0;
        while (cursor < source.length()) {
            if (source.charAt(cursor) != '{') {
                cursor++;
                continue;
            }
            if (cursor + 1 < source.length() && source.charAt(cursor + 1) == '{') {
                cursor += 2;
                continue;
            }
            int close = source.indexOf('}', cursor + 1);
            if (close < 0) {
                break;
            }
            int index = parseArgumentIndex(source, cursor + 1, close);
            if (index < 0) {
                cursor = close + 1;
                continue;
            }
            appendComponent(output, source.substring(literalStart, cursor), style, defaultFont);
            if (index < arguments.length && arguments[index] != null) {
                output.append(argumentComponent(nexo, arguments[index], style, defaultFont));
            } else {
                appendComponent(output, source.substring(cursor, close + 1), style, defaultFont);
            }
            cursor = close + 1;
            literalStart = cursor;
        }
        appendComponent(output, unescapeBraces(source.substring(literalStart)), style, defaultFont);
    }

    private static @NotNull Component argumentComponent(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Object argument,
            @NotNull Text.Style parent,
            @NotNull Location defaultFont
    ) {
        Component component;
        if (argument instanceof Text text) {
            component = component(nexo, text);
        } else if (argument instanceof Component value) {
            component = value;
        } else {
            component = Component.literal(String.valueOf(argument));
        }
        return component.copy().withStyle(component.getStyle().applyTo(minecraftStyle(parent, defaultFont)));
    }

    private static void appendRuns(
            @NotNull MutableComponent output,
            @NotNull List<Text.Run> runs
    ) {
        for (Text.Run run : runs) {
            appendComponent(output, run.text(), run.style(), DEFAULT_NEXO_FONT);
        }
    }

    private static void appendComponent(
            @NotNull MutableComponent output,
            @NotNull String text,
            @NotNull Text.Style style,
            @NotNull Location defaultFont
    ) {
        if (!text.isEmpty()) {
            output.append(component(text, style, defaultFont));
        }
    }

    private static @NotNull MutableComponent component(
            @NotNull String text,
            @NotNull Text.Style style,
            @NotNull Location defaultFont
    ) {
        return Component.literal(text).withStyle(minecraftStyle(style, defaultFont));
    }

    private static void appendLiteral(
            @NotNull List<Text.Run> output,
            @NotNull String text,
            @NotNull Text.Style style
    ) {
        if (!text.isEmpty()) {
            output.add(new Text.Run(text, style));
        }
    }

    private static @NotNull List<Text.Run> withParentStyle(
            @NotNull List<Text.Run> runs,
            @NotNull Text.Style parent
    ) {
        List<Text.Run> result = new ArrayList<>(runs.size());
        for (Text.Run run : runs) {
            Text.Style child = run.style();
            result.add(new Text.Run(run.text(), new Text.Style(
                    child.font() != null ? child.font() : parent.font(),
                    child.size() == Text.DEFAULT_SIZE ? parent.size() : child.size(),
                    child.color() != null ? child.color() : parent.color(),
                    parent.bold() || child.bold(),
                    parent.italic() || child.italic(),
                    parent.underlined() || child.underlined(),
                    parent.strikethrough() || child.strikethrough(),
                    parent.obfuscated() || child.obfuscated()
            )));
        }
        return result;
    }

    private static int parseArgumentIndex(@NotNull String source, int start, int end) {
        if (start >= end) {
            return -1;
        }
        int value = 0;
        for (int index = start; index < end; index++) {
            char character = source.charAt(index);
            if (character < '0' || character > '9') {
                return -1;
            }
            value = value * 10 + character - '0';
            if (value > 1000) {
                return -1;
            }
        }
        return value;
    }

    private static @NotNull String unescapeBraces(@NotNull String value) {
        return value.replace("{{", "{").replace("}}", "}");
    }

    private static @NotNull Style minecraftStyle(
            @NotNull Text.Style style,
            @NotNull Location defaultFont
    ) {
        ResourceLocation font = style.font() != null ? NexoMinecraft.rl(style.font()) : NexoMinecraft.rl(defaultFont);
        Style result = Style.EMPTY.withFont(font)
                .withBold(style.bold())
                .withItalic(style.italic())
                .withUnderlined(style.underlined())
                .withStrikethrough(style.strikethrough())
                .withObfuscated(style.obfuscated());
        if (style.color() != null) {
            result = result.withColor(TextColor.fromRgb(style.color()));
        }
        return result;
    }

}

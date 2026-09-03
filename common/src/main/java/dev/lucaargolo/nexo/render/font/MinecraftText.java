package dev.lucaargolo.nexo.render.font;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftText {

    private MinecraftText() {
    }

    public static @NotNull MutableComponent component(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Text text) {
        MutableComponent component = Component.empty();
        for(Text.Run run : runs(nexo, text)) {
            component.append(component(run.text(), run.style()));
        }
        return component;
    }


    public static @NotNull MutableComponent component(@NotNull String text, @NotNull Text.Style style) {
        return Component.literal(text).withStyle(minecraftStyle(style));
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
        List<Text> arguments = text.translation().arguments();
        for (Text.Run run : Text.parse(translated).runs()) {
            appendRunsWithTextArguments(nexo, result, run.text(), run.style(), arguments);
        }
        return List.copyOf(result);
    }

    private static void appendRunsWithTextArguments(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull List<Text.Run> output, @NotNull String source, @NotNull Text.Style style, @NotNull List<Text> arguments) {
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
            int index = close == cursor + 1 ? -1 : 0;
            for (int digit = cursor + 1; index >= 0 && digit < close; digit++) {
                char character = source.charAt(digit);
                if (character < '0' || character > '9') {
                    index = -1;
                    break;
                }
                index = index * 10 + character - '0';
                if (index > 1000) {
                    index = -1;
                }
            }
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
        appendLiteral(output, source.substring(literalStart).replace("{{", "{").replace("}}", "}"), style);
    }

    private static void appendLiteral(@NotNull List<Text.Run> output, @NotNull String text, @NotNull Text.Style style) {
        if (!text.isEmpty()) {
            output.add(new Text.Run(text, style));
        }
    }

    private static @NotNull List<Text.Run> withParentStyle(@NotNull List<Text.Run> runs, @NotNull Text.Style parent) {
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

    private static @NotNull Style minecraftStyle(@NotNull Text.Style style) {
        ResourceLocation font = style.font() != null ? NexoMinecraft.rl(style.font()) : NexoMinecraft.rl(Graphics2D.DEFAULT_FONT);
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

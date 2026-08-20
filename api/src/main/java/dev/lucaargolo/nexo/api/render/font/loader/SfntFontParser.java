package dev.lucaargolo.nexo.api.render.font.loader;

import dev.lucaargolo.nexo.api.render.font.Font;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

public final class SfntFontParser {

    private static final int DIRECTORY_HEADER_SIZE = 12;
    private static final int TABLE_RECORD_SIZE = 16;
    private static final int TRUE_TYPE_SIGNATURE = 0x00010000;
    private static final int APPLE_TRUE_SIGNATURE = 0x74727565;
    private static final int CFF_SIGNATURE = 0x4F54544F;
    private static final int CMAP_TAG = 0x636D6170;



    private SfntFontParser() {
    }

    public static @NotNull Font parse(byte @NotNull [] data, boolean trueTypeOnly) throws IOException {
        if (data.length < DIRECTORY_HEADER_SIZE) {
            throw new IOException("Font is too small to contain an sfnt header");
        }

        int signature = readInt(data, 0);
        if (signature != TRUE_TYPE_SIGNATURE && signature != APPLE_TRUE_SIGNATURE && signature != CFF_SIGNATURE) {
            throw new IOException("Unsupported sfnt signature: 0x" + Integer.toHexString(signature));
        }

        int tableCount = readUnsignedShort(data, 4);
        long directoryEnd = DIRECTORY_HEADER_SIZE + (long) tableCount * TABLE_RECORD_SIZE;
        if (directoryEnd > data.length) {
            throw new IOException("Font table directory exceeds the available data");
        }

        boolean glyf = false;
        boolean loca = false;
        boolean cff = false;
        int cmapOffset = -1;
        int cmapLength = 0;
        for (int index = 0; index < tableCount; index++) {
            int recordOffset = DIRECTORY_HEADER_SIZE + index * TABLE_RECORD_SIZE;
            int tag = readInt(data, recordOffset);
            long tableOffset = readUnsignedInt(data, recordOffset + 8);
            long tableLength = readUnsignedInt(data, recordOffset + 12);
            if (tableOffset > data.length || tableLength > data.length - tableOffset) {
                throw new IOException("Font table exceeds the available data");
            }

            glyf |= tag == 0x676C7966;
            loca |= tag == 0x6C6F6361;
            cff |= tag == 0x43464620 || tag == 0x43464632;
            if (tag == CMAP_TAG) {
                cmapOffset = Math.toIntExact(tableOffset);
                cmapLength = Math.toIntExact(tableLength);
            }
        }

        boolean trueTypeOutline = glyf && loca;
        if (!cff && !trueTypeOutline) {
            throw new IOException("Font has no supported TrueType or CFF outline tables");
        }
        if (signature == CFF_SIGNATURE && !cff) {
            throw new IOException("OTTO font has no CFF outline table");
        }
        if (signature != CFF_SIGNATURE && !trueTypeOutline) {
            throw new IOException("TrueType sfnt font has no glyf and loca tables");
        }
        if (trueTypeOnly && !trueTypeOutline) {
            throw new IOException("TTF resources must contain TrueType outlines");
        }
        if (cmapOffset < 0) {
            throw new IOException("Font has no Unicode character map");
        }

        int[] supportedGlyphs = parseCharacterMap(data, cmapOffset, cmapLength);
        if (supportedGlyphs.length == 0) {
            throw new IOException("Font Unicode character map contains no glyphs");
        }

        return new Font(data, supportedGlyphs, trueTypeOutline);
    }

    private static int @NotNull [] parseCharacterMap(byte @NotNull [] data, int cmapOffset, int cmapLength) throws IOException {
        requireRange(data, cmapOffset, cmapLength, "cmap table");
        if (cmapLength < 4 || readUnsignedShort(data, cmapOffset) != 0) {
            throw new IOException("Invalid cmap table header");
        }

        int recordCount = readUnsignedShort(data, cmapOffset + 2);
        if (4L + recordCount * 8L > cmapLength) {
            throw new IOException("cmap encoding records exceed the table");
        }

        int selectedOffset = -1;
        int selectedFormat = -1;
        int selectedScore = -1;
        for (int index = 0; index < recordCount; index++) {
            int recordOffset = cmapOffset + 4 + index * 8;
            int platform = readUnsignedShort(data, recordOffset);
            int encoding = readUnsignedShort(data, recordOffset + 2);
            long relativeOffset = readUnsignedInt(data, recordOffset + 4);
            if (relativeOffset > cmapLength - 2L) {
                continue;
            }

            int subtableOffset = cmapOffset + Math.toIntExact(relativeOffset);
            int format = readUnsignedShort(data, subtableOffset);
            int formatScore = formatScore(format);
            int encodingScore = encodingScore(platform, encoding);
            if (formatScore < 0 || encodingScore < 0) {
                continue;
            }
            int score = formatScore + encodingScore;
            if (score > selectedScore) {
                selectedOffset = subtableOffset;
                selectedFormat = format;
                selectedScore = score;
            }
        }

        return switch (selectedFormat) {
            case 0 -> parseFormat0(data, selectedOffset, cmapOffset + cmapLength);
            case 4 -> parseFormat4(data, selectedOffset, cmapOffset + cmapLength);
            case 6 -> parseFormat6(data, selectedOffset, cmapOffset + cmapLength);
            case 12 -> parseFormat12(data, selectedOffset, cmapOffset + cmapLength, false);
            case 13 -> parseFormat12(data, selectedOffset, cmapOffset + cmapLength, true);
            default -> throw new IOException("Font has no supported Unicode cmap subtable");
        };
    }

    private static int formatScore(int format) {
        return switch (format) {
            case 12 -> 50;
            case 13 -> 45;
            case 4 -> 40;
            case 6 -> 30;
            case 0 -> 20;
            default -> -1;
        };
    }

    private static int encodingScore(int platform, int encoding) {
        if (platform == 0) {
            return 20;
        }
        if (platform == 3 && encoding == 10) {
            return 15;
        }
        if (platform == 3 && encoding == 1) {
            return 10;
        }
        return -1;
    }

    private static int @NotNull [] parseFormat0(byte @NotNull [] data, int offset, int tableEnd) throws IOException {
        int length = readUnsignedShort(data, offset + 2);
        requireSubtable(data, offset, length, tableEnd, 262, "cmap format 0");
        IntArrayBuilder glyphs = new IntArrayBuilder();
        for (int codePoint = 0; codePoint < 256; codePoint++) {
            if ((data[offset + 6 + codePoint] & 0xFF) != 0) {
                glyphs.add(codePoint);
            }
        }
        return glyphs.toArray();
    }

    private static int @NotNull [] parseFormat6(byte @NotNull [] data, int offset, int tableEnd) throws IOException {
        int length = readUnsignedShort(data, offset + 2);
        requireSubtable(data, offset, length, tableEnd, 10, "cmap format 6");
        int firstCode = readUnsignedShort(data, offset + 6);
        int entryCount = readUnsignedShort(data, offset + 8);
        if (10L + entryCount * 2L > length) {
            throw new IOException("cmap format 6 glyph array exceeds the subtable");
        }

        IntArrayBuilder glyphs = new IntArrayBuilder();
        for (int index = 0; index < entryCount; index++) {
            if (readUnsignedShort(data, offset + 10 + index * 2) != 0) {
                glyphs.add(firstCode + index);
            }
        }
        return glyphs.toArray();
    }

    private static int @NotNull [] parseFormat4(byte @NotNull [] data, int offset, int tableEnd) throws IOException {
        int length = readUnsignedShort(data, offset + 2);
        requireSubtable(data, offset, length, tableEnd, 16, "cmap format 4");
        int segmentCountX2 = readUnsignedShort(data, offset + 6);
        if ((segmentCountX2 & 1) != 0 || segmentCountX2 == 0) {
            throw new IOException("Invalid cmap format 4 segment count");
        }

        int segmentCount = segmentCountX2 / 2;
        int endCodes = offset + 14;
        int startCodes = endCodes + segmentCount * 2 + 2;
        int deltas = startCodes + segmentCount * 2;
        int rangeOffsets = deltas + segmentCount * 2;
        if ((long) rangeOffsets + segmentCount * 2L > offset + length) {
            throw new IOException("cmap format 4 segments exceed the subtable");
        }

        IntArrayBuilder glyphs = new IntArrayBuilder();
        for (int segment = 0; segment < segmentCount; segment++) {
            int start = readUnsignedShort(data, startCodes + segment * 2);
            int end = readUnsignedShort(data, endCodes + segment * 2);
            if (start > end) {
                throw new IOException("Invalid cmap format 4 segment range");
            }

            int delta = readUnsignedShort(data, deltas + segment * 2);
            int rangeOffsetAddress = rangeOffsets + segment * 2;
            int rangeOffset = readUnsignedShort(data, rangeOffsetAddress);
            for (int codePoint = start; codePoint <= end && codePoint != 0xFFFF; codePoint++) {
                int glyphIndex;
                if (rangeOffset == 0) {
                    glyphIndex = (codePoint + delta) & 0xFFFF;
                } else {
                    int glyphAddress = rangeOffsetAddress + rangeOffset + (codePoint - start) * 2;
                    if (glyphAddress < offset || glyphAddress + 2 > offset + length) {
                        throw new IOException("cmap format 4 glyph index exceeds the subtable");
                    }
                    glyphIndex = readUnsignedShort(data, glyphAddress);
                    if (glyphIndex != 0) {
                        glyphIndex = (glyphIndex + delta) & 0xFFFF;
                    }
                }
                if (glyphIndex != 0) {
                    glyphs.add(codePoint);
                }
            }
        }
        return glyphs.toArray();
    }

    private static int @NotNull [] parseFormat12(
            byte @NotNull [] data,
            int offset,
            int tableEnd,
            boolean constantGlyph
    ) throws IOException {
        long length = readUnsignedInt(data, offset + 4);
        if (length > Integer.MAX_VALUE) {
            throw new IOException("cmap format 12/13 subtable is too large");
        }
        requireSubtable(data, offset, (int) length, tableEnd, 16, "cmap format 12/13");
        long groupCount = readUnsignedInt(data, offset + 12);
        if (groupCount > Integer.MAX_VALUE || 16L + groupCount * 12L > length) {
            throw new IOException("cmap format 12/13 groups exceed the subtable");
        }

        IntArrayBuilder glyphs = new IntArrayBuilder();
        for (int group = 0; group < (int) groupCount; group++) {
            int groupOffset = offset + 16 + group * 12;
            long start = readUnsignedInt(data, groupOffset);
            long end = readUnsignedInt(data, groupOffset + 4);
            long glyphIndex = readUnsignedInt(data, groupOffset + 8);
            if (start > end || end > Character.MAX_CODE_POINT) {
                throw new IOException("Invalid cmap format 12/13 group range");
            }

            for (int codePoint = (int) start; codePoint <= (int) end; codePoint++) {
                long mappedGlyph = constantGlyph ? glyphIndex : glyphIndex + codePoint - start;
                if (mappedGlyph != 0 && (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE)) {
                    glyphs.add(codePoint);
                }
            }
        }
        return glyphs.toArray();
    }

    private static void requireSubtable(
            byte @NotNull [] data,
            int offset,
            int length,
            int tableEnd,
            int minimumLength,
            @NotNull String name
    ) throws IOException {
        if (length < minimumLength || offset < 0 || offset > tableEnd || length > tableEnd - offset) {
            throw new IOException(name + " exceeds the cmap table");
        }
        requireRange(data, offset, length, name);
    }

    private static void requireRange(byte @NotNull [] data, int offset, int length, @NotNull String name) throws IOException {
        if (offset < 0 || length < 0 || offset > data.length || length > data.length - offset) {
            throw new IOException(name + " exceeds the available data");
        }
    }

    private static int readUnsignedShort(byte @NotNull [] data, int offset) {
        return (data[offset] & 0xFF) << 8 | (data[offset + 1] & 0xFF);
    }

    private static int readInt(byte @NotNull [] data, int offset) {
        return (data[offset] & 0xFF) << 24
                | (data[offset + 1] & 0xFF) << 16
                | (data[offset + 2] & 0xFF) << 8
                | (data[offset + 3] & 0xFF);
    }

    private static long readUnsignedInt(byte @NotNull [] data, int offset) {
        return Integer.toUnsignedLong(readInt(data, offset));
    }

    private static final class IntArrayBuilder {

        private int @NotNull [] values = new int[256];
        private int size;

        private void add(int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        private int @NotNull [] toArray() {
            return Arrays.copyOf(values, size);
        }

    }

}

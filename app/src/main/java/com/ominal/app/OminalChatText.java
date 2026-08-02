package com.ominal.app;

final class OminalChatText {

    private OminalChatText() {}

    static String forDisplay(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;

        String[] lines = value.split("\n", -1);
        StringBuilder output = new StringBuilder(value.length());
        char fenceCharacter = 0;
        int fenceLength = 0;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int marker = fenceMarker(line);
            if (marker != 0) {
                char character = (char) (marker >>> 16);
                int length = marker & 0xffff;
                if (fenceCharacter == 0) {
                    fenceCharacter = character;
                    fenceLength = length;
                } else if (character == fenceCharacter && length >= fenceLength) {
                    fenceCharacter = 0;
                    fenceLength = 0;
                }
                output.append(line);
            } else if (fenceCharacter != 0) {
                output.append(line);
            } else {
                output.append(decodeLine(line));
            }
            if (index + 1 < lines.length) output.append('\n');
        }
        return output.toString();
    }

    private static int fenceMarker(String line) {
        int offset = 0;
        while (offset < line.length() && offset < 3 && line.charAt(offset) == ' ') offset++;
        if (offset >= line.length()) return 0;
        char marker = line.charAt(offset);
        if (marker != '`' && marker != '~') return 0;
        int end = offset;
        while (end < line.length() && line.charAt(end) == marker) end++;
        int length = end - offset;
        return length >= 3 ? (marker << 16) | length : 0;
    }

    private static String decodeLine(String line) {
        StringBuilder output = new StringBuilder(line.length());
        int inlineCodeTicks = 0;
        for (int index = 0; index < line.length();) {
            char current = line.charAt(index);
            if (current == '`') {
                int end = index;
                while (end < line.length() && line.charAt(end) == '`') end++;
                int ticks = end - index;
                if (inlineCodeTicks == 0) inlineCodeTicks = ticks;
                else if (inlineCodeTicks == ticks) inlineCodeTicks = 0;
                output.append(line, index, end);
                index = end;
                continue;
            }

            if (inlineCodeTicks == 0 && current == '\\'
                && (index == 0 || line.charAt(index - 1) != '\\')) {
                int consumed = appendEscapedCodePoint(line, index, output);
                if (consumed > 0) {
                    index += consumed;
                    continue;
                }
            }

            output.append(current);
            index++;
        }
        return output.toString();
    }

    private static int appendEscapedCodePoint(String value, int offset, StringBuilder output) {
        if (offset + 6 <= value.length() && value.charAt(offset + 1) == 'u') {
            int first = parseHex(value, offset + 2, 4);
            if (first < 0) return 0;
            if (Character.isHighSurrogate((char) first)
                && offset + 12 <= value.length()
                && value.charAt(offset + 6) == '\\'
                && value.charAt(offset + 7) == 'u') {
                int second = parseHex(value, offset + 8, 4);
                if (second >= 0 && Character.isLowSurrogate((char) second)) {
                    output.append(Character.toChars(Character.toCodePoint(
                        (char) first, (char) second)));
                    return 12;
                }
            }
            output.append((char) first);
            return 6;
        }

        if (offset + 10 <= value.length() && value.charAt(offset + 1) == 'U') {
            int codePoint = parseHex(value, offset + 2, 8);
            if (Character.isValidCodePoint(codePoint)) {
                output.append(Character.toChars(codePoint));
                return 10;
            }
        }
        return 0;
    }

    private static int parseHex(String value, int offset, int length) {
        int result = 0;
        for (int index = 0; index < length; index++) {
            int digit = Character.digit(value.charAt(offset + index), 16);
            if (digit < 0) return -1;
            result = (result << 4) | digit;
        }
        return result;
    }
}

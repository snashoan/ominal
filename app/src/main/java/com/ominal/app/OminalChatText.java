package com.ominal.app;

import java.nio.charset.StandardCharsets;

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

            if (inlineCodeTicks == 0) {
                int consumed = appendRepairedUtf8(line, index, output);
                if (consumed > 0) {
                    index += consumed;
                    continue;
                }
            }

            output.append(current);
            index++;
        }
        return normalizeInlineMath(output.toString());
    }

    private static int appendRepairedUtf8(String value, int offset, StringBuilder output) {
        int first = legacyByte(value.charAt(offset));
        int length;
        if (first >= 0xc2 && first <= 0xdf) length = 2;
        else if (first >= 0xe0 && first <= 0xef) length = 3;
        else if (first >= 0xf0 && first <= 0xf4) length = 4;
        else return 0;
        if (offset + length > value.length()) return 0;

        byte[] encoded = new byte[length];
        encoded[0] = (byte) first;
        for (int index = 1; index < length; index++) {
            int continuation = legacyByte(value.charAt(offset + index));
            if (continuation < 0x80 || continuation > 0xbf) return 0;
            encoded[index] = (byte) continuation;
        }

        String decoded = new String(encoded, StandardCharsets.UTF_8);
        if (decoded.indexOf('\ufffd') >= 0 || decoded.codePointCount(0, decoded.length()) != 1)
            return 0;
        output.append(decoded);
        return length;
    }

    private static int legacyByte(char value) {
        if (value <= 0xff) return value;
        switch (value) {
            case '\u20ac': return 0x80;
            case '\u201a': return 0x82;
            case '\u0192': return 0x83;
            case '\u201e': return 0x84;
            case '\u2026': return 0x85;
            case '\u2020': return 0x86;
            case '\u2021': return 0x87;
            case '\u02c6': return 0x88;
            case '\u2030': return 0x89;
            case '\u0160': return 0x8a;
            case '\u2039': return 0x8b;
            case '\u0152': return 0x8c;
            case '\u017d': return 0x8e;
            case '\u2018': return 0x91;
            case '\u2019': return 0x92;
            case '\u201c': return 0x93;
            case '\u201d': return 0x94;
            case '\u2022': return 0x95;
            case '\u2013': return 0x96;
            case '\u2014': return 0x97;
            case '\u02dc': return 0x98;
            case '\u2122': return 0x99;
            case '\u0161': return 0x9a;
            case '\u203a': return 0x9b;
            case '\u0153': return 0x9c;
            case '\u017e': return 0x9e;
            case '\u0178': return 0x9f;
            default: return -1;
        }
    }

    private static String normalizeInlineMath(String line) {
        StringBuilder output = new StringBuilder(line.length() + 8);
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

            if (inlineCodeTicks == 0 && current == '$' && isSingleDollar(line, index)
                && !isEscaped(line, index) && index + 1 < line.length()
                && !Character.isWhitespace(line.charAt(index + 1))) {
                int close = inlineMathClose(line, index + 1);
                if (close > index + 1) {
                    String expression = line.substring(index + 1, close);
                    if (looksLikeMath(expression)) {
                        output.append("$$").append(expression).append("$$");
                        index = close + 1;
                        continue;
                    }
                }
            }

            output.append(current);
            index++;
        }
        return output.toString();
    }

    private static int inlineMathClose(String value, int offset) {
        for (int index = offset; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '`') return -1;
            if (current == '$' && isSingleDollar(value, index) && !isEscaped(value, index)
                && index > offset && !Character.isWhitespace(value.charAt(index - 1))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isSingleDollar(String value, int offset) {
        return (offset == 0 || value.charAt(offset - 1) != '$')
            && (offset + 1 >= value.length() || value.charAt(offset + 1) != '$');
    }

    private static boolean isEscaped(String value, int offset) {
        int slashes = 0;
        for (int index = offset - 1; index >= 0 && value.charAt(index) == '\\'; index--)
            slashes++;
        return (slashes & 1) != 0;
    }

    private static boolean looksLikeMath(String value) {
        if (value.isEmpty()) return false;
        boolean whitespace = false;
        boolean letterOrDigit = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current)) whitespace = true;
            if (Character.isLetterOrDigit(current)) letterOrDigit = true;
            if (current > 0x7f || "\\_^{}=<>[]()+-*/".indexOf(current) >= 0) return true;
        }
        return !whitespace && letterOrDigit && value.length() <= 24;
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

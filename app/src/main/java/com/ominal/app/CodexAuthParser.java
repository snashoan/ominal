package com.ominal.app;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CodexAuthParser {
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https://[^\\s\\u001B]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIBED_CODE_PATTERN = Pattern.compile(
        "(?i)(?:one-time code|device code|code)(?:\\s+is)?\\s*:?\\s*"
            + "([A-Z0-9]{4,}(?:-[A-Z0-9]{3,})+)");
    private static final Pattern STANDALONE_CODE_PATTERN = Pattern.compile(
        "\\b[A-Z0-9]{4,}-[A-Z0-9]{4,}\\b");
    private static final Pattern ANSI_PATTERN = Pattern.compile(
        "\\u001B\\[[;\\d]*[ -/]*[@-~]");

    private CodexAuthParser() {}

    static String findUrl(String output) {
        if (output == null || output.isEmpty()) return null;
        Matcher matcher = URL_PATTERN.matcher(output);
        if (!matcher.find()) return null;
        return matcher.group().replaceAll("[),.;]+$", "");
    }

    static String findDeviceCode(String output) {
        if (output == null || output.isEmpty()) return null;
        String clean = ANSI_PATTERN.matcher(output).replaceAll(" ");
        Matcher described = DESCRIBED_CODE_PATTERN.matcher(clean);
        if (described.find()) return described.group(1).toUpperCase(Locale.ROOT);
        Matcher standalone = STANDALONE_CODE_PATTERN.matcher(clean);
        return standalone.find() ? standalone.group().toUpperCase(Locale.ROOT) : null;
    }
}

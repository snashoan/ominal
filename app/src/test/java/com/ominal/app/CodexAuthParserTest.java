package com.ominal.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CodexAuthParserTest {
    @Test
    public void extractsDeviceUrlWithoutTrailingPunctuation() {
        assertEquals("https://auth.openai.com/codex/device",
            CodexAuthParser.findUrl("Open https://auth.openai.com/codex/device."));
    }

    @Test
    public void extractsDescribedDeviceCode() {
        assertEquals("ABCD-EFGH",
            CodexAuthParser.findDeviceCode("Enter this one-time code: abcd-efgh"));
    }

    @Test
    public void extractsAnsiWrappedStandaloneCode() {
        assertEquals("WXYZ-1234",
            CodexAuthParser.findDeviceCode("\u001B[1mWXYZ-1234\u001B[0m"));
    }

    @Test
    public void returnsNullWhenAuthDetailsAreAbsent() {
        assertNull(CodexAuthParser.findUrl("Waiting for sign-in"));
        assertNull(CodexAuthParser.findDeviceCode("Waiting for sign-in"));
    }
}

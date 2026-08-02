package com.ominal.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OminalChatTextTest {

    @Test
    public void decodesEscapedUnicodeForRenderedChat() {
        assertEquals("Ready • done", OminalChatText.forDisplay("Ready \\u2022 done"));
        assertEquals("Smile 😀", OminalChatText.forDisplay("Smile \\uD83D\\uDE00"));
        assertEquals("Rocket 🚀", OminalChatText.forDisplay("Rocket \\U0001F680"));
    }

    @Test
    public void preservesUnicodeEscapesInsideCode() {
        assertEquals("Use `\\u2022` here", OminalChatText.forDisplay("Use `\\u2022` here"));
        assertEquals("```java\nString value = \"\\u2022\";\n```",
            OminalChatText.forDisplay("```java\nString value = \"\\u2022\";\n```"));
    }

    @Test
    public void preservesEscapedAndInvalidSequences() {
        assertEquals("\\\\u2022", OminalChatText.forDisplay("\\\\u2022"));
        assertEquals("\\uZZZZ", OminalChatText.forDisplay("\\uZZZZ"));
    }
}

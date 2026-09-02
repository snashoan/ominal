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

    @Test
    public void repairsUtf8DecodedAsLegacyText() {
        assertEquals("Peter Grünwald — 2021–2024",
            OminalChatText.forDisplay("Peter GrÃ¼nwald â\u0080\u0094 2021â\u0080\u00932024"));
        assertEquals("Keep `GrÃ¼nwald` literal",
            OminalChatText.forDisplay("Keep `GrÃ¼nwald` literal"));
    }

    @Test
    public void adaptsStandardInlineMathForNativeRenderer() {
        assertEquals("Loss $$L \\sim N^{-\\alpha}$$ and $$M$$ bits",
            OminalChatText.forDisplay("Loss $L \\sim N^{-\\alpha}$ and $M$ bits"));
        assertEquals("$$\\boxed{x = 1}$$\nnext",
            OminalChatText.forDisplay("$$\\boxed{x = 1}$$\nnext"));
        assertEquals("Price $5 and $10; use `$M$` in code",
            OminalChatText.forDisplay("Price $5 and $10; use `$M$` in code"));
    }
}

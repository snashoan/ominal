package com.ominal.app;

import io.noties.prism4j.annotations.PrismBundle;

/** Lazily generated grammars for the code languages most often emitted in chat. */
@PrismBundle(
    include = {
        "c", "cpp", "csharp", "css", "dart", "go", "java", "javascript", "json",
        "kotlin", "latex", "makefile", "markup", "markdown", "python", "sql", "swift",
        "yaml"
    },
    grammarLocatorClassName = ".OminalPrismGrammarLocator"
)
final class OminalSyntaxBundle {
    private OminalSyntaxBundle() {
    }
}

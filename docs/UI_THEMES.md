# GIR custom themes

GIR shows immutable Light and Dark themes in the Appearance list. Intelligence
harnesses can create named themes in `/root/.ominal/themes` when the user requests
a custom interface. Valid named themes appear in a separate Custom section and
never modify either built-in theme.

## Commands

```sh
ominal-theme list
ominal-theme create custom-x "Custom X"
ominal-theme set custom-x color.canvas '#080808'
ominal-theme set custom-x surface.composerInput.radius 22
ominal-theme use custom-x
ominal-theme reset
```

`use`, `set` on the active theme, and `reset` emit a UI reload event. The active
harness and conversation stay alive while Android reconstructs the visual tree.
Selecting Light or Dark writes `default` to the active-theme file and preserves
all named theme files. Selecting a custom entry activates only that named file.
Missing or invalid active files resolve to the last selected built-in appearance.

## Theme files

Each named theme is a properties file such as
`/root/.ominal/themes/custom-x.properties`. The active file contains only the
selected theme ID. IDs use lowercase letters, numbers, `_`, and `-`, with a
maximum length of 32 characters.

Supported visual properties are generated in `custom.properties`. They cover
the application palette and every native surface used by chat, the drawer,
composer, controls, terminal blocks, and the display home.

Role icons can be replaced with a monochrome PNG or WebP relative to the theme
directory:

```properties
icon.chat-history=icons/chat-history.png
icon.screen=icons/screen.png
icon.account-and-settings=icons/account.png
icon.new-chat=icons/new-chat.png
icon.attach-file=icons/attach.png
icon.agent-controls=icons/controls.png
icon.send-message=icons/send.png
icon.chat=icons/chat.png
icon.back=icons/back.png
icon.home=icons/home.png
icon.open-windows=icons/windows.png
icon.keyboard=icons/keyboard.png
```

Icon files are confined to the theme directory, limited to PNG/WebP, 2 MiB,
and 1024 pixels per edge. GIR applies the active surface tint so icons remain
legible in both appearance modes.

## Boundary

Themes can change presentation, not behavior. Control meaning, accessibility
labels, consent prompts, authentication routing, and built-in theme files are
immutable. This gives the intelligence broad visual control without allowing a
theme to disguise or remove a security-sensitive action.

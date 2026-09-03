# Monopot runtime protocol

Monopot is the provider-neutral event canvas between GIR chat and a runtime
adapter. Android owns chat identity, ordering, rendering, and user consent. The
runtime owns provider integration and may implement it with a CLI, a local
process, a browser, or another transport.

The Android core must not contain provider DOM selectors, session cookies, or
authentication tokens.

Monopot is not a backend or a network service. The shipped transport is local
JSONL over process stdio. Every conversation also receives an append-only event
projection at `./.ominal/monopot/monopot.jsonl` inside its Linux workspace.

## Runtime contract

Before each turn, an adapter can read these device-resident inputs:

- `./.ominal/runtime.json`: the active conversation, display, permissions, and
  Monopot contract.
- `/root/.ominal/profile.json`: the provider-neutral user profile. This is the
  canonical on-device copy made available consistently to selected runtimes.
- `./.ominal/chats/archive.jsonl`: a protected snapshot of other non-incognito
  conversations. Use `gir-chats list`, `gir-chats search`, or `gir-chats show`
  to read it without loading unrelated history into every turn.
- The prompt and developer-instruction files passed on the adapter command line.

GIR does not replace a harness's authentication flow or alter its model behavior.
An adapter may pass the supplied profile to its provider as
normal model context; GIR itself does not upload or synchronize the profile.

## Envelope

Each JSONL event uses `protocol: "monopot/1"` and contains:

- `chatId`: stable GIR conversation identity.
- `turnId`: correlation identity for one user turn.
- `sequence`: monotonically increasing ordering within the turn.
- `harnessId`: selected intelligence harness.
- `transportId`: runtime adapter carrying this turn.
- `channel`, `state`, `summary`, `detail`: normalized event payload.
- `timestamp`: event creation time.

`transportId` is additive. Readers of older streams fall back to `harnessId`
when it is absent.

## Web-backed example

A user can install a runtime adapter named `web.chatgpt`. It receives the active
chat turn, drives the user's authenticated web session, and emits normalized
`message`, `operation`, `artifact`, `usage`, and `result` events. GIR sees
only Monopot events:

```json
{
  "protocol": "monopot/1",
  "chatId": "chat-42",
  "turnId": "chat-42-9",
  "sequence": 4,
  "harnessId": "chatgpt",
  "transportId": "web.chatgpt",
  "channel": "message",
  "state": "delta",
  "summary": "",
  "detail": {"delta": "The next part of the response"},
  "timestamp": 1786350000000
}
```

Provider-specific navigation, extraction, retry policy, and authentication stay
inside that adapter. Replacing the adapter does not change chat history or the
Android UI contract.

## Installable adapters

A runtime exposes a custom harness by registering a self-contained local package:

```text
gir-harness register ./manifest.json --icon ./icon.webp
```

The canonical package is stored at
`/root/.ominal/harness-registry/<harness>/manifest.json`. GIR observes this
directory and makes a valid adapter available without an app restart. Legacy
manifests under `/root/.ominal/harness-capabilities/` remain supported.

```json
{
  "schemaVersion": 1,
  "harness": "example",
  "binaryVersion": "1.0.0",
  "identity": {
    "name": "Example runtime",
    "publisher": "Example publisher",
    "provider": "example"
  },
  "transport": {
    "id": "example.monopot-stdio",
    "outputFormat": "monopot-jsonl",
    "adapterCommand": "gir-example-adapter"
  },
  "autonomy": {
    "enabledByDefault": false,
    "flag": ""
  },
  "models": [
    {"id": "example-model", "label": "Example model", "efforts": []}
  ],
  "commands": []
}
```

Optional PNG or WebP artwork can be declared without remote loading:

```json
{
  "presentation": {
    "icon": {
      "file": "icon.webp",
      "monochrome": "icon-monochrome.webp",
      "sha256": "<sha256-of-icon.webp>"
    }
  }
}
```

Artwork is filename constrained, size bounded, dimension checked, and decoded
as a bitmap. Missing or invalid artwork falls back to GIR's neutral runtime icon.

`adapterCommand` is a single executable basename resolved inside the Linux
runtime. Shell fragments and embedded arguments are rejected. For a turn, GIR
invokes it with this stable shape:

```text
gir-example-adapter turn \
  --protocol monopot/1 \
  --harness example \
  --workspace /root/workspace \
  --thread <saved-thread-or-empty> \
  --prompt-file <path> \
  --instructions-file <path> \
  --model <selected-model-or-empty> \
  --effort <selected-effort-or-empty>
```

The adapter writes one `monopot/1` JSON object per stdout line. It may emit
state, thread, message, operation, artifact, usage, input-request, trace, and
result channels. A terminal result uses `state: "complete"`, `"cancelled"`, or
`"error"` and places the final visible text in `detail.message`. Diagnostics go
to stderr. GIR supplies canonical chat IDs, turn IDs, ordering, timestamps, and
transport identity when it records the stream.

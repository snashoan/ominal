# Monopot runtime protocol

Monopot is the provider-neutral event canvas between Monolith chat and a runtime
adapter. Android owns chat identity, ordering, rendering, and user consent. The
runtime owns provider integration and may implement it with a CLI, a local
process, a browser, or another transport.

The Android core must not contain provider DOM selectors, session cookies, or
authentication tokens.

## Envelope

Each JSONL event uses `protocol: "monopot/1"` and contains:

- `chatId`: stable Monolith conversation identity.
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
`message`, `operation`, `artifact`, `usage`, and `result` events. Monolith sees
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

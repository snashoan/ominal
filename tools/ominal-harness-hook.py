#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path


BRIDGE_KEY = "monolith-chat-bridge"
EVENTS = ("PreToolUse", "PostToolUse", "PreInvocation", "PostInvocation", "Stop")


def command(event):
    return {
        "type": "command",
        "command": f"ominal-harness-hook {event}",
        "timeout": 10,
    }


def install():
    path = Path.home() / ".gemini" / "config" / "hooks.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        current = json.loads(path.read_text()) if path.is_file() else {}
    except (OSError, json.JSONDecodeError):
        current = {}
    if not isinstance(current, dict):
        current = {}
    current[BRIDGE_KEY] = {
        "PreToolUse": [{"matcher": "*", "hooks": [command("PreToolUse")]}],
        "PostToolUse": [{"matcher": "*", "hooks": [command("PostToolUse")]}],
        "PreInvocation": [command("PreInvocation")],
        "PostInvocation": [command("PostInvocation")],
        "Stop": [command("Stop")],
    }
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(current, indent=2) + "\n")
    temporary.replace(path)


def response(event):
    if event == "PreToolUse":
        return {"decision": "allow"}
    if event in ("PreInvocation", "PostInvocation"):
        return {"injectSteps": []}
    if event == "Stop":
        return {"decision": ""}
    return {}


def emit(event):
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, OSError):
        payload = {}
    event_file = os.environ.get("OMINAL_HARNESS_EVENT_FILE", "")
    if event_file:
        path = Path(event_file)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("a", encoding="utf-8") as output:
            output.write(json.dumps({"hook": event, "payload": payload}, separators=(",", ":")))
            output.write("\n")
    json.dump(response(event), sys.stdout, separators=(",", ":"))
    sys.stdout.write("\n")


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: ominal-harness-hook --install|EVENT")
    if sys.argv[1] == "--install":
        install()
        return
    if sys.argv[1] not in EVENTS:
        raise SystemExit(f"unsupported hook event: {sys.argv[1]}")
    emit(sys.argv[1])


if __name__ == "__main__":
    main()

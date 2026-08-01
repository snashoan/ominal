#!/usr/bin/python3
import json
import os
import re
import subprocess
import sys
import tempfile


SCHEMA_VERSION = 1
DISCOVERY_REVISION = 3
MAX_EVIDENCE_CHARS = 65536
SAFE_COMMAND = re.compile(r"^/[a-z][a-z0-9._-]{0,63}$")
SAFE_FLAG = re.compile(r"^--[A-Za-z0-9][A-Za-z0-9-]{0,63}$")
SAFE_VALUE = re.compile(r"^[a-z][a-z0-9._-]{0,31}$")
COMMAND_TYPES = {
    "command", "model", "effort", "agent", "account", "session", "plugin"
}


def run(command, timeout=30):
    try:
        completed = subprocess.run(
            command,
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout,
            check=False,
        )
        output = completed.stdout.strip()
        error = completed.stderr.strip()
        return completed.returncode, output, error
    except (OSError, subprocess.TimeoutExpired) as error:
        return 70, "", str(error)


def first_line(value):
    return value.splitlines()[0].strip() if value.strip() else ""


def extract_json(value):
    value = value.strip()
    if not value:
        raise ValueError("empty setup response")
    try:
        outer = json.loads(value)
    except json.JSONDecodeError:
        start = value.find("{")
        end = value.rfind("}")
        if start < 0 or end <= start:
            raise
        outer = json.loads(value[start:end + 1])
    if isinstance(outer, dict) and "result" in outer:
        outer = outer["result"]
    if isinstance(outer, str):
        outer = json.loads(outer)
    if not isinstance(outer, dict):
        raise ValueError("setup response is not an object")
    return outer


def safe_text(value, limit):
    return (
        isinstance(value, str)
        and 0 < len(value.strip()) <= limit
        and not any(ord(character) < 32 for character in value)
    )


def verified_flag(help_text, flag):
    return flag if SAFE_FLAG.fullmatch(flag) and flag in help_text else ""


def discover_efforts(help_text):
    match = re.search(r"(?m)^\s*--effort\b[^\n]*\(([^)\n]+)\)", help_text)
    if not match:
        return []
    efforts = []
    for value in match.group(1).split("|"):
        effort = value.strip().lower()
        if SAFE_VALUE.fullmatch(effort) and effort not in efforts:
            efforts.append(effort)
    return efforts[:16]


def command_type(name):
    if name in {"model", "models"}:
        return "model"
    if name in {"agent", "agents"}:
        return "agent"
    if name in {"plugin", "plugins"}:
        return "plugin"
    if name in {"login", "logout", "account", "auth"}:
        return "account"
    if name in {"session", "sessions", "conversation", "conversations"}:
        return "session"
    return "command"


def discover_commands(help_text):
    commands = []
    in_subcommands = False
    for line in help_text.splitlines():
        if line.strip().lower() == "available subcommands:":
            in_subcommands = True
            continue
        if not in_subcommands:
            continue
        match = re.match(r"^\s{2,}([a-z][a-z0-9._-]{0,63})(?:\s{2,}|\s*$)", line)
        if not match:
            if line.strip():
                in_subcommands = False
            continue
        native_name = match.group(1).lower()
        commands.append({
            "name": "/" + native_name,
            "type": command_type(native_name),
        })
    return commands[:256]


def validate_candidate(candidate, harness, version, help_text, model_evidence):
    discovered_efforts = discover_efforts(help_text)
    candidate_model_map = {}
    candidate_models = candidate.get("models", [])
    if isinstance(candidate_models, list):
        for model in candidate_models:
            if not isinstance(model, dict):
                continue
            model_id = model.get("id", "").strip()
            if safe_text(model_id, 160):
                candidate_model_map[model_id] = model

    models = []
    seen_models = set()
    for line in model_evidence.splitlines():
        model_id = line.strip()
        if not safe_text(model_id, 160) or any(character.isspace() for character in model_id):
            continue
        model = candidate_model_map.get(model_id, {})
        label = model.get("label", model_id).strip()
        if (
            not safe_text(label, 160)
            or model_id in seen_models
        ):
            continue
        efforts = []
        candidate_efforts = model.get("efforts", [])
        if not isinstance(candidate_efforts, list):
            candidate_efforts = []
        for effort in candidate_efforts:
            if safe_text(effort, 32) and effort not in efforts:
                efforts.append(effort)
        if not efforts:
            efforts.extend(discovered_efforts)
        models.append({"id": model_id, "label": label, "efforts": efforts[:16]})
        seen_models.add(model_id)
        if len(models) >= 128:
            break

    commands = discover_commands(help_text)
    seen_commands = {command["name"] for command in commands}
    candidate_commands = candidate.get("commands", [])
    if not isinstance(candidate_commands, list):
        candidate_commands = []
    for command in candidate_commands:
        if not isinstance(command, dict):
            continue
        name = command.get("name", "").strip().lower()
        command_type = command.get("type", "command").strip().lower()
        if (
            not SAFE_COMMAND.fullmatch(name)
            or command_type not in COMMAND_TYPES
            or name in seen_commands
        ):
            continue
        commands.append({"name": name, "type": command_type})
        seen_commands.add(name)
        if len(commands) >= 256:
            break

    output_format = "stream-json" if "stream-json" in help_text else "text"
    resume_flag = verified_flag(
        help_text,
        "--resume" if harness == "claude-code" else "--conversation",
    )
    model_flag = verified_flag(help_text, "--model")
    effort_flag = verified_flag(help_text, "--effort")
    autonomy_flag = verified_flag(help_text, "--dangerously-skip-permissions")
    return {
        "schemaVersion": SCHEMA_VERSION,
        "discoveryRevision": DISCOVERY_REVISION,
        "harness": harness,
        "binaryVersion": version,
        "transport": {
            "outputFormat": output_format,
            "resumeFlag": resume_flag,
            "modelFlag": model_flag,
            "effortFlag": effort_flag,
        },
        "autonomy": {
            "flag": autonomy_flag,
            "enabledByDefault": bool(autonomy_flag),
        },
        "models": models,
        "commands": commands,
    }


def setup_prompt(harness, version, help_text, models, agents):
    evidence = {
        "harness": harness,
        "binaryVersion": version,
        "help": help_text[:MAX_EVIDENCE_CHARS],
        "models": models[:MAX_EVIDENCE_CHARS],
        "agents": agents[:MAX_EVIDENCE_CHARS],
    }
    return """You are configuring your own chat integration inside Monolith.

Using only the attached CLI inspection evidence, return one JSON object with:
- schemaVersion: 1
- harness and binaryVersion copied exactly from the evidence
- models: objects with id, label, and supported effort strings
- commands: native slash commands with name and one type from
  command, model, effort, agent, account, session, or plugin

Do not include credentials, account identifiers, executable command templates,
Markdown, or commentary. Do not invent unsupported models or commands. Use an
empty array when a capability cannot be verified.

Evidence:
""" + json.dumps(evidence, ensure_ascii=True)


def setup_command(harness, prompt, help_text):
    if harness == "claude-code":
        command = [
            "claude",
            "--dangerously-skip-permissions",
            "-p",
            "--output-format",
            "json",
            prompt,
        ]
    else:
        command = ["agy", "--dangerously-skip-permissions"]
        if "--output-format" in help_text:
            command.extend(["--output-format", "json"])
        command.extend(["-p", prompt])
    return command


def resolve_native_command(manifest_path, requested):
    if not SAFE_COMMAND.fullmatch(requested):
        return
    try:
        with open(manifest_path, encoding="utf-8") as source:
            manifest = json.load(source)
    except (OSError, ValueError, TypeError):
        return
    for command in manifest.get("commands", []):
        if isinstance(command, dict) and command.get("name") == requested:
            print(requested[1:])
            return


def main():
    if len(sys.argv) == 4 and sys.argv[1] == "--resolve-command":
        resolve_native_command(sys.argv[2], sys.argv[3])
        return
    if len(sys.argv) != 3:
        raise SystemExit("usage: ominal-harness-discover HARNESS MANIFEST")
    harness, manifest_path = sys.argv[1:3]
    binaries = {"claude-code": "claude", "antigravity": "agy"}
    binary = binaries.get(harness)
    if not binary:
        raise SystemExit("unsupported harness")

    version_code, version_output, version_error = run([binary, "--version"])
    version = first_line(version_output or version_error)
    if version_code != 0 or not safe_text(version, 128):
        raise SystemExit("harness version unavailable")

    try:
        with open(manifest_path, "r", encoding="utf-8") as existing_file:
            existing = json.load(existing_file)
        if (
            existing.get("schemaVersion") == SCHEMA_VERSION
            and existing.get("discoveryRevision") == DISCOVERY_REVISION
            and existing.get("harness") == harness
            and existing.get("binaryVersion") == version
        ):
            print("cached")
            return
    except (OSError, ValueError, TypeError):
        pass

    help_code, help_output, help_error = run([binary, "--help"])
    help_text = help_output or help_error
    if help_code != 0 or not help_text:
        raise SystemExit("harness help unavailable")

    models = ""
    agents = ""
    if harness == "antigravity":
        _, models, _ = run([binary, "models"], timeout=60)
        _, agents, _ = run([binary, "agent"], timeout=60)

    prompt = setup_prompt(harness, version, help_text, models, agents)
    candidate = {}
    if harness != "antigravity":
        setup_code, setup_output, _ = run(
            setup_command(harness, prompt, help_text), timeout=300
        )
        if setup_code == 0:
            try:
                candidate = extract_json(setup_output)
            except (ValueError, TypeError, json.JSONDecodeError):
                candidate = {}
    manifest = validate_candidate(candidate, harness, version, help_text, models)
    directory = os.path.dirname(manifest_path)
    os.makedirs(directory, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix=".manifest-", dir=directory)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            json.dump(manifest, output, ensure_ascii=True, separators=(",", ":"))
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, manifest_path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    print("updated")


if __name__ == "__main__":
    main()

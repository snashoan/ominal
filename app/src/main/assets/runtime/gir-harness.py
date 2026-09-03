#!/usr/bin/env python3
"""Install and inspect self-contained local GIR harness packages."""

import argparse
import hashlib
import json
import os
import re
import shutil
import tempfile
from pathlib import Path


HARNESS_ID = re.compile(r"[a-z0-9][a-z0-9-]{0,63}\Z")
ADAPTER = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}\Z")
ICON_FILE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}\.(?:png|webp)\Z", re.I)
MAX_MANIFEST_BYTES = 256 * 1024
MAX_ICON_BYTES = 256 * 1024
REGISTRY = Path(os.environ.get("GIR_HARNESS_REGISTRY", "/root/.ominal/harness-registry"))


def load_manifest(path):
    if not path.is_file() or path.stat().st_size > MAX_MANIFEST_BYTES:
        raise SystemExit("Manifest is missing or too large.")
    try:
        manifest = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise SystemExit(f"Invalid manifest: {error}") from error
    harness = str(manifest.get("harness", ""))
    transport = manifest.get("transport") or {}
    adapter = str(transport.get("adapterCommand", ""))
    if manifest.get("schemaVersion") != 1 or not HARNESS_ID.fullmatch(harness):
        raise SystemExit("Manifest has an invalid schema or harness id.")
    if transport.get("outputFormat") != "monopot-jsonl" or not ADAPTER.fullmatch(adapter):
        raise SystemExit("Manifest must declare a safe monopot-jsonl adapter command.")
    return manifest


def checked_icon(path):
    if not path.is_file() or path.stat().st_size <= 0 or path.stat().st_size > MAX_ICON_BYTES:
        raise SystemExit(f"Icon is missing or too large: {path}")
    if not ICON_FILE.fullmatch(path.name):
        raise SystemExit("Harness icons must be PNG or WebP files with simple filenames.")
    return path.read_bytes()


def register(args):
    source = Path(args.manifest).expanduser().resolve()
    manifest = load_manifest(source)
    presentation = manifest.get("presentation")
    if not isinstance(presentation, dict):
        presentation = {}
    raw_icon = presentation.get("icon")
    icon_spec = raw_icon.copy() if isinstance(raw_icon, dict) else {}
    if args.icon:
        icon_spec["file"] = Path(args.icon).name
    if args.monochrome_icon:
        icon_spec["monochrome"] = Path(args.monochrome_icon).name

    assets = {}
    for field, explicit in (("file", args.icon), ("monochrome", args.monochrome_icon)):
        name = str(icon_spec.get(field, "")).strip()
        if not name:
            continue
        if not ICON_FILE.fullmatch(name):
            raise SystemExit(f"Invalid {field} icon filename.")
        icon_path = Path(explicit).expanduser().resolve() if explicit else source.parent / name
        assets[name] = checked_icon(icon_path)
    if icon_spec:
        if "file" in icon_spec and icon_spec["file"] in assets:
            icon_spec["sha256"] = hashlib.sha256(assets[icon_spec["file"]]).hexdigest()
        presentation["icon"] = icon_spec
        manifest["presentation"] = presentation

    REGISTRY.mkdir(parents=True, exist_ok=True)
    destination = REGISTRY / manifest["harness"]
    destination.mkdir(mode=0o700, parents=True, exist_ok=True)
    stage = Path(tempfile.mkdtemp(prefix=".gir-harness-", dir=REGISTRY))
    try:
        for name, data in assets.items():
            path = stage / name
            path.write_bytes(data)
            path.chmod(0o600)
        staged_manifest = stage / "manifest.json"
        staged_manifest.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        staged_manifest.chmod(0o600)
        for path in stage.iterdir():
            os.replace(path, destination / path.name)
    finally:
        shutil.rmtree(stage, ignore_errors=True)
    print(f"Registered {manifest['harness']} for GIR.")


def list_harnesses(_args):
    if not REGISTRY.is_dir():
        return
    for manifest_path in sorted(REGISTRY.glob("*/manifest.json")):
        try:
            manifest = load_manifest(manifest_path)
        except SystemExit:
            continue
        identity = manifest.get("identity") or {}
        print(f"{manifest['harness']}\t{identity.get('name', manifest['harness'])}")


def main():
    parser = argparse.ArgumentParser(prog="gir-harness")
    commands = parser.add_subparsers(dest="command", required=True)
    install = commands.add_parser("register", help="register a local Monopot adapter")
    install.add_argument("manifest")
    install.add_argument("--icon")
    install.add_argument("--monochrome-icon")
    install.set_defaults(run=register)
    listing = commands.add_parser("list", help="list registered local adapters")
    listing.set_defaults(run=list_harnesses)
    args = parser.parse_args()
    args.run(args)


if __name__ == "__main__":
    main()

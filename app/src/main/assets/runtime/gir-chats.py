#!/usr/bin/env python3
"""Read GIR's protected, provider-neutral conversation snapshot."""

import argparse
import json
import os
from pathlib import Path


def archive_path():
    configured = os.environ.get("GIR_CHAT_ARCHIVE", "").strip()
    return Path(configured) if configured else Path.cwd() / ".ominal/chats/archive.jsonl"


def conversations():
    path = archive_path()
    if not path.is_file():
        return []
    result = []
    with path.open(encoding="utf-8") as source:
        for line in source:
            try:
                item = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(item, dict) and item.get("id"):
                result.append(item)
    return result


def list_chats(_args):
    for chat in conversations():
        print(f"{chat['id']}\t{chat.get('title', 'Untitled')}\t{chat.get('updatedAt', 0)}")


def show_chat(args):
    chat = next((item for item in conversations() if item["id"] == args.chat_id), None)
    if chat is None:
        raise SystemExit("Chat not found in this snapshot.")
    print(f"# {chat.get('title', 'Untitled')}\n")
    for message in chat.get("messages", []):
        role = str(message.get("role", "message")).capitalize()
        text = str(message.get("text", "")).strip()
        if text:
            print(f"{role}: {text}\n")


def search_chats(args):
    query = args.query.casefold()
    for chat in conversations():
        for message in chat.get("messages", []):
            text = str(message.get("text", "")).strip()
            if query not in text.casefold() and query not in str(chat.get("title", "")).casefold():
                continue
            compact = " ".join(text.split())
            if len(compact) > 240:
                compact = compact[:237].rstrip() + "..."
            print(f"{chat['id']}\t{chat.get('title', 'Untitled')}\t{message.get('role', '')}\t{compact}")


def main():
    parser = argparse.ArgumentParser(prog="gir-chats")
    commands = parser.add_subparsers(dest="command", required=True)
    listing = commands.add_parser("list", help="list other conversations")
    listing.set_defaults(run=list_chats)
    show = commands.add_parser("show", help="read one conversation")
    show.add_argument("chat_id")
    show.set_defaults(run=show_chat)
    search = commands.add_parser("search", help="search other conversations")
    search.add_argument("query")
    search.set_defaults(run=search_chats)
    args = parser.parse_args()
    args.run(args)


if __name__ == "__main__":
    main()

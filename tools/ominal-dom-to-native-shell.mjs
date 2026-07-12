#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const args = parseArgs(process.argv.slice(2));
const inputPath = args.input || args._[0];
const outputPath = args.output || "build-logs/ominal-native-shell-tokens.json";
const markdownPath = args.markdown || "build-logs/ominal-native-shell.md";

if (!inputPath || args.help) {
  printHelp();
  process.exit(inputPath ? 0 : 2);
}

const html = fs.readFileSync(inputPath, "utf8");
const snapshot = analyzeDom(html);
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, JSON.stringify(snapshot, null, 2) + "\n");
fs.writeFileSync(markdownPath, renderMarkdown(snapshot));

console.log(`wrote ${outputPath}`);
console.log(`wrote ${markdownPath}`);

function analyzeDom(htmlText) {
  const cleaned = htmlText
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, "")
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, "")
    .replace(/<!--[\s\S]*?-->/g, "");

  const elements = [];
  const tagRegex = /<([a-zA-Z][\w:-]*)([^>]*)>/g;
  let match;
  while ((match = tagRegex.exec(cleaned)) !== null) {
    const tag = match[1].toLowerCase();
    if (tag.startsWith("!") || tag === "meta" || tag === "link") continue;
    elements.push({
      tag,
      attrs: parseAttrs(match[2] || ""),
      index: elements.length
    });
  }

  const regions = classifyRegions(elements);
  return {
    source: path.basename(inputPath),
    generatedAt: new Date().toISOString(),
    warning: "Semantic extraction only. Do not copy proprietary DOM, CSS, class names, or assets into Ominal.",
    counts: countTags(elements),
    regions,
    nativeShell: {
      appBar: buildAppBar(regions),
      navigation: buildNavigation(regions),
      conversation: buildConversation(regions),
      composer: buildComposer(regions),
      tools: buildTools(regions)
    }
  };
}

function parseAttrs(attrText) {
  const attrs = {};
  const attrRegex = /([:@\w.-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?/g;
  let match;
  while ((match = attrRegex.exec(attrText)) !== null) {
    const key = match[1].toLowerCase();
    attrs[key] = (match[2] ?? match[3] ?? match[4] ?? "").trim();
  }
  return attrs;
}

function classifyRegions(elements) {
  const regions = {
    sidebar: [],
    conversation: [],
    message: [],
    composer: [],
    send: [],
    attachment: [],
    modelPicker: [],
    tool: []
  };

  for (const el of elements) {
    const haystack = [
      el.tag,
      el.attrs.role,
      el.attrs["aria-label"],
      el.attrs["data-testid"],
      el.attrs.id,
      el.attrs.name,
      el.attrs.type
    ].filter(Boolean).join(" ").toLowerCase();

    const item = summarizeElement(el, haystack);
    if (/\b(nav|sidebar|history|conversation list|chat list)\b/.test(haystack)) regions.sidebar.push(item);
    if (/\b(main|conversation|thread|messages?)\b/.test(haystack)) regions.conversation.push(item);
    if (/\b(article|message|assistant|user)\b/.test(haystack)) regions.message.push(item);
    if (/\b(textarea|textbox|composer|prompt|input|message input)\b/.test(haystack)) regions.composer.push(item);
    if (/\b(send|submit)\b/.test(haystack)) regions.send.push(item);
    if (/\b(attach|upload|file|paperclip)\b/.test(haystack)) regions.attachment.push(item);
    if (/\b(model|picker|selector|switcher)\b/.test(haystack)) regions.modelPicker.push(item);
    if (/\b(tool|canvas|terminal|display|computer|workspace)\b/.test(haystack)) regions.tool.push(item);
  }

  for (const key of Object.keys(regions)) {
    regions[key] = dedupe(regions[key]).slice(0, 24);
  }
  return regions;
}

function summarizeElement(el, haystack) {
  return {
    tag: el.tag,
    role: el.attrs.role || "",
    aria: scrubLabel(el.attrs["aria-label"] || ""),
    testid: scrubLabel(el.attrs["data-testid"] || ""),
    signal: haystack.replace(/\s+/g, " ").slice(0, 160)
  };
}

function scrubLabel(value) {
  return value.replace(/\s+/g, " ").trim().slice(0, 80);
}

function dedupe(items) {
  const seen = new Set();
  const result = [];
  for (const item of items) {
    const key = `${item.tag}|${item.role}|${item.aria}|${item.testid}`;
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(item);
  }
  return result;
}

function countTags(elements) {
  const counts = {};
  for (const el of elements) counts[el.tag] = (counts[el.tag] || 0) + 1;
  return Object.fromEntries(Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 20));
}

function buildAppBar(regions) {
  return {
    android: "LinearLayout horizontal",
    slots: ["brand mark", "active chat title", "history trigger", "agent trigger"],
    evidence: regions.modelPicker.length ? "model picker signals present" : "default Ominal/Codex agent picker"
  };
}

function buildNavigation(regions) {
  return {
    android: "AlertDialog or modal drawer",
    slots: ["New chat", "chat history list"],
    evidence: regions.sidebar.length ? "sidebar/history signals present" : "no sidebar signals; keep compact Chats picker"
  };
}

function buildConversation(regions) {
  return {
    android: "ScrollView + vertical LinearLayout",
    slots: ["system bubble", "user bubble", "assistant bubble"],
    evidence: regions.message.length ? "message/article signals present" : "fallback to existing Ominal message bubbles"
  };
}

function buildComposer(regions) {
  return {
    android: "two-row composer on phone, single row on wide screens",
    slots: ["File", "Shell", "View", "prompt input", "Send"],
    evidence: [
      regions.composer.length ? "textbox/composer signals present" : "no composer signals",
      regions.attachment.length ? "attachment signals present" : "Ominal attachment tool",
      regions.send.length ? "send/submit signals present" : "Ominal send button"
    ].join("; ")
  };
}

function buildTools(regions) {
  return {
    android: "full tool surface on phone, split pane on wide screens",
    slots: ["terminal workspace", "agent display"],
    evidence: regions.tool.length ? "tool/workspace signals present" : "Ominal-specific executable tools"
  };
}

function renderMarkdown(snapshot) {
  const shell = snapshot.nativeShell;
  return `# Ominal Native Shell Tokens

Source: ${snapshot.source}

${snapshot.warning}

## Native Mapping

- App bar: ${shell.appBar.android} -> ${shell.appBar.slots.join(", ")}
- Navigation: ${shell.navigation.android} -> ${shell.navigation.slots.join(", ")}
- Conversation: ${shell.conversation.android} -> ${shell.conversation.slots.join(", ")}
- Composer: ${shell.composer.android} -> ${shell.composer.slots.join(", ")}
- Tools: ${shell.tools.android} -> ${shell.tools.slots.join(", ")}

## Evidence

- App bar: ${shell.appBar.evidence}
- Navigation: ${shell.navigation.evidence}
- Conversation: ${shell.conversation.evidence}
- Composer: ${shell.composer.evidence}
- Tools: ${shell.tools.evidence}
`;
}

function parseArgs(argv) {
  const result = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (!arg.startsWith("--")) {
      result._.push(arg);
      continue;
    }
    const key = arg.slice(2);
    if (key === "help") {
      result.help = true;
      continue;
    }
    result[key] = argv[++i];
  }
  return result;
}

function printHelp() {
  console.log(`Usage: node tools/ominal-dom-to-native-shell.mjs <snapshot.html> [--output file.json] [--markdown file.md]

Analyze a saved chat webapp DOM snapshot and emit native Android shell tokens for Ominal.
Use only semantic structure. Do not copy proprietary DOM, CSS, class names, or assets.`);
}

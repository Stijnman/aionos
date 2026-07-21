# aionos

**Local AI-powered operating layer for Android** — natural language → on-device actions via Accessibility services.

> Thinks local · Always mobile · Stays private

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Privacy](https://img.shields.io/badge/inference-on--device-blue.svg)](#privacy)

## Vision

aionos turns a phone into an **autonomous agent** that understands everyday language and executes multi-step UI tasks **without sending screen contents to the cloud** (unless you explicitly enable a remote model).

## Architecture (target)

```
┌─────────────────────────────────────────┐
│  Voice / Text / Notification intents    │
└──────────────────┬──────────────────────┘
                   ▼
┌─────────────────────────────────────────┐
│  Planner (local LLM: Ollama / MediaPipe │
│  / on-device GGUF)                      │
└──────────────────┬──────────────────────┘
                   ▼
┌─────────────────────────────────────────┐
│  Action graph → AccessibilityService    │
│  (tap, scroll, type, open app, read)    │
└──────────────────┬──────────────────────┘
                   ▼
┌─────────────────────────────────────────┐
│  Audit log (on-device only)              │
└─────────────────────────────────────────┘
```

## Status

This repository currently holds the **public product definition, privacy model, and scaffold**. Application source (Kotlin/Compose) lands in `android/` as modules stabilize.

| Component | Status |
|-----------|--------|
| Product README & privacy model | ✅ |
| Repo scaffold | ✅ |
| Accessibility action layer | 🚧 planned |
| Local LLM bridge | 🚧 planned |
| Play/F-Droid packaging | ⏳ later |

## Privacy principles

1. **Local-first** — default inference on device or on your LAN (e.g. Ollama).
2. **No silent exfil** — screen text and credentials never leave the device without an explicit user toggle.
3. **Least privilege** — Accessibility is used only for user-initiated agent tasks; clear stop/kill switch.
4. **Transparent audit** — every action is logged locally for review.

## Planned permissions

| Permission | Why |
|------------|-----|
| Accessibility | UI automation for agent actions |
| Overlay (optional) | Floating status / confirmations |
| Microphone (optional) | Voice commands |
| Notifications | Intent intake / completion alerts |

## Development roadmap

1. Minimal Kotlin app + AccessibilityService hello-world
2. Intent parser (rules → LLM-assisted)
3. Safe action allowlist (open apps, scroll, type in focused fields)
4. Local LLM connector (Ollama HTTP on LAN / Termux)
5. Confirmation UX for destructive actions
6. F-Droid metadata

## Related projects

- [emotional-messaging-helper](https://github.com/Stijnman/emotional-messaging-helper) — on-device messaging companion
- [computer-use-linux](https://github.com/Stijnman/computer-use-linux) — desktop control patterns (inspiration)

## Contributing

Issues and design docs welcome. Prefer privacy-preserving patches over cloud convenience.

## License

MIT © 2026 Stijnman

# AionOS

Local AI operating layer for Android. Turn natural language (voice or text) into on-device actions through the Accessibility API. Private by default: all inference, parsing, and execution happens on the device; nothing leaves it unless you explicitly opt in.

> Status: 0.2.1 alpha. Under active development. Expect rough edges and breaking changes.

## Why

Most Android voice assistants ship your commands to a cloud model and execute whatever comes back. AionOS inverts that: the LLM only produces a typed, validated action plan, and a policy layer decides what is allowed to run. Even with a remote model enabled, the action itself is always executed locally under the same safety policy.

## How it works

```
User input (voice/text)
        ↓
Intent Parser → Local LLM (MediaPipe on-device, or Ollama on your LAN)
        ↓
Action Parser → typed action JSON
        ↓
ActionPolicy preflight (bounds, tiers, package allowlist)
        ↓
SafeActionExecutor — [Kill Switch] [Safety Tiers] [Stuck Detection] [Audit Log]
        ↓
Accessibility API (tap / scroll / type / open)
```

## Safety model

Every action is classified before execution. Policy is enforced in code, not by prompt.

| Tier | Actions | Behavior |
|------|---------|----------|
| TIER 1 | scroll, swipe, back, home, wait, read | Auto-execute |
| TIER 2 | tap, type (non-password), open app | Log + execute |
| TIER 3 | type (password), send, delete | Require explicit user confirmation |
| TIER 4 | install APK, grant permissions | **Blocked — cannot be executed at all** |

Additional guarantees:

- TIER_3 confirmation cannot be bypassed by any setting.
- Coordinates, text input, scroll requests, wait durations, and package names are bounded before any Accessibility call.
- Passwords and typed text are redacted from the audit log.
- Vision detections can only become tap proposals on interactive elements with an allowlisted label — never auto-executed.

## Modules

| Module | File | Purpose |
|--------|------|----------|
| Action Models | `action/ActionModels.kt` | Typed actions with safety tiers |
| Tree Parser | `parser/AccessibilityTreeParser.kt` | Accessibility tree → LLM text |
| Action Parser | `parser/ActionParser.kt` | LLM JSON → typed actions |
| Safe Executor | `action/SafeActionExecutor.kt` | Tiered safety + stuck detection |
| Audit Log | `audit/AuditLog.kt` | SQLite audit trail (on-device only) |
| Encrypted Prefs | `security/EncryptedPrefs.kt` | AES-256 encrypted settings |
| LLM Bridge | `llm/LLMBridge.kt` | Abstraction + prompt builders |
| MediaPipe | `llm/MediaPipeBridge.kt` | On-device LLM inference (TFLite) |
| Ollama | `llm/OllamaBridge.kt` | LAN LLM via OpenAI-compatible API |
| Voice Input | `voice/VoiceInputManager.kt` | Vosk on-device STT |
| Plugin Loader | `plugin/PluginLoader.kt` | APK plugin system (manifest-validated) |
| Vision Fallback | `vision/VisionFallback.kt` | MediaPipe object detection → proposals only |
| Overlay Bubble | `service/OverlayBubbleService.kt` | Floating quick-access bubble |
| Main Activity | `MainActivity.kt` | Compose UI (dashboard / audit / settings) |

## Build

Requires the Android SDK. From the repo root:

```bash
cd android
./gradlew assembleFdroidDebug   # F-Droid flavor
./gradlew assemblePlayDebug     # Play Store flavor
```

Validate locally (unit tests, lint, release build, instrumentation):

```bash
./gradlew testFdroidDebugUnitTest
./gradlew lintFdroidDebug
./gradlew assembleFdroidDebug
./gradlew assembleFdroidRelease
./gradlew connectedFdroidDebugAndroidTest
```

The GitHub Actions workflow is the authoritative verification environment — it runs the same pipeline on an emulator.

## Setup on device

1. Install the APK, then enable the AionOS Accessibility Service in system settings.
2. Optional: download a Vosk model for offline voice input (in-app, opt-in).
3. Optional: run Ollama on your LAN and point AionOS at it for stronger models.
4. Optional: download a MediaPipe TFLite model for fully offline inference.

## Privacy posture

- Audit logs are SQLite, on-device, exportable via the Storage Access Framework.
- OpenRouter (the only remote provider) is disabled by default. Enabling it requires entering a key (stored in encrypted preferences) and a separate explicit consent toggle. Prompts sent to it are not anonymized — treat enabling it as disclosure of the selected prompt data.
- Screen capture for vision analysis requires explicit MediaProjection consent; frames are analyzed locally and recycled after one-shot analysis.
- Cleartext Ollama endpoints are validated local-only; embedded credentials in endpoints are rejected.

## CI/CD

| Workflow | Trigger | Purpose |
|----------|---------|----------|
| Build | Push to `main`/`develop`, PRs | Compiles app, catches errors |
| Test | Push to `main`/`develop`, PRs | Unit tests + lint |
| Release | Tags `v*`, manual | Release APK/AAB + GitHub Release |

Tag a release:

```bash
git tag v0.2.1-alpha
git push origin v0.2.1-alpha
```

Setup details: [.github/CI-CD-SETUP.md](.github/CI-CD-SETUP.md)

## Roadmap

- Finish the draft multi-feature PR (SAF export, multi-language voice, widget, shortcuts)
- Dependency modernization (Gradle 9, Vosk 0.3.75 — see open dependabot PRs)
- Broader action vocabulary and per-app policies

## License

MIT

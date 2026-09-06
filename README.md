# AionOS Android

Local AI-powered operating layer for Android. Natural language to on-device actions via Accessibility services. Private by default.

## Architecture

```
User Input (Voice/Text) → Intent Parser → Local LLM (MediaPipe/Ollama)
                                               ↓
                     Action Executor ← Action Parser (JSON)
                            ↓
               [Kill Switch] [Safety Tiers] [Stuck Detection] [Audit Log]
                            ↓
                    Accessibility API (tap/scroll/type/open)
```

## Modules

| Module | File | Purpose |
|--------|------|----------|
| **Action Models** | `action/ActionModels.kt` | Typed actions with safety tiers |
| **Tree Parser** | `parser/AccessibilityTreeParser.kt` | Accessibility tree → LLM text |
| **Action Parser** | `parser/ActionParser.kt` | LLM JSON → typed actions |
| **Safe Executor** | `action/SafeActionExecutor.kt` | Tiered safety + stuck detection |
| **Audit Log** | `audit/AuditLog.kt` | SQLite audit trail (on-device only) |
| **Encrypted Prefs** | `security/EncryptedPrefs.kt` | AES-256 encrypted settings |
| **LLM Bridge** | `llm/LLMBridge.kt` | Abstraction + prompt builders |
| **MediaPipe** | `llm/MediaPipeBridge.kt` | On-device LLM inference (TensorFlow Lite) |
| **Ollama** | `llm/OllamaBridge.kt` | LAN LLM via OpenAI-compatible API |
| **Voice Input** | `voice/VoiceInputManager.kt` | Vosk on-device STT |
| **Plugin Loader** | `plugin/PluginLoader.kt` | APK plugin system |
| **Vision Fallback** | `vision/VisionFallback.kt` | MediaPipe object detection |
| **Overlay Bubble** | `service/OverlayBubbleService.kt` | Floating quick-access bubble |
| **Main Activity** | `MainActivity.kt` | Compose UI (dashboard/audit/settings) |

## Safety Model

| Tier | Actions | Behavior |
|------|---------|----------|
| TIER 1 | scroll, swipe, back, home, wait, read | Auto-execute |
| TIER 2 | tap, type (non-password), open app | Log + execute |
| TIER 3 | type (password), send, delete | Require confirmation |
| TIER 4 | install APK, grant permissions | **Blocked** |

## Building

```bash
cd android
./gradlew assembleFdroidDebug   # F-Droid flavor
./gradlew assemblePlayDebug     # Play Store flavor
```

## Setup

1. Enable Accessibility Service for AionOS in system settings
2. (Optional) Download a Vosk model for voice input
3. (Optional) Run Ollama on your LAN for stronger LLMs
4. (Optional) Download a MediaPipe TFLite model for on-device inference

## Dependencies

- Kotlin 1.9+
- Jetpack Compose
- Ktor (Ollama client)
- EncryptedSharedPreferences
- Vosk (optional, voice)
- MediaPipe Tasks (optional, on-device LLM + vision)

## CI/CD Pipeline

Automated workflows for building, testing, and releasing:

### Workflows

| Workflow | Trigger | Purpose |
|----------|---------|----------|
| **Build** | Push to `main`/`develop`, PRs | Compiles app, catches errors |
| **Test** | Push to `main`/`develop`, PRs | Unit tests + lint checks |
| **Release** | Tags `v*`, manual trigger | Release APK/AAB + GitHub Release |

### Quick Start

**View workflows:** Go to [Actions](https://github.com/Stijnman/aionos/actions) tab

**Create a release:**
```bash
git tag v0.2.1-alpha
git push origin v0.2.1-alpha
```

**Documentation:** See [.github/CI-CD-SETUP.md](.github/CI-CD-SETUP.md)

## License

MIT

## Implemented feature set

The current alpha includes the original continuation tasks plus remediation work for orchestration outcomes, remote-provider opt-in, privacy-safe action reporting, accessibility-node cleanup, bounded vision proposals, plugin manifest validation, model-download integrity, and service setup notifications. Screen capture requires explicit MediaProjection consent; captured frames are analyzed locally and recycled after one-shot vision analysis. Audit logs can be exported through the Storage Access Framework.

The application remains local-first. OpenRouter is disabled unless the user selects it, stores a key in encrypted preferences, and explicitly enables remote-provider consent. The app does not claim that prompts are anonymized; callers must treat remote-provider transmission as disclosure of the selected prompt data.

The project includes parser, action-policy, network-policy, vision, and Android instrumentation smoke-test coverage. The F-Droid workflow runs unit tests, lint, debug and release APK assembly, and an emulator smoke test.

### Validation

From `android/`, run:

```bash
./gradlew testFdroidDebugUnitTest
./gradlew lintFdroidDebug
./gradlew assembleFdroidDebug
./gradlew assembleFdroidRelease
./gradlew connectedFdroidDebugAndroidTest
```

Android SDK components must be installed locally or supplied by the CI runner. The Vosk model remains opt-in and is downloaded only after the user taps the download button in protected settings.

## 0.2.1 alpha remediation improvements

The 0.2.1 alpha adds a centralized `ActionPolicy` preflight layer. It rejects TIER_4 actions and bounds coordinates, text input, scroll requests, wait durations, and Android package names before any Accessibility API call. TIER_3 actions always require confirmation; the setting cannot bypass that boundary. Password and typed-text contents are redacted from audit details. Ollama endpoints are validated before encrypted persistence, and embedded credentials are rejected.

Vision fallback now has a confidence-gated proposal layer and an interactive-label allowlist; generic object detections cannot become taps. Proposals are reported to the user and are not executed automatically. Plugin dispatch requires an explicitly selected loaded plugin, a matching manifest package, a declared action, bounded parameter names, and bounded parameter values. Release packaging includes the required service metadata, model asset, XML theme, and icon resources.

The remediation also fixes fail-fast orchestration, single-collector voice lifecycle management, accessibility-node recycling, non-sensitive typed-input result messages, local-only cleartext Ollama validation, encrypted OpenRouter consent settings, atomic/checksummable Vosk installation, first-run setup notification delivery, and Android instrumentation scaffolding. Full APK, unit-test, lint, and emulator execution still requires an Android SDK; the GitHub Actions workflow is the authoritative verification environment.

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
|--------|------|---------|
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
|----------|---------|---------|
| **Build** | Push to `main`/`develop`, PRs | Compiles app, catches errors |
| **Test** | Push to `main`/`develop`, PRs | Unit tests + lint checks |
| **Release** | Tags `v*`, manual trigger | Release APK/AAB + GitHub Release |

### Quick Start

**View workflows:** Go to [Actions](https://github.com/Stijnman/aionos/actions) tab

**Create a release:**
```bash
git tag v0.1.0
git push origin v0.1.0
```

**Documentation:** See [.github/CI-CD-SETUP.md](.github/CI-CD-SETUP.md)

## License

MIT

## Implemented feature set

The current alpha includes all eight continuation tasks. Screen capture uses an explicit MediaProjection consent flow and retains the captured bitmap in memory only. Audit logs can be exported as [...]

The project also includes deterministic unit coverage for `ActionParser` and the executor's TIER_4 policy predicate, plus a GitHub Actions workflow at `.github/workflows/android-fdroid.yml` that[...]

### Validation

From `android/`, run:

```bash
./gradlew testFdroidDebugUnitTest
./gradlew lintFdroidDebug
./gradlew assembleFdroidDebug
```

Android SDK components must be installed locally or supplied by the CI runner. The Vosk model remains opt-in and is downloaded only after the user taps the download button in protected settings.

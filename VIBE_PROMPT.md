# AionOS Vibe Prompt
# Copy-paste this into your AI coding assistant to continue the project

You are working on **AionOS** — a local-first AI operating layer for Android that turns natural language into UI actions via Accessibility API. Privacy by default. No screen data leaves the device.

## Architecture ( memorize this )

```
User Input (Voice/Text) 
    → AgentOrchestrator.executeIntent()
    → AgentAccessibilityService.getCurrentTree() 
    → LLMBridge.generate(systemPrompt + uiTree + intent)
    → ActionParser.parse(JSON) → validate()
    → SafeActionExecutor.execute() 
    → [Kill Switch] [Safety Tiers] [Stuck Detection] [Audit Log]
    → Accessibility API (tap/scroll/type/open)
```

## Key Conventions

1. **Package structure**: `com.aionos.{module}` — action, agent, audit, llm, parser, plugin, security, service, ui, ui/theme, vision, voice
2. **Safety tiers**: TIER_1 (auto), TIER_2 (log+exec), TIER_3 (confirm), TIER_4 (blocked). NEVER bypass.
3. **LLM output**: Strict JSON array of actions. No markdown. Example: `[{"action":"tap","x":540,"y":1200}]`
4. **On-device first**: Default provider is Ollama on LAN. MediaPipe is experimental opt-in.
5. **All actions audited**: SQLite `AuditLog`. Encrypted prefs via `EncryptedSharedPreferences`.
6. **Kill switch**: `prefs.isAgentEnabled` checked every execution. Emergency stop resets everything.
7. **Stuck-loop detection**: Same action class + same tree hash + <5s gap = blocked after 3 repeats.

## Tech Stack
- Kotlin 1.9, Jetpack Compose (Material3), minSdk 26
- Ktor client for Ollama HTTP
- kotlinx.serialization for JSON
- EncryptedSharedPreferences (AES-256)
- Optional: Vosk (voice), MediaPipe (on-device LLM + vision)

## Current State
- 35 source files across 12 modules
- Core: AccessibilityService, SafeActionExecutor, TreeParser, ActionParser
- LLM: OllamaBridge (OpenAI-compatible), MediaPipeBridge (TFLite)
- UI: Compose dashboard with command input, voice button, transcript, audit log, settings
- Services: OverlayBubbleService (foreground), AgentAccessibilityService
- Extensions: VoiceInputManager, PluginLoader, VisionFallback

## When Writing Code
- Use `Result<T>` for operation outcomes
- Suspend functions for async
- `withContext(Dispatchers.IO)` for disk/network, `Dispatchers.Main` for UI
- Recycle `AccessibilityNodeInfo` nodes when obtained via `findNode*`
- All destructive actions need confirmation dialog flow
- Never hardcode package names — use constants or discovery

## Next Tasks (pick one)
1. Add screenshot capture via MediaProjection for VisionFallback
2. Implement Storage Access Framework for audit log export
3. Add Vosk model download manager (auto-download on first run)
4. Write unit tests for ActionParser and SafeActionExecutor
5. Add plugin execution UI (scan, list, trigger)
6. Implement biometric lock for settings access
7. Add CI/CD GitHub Actions for F-Droid builds
8. Create onboarding flow for first-time users

## File Locations
```
android/app/src/main/java/com/aionos/
  ├── MainActivity.kt, AionosApplication.kt
  ├── action/      → ActionModels.kt, SafeActionExecutor.kt
  ├── agent/       → AgentOrchestrator.kt
  ├── audit/       → AuditLog.kt
  ├── llm/         → LLMBridge.kt, MediaPipeBridge.kt, OllamaBridge.kt
  ├── parser/      → AccessibilityTreeParser.kt, ActionParser.kt
  ├── plugin/      → PluginLoader.kt
  ├── security/    → EncryptedPrefs.kt
  ├── service/     → AgentAccessibilityService.kt, OverlayBubbleService.kt
  ├── ui/          → AgentViewModel.kt, ConfirmationDialog.kt
  ├── ui/theme/    → Color.kt, Theme.kt, Type.kt
  ├── vision/      → VisionFallback.kt
  └── voice/       → VoiceInputManager.kt
```

Ask me what to build next.

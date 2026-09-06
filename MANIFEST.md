# AionOS Android — Current Repository Manifest
# Generated from 84 tracked-or-source files; source inventory below.
# Regenerate after material source-tree changes.

## Current Android source and resource files

| File | Bytes |
|---|---:|
| `.github/CI-CD-SETUP.md` | 4,572 |
| `.github/workflows/android-fdroid.yml` | 1,237 |
| `.github/workflows/build.yml` | 808 |
| `.github/workflows/release.yml` | 1,550 |
| `.github/workflows/test.yml` | 1,786 |
| `MANIFEST.md` | 3,431 |
| `QUICKSTART.md` | 1,479 |
| `README.md` | 5,353 |
| `VIBE_PROMPT.md` | 3,814 |
| `android/README.md` | 248 |
| `android/app/build.gradle.kts` | 3,394 |
| `android/app/src/androidTest/java/com/aionos/AppSmokeTest.kt` | 466 |
| `android/app/src/main/AndroidManifest.xml` | 2,310 |
| `android/app/src/main/assets/efficientdet-lite0.tflite` | 4,602,795 |
| `android/app/src/main/java/com/aionos/AionosApplication.kt` | 240 |
| `android/app/src/main/java/com/aionos/MainActivity.kt` | 27,652 |
| `android/app/src/main/java/com/aionos/action/ActionModels.kt` | 2,993 |
| `android/app/src/main/java/com/aionos/action/SafeActionExecutor.kt` | 11,667 |
| `android/app/src/main/java/com/aionos/agent/AgentOrchestrator.kt` | 8,958 |
| `android/app/src/main/java/com/aionos/audit/AuditLog.kt` | 7,094 |
| `android/app/src/main/java/com/aionos/audit/AuditLogExporter.kt` | 694 |
| `android/app/src/main/java/com/aionos/llm/LLMBridge.kt` | 2,410 |
| `android/app/src/main/java/com/aionos/llm/MediaPipeBridge.kt` | 2,382 |
| `android/app/src/main/java/com/aionos/llm/OllamaBridge.kt` | 3,405 |
| `android/app/src/main/java/com/aionos/llm/OpenRouterBridge.kt` | 2,219 |
| `android/app/src/main/java/com/aionos/parser/AccessibilityTreeParser.kt` | 4,119 |
| `android/app/src/main/java/com/aionos/parser/ActionParser.kt` | 3,619 |
| `android/app/src/main/java/com/aionos/plugin/PluginLoader.kt` | 3,947 |
| `android/app/src/main/java/com/aionos/security/ActionPolicy.kt` | 1,597 |
| `android/app/src/main/java/com/aionos/security/BiometricGate.kt` | 1,489 |
| `android/app/src/main/java/com/aionos/security/EncryptedPrefs.kt` | 4,737 |
| `android/app/src/main/java/com/aionos/security/NetworkPolicy.kt` | 1,429 |
| `android/app/src/main/java/com/aionos/service/AgentAccessibilityService.kt` | 4,674 |
| `android/app/src/main/java/com/aionos/service/OverlayBubbleService.kt` | 5,383 |
| `android/app/src/main/java/com/aionos/ui/AgentViewModel.kt` | 4,147 |
| `android/app/src/main/java/com/aionos/ui/ConfirmationDialog.kt` | 2,335 |
| `android/app/src/main/java/com/aionos/ui/Onboarding.kt` | 1,487 |
| `android/app/src/main/java/com/aionos/ui/theme/Color.kt` | 274 |
| `android/app/src/main/java/com/aionos/ui/theme/Theme.kt` | 1,561 |
| `android/app/src/main/java/com/aionos/ui/theme/Type.kt` | 478 |
| `android/app/src/main/java/com/aionos/vision/ScreenCaptureManager.kt` | 3,663 |
| `android/app/src/main/java/com/aionos/vision/VisionActionMapper.kt` | 1,093 |
| `android/app/src/main/java/com/aionos/vision/VisionCoordinator.kt` | 712 |
| `android/app/src/main/java/com/aionos/vision/VisionFallback.kt` | 2,426 |
| `android/app/src/main/java/com/aionos/voice/VoiceInputManager.kt` | 3,754 |
| `android/app/src/main/java/com/aionos/voice/VoskModelManager.kt` | 4,607 |
| `android/app/src/main/res/drawable/bubble_background.xml` | 325 |
| `android/app/src/main/res/drawable/ic_agent.xml` | 633 |
| `android/app/src/main/res/layout/overlay_bubble.xml` | 653 |
| `android/app/src/main/res/values/strings.xml` | 3,369 |
| `android/app/src/main/res/values/styles.xml` | 554 |
| `android/app/src/main/res/xml/accessibility_service_config.xml` | 652 |
| `android/app/src/test/java/com/aionos/parser/ActionParserTest.kt` | 1,420 |
| `android/app/src/test/java/com/aionos/security/ActionPolicyTest.kt` | 900 |
| `android/app/src/test/java/com/aionos/security/NetworkPolicyTest.kt` | 822 |
| `android/app/src/test/java/com/aionos/vision/VisionActionMapperTest.kt` | 926 |
| `android/build.gradle.kts` | 223 |
| `android/settings.gradle.kts` | 388 |
| `docs/ARCHITECTURE.md` | 2,435 |
| `metadata/com.aionos.yml` | 1,640 |
| `scripts/setup_openrouter.sh` | 1,953 |
| `setup.sh` | 993 |

## Build and release inventory

| Area | Files |
|---|---|
| Android build | `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/app/build.gradle.kts`, Gradle wrapper |
| CI | `.github/workflows/android-fdroid.yml`, `.github/workflows/build.yml`, `.github/workflows/release.yml`, `.github/workflows/test.yml` |
| F-Droid | `metadata/com.aionos.yml`, `fastlane/metadata/android/en-US/*` |
| Documentation | `README.md`, `QUICKSTART.md`, `android/README.md`, `docs/ARCHITECTURE.md`, `VIBE_PROMPT.md` |

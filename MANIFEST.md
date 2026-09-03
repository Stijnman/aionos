# AionOS Android — Generated File Manifest
# Total: 35 files, 111,636 bytes (109.0 KB)
# Generated: 2026-09-02
# Research-based corrections applied:
#   - MediaPipe uses TensorFlow Lite Flatbuffer (.bin), NOT GGUF
#   - Ollama uses OpenAI-compatible /v1/chat/completions
#   - AccessibilityService requires canPerformGestures="true"
#   - Vosk 0.3.47 is current Android version

## Core Architecture
| File | Package | Purpose | Size |
|------|---------|---------|------|
| ActionModels.kt | com.aionos.action | Typed actions with safety tiers | 2,904 B |
| SafeActionExecutor.kt | com.aionos.action | Tiered safety + stuck detection | 10,697 B |
| AccessibilityTreeParser.kt | com.aionos.parser | Tree → LLM text | 4,119 B |
| ActionParser.kt | com.aionos.parser | LLM JSON → typed actions | 4,192 B |
| AgentAccessibilityService.kt | com.aionos.service | Core accessibility service | 4,674 B |
| OverlayBubbleService.kt | com.aionos.service | Floating bubble | 5,383 B |

## Agent Brain
| File | Package | Purpose | Size |
|------|---------|---------|------|
| AgentOrchestrator.kt | com.aionos.agent | Central coordinator | 5,795 B |
| AgentViewModel.kt | com.aionos.ui | Compose state management | 3,721 B |

## LLM Layer
| File | Package | Purpose | Size |
|------|---------|---------|------|
| LLMBridge.kt | com.aionos.llm | Interface + prompts | 2,410 B |
| MediaPipeBridge.kt | com.aionos.llm | On-device inference (TFLite) | 2,382 B |
| OllamaBridge.kt | com.aionos.llm | LAN inference (OpenAI API) | 3,405 B |

## Security & Audit
| File | Package | Purpose | Size |
|------|---------|---------|------|
| EncryptedPrefs.kt | com.aionos.security | AES-256 settings | 3,634 B |
| AuditLog.kt | com.aionos.audit | SQLite audit trail | 6,836 B |

## Extensions
| File | Package | Purpose | Size |
|------|---------|---------|------|
| VoiceInputManager.kt | com.aionos.voice | Vosk STT | 3,634 B |
| PluginLoader.kt | com.aionos.plugin | APK plugin system | 2,573 B |
| VisionFallback.kt | com.aionos.vision | Object detection fallback | 2,426 B |

## UI
| File | Package | Purpose | Size |
|------|---------|---------|------|
| MainActivity.kt | com.aionos | Compose main screen | 18,403 B |
| ConfirmationDialog.kt | com.aionos.ui | TIER 3 dialogs | 2,335 B |
| Theme.kt | com.aionos.ui.theme | Material3 theme | 1,561 B |
| Color.kt | com.aionos.ui.theme | Color definitions | 274 B |
| Type.kt | com.aionos.ui.theme | Typography | 478 B |

## Resources & Config
| File | Location | Purpose | Size |
|------|----------|---------|------|
| strings.xml | res/values/ | Translations | 3,369 B |
| overlay_bubble.xml | res/layout/ | Bubble layout | 653 B |
| bubble_background.xml | res/drawable/ | Bubble shape | 325 B |
| ic_agent.xml | res/drawable/ | Agent icon | 633 B |
| accessibility_service_config.xml | res/xml/ | Service config | 652 B |
| AndroidManifest.xml | root/ | Manifest | 2,131 B |

## Build
| File | Purpose | Size |
|------|---------|------|
| build.gradle.kts | Dependencies & flavors | 3,238 B |
| settings.gradle.kts | Project settings | 391 B |
| gradle.properties | Gradle config | 136 B |
| proguard-rules.pro | Obfuscation rules | 740 B |

## Meta
| File | Purpose | Size |
|------|---------|------|
| AionosApplication.kt | Application class | 240 B |
| fdroid_metadata.yml | F-Droid store data | 1,470 B |
| README_ANDROID.md | Documentation | 2,680 B |
| MANIFEST.md | This file | — |

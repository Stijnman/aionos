# AionOS — Quick Start

## 1. Extract & Setup

```bash
unzip aionos-project-v0.1.0-alpha.zip
cd aionos-project
./setup.sh
```

## 2. Build

```bash
cd android
./gradlew assembleFdroidDebug
```

## 3. Install

```bash
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

## 4. Configure

1. Open AionOS → Enable Accessibility Service when prompted
2. Settings → LLM Provider → Ollama (or MediaPipe if model downloaded)
3. If using Ollama: Run `ollama run llama3.2` on your LAN machine
4. Settings → Ollama Host → `http://YOUR_LAN_IP:11434`

## 5. Use

- **Text**: Type command → tap send
- **Voice**: Tap mic → speak → agent executes
- **Bubble**: Enable floating bubble for quick access anywhere
- **Emergency**: Tap "Emergency Stop" or toggle agent off

## Project Structure

```
aionos-project/
├── android/              # Android project
│   ├── app/
│   │   ├── build.gradle.kts
│   │   ├── proguard-rules.pro
│   │   └── src/main/
│   │       ├── java/com/aionos/     # All Kotlin source
│   │       └── res/                 # XML resources
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
├── metadata/             # F-Droid metadata
├── fastlane/             # Store assets
├── README.md
└── setup.sh
```

## Vibe Coding

Copy `VIBE_PROMPT.md` into your AI assistant to continue development.

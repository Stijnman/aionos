#!/bin/bash
# AionOS Project Setup Script
set -e
PROJECT_NAME="${1:-aionos}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "🚀 Setting up AionOS project: $PROJECT_NAME"
echo ""
echo "📋 Checking prerequisites..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Install JDK 17+"
    exit 1
fi
echo "   ✓ Java: $(java -version 2>&1 | head -n 1 | cut -d'"' -f2)"
if command -v git &> /dev/null; then
    echo "   ✓ Git: $(git --version)"
fi
echo ""
echo "📁 Project files: $(find "$SCRIPT_DIR/android" -type f | wc -l)"
echo ""
echo "🔧 Next steps:"
echo "   1. cd android"
echo "   2. ./gradlew assembleFdroidDebug"
echo "   3. adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk"
echo ""
echo "📝 Optional:"
echo "   • Ollama: Install on LAN → Settings → Ollama Host"
echo "   • Vosk: Download model → app/files/vosk-model/"
echo "   • MediaPipe: Download TFLite → app/files/models/"
echo ""
echo "✅ Ready!"

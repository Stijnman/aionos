#!/bin/bash
# setup_openrouter.sh - Automated OpenRouter API Key Setup
#
# Privacy (aligned with README / docs/ARCHITECTURE.md):
# OpenRouter is an explicit remote opt-in. Prompts sent to OpenRouter are NOT
# anonymized — treat transmission as disclosure of the selected prompt data.
# Local-first default remains Ollama / on-device; remote use requires consent.

set -euo pipefail

# Check if OPENROUTER_API_KEY exists in local.properties
if ! grep -q "OPENROUTER_API_KEY" local.properties 2>/dev/null; then
  read -r -p "Enter OpenRouter API Key: " api_key
  echo "OPENROUTER_API_KEY=$api_key" >> local.properties
  echo "✅ API key saved to local.properties"
else
  echo "✅ OPENROUTER_API_KEY already exists in local.properties"
  api_key=$(grep "OPENROUTER_API_KEY" local.properties | cut -d '=' -f2-)
fi

echo "📝 Reminder: OpenRouter prompts are not anonymized. Enable remote-provider consent in-app only when you accept that disclosure."

# Validate the key by making a test API call
response=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $api_key" \
  -H "Content-Type: application/json" \
  -d '{"model": "openai/gpt-4o-mini", "messages": [{"role": "user", "content": "Test"}]}' \
  https://openrouter.ai/api/v1/chat/completions)

if [ "$response" -eq 200 ]; then
  echo "✅ OpenRouter API key is valid."
else
  echo "❌ Invalid API key (HTTP $response). Check and retry."
  exit 1
fi

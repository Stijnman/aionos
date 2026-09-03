#!/bin/bash
# setup_openrouter.sh - Automated OpenRouter API Key Setup

# Check if OPENROUTER_API_KEY exists in local.properties
if ! grep -q "OPENROUTER_API_KEY" local.properties; then
  read -p "Enter OpenRouter API Key: " api_key
  echo "OPENROUTER_API_KEY=$api_key" >> local.properties
  echo "✅ API key saved to local.properties"
else
  echo "✅ OPENROUTER_API_KEY already exists in local.properties"
fi

# Validate the key by making a test API call
if [ -n "$api_key" ]; then
  response=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $api_key" \
    -H "Content-Type: application/json" \
    -d '{"model": "openai/gpt-4o-mini", "messages": [{"role": "user", "content": "Test"}]}' \
    https://openrouter.ai/api/v1/chat/completions)

  if [ "$response" -eq 200 ]; then
    echo "✅ OpenRouter API key is valid."
  else
    echo "❌ Invalid API key. Check and retry."
    exit 1
  fi
else
  api_key=$(grep "OPENROUTER_API_KEY" local.properties | cut -d '=' -f2)
  response=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $api_key" \
    -H "Content-Type: application/json" \
    -d '{"model": "openai/gpt-4o-mini", "messages": [{"role": "user", "content": "Test"}]}' \
    https://openrouter.ai/api/v1/chat/completions)

  if [ "$response" -eq 200 ]; then
    echo "✅ OpenRouter API key is valid."
  else
    echo "❌ Invalid API key. Check and retry."
    exit 1
  fi
fi

# Add privacy disclaimer to README.md if not present
if ! grep -q "Cloud fallback" README.md; then
  echo "
## Privacy
- **Local-first**: All processing happens on-device by default.
- **Cloud fallback**: Optionally enable OpenRouter for complex tasks. Data is anonymized before sending.
- **No silent exfiltration**: Screen contents never leave your device without explicit consent." >> README.md
  echo "✅ Privacy disclaimer added to README.md"
else
  echo "✅ Privacy disclaimer already exists in README.md"
fi
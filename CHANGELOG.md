# Changelog

## 0.2.1 alpha — remediation

### Action policy

- Centralized `ActionPolicy` preflight layer: rejects TIER_4 actions and bounds coordinates, text input, scroll requests, wait durations, and Android package names before any Accessibility API call.
- TIER_3 actions always require confirmation; the setting cannot bypass that boundary.
- Password and typed-text contents are redacted from audit details.
- Ollama endpoints validated before encrypted persistence; embedded credentials rejected; cleartext endpoints limited to local hosts.

### Vision

- Confidence-gated proposal layer and interactive-label allowlist; generic object detections cannot become taps.
- Proposals are reported to the user and are not executed automatically.

### Plugins

- Dispatch requires an explicitly selected loaded plugin, matching manifest package, a declared action, bounded parameter names, and bounded parameter values.

### Fixes

- Fail-fast orchestration, single-collector voice lifecycle management, accessibility-node recycling, non-sensitive typed-input result messages, encrypted OpenRouter consent settings, atomic/checksummable Vosk installation, first-run setup notification delivery, and Android instrumentation scaffolding.
- Release packaging includes required service metadata, model asset, XML theme, and icon resources.

## Earlier alphas

- Initial feature set: intent parsing, local LLM bridges (MediaPipe / Ollama), typed action execution with safety tiers, on-device SQLite audit log, AES-256 encrypted preferences, Vosk voice input, APK plugin system, vision fallback, overlay bubble, Compose UI.
- Added: remote-provider opt-in (OpenRouter, off by default), privacy-safe action reporting, accessibility-node cleanup, bounded vision proposals, plugin manifest validation, model-download integrity, service setup notifications.
- Test coverage: parser, action-policy, network-policy, vision, and Android instrumentation smoke tests. F-Droid workflow runs unit tests, lint, debug and release APK assembly, and an emulator smoke test.
- Full APK, unit-test, lint, and emulator execution requires an Android SDK; the GitHub Actions workflow is the authoritative verification environment.

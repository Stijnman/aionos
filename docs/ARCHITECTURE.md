# aionos Architecture

## Layers

1. **Ingress** — notification listeners, optional voice STT, text chat bubble
2. **Planner** — converts natural language into an action graph (JSON)
3. **Policy** — allow/deny list, confirmation requirements, rate limits
4. **Executor** — AccessibilityService adapters (click, swipe, type, back, home)
5. **Memory** — short-term task state only; no cloud sync by default
6. **Audit** — append-only local log of intents and actions

## Action graph schema (draft)

```json
{
  "goal": "Open Notes and create a draft",
  "steps": [
    {"op": "launch", "package": "com.example.notes"},
    {"op": "wait_for", "text": "New", "timeout_ms": 5000},
    {"op": "click", "text": "New"},
    {"op": "type", "text": "Shopping list"}
  ],
  "require_confirm": false
}
```

## Threat model (summary)

| Threat | Mitigation |
|--------|------------|
| Malicious remote model | Default local-only; explicit opt-in for remote |
| Accessibility abuse | User kill switch; confirm for payments/permissions |
| Log leakage | Local storage; optional encrypted at rest |
| Prompt injection from screen | Treat UI text as untrusted data, never as code |

## Non-goals

- Silent background surveillance
- Cloud training on user screen content
- Bypassing device lock / banking 2FA without explicit UX

## Defensive execution pipeline

Every parsed action passes through `ActionPolicy` before it reaches `SafeActionExecutor`. The policy rejects TIER_4 actions, incomplete or out-of-range coordinates, excessive text, invalid Android package names, oversized scroll requests, and waits longer than 30 seconds. The executor records policy failures in the local audit log and checks the encrypted kill switch before any Android API call.

Vision fallback is intentionally proposal-only. `VisionActionMapper` requires a confidence of at least 0.80 and verifies that the proposed tap center is inside the captured display. A proposal still follows the normal safety and confirmation path; the vision layer never dispatches gestures directly.

Plugin actions are explicitly targeted to a loaded plugin, must be declared in that plugin’s manifest, and may only include declared parameter names. Ollama endpoints are validated before encrypted persistence: only HTTP/HTTPS hosts without embedded credentials are accepted. Screen captures are held in memory and are not uploaded or persisted by the capture coordinator.


## Remediation guarantees

The orchestrator now stops on the first failed action, reports an error rather than a false completion, and keeps only sanitized action-class history. Voice collection is single-owner and cancellable; transcripts are cleared before dispatch. The view model reports an unavailable AccessibilityService instead of silently dropping commands and can reload the provider bridge after settings changes.

OpenRouter is a deliberately explicit remote provider. Its API key is stored in encrypted preferences, selection requires remote-provider consent, and the default remains Ollama. The application must not describe remote prompts as anonymized unless an actual redaction pipeline has run. Cleartext Ollama endpoints are restricted to local addresses by `NetworkPolicy`; HTTPS endpoints remain available for configured deployments.

Vision uses explicit MediaProjection consent, one-shot local analysis, an allowlist of recognized interactive labels, and proposal-only output. Captured bitmaps are recycled after analysis, and proposals are not automatically executed. The bundled EfficientDet-Lite0 asset is packaged under `android/app/src/main/assets/`.

Temporary `AccessibilityNodeInfo` children obtained during tree traversal are recycled after use. Vosk model installation is bounded, zip-slip checked, optionally checksum-verified, and staged before replacement. The first-run accessibility setup notification is now implemented rather than silently ignored.

CI is expected to run unit tests, lint, debug and release F-Droid APK assembly, and an emulator instrumentation smoke test. A local environment without an Android SDK can validate Gradle configuration and static resources but cannot claim a compiled APK or passing Android tests.

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

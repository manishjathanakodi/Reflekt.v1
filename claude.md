# Reflekt — Android App

## Project
- Package: com.reflekt.journal
- Min SDK: 26 · Target SDK: 34
- Language: Kotlin · UI: Jetpack Compose ONLY (no XML ever)
- Architecture: Clean Architecture + MVVM + Hilt
- Spec: reflekt_spec_v4_final.docx
- UI Reference: reflekt_complete_ui.html

## Hard Rules (never break these)
- Zero network calls for user data — 100% on-device
- All encryption keys via Android Keystore only — never hardcoded
- LLM inference via MediaPipe on-device only — no external AI APIs
- Never use View-based code — Compose only

## Light Mode Rule
Only background (#F0EBE1) and nav bar (#E8E1D4) change in light mode.
Cards (#1A2030), inputs (#1E2538), chat bubbles → stay dark in both modes.
Use darkColorScheme() as base for LightColorScheme too.

## Current Phase
Phase 6

## Verification
I build in Android Studio after each phase and report errors back.
Do one phase at a time. Stop and confirm when complete.
```

---


# Instant Ledger — Implementation Plan

**Repository:** `C:\Users\Deepankar\Downloads\instant-ledger-0.3.0`
**App version:** 0.3.0, applicationId `com.instantledger`
**Android:** minSdk 26, targetSdk 35, compileSdk 35, JDK 17
**Build:** Gradle Kotlin DSL, AGP 8.5.x, Kotlin 2.0.x, KSP, Hilt 2.50+

## Status

This document summarizes the implementation plan for hardening Instant Ledger
(security, reliability, build modernization, and features). See the source plan
at `C:\Users\Deepankar\.local\share\kilo\plans\1792327796077-instant-ledger-implementation-plan.md`.

## Phases (in order)

- **Phase 0 — Baseline & plan** (this document + docs/SECURITY.md)
- **Phase 1 — Backup & encryption security** (P0)
- **Phase 2 — SMS parsing & reliability** (P1)
- **Phase 3 — Build & platform modernization** (P2)
- **Phase 4 — Architecture & testability** (P1/P3)
- **Phase 5 — UI modernization** (P3)
- **Phase 6 — Budgets, savings, insights** (P3)
- **Phase 7 — Categories, currency, transaction management** (P3)
- **Phase 8 — Export & reports** (P4)
- **Phase 9 — CI/CD & repository quality** (P4)
- **Phase 10 — Final verification** (all)

## Acceptance gates

- **P0 gate:** No path silently accepts tampered encrypted data or destroys current records after a failed restore.
- **P1 gate:** Core transaction processing covered by meaningful tests; no false-success exception handling.
- **P2 gate:** App builds with chosen toolchain; migration safety documented.
- **P3 gate:** Displayed values derive from actual data; empty states do not fabricate information.
- **P4 gate:** Exported files valid and safe; reports match underlying data.

## Migration & compatibility risks

- **Room schema version 8 → 9+**: Only add if a new entity/column is needed. Any migration must be non-destructive (ALTER TABLE only, no DROP). Migration tests required.
- **SQLCipher upgrade**: Must verify existing encrypted DB opens after upgrade. Test on a copy of an existing DB file.
- **Backup format change**: New backups use AES-GCM + random salt + 600k PBKDF2. Old backups (AES/CBC + fixed salt + 10k) must remain restorable via a legacy restore path. Format versioning in manifest.
- **targetSdk 35**: May require new runtime permission handling (foreground service, notifications). Test on API 35 emulator/device.
- **No git repo**: Cannot branch/commit. Plan assumes local file edits only; user must init git separately if desired.

## Environment limitations

- Not a git repository → cannot create feature branch, commit, or push. User must init git if version control is needed.
- No Android SDK/emulator confirmed → `connectedDebugAndroidTest` may not run; unit tests + lint + assemble are the verifiable checks.
- `gradlew.bat` present but gradle execution may be blocked by sandbox permissions; baseline build results not yet captured.

## Open questions

1. Should a git repo be initialized locally before making changes?
2. Is an Android SDK/emulator available for instrumented tests?
3. Should targetSdk jump directly to 35, or intermediate to 34→35?
4. Is adding a chart library (e.g., MPAndroidChart) acceptable, or should charts be lightweight Compose only?
5. Should OCR be deferred entirely (out of scope) or evaluated as optional?

## Files expected to change

`app/build.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, `AndroidManifest.xml`, `app/proguard-rules.pro`, `app/.../backup/BackupManager.kt`, `app/.../security/EncryptionManager.kt`, `app/.../worker/SmsProcessWorker.kt`, `app/.../domain/parser/SMSParser.kt`, `app/.../domain/parser/SMSValidationGate.kt`, `app/.../domain/parser/TransactionParser.kt`, plus new test files, new CI workflow, new docs files.

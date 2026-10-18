# Instant Ledger v0.3.0 — Hardening Release

**Release date:** 2025-01-29

This is a **hardening and correctness release**. No new features; focus on stability, trust, and deterministic behavior.

---

## What Was Fixed vs v0.2.0

### SMS Parsing & Validation

- **Balance-only alerts:** Messages that only report balance (e.g. "Avl Bal: Rs 50,000" with no transaction) are now explicitly rejected and are not saved as transactions. Real transactions that mention balance (e.g. "Rs 500 debited. Avl Bal: Rs 45,000") are still accepted.
- **Statement alerts:** Alerts such as "statement generated", "monthly statement available" are explicitly excluded so they are never saved as transactions.
- **Failed / declined / reversed:** Messages indicating failed, declined, or reversed transactions are rejected and not saved. Valid refund/reversal transactions (e.g. "Rs 500 refunded") remain accepted via existing verb rules.
- **Acceptance rule:** Balance-only rejection only applies when the message does not contain a strong transaction verb (credited, debited, paid, etc.), avoiding false rejection of real payments that mention balance.

### Notification & Deep-Link

- **Notification tap:** When you open the app from a pending-transaction notification, the app opens to the Today tab (pending list). The intent extra is now consumed once so that configuration changes or recomposition do not repeatedly switch the user back to the pending section.

### Unchanged (Already Correct in v0.2.0)

- **SMS ingestion:** BroadcastReceiver still does no DB or UI work; all processing is in `SmsProcessWorker` (WorkManager). Duplicate and user-ignored hashes are checked before insert.
- **Filter UI:** Apply/Clear buttons remain sticky and visible; filters are category, payment mode, and approval status only; totals are purely numeric.
- **Category icons:** Predefined icon set only; consistent across Home, category summary, and transaction list.
- **Approval flow:** All new auto-captured transactions are saved with `isApproved = false`; single and grouped notifications; no overlay.

---

## Stability & Correctness

- No database schema changes.
- No new permissions.
- Package name unchanged.
- No cloud, login, analytics, or tracking.
- No auto-approval; no silent discard of valid money movement.
- SMS validation is stricter without dropping real transactions that mention balance or use standard verbs/rails.

---

## Technical Summary

| Item        | v0.3.0 |
|------------|--------|
| versionCode | 4      |
| versionName | 0.3.0  |
| Min SDK    | 26     |
| Target SDK | 34     |

---

## Upgrade from v0.2.0

1. Install the v0.3.0 APK over your existing install (data preserved).
2. No migration or settings change required.
3. You may see fewer non-transaction SMS (balance-only, statements, failed/reversed) captured as pending; real payments are unaffected.

---

## Download & Links

- **Release:** [v0.3.0](https://github.com/Deepankar-Siddharth/instant-ledger/releases/tag/v0.3.0)
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)
- **Issues:** [GitHub Issues](https://github.com/Deepankar-Siddharth/instant-ledger/issues)

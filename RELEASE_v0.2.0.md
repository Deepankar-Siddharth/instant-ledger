# Instant Ledger v0.2.0 — Latest

Stability, parsing, UI, and approval-flow update following v0.1.0.

**Features:**

- Local, encrypted SMS-based transaction capture (unchanged)
- Pending vs approved transaction workflow — **no overlay;** notifications only
- Manual transaction entry with merchant dropdown and “Other” option
- Category-based organization with **predefined icons** and colors (no custom emoji)
- Merchant management from Settings
- CSV export
- Light theme, Material 3 UI
- **Smarter SMS capture:** only real money-movement messages (amount + verb); OTPs, balance-only, failed, and promo messages excluded
- **Stable when SMS arrives:** all parsing and DB writes run off the main thread via WorkManager
- **Clear filters:** category, payment mode, approval status only; debit/credit toggles removed; totals = pure math (incoming/outgoing from amounts)

**Privacy-first (unchanged):**

- No cloud
- No accounts
- No analytics
- No SMS storage (hash only for duplicates/ignored)

This release is recommended for all users upgrading from v0.1.0 and for new installs.

---

## Overview

Instant Ledger v0.2.0 is a stability and UX update that addresses crashes on SMS arrival, tightens SMS parsing, removes overlay-based approval flow, and refines filters and transaction cards. All SMS handling is off the main thread; only real transaction messages are captured; and valid detections are always saved as pending for your approval—never auto-approved or silently discarded.

## What's Included

### Stability & Crash Fixes

- **No crash on SMS:** App no longer crashes when an SMS arrives while the app is open. The SMS receiver does no heavy work and never touches the UI or database directly; all parsing and DB writes happen in a WorkManager worker.
- **Multipart & duplicate handling:** Multipart SMS is combined before processing; a normalized hash prevents duplicate inserts and respects user-ignored messages (same SMS is not re-offered after you tap Ignore).
- **Defensive parsing:** Null checks and try/catch around parsing; failures are logged without sensitive data and do not crash the app.

### SMS Parsing Improvements

- **Capture only real money movement:** Messages must have an amount and a transaction verb (e.g. credited, debited, paid, received, refund, transfer). OTPs, balance-only alerts, failed/declined transactions, and promotional messages are excluded.
- **No auto-ignore:** Every valid detection is saved as **pending** for you to approve or ignore. No silent discard of valid money transactions.
- **Validation gate:** New `SMSValidationGate` enforces the above rules before any transaction is created.

### Approval & Notification Behavior

- **Overlay removed:** All draw-over-app / overlay permission and related UI are removed. Approval is done in-app only.
- **Notifications:** New auto-captured transactions trigger a notification. If multiple are pending, a grouped reminder notification is used.
- **Single processing path:** SMS and notification-based capture both enqueue the same `SmsProcessWorker` for parsing and insert, keeping behavior consistent and safe.

### Filter System Cleanup

- **Filter UI fixed:** Filter bottom sheet Apply and Clear buttons are always visible and no longer clipped by system navigation.
- **Debit/credit removed:** Incoming/outgoing toggles are removed from filters. Filters are: **category**, **payment mode**, and **approval status** only.
- **Pure numeric totals:** Incoming = sum(amount > 0), Outgoing = sum(amount < 0); no filter toggles for type.

### UI Refinements

- **Transaction card layout:** Source chip (Auto / Manual / Edited) on top; category below; then merchant/description; optional notes. Tighter layout for better information density.
- **Category icons:** Only predefined Material-style icons (e.g. home, work, shopping, other). Custom emoji support removed; existing emoji metadata is migrated to icon keys on read. Icons are category-based and consistent across cards and lists.
- **Light theme only** (unchanged).

### Category & Merchant Data Integrity

- Categories without a selection stay pending until you choose one.
- Merchants are managed from Settings.
- Manual entry: merchant dropdown with managed merchants; “Other” reveals an optional text field for ad-hoc merchant name.

### Technical Additions

- **SmsProcessWorker:** WorkManager worker that does parsing, validation, duplicate/hash check, DB insert, and notifications—all off the main thread.
- **SMSValidationGate:** Validates message body for amount + verb and excludes OTP, balance-only, failed, and promo patterns.
- **IgnoredHashesStore:** Persists hashes of messages you chose to ignore so the same SMS is not captured again.
- **CategoryIcons:** Predefined icon set used for all categories with consistent sizing in cards and lists.

## What Was Removed

- Overlay / draw-over-other-apps permission and any UI that depended on it.
- Debit/credit (incoming/outgoing) filter toggles.
- Custom emoji support for category icons (replaced by predefined icons).

## Known Limitations

- SMS parsing accuracy still depends on bank message format; some formats may not be recognized.
- Manual entry required for unsupported banks or non-standard SMS.
- Light theme only; no dark theme.
- No cloud sync, no accounts (by design).
- Backup password cannot be recovered if forgotten.
- Android 8.0 (API 26) or later; optimized for Android 14 (API 34).

## Who This Release Is For

- **All v0.1.0 users** upgrading for stability and clearer approval flow.
- **New users** who want a privacy-first, offline SMS ledger with predictable behavior and no overlay.
- Anyone who had crashes on SMS arrival or wanted stricter capture (no OTPs/promos) and clearer filters.

## Installation & Upgrade

1. Download the v0.2.0 APK from the [Releases](https://github.com/Deepankar-Siddharth/instant-ledger/releases/tag/v0.2.0) page.
2. Install over existing Instant Ledger (data preserved) or install fresh.
3. No new permissions; overlay permission is no longer used or requested.
4. Grant SMS (and optional notification listener) if you use auto-capture.

## First-Time Setup (Unchanged)

1. **Grant Permissions:** Allow SMS access for automatic capture (optional).
2. **Create Categories:** Add categories; choose from the predefined icon set.
3. **Test Transaction:** Add a manual transaction; use merchant dropdown or “Other.”
4. **Configure Security:** Set up biometric lock or PIN (optional).
5. **Create Backup:** Set up backup with a strong password.

## Privacy & Security (Unchanged)

- **No Data Upload:** All processing is local.
- **Encrypted Storage:** Database and sensitive settings are encrypted.
- **No Analytics:** No tracking or analytics.
- **No Accounts:** No registration or login.
- **No SMS Storage:** Only hashes used for duplicate/ignore logic; raw SMS not stored.

## Support

- **Issues:** [GitHub Issues](https://github.com/Deepankar-Siddharth/instant-ledger/issues)
- **Documentation:** [README.md](README.md)
- **In-App:** “How it Works” in Settings

## What's Next

Future releases may include:

- Further bank SMS format coverage
- Sender trust scoring
- Recurring transaction detection
- Spending forecasts and goal tracking

## Acknowledgments

Built with Jetpack Compose, Room, SQLCipher, Material 3, Hilt, and WorkManager.

## License

Apache License 2.0 — see LICENSE file.

---

**Download:** [Release v0.2.0](https://github.com/Deepankar-Siddharth/instant-ledger/releases/tag/v0.2.0)  
**Changelog:** [CHANGELOG.md](CHANGELOG.md)  
**Report issues:** [GitHub Issues](https://github.com/Deepankar-Siddharth/instant-ledger/issues)

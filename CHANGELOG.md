# Changelog

All notable changes to Instant Ledger will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2025-01-29

### Fixed
- **SMS validation:** Balance-only alerts (e.g. "Avl Bal: Rs X" with no transaction) are now explicitly rejected so they are not saved as transactions. Real transactions that mention balance (e.g. "Rs 500 debited. Avl Bal: Rs 45,000") are still accepted.
- **Failed/reversed:** Messages indicating failed, declined, or reversed transactions are rejected and not saved.
- **Notification deep-link:** Opening the app from a pending-transaction notification now consumes the intent extra so the app does not re-open to the pending section on every recomposition or config change.

### Changed
- **SMS validation gate:** Statement alerts (e.g. "statement generated", "monthly statement") are explicitly excluded. Credit limit and spend/usage alerts remain excluded.
- **Acceptance rule:** Balance-only messages are rejected only when they do not contain a strong transaction verb (credited, debited, paid, etc.), avoiding false rejection of real transactions that mention balance.

### Technical
- No database schema changes. No new permissions. Package name unchanged. Focus on correctness and determinism; no auto-approval; no silent discard of valid money movement.

---

## [0.2.0] - 2025-01-29

### Fixed
- **Stability:** App no longer crashes when SMS arrives while the app is open. SMS handling is fully off the main thread.
- **Filter UI:** Filter bottom sheet Apply/Clear buttons are always visible and no longer clipped by system navigation.
- **Transaction cards:** Layout adjusted for better information density; source chip on top, category below, merchant/description clearly separated.

### Changed
- **SMS handling:** `SMSReceiver` no longer touches the database or UI. It only extracts message text, combines multipart SMS, and enqueues `SmsProcessWorker` for all parsing and DB writes.
- **SMS parsing:** Stricter validation gate—only real money movement messages (amount + transaction verb) are captured. OTPs, balance-only alerts, failed/declined transactions, and promotional messages are excluded. No auto-ignore; valid detections are saved as pending for user approval.
- **Duplicate/race safety:** Normalized hash check before insert; multipart and duplicate SMS are guarded. Ignored transactions are persisted by hash so the same SMS is not re-offered after user ignores it.
- **Approval flow:** All overlay / draw-over-app code removed. New auto-captured transactions are saved as pending and trigger a notification; multiple pending items use a grouped reminder notification.
- **Filters:** Debit/credit (incoming/outgoing) filter options removed. Filters now only include category, payment mode, and approval status. Totals remain purely numeric: incoming = sum(amount > 0), outgoing = sum(amount < 0).
- **Category icons:** Custom emoji support removed. Categories use a predefined icon set only; icons are category-based and consistent. Old emoji metadata is migrated to icon keys on read.
- **Transaction card:** Source chip (Auto / Manual / Edited) on top; category displayed below; merchant/description and optional notes follow. Light theme only.

### Added
- **SmsProcessWorker:** Dedicated WorkManager worker for SMS parsing, validation, duplicate/hash check, DB insert, and notifications—all off the main thread.
- **SMSValidationGate:** Ensures only messages with amount and transaction verb (credited, debited, paid, received, refund, transfer) are accepted; excludes OTP, balance-only, failed, and promo patterns.
- **IgnoredHashesStore:** Persists hashes of user-ignored messages so the same SMS is not re-captured.
- **CategoryIcons:** Predefined Material icon set for categories (e.g. home, work, shopping, other) with consistent rendering in cards and lists.

### Removed
- Overlay / draw-over-other-apps permission and related UI flow.
- Debit/credit toggles from the filter bottom sheet.
- Custom emoji support for category icons.

### Technical
- Minimum/target SDK unchanged (API 26–34). No database schema changes. Package name unchanged. No cloud sync or accounts; no auto-approval; no silent discard of valid money transactions.

---

## [0.1.0] - 2024-01-XX

### Added
- Automatic transaction capture from bank SMS messages
- Support for major Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOI)
- Manual transaction entry (Cash, UPI, Card, Bank)
- Transaction approval workflow with mandatory categorization
- Category management with custom icons/emojis and colors
- Merchant management with user-defined display names
- Time-based transaction views (Today, Weekly, Monthly, Yearly)
- Pending transaction notifications
- Daily reminder notifications for unapproved transactions
- Password-protected backup and restore system
- CSV/JSON data export
- Biometric authentication (fingerprint/face unlock)
- PIN lock with fallback
- Privacy mode (masks transaction amounts)
- SQLCipher database encryption
- EncryptedSharedPreferences for sensitive settings
- Transaction detail view with edit/delete capabilities
- Category and merchant filtering
- Database migration system for schema updates

### Security
- SQLCipher encryption for all financial data
- Android Keystore-backed preference encryption
- Hardware-bound encryption keys
- No cloud sync or data transmission
- Complete local data processing

### Privacy
- No analytics or tracking
- No account creation required
- No data uploads
- SMS content processed locally only
- Raw SMS text never displayed in UI

### Known Limitations
- SMS parsing accuracy depends on bank SMS format consistency
- Some bank SMS formats may not be recognized
- Manual entry required for unsupported banks
- Backup password cannot be recovered if forgotten
- Restore requires app restart (automatic)

### Technical Details
- Minimum Android version: 8.0 (API 26)
- Target Android version: 14 (API 34)
- Built with Jetpack Compose and Material 3
- Room database with SQLCipher encryption
- Hilt dependency injection
- WorkManager for background tasks

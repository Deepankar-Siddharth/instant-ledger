# Changelog

All notable changes to Instant Ledger will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

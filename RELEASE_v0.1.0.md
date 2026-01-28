# Instant Ledger v0.1.0 - Initial Release

## Overview

Instant Ledger v0.1.0 is the first public release of a privacy-first, offline-only personal finance ledger for Android. This release provides core functionality for automatic transaction capture from bank SMS messages, manual transaction entry, and complete data management—all while keeping your financial data entirely on your device.

## What's Included

### Core Features

**Automatic Transaction Capture**
- Monitors bank SMS messages in the background
- Extracts transaction details (amount, merchant, timestamp) locally
- Supports major Indian banks: SBI, HDFC, ICICI, Axis, Kotak, PNB, BOI
- All parsing happens on-device—no data leaves your device

**Transaction Management**
- Pending transaction workflow: All detected transactions require your explicit categorization
- Manual transaction entry: Add Cash, UPI, Card, or Bank transactions
- Edit and delete capabilities for all transactions
- Time-based views: Today, Weekly, Monthly, Yearly

**Category System**
- Create custom categories with visual identity (emoji or Material icon + color)
- Categories are mandatory for transaction approval
- Category metadata applies consistently across all transactions

**Merchant Management**
- User-defined merchant display names
- Transaction-level merchant overrides
- Usage statistics per merchant
- Clean, reusable merchant data

**Data Protection**
- SQLCipher database encryption (AES-256)
- EncryptedSharedPreferences for sensitive settings
- Biometric authentication (fingerprint/face unlock)
- PIN lock with fallback
- Privacy mode (masks amounts until tapped)

**Backup & Restore**
- Password-protected backup system
- Complete data export (database + preferences)
- Cross-device restore capability
- CSV/JSON export for external analysis

**Notifications**
- Instant notifications for new transactions
- Daily reminders for unapproved transactions
- Configurable notification preferences

## Known Limitations

**SMS Parsing**
- Parsing accuracy depends on bank SMS format consistency
- Some bank SMS formats may not be recognized
- Manual entry required for unsupported banks or non-standard formats
- SMS parsing patterns may need updates for new bank formats

**Backup & Restore**
- Backup password cannot be recovered if forgotten
- Restore requires app restart (automatic, but may interrupt workflow)
- Large databases may take time to backup/restore

**Platform Support**
- Android 8.0 (API 26) or later required
- Optimized for Android 14 (API 34)
- SMS capture requires SMS permissions (can be disabled for manual-only mode)

**User Experience**
- Light theme only (dark theme not available)
- No cloud sync (by design, for privacy)
- No account system (by design, for privacy)

## Who This Release Is For

**Primary Users**
- Privacy-conscious individuals who want complete control over their financial data
- Users who prefer offline-first applications
- People who want automatic transaction tracking without cloud services
- Users of major Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOI)

**Use Cases**
- Personal expense tracking
- Transaction categorization and analysis
- Financial data backup and portability
- Offline finance management

**Not Recommended For**
- Users who require cloud sync across devices
- Users who need multi-user or family account features
- Users who require real-time bank account balance updates
- Users who need advanced financial analysis or reporting features

## Installation

1. Download the APK from the Releases page
2. Enable "Install from Unknown Sources" if needed
3. Install the APK
4. Grant SMS permissions when prompted (or use manual-only mode)
5. Start adding transactions

## First-Time Setup

1. **Grant Permissions**: Allow SMS access for automatic capture (optional)
2. **Create Categories**: Add your first categories (Main, Work, Personal, etc.)
3. **Test Transaction**: Add a manual transaction to verify setup
4. **Configure Security**: Set up biometric lock or PIN (optional)
5. **Create Backup**: Set up your first backup with a strong password

## Privacy & Security

- **No Data Upload**: All processing happens locally
- **Encrypted Storage**: Database and settings are encrypted
- **No Analytics**: No tracking or analytics code
- **No Accounts**: No registration or login required
- **Open Source**: Code is available for review

## Support

- **Issues**: Report bugs or request features on GitHub Issues
- **Documentation**: See README.md for detailed documentation
- **In-App Help**: Check "How it Works" section in Settings

## What's Next

Future releases will include:
- Improved SMS parsing accuracy
- Sender trust scoring
- Recurring transaction detection
- Spending forecasts
- Goal tracking
- Additional bank support

## Acknowledgments

Built with:
- Jetpack Compose
- Room Database
- SQLCipher
- Material 3
- Hilt

## License

Apache License 2.0 - See LICENSE file for details.

---

**Download**: [Release Assets](https://github.com/yourusername/instant-ledger/releases/tag/v0.1.0)

**Documentation**: [README.md](README.md)

**Report Issues**: [GitHub Issues](https://github.com/yourusername/instant-ledger/issues)

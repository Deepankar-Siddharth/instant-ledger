# Instant Ledger

A privacy-first, offline-only personal finance ledger for Android. Track your transactions automatically from bank SMS messages, with complete control over your financial data.

## Core Principles

**Privacy First**: All data processing happens locally on your device. No cloud sync, no accounts, no analytics, no data uploads.

**User Control**: Transactions are never auto-approved. Every transaction requires your explicit categorization and approval.

**Security**: Industry-standard SQLCipher encryption protects your financial data at rest. Biometric lock and PIN protection available.

**Simplicity**: Clean, utility-focused interface. Light theme only. No distractions, no gamification.

## Key Features

### Automatic Transaction Capture
- Monitors bank SMS messages in the background
- Extracts transaction details (amount, merchant, timestamp) locally
- Supports major Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOI)
- All parsing happens on-device using pattern matching

### Manual Transaction Entry
- Add cash, UPI, card, or bank transactions manually
- Full timestamp control for accurate record-keeping
- Merchant management with custom display names

### Transaction Management
- **Pending Transactions**: Newly detected transactions require categorization before approval
- **Approved Transactions**: Fully categorized transactions appear in your ledger
- Clear visual separation between pending and approved states
- Edit, delete, or modify any transaction at any time

### Category System
- Custom categories with visual identity (emoji or Material icon + color)
- Category metadata applies to all transactions using that category
- Categories are mandatory for transaction approval

### Merchant Management
- User-defined merchant display names
- Transaction-level merchant overrides
- Usage statistics per merchant
- Clean, reusable merchant data

### Time-Based Views
- **Today**: Current day's transactions with manual entry actions
- **Weekly**: Transactions grouped by day
- **Monthly**: Transactions grouped by week
- **Yearly**: Transactions grouped by month

### Data Export & Backup
- CSV/JSON export for external analysis
- Password-protected backup system
- Complete data restore capability
- Cross-device backup support

### Security Features
- SQLCipher database encryption
- EncryptedSharedPreferences for sensitive settings
- Biometric authentication (fingerprint/face unlock)
- PIN lock fallback
- Privacy mode (masks amounts until tapped)

## Privacy & Security Guarantees

### Data Storage
- All data is stored locally on your device
- Database encrypted using SQLCipher (AES-256)
- Settings encrypted using Android Keystore-backed EncryptedSharedPreferences
- No data leaves your device under any circumstances

### SMS Processing
- SMS content is processed locally only
- Raw SMS text is never displayed in the UI
- SMS parsing happens entirely on-device
- No SMS data is uploaded or transmitted

### Permissions
- **RECEIVE_SMS**: Required for automatic transaction capture (can be disabled)
- **READ_SMS**: Used for parsing bank SMS messages
- **POST_NOTIFICATIONS**: For transaction detection alerts
- **USE_BIOMETRIC**: Optional, for biometric lock feature
- **VIBRATE**: Optional, for haptic feedback

All permissions are requested only when needed. You can use the app in manual-entry-only mode without SMS permissions.

### Data Deletion
- Complete data deletion available in Settings
- Removes all transactions, categories, merchants, and settings
- Regenerates encryption keys
- No data recovery possible after deletion

## How It Works

### Transaction Flow

1. **SMS Received**: Bank sends transaction SMS to your device
2. **Local Parsing**: App extracts amount, merchant, and timestamp using pattern matching
3. **Notification**: You receive a notification about the detected transaction
4. **Categorization**: Transaction appears in "Today" tab under "Needs your confirmation"
5. **Approval**: Selecting a category automatically approves the transaction
6. **Storage**: Approved transaction is stored in encrypted database

### Manual Entry Flow

1. **Add Transaction**: Tap manual entry button (Cash, UPI, Card, or Bank)
2. **Enter Details**: Fill in amount, merchant (optional), category, and payment mode
3. **Save**: Transaction is saved as pending until categorized
4. **Categorize**: Assign a category to approve the transaction

### Backup & Restore

1. **Create Backup**: Settings → Backup & Restore → Create Backup
2. **Set Password**: Choose a strong password (remember it - cannot be recovered)
3. **Save File**: Choose location using Android file picker
4. **Restore**: Select backup file and enter password
5. **Restart**: App automatically restarts after successful restore

## Screenshots

<!-- Screenshots will be added here -->
<!-- 
- Main Dashboard (Today view)
- Transaction Detail Screen
- Category Management
- Merchant Management
- Backup & Restore Screen
- Settings Hub
-->

## Building Locally

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 34 (Android 14)
- Gradle 8.4+

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/yourusername/instant-ledger.git
cd instant-ledger
```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Build the debug APK:
```bash
./gradlew assembleDebug
```

5. Install on device:
```bash
./gradlew installDebug
```

### Build Configuration

The app uses:
- **Kotlin**: 1.9.20+
- **Gradle**: 8.4+
- **Android Gradle Plugin**: 8.1.4
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Dependencies

Key dependencies:
- Jetpack Compose (UI framework)
- Room (Database)
- SQLCipher (Database encryption)
- Hilt (Dependency injection)
- WorkManager (Background tasks)
- Material 3 (Design system)

See `app/build.gradle.kts` for complete dependency list.

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/instantledger/
│   │   │   ├── data/
│   │   │   │   ├── database/          # Room database, entities, DAOs
│   │   │   │   ├── model/             # Domain models
│   │   │   │   ├── preferences/       # SharedPreferences management
│   │   │   │   └── repository/       # Data repositories
│   │   │   ├── security/              # Encryption, biometrics
│   │   │   ├── service/               # SMS receiver, transaction capture
│   │   │   ├── ui/                    # Compose UI screens
│   │   │   ├── backup/                # Backup/restore functionality
│   │   │   ├── notification/          # Notification management
│   │   │   ├── worker/                # WorkManager workers
│   │   │   └── util/                  # Utility classes
│   │   ├── res/                       # Resources (layouts, drawables, values)
│   │   └── assets/                     # Asset files (SMS patterns)
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Roadmap

### Phase 1: Stability (Current)
- ✅ Core transaction capture and management
- ✅ Category and merchant management
- ✅ Backup and restore system
- ✅ Biometric and PIN security
- 🔄 SMS parsing improvements
- 🔄 Notification system refinements

### Phase 2: Trust & Retention
- Sender trust scoring
- Merchant learning engine
- Recurring transaction detection
- Spending forecasts
- Goal tracking

### Phase 3: Advanced Features
- Receipt scanning (OCR)
- Multi-currency support
- Advanced filtering and search
- Export formats (PDF reports)

## Contributing

Contributions are welcome. Please read our contributing guidelines before submitting pull requests.

### Development Guidelines

- Follow Kotlin coding conventions
- Use Material 3 design principles
- Maintain privacy-first approach
- Write tests for new features
- Update documentation for user-facing changes

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Disclaimer

Instant Ledger is provided as-is for personal finance tracking. The developers are not responsible for any financial decisions made using this application. Always verify transaction details with your bank statements.

## Support

For issues, feature requests, or questions:
- Open an issue on GitHub
- Check existing documentation
- Review the "How it Works" section in-app

---

**Instant Ledger** - Your financial data, your control, your device.

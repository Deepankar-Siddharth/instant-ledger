# Recommended Folder Structure

This document outlines the recommended folder structure for the Instant Ledger repository.

## Root Level

```
instant-ledger/
├── .github/                    # GitHub-specific files
│   ├── workflows/              # GitHub Actions CI/CD
│   └── ISSUE_TEMPLATE/         # Issue templates
├── app/                        # Android application module
├── docs/                       # Additional documentation
│   ├── ARCHITECTURE.md         # Architecture documentation
│   ├── CONTRIBUTING.md         # Contributing guidelines
│   └── SECURITY.md             # Security documentation
├── .gitignore                  # Git ignore rules
├── CHANGELOG.md                # Version changelog
├── LICENSE                     # Apache 2.0 license
├── README.md                   # Main project README
├── build.gradle.kts           # Root build file
├── gradle.properties           # Gradle properties
├── settings.gradle.kts         # Gradle settings
└── gradlew                     # Gradle wrapper (Unix)
```

## App Module Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/instantledger/
│   │   │   ├── InstantLedgerApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── database/          # Room database
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── dao/           # Data Access Objects
│   │   │   │   │   └── entities/      # Room entities
│   │   │   │   ├── model/             # Domain models
│   │   │   │   ├── preferences/       # SharedPreferences
│   │   │   │   └── repository/       # Data repositories
│   │   │   ├── security/              # Security & encryption
│   │   │   │   ├── EncryptionManager.kt
│   │   │   │   ├── PinLockManager.kt
│   │   │   │   └── SQLCipherLoader.kt
│   │   │   ├── service/               # Background services
│   │   │   │   ├── SMSReceiver.kt
│   │   │   │   ├── TransactionCaptureService.kt
│   │   │   │   └── NotificationTransactionListener.kt
│   │   │   ├── ui/                    # UI components
│   │   │   │   ├── main/              # Main screen
│   │   │   │   ├── dashboard/         # Dashboard components
│   │   │   │   ├── detail/            # Transaction detail
│   │   │   │   ├── manual/            # Manual entry
│   │   │   │   ├── settings/          # Settings screens
│   │   │   │   ├── navigation/        # Navigation
│   │   │   │   └── theme/             # Theme configuration
│   │   │   ├── backup/                # Backup/restore
│   │   │   │   └── BackupManager.kt
│   │   │   ├── notification/          # Notifications
│   │   │   │   └── TransactionNotificationManager.kt
│   │   │   ├── worker/                # WorkManager workers
│   │   │   │   ├── PendingReminderWorker.kt
│   │   │   │   └── PendingReminderScheduler.kt
│   │   │   └── util/                  # Utilities
│   │   │       ├── MerchantDisplayHelper.kt
│   │   │       └── CategoryManager.kt
│   │   ├── res/                       # Android resources
│   │   │   ├── drawable/              # Drawable resources
│   │   │   ├── mipmap-*/              # App icons
│   │   │   ├── values/                 # Values (strings, colors, themes)
│   │   │   └── xml/                   # XML resources
│   │   ├── assets/                     # Asset files
│   │   │   └── indian_bank_sms_patterns.json
│   │   └── AndroidManifest.xml
│   └── test/                          # Unit tests
│       └── java/com/instantledger/
├── build.gradle.kts
├── proguard-rules.pro
└── proguard-rules-consumer.pro
```

## Documentation Files

```
docs/
├── ARCHITECTURE.md              # System architecture overview
├── CONTRIBUTING.md              # Contribution guidelines
├── SECURITY.md                  # Security model and practices
├── SMS_PARSING.md              # SMS parsing implementation details
└── DATABASE_SCHEMA.md          # Database schema documentation
```

## GitHub Files

```
.github/
├── workflows/
│   └── ci.yml                  # CI/CD pipeline
└── ISSUE_TEMPLATE/
    ├── bug_report.md
    └── feature_request.md
```

## Key Files to Include

### Root Level
- `README.md` - Main project documentation
- `CHANGELOG.md` - Version history
- `LICENSE` - Apache 2.0 license
- `.gitignore` - Git ignore patterns
- `CONTRIBUTING.md` - Contribution guidelines (or in docs/)

### Documentation
- `docs/ARCHITECTURE.md` - Technical architecture
- `docs/SECURITY.md` - Security practices and threat model
- `docs/CONTRIBUTING.md` - Detailed contribution guide

### GitHub
- `.github/ISSUE_TEMPLATE/` - Issue templates
- `.github/workflows/` - CI/CD workflows
- `.github/PULL_REQUEST_TEMPLATE.md` - PR template

## File Naming Conventions

- **Kotlin files**: PascalCase (e.g., `BackupManager.kt`)
- **Resource files**: snake_case (e.g., `ic_launcher_foreground.xml`)
- **Documentation**: UPPERCASE (e.g., `README.md`, `CHANGELOG.md`)
- **Config files**: lowercase (e.g., `.gitignore`, `build.gradle.kts`)

## Recommended Additions

### For Open Source Release

1. **LICENSE** - Apache 2.0 license file
2. **CONTRIBUTING.md** - Contribution guidelines
3. **CODE_OF_CONDUCT.md** - Code of conduct (optional but recommended)
4. **SECURITY.md** - Security policy and reporting
5. **.github/ISSUE_TEMPLATE/** - Issue templates
6. **.github/PULL_REQUEST_TEMPLATE.md** - PR template
7. **docs/** - Additional documentation

### For CI/CD

1. **.github/workflows/ci.yml** - Automated testing
2. **.github/workflows/release.yml** - Release automation
3. **.github/workflows/lint.yml** - Code quality checks

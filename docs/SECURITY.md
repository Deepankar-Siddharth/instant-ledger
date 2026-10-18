# Instant Ledger — Security (docs/SECURITY.md)

## Threat model

Instant Ledger stores personal financial data (transactions, merchants, categories,
goals) in a SQLCipher-encrypted Room database and creates password-protected encrypted
backup files. The app is not a default SMS app by design; bank SMS capture is a
best-effort secondary path via a notification listener and SMS receiver.

Key assets protected:
- Encrypted Room database (SQLCipher passphrase stored in EncryptedSharedPreferences backed by Android Keystore)
- User backup password (never stored; used only to derive a per-operation key)
- Encryption key material (kept in Android Keystore, never exposed as raw bytes)

## Backup format

### Format version 2 (current)

Encrypted ZIP container with an authenticated header:

| Field | Size | Description |
|-------|------|-------------|
| magic | 4 bytes | `ILB2` |
| version | 2 bytes (big-endian) | `2` |
| algorithm | 1 byte | `1` = AES-256-GCM |
| kdf | 1 byte | `1` = PBKDF2-HMAC-SHA256 |
| kdf iterations | 4 bytes (big-endian) | `600000` |
| salt length | 1 byte | `16` |
| salt | 16 bytes | random per backup |
| nonce length | 1 byte | `12` |
| nonce | 12 bytes | random per encryption operation |
| auth tag length | 2 bytes (big-endian) | `16` |
| auth tag | 16 bytes | GCM authentication tag |
| payload | remainder | GCM-encrypted ZIP archive |

Encryption: AES-256-GCM with a key derived via PBKDF2-HMAC-SHA256 (600,000 iterations,
random 16-byte salt) from the user-provided password. A random 12-byte nonce is used per
operation. The 16-byte GCM authentication tag is stored in the header and verified before
decryption; any alteration of ciphertext or header causes decryption to fail.

### Format version 1 (legacy)

Old backups created before this hardening use:

- AES-256-CBC / PKCS5Padding (no authentication)
- Fixed salt `"InstantLedgerBackup2024"`
- PBKDF2-HMAC-SHA256, 10,000 iterations
- 16-byte random IV prepended to the encrypted ZIP stream

Legacy backups remain restorable via the legacy restore path for backward compatibility,
but new backups are always created in format version 2.

## Restore safety

Restore is atomic: the entire backup is parsed and validated in memory (header,
authentication tag, manifest, all entries) before any change is applied to the database
or SharedPreferences. All changes are applied in a single Room transaction; on any
failure (bad password, tampered data, malformed header, unsupported version, truncated
file) the transaction is rolled back and existing records are left unchanged.

## Encryption key handling

`EncryptionManager.getEncryptionKey()` no longer returns raw key bytes
(`secretKey.encoded`). Key material stays in Android Keystore (GCM mode, no padding,
PURPOSE_ENCRYPT | PURPOSE_DECRYPT). Callers that need encryption/decryption must use the
Keystore-backed key directly rather than exporting it.

## Logging

No passwords, keys, passphrases, or financial data (amounts, merchants, raw SMS text) are
written to `android.util.Log`. Logs use operation-level messages only.

## Manifest

`android:allowBackup="false"` — system auto-backup is disabled; backup/restore is
performed exclusively through the app-owned, password-encrypted backup path.

## Known limitations

- SQLCipher native libraries must be loaded before database access; failure surfaces as
  `EncryptionException`.
- PBKDF2 iteration count (600,000) may be slow on low-end devices; this is intentional
  for key derivation strength.
- Legacy format version 1 restores are supported for data migration but produce no new
  backups in that format.

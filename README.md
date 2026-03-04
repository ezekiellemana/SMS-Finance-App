# SMS Finance Widget — Android App

A complete Android application that automatically reads financial SMS messages from Tanzanian banks and mobile money services, extracts transaction data, and displays real-time summaries through home screen widgets and a full dashboard.

---

## 🏗 Architecture

```
MVVM + Clean Architecture + Hilt DI
```

```
SMS → BroadcastReceiver → PatternEngine → Room DB → ViewModel → Compose UI
                                                  ↓
                                            AppWidgetProvider
```

---

## 📁 Project Structure

```
app/src/main/
├── kotlin/com/smsfinance/
│   ├── SMSFinanceApp.kt              # @HiltAndroidApp entry point
│   ├── data/
│   │   ├── entity/TransactionEntity.kt   # Room @Entity
│   │   ├── dao/TransactionDao.kt         # Room @Dao with Flow
│   │   └── database/AppDatabase.kt      # Encrypted Room DB (SQLCipher)
│   ├── domain/
│   │   └── model/Transaction.kt         # Domain models
│   ├── repository/
│   │   └── TransactionRepository.kt     # Single source of truth
│   ├── viewmodel/
│   │   ├── DashboardViewModel.kt        # Dashboard data + summary
│   │   ├── TransactionViewModel.kt      # List + filter + CRUD
│   │   └── SettingsViewModel.kt         # Privacy, PIN, biometric
│   ├── ui/
│   │   ├── MainActivity.kt              # Nav host + permissions
│   │   ├── dashboard/DashboardScreen.kt # Balance card + chart + recent txns
│   │   ├── transactions/
│   │   │   ├── TransactionListScreen.kt  # Filterable full list
│   │   │   └── TransactionDetailScreen.kt # Detail + AddTransaction
│   │   ├── auth/PinScreen.kt            # Numeric PIN keypad
│   │   ├── settings/SettingsScreen.kt   # Privacy/security/theme
│   │   └── theme/Theme.kt              # Material3 dark/light
│   ├── widget/
│   │   ├── SmallFinanceWidget.kt        # 2×1 balance widget
│   │   ├── MediumFinanceWidget.kt       # 4×2 with last 3 txns
│   │   └── WidgetUpdateManager.kt       # Triggers widget refresh
│   ├── receiver/
│   │   ├── SmsReceiver.kt              # Listens for SMS_RECEIVED
│   │   └── BootReceiver.kt             # Restores widget on boot
│   ├── util/
│   │   ├── SmsPatternEngine.kt         # Core regex parser
│   │   └── PreferencesManager.kt       # DataStore wrapper
│   └── di/
│       └── AppModule.kt                # Hilt providers
└── res/
    ├── layout/
    │   ├── widget_small.xml            # 2×1 widget layout
    │   └── widget_medium.xml           # 4×2 widget layout
    ├── xml/
    │   ├── small_widget_info.xml       # AppWidgetProviderInfo
    │   ├── medium_widget_info.xml
    │   ├── backup_rules.xml
    │   └── data_extraction_rules.xml
    ├── drawable/widget_background.xml  # Gradient rounded rect
    └── values/strings.xml, themes.xml
```

---

## 🔑 Key Features

### SMS Detection & Parsing
- **Supported senders**: NMB, CRDB, M-Pesa (Vodacom), Mixx by Yas
- **Pattern engine** handles TZS amounts in multiple formats
- Detects: `received`, `withdrawn`, `sent to`, `payment of`, Swahili equivalents
- Processes within ~2 seconds using `goAsync()` + Coroutines

### Sample SMS Messages (Test Data)

```
# NMB Deposit
FROM: NMB
"You have received TZS 50,000.00 from JOHN DOE. Your balance is TZS 250,000.00. Ref: NMB123456789"

# CRDB Withdrawal  
FROM: CRDB
"CRDB: Payment of TZS 30,000 to LIPA NA CRDB. Balance: TZS 120,000. Ref: TXN987654"

# M-Pesa Deposit
FROM: MPESA
"MR JOHN confirmed. You have received TZS 25,000 from JANE DOE 0712345678 on 01/12/24. New M-Pesa balance is TZS 75,000."

# Mixx Withdrawal (Swahili)
FROM: MIXX
"Umetuma TZS 5,000 kwa 0712345678. Salio lako ni TZS 40,000."
```

### Database (Room + SQLCipher)
- AES-256 encrypted database
- Passphrase stored in EncryptedSharedPreferences (AES-256-GCM)
- Indexed on `type` and `date` columns for fast queries
- Supports 100k+ transactions efficiently

### Widgets
| Widget | Size | Features |
|--------|------|----------|
| Small  | 2×1  | Balance, monthly income/expenses |
| Medium | 4×2  | Balance summary + last 3 transactions |

Both widgets:
- Tap to open full dashboard
- Privacy mode (hides all amounts with ••••)
- Auto-update after every new SMS

### Security
- SQLCipher database encryption
- EncryptedSharedPreferences for keys
- Optional PIN (SHA-256 hashed, never stored raw)
- Optional biometric authentication
- Privacy mode toggle
- No data ever leaves device

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android device/emulator with API 26+

### Setup
1. Clone / copy project to Android Studio
2. Sync Gradle dependencies
3. Run on device (emulator SMS testing requires ADB commands)

### Testing SMS Parsing
```bash
# Send test SMS via ADB
adb emu sms send NMB "You have received TZS 50,000.00 from JOHN DOE. Balance: TZS 250,000.00"
adb emu sms send MPESA "You have received TZS 25,000 from JANE 0712345678. New M-Pesa balance is TZS 75,000."
```

---

## 📦 Dependencies

| Library | Purpose |
|---------|---------|
| Room 2.6.1 | Local database |
| Hilt 2.52 | Dependency injection |
| Jetpack Compose | Modern UI |
| SQLCipher | Database encryption |
| Security Crypto | EncryptedSharedPreferences |
| Biometric | Fingerprint/face auth |
| DataStore | Preferences storage |
| Coroutines + Flow | Async/reactive |
| Material3 | Design system |

---

## 🔒 Privacy

All processing is **100% local**. SMS data never leaves the device. The app:
- Does NOT require internet permission
- Does NOT use analytics or crash reporting
- Excludes database from Android backup

---

## 🗺 Future Enhancements (per SRS)
- Cloud backup (opt-in)
- Budget planning with alerts
- AI spending predictions
- Export to Excel/PDF
- Bank API integration
- Swahili full localization

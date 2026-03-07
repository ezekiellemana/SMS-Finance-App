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

### SMS Detection & Parsing — `SmsPatternEngine` v3.1

**Supported senders (Tanzania):**

| Category | Services |
|----------|----------|
| Banks | NMB, CRDB, NBC, Equity, Stanbic, ABSA, EXIM, DTB |
| Mobile Money | M-Pesa (Vodacom), Mixx by Yas (formerly Tigo Pesa), Airtel Money, HaloPesa, T-Pesa (TTCL), AzamPesa, SelcomPesa, EzyPesa (EITC) |
| International | NALA (international transfers → M-Pesa wallet) |
| Loans | SONGESHA (standalone + embedded in M-Pesa / HaloPesa) |

- Handles Swahili and English formats, TZS/TSH/Tsh prefixes, no-space amounts (`TSH66000`), and double-prefix (`TZS TZS`)
- Sender alias map resolves all real-world sender ID variants to internal keys (`"VODACOM-TZ"` → `mpesa`, `"HaloPesa"` → `halo`)
- Reference extraction supports `Kumb:`, `REF:`, `Ref:`, and transaction-ID-at-start formats

#### Reminder / Alert Filtering — `isReminderSms()`

All SMS where **no money has moved** are silently dropped before pattern matching runs, so they never corrupt the user's balance. Covers:
- SONGESHA loan reminders (`Unakumbushwa kurejesha deni lako la…`)
- NMB Mshiko Fasta reminders (`umebakiwa na Siku N kurejesha…`, `Muda wa kurejesha…umefika`)
- Generic alerts: low balance, OTP/PIN, account suspended, promotional, overdue payment

#### Real SMS Formats Tested

```
# NMB — Deposit
"Kiasi cha TZS 50000 kimewekwa kwenye akaunti yako inayoishia na 34147 tarehe 04-03-2026."

# NMB — Withdrawal (send)
"Kumb: GWX101931762870 Imethibitishwa. Kiasi cha TSH66000 kimetumwa kutoka katika akaunti inayoishia na 5389 kwenda EZEKIEL AUGUSTINO LEMANA 255752772587."

# NMB — Loan repayment deducted
"801NDGL260270012. Kiasi cha TZS TZS 19,300.00 kimetolewa kwenye akaunti yako inayoishia na 34147 kurejesha Mshiko Fasta."

# NMB — Reminder (ignored, no transaction stored)
"520NDGL260460008 Mpendwa Mteja. umebakiwa na Siku 1 kurejesha TZS 39,900.00 uliyopokea kupitia Mshiko Fasta."

# M-Pesa — Deposit
"DC22N8FK524 Imethibitishwa. Umepokea Tsh100.00 kutoka 255792892289 - ESTHER BALADIGA mnamo ..."

# HaloPesa — Deposit
"Umepokea 20,000 TZS kutoka STEWART ERNEST MASIMA (0626233330). Salio jipya: 30,666.00 TZS"

# SONGESHA — Loan deduction (stored as WITHDRAWAL)
"Umelipa Tsh 100 kama makato ya deni la  SONGESHA. Salio la deni lako kwa sasa ni Tsh 32,058."

# SONGESHA — Reminder (ignored)
"Unakumbushwa kurejesha deni lako la Tsh 28507 la SONGESHA. Ili kulipa deni weka pesa kwenye akaunti yako."

# NALA — International transfer received
"Mambo! Beatha Massawe sent you TSh253,584.00 to your M-Pesa wallet using NALA nala.com"
```

---

### SMS History Importer — `SmsHistoryImporter`

- On first setup, scans the device inbox for SMS from all registered financial senders
- Import window: **last 3 days** per sender (prevents flooding the DB with old history)
- Runs once during onboarding; respects the `setupAt` timestamp so historical messages never overwrite the user's entered opening balances

---

### Balance Calculation

- **Formula:** `openingBalance + newIncome(after setupAt) − newExpenses(after setupAt)`
- Opening balances stored in **DataStore only** — never as fake transactions in Room
- `setupAt` = `SETUP_COMPLETED_AT` from DataStore — transactions before this are excluded from the balance calculation but still visible in history
- Hero card income/expenses show **all-time totals** regardless of `setupAt`
- Fully reactive via nested `combine()` flows — balance updates instantly when DataStore or DB changes, no app restart required

---

### Onboarding — Multi-page with Bottom Sheet Service Picker

- **Page 1** — Welcome + name input
- **Page 2** — Service selection via bottom sheet (Banks / Mobile Money separately)
    - Selectable: all 8 banks + 9 mobile money providers including NALA
- **Page 3** — Opening balance input per selected service
    - Auto-formatted money input (raw digits stored, displayed as `100,000`)
    - `TZS` suffix shown inline when field has a value
- Smooth HorizontalPager transitions: fade + scale + parallax + subtle `rotationY`
- Typewriter greeting animation on Dashboard after onboarding completes
- Onboarding state persisted in DataStore — survives process death

---

### Multi-User / Family Accounts — `MultiUserViewModel`

- Multiple named profiles each with emoji avatar, hex colour accent, and optional photo
- Active profile drives colour theming app-wide
- Profile photo picked from device gallery via `ActivityResultContracts.GetContent`
- DB schema: `user_profiles` table with `photo_uri` column (Room migration v6)
- Family Accounts screen loads onboarding user name as default profile name

---

### Profile Colour Theming

Active profile's hex accent colour flows through the entire UI:
- Bottom nav bar: animated ring on profile avatar tab when selected
- Hero balance card: 1.5dp gradient border + ambient glow orb
- Transaction row cards: 1dp gradient border
- Fallback to `AccentTeal` when no profile colour is set

---

### Dashboard

- Real-time balance card with animated glow
- Recent Activity fills available screen height without scrolling — `BoxWithConstraints` calculates max visible cards from actual height; "See all" appears only when there are more
- Typewriter greeting on first load after onboarding
- Reactive — updates the instant a new SMS is processed

---

### Full Application Screens

| Screen | Description |
|--------|-------------|
| Dashboard | Balance hero, income/expense summary, recent transactions |
| Transaction List | Full history, search, date filter, swipe-to-delete |
| Transaction Detail | Full SMS body, category, reference, manual edit |
| Alerts | Spending limit alerts with push notifications |
| Budget | Category budgets with progress tracking |
| Recurring | Auto-detected recurring transactions |
| Investments | Savings goals and investment portfolio tracker |
| Family Accounts | Multi-user profile management |
| Settings | PIN, biometric, language, theme, privacy mode |

All screens share `AppScreenScaffold` with a consistent `BigFab` pattern.

---

### Bottom Navigation

- 5 tabs: Home, Transactions, (FAB), Alerts, Settings
- Profile avatar tab (58dp with emoji + optional photo + colour ring) replaces generic icon
- Height 88dp, rounded corners 28dp, horizontal gradient glow top border
- Scale-bounce animation on tab select (1.12×)

---

### Localisation — i18n

- **English** (`values/strings.xml`) — primary
- **Swahili** (`values-sw/strings.xml`) — complete translations for all screens
- Language toggle in Settings using `LocaleHelper` + `Activity.recreate()`
- `lint.xml` suppresses `MissingTranslation` for known untranslatable strings

---

### Database (Room + SQLCipher)

- AES-256 encrypted with passphrase in EncryptedSharedPreferences (AES-256-GCM)
- Current schema version: **6** (`photo_uri` column added to `user_profiles`)
- Indexed on `type` and `date` for fast queries
- Supports 50,000+ transactions efficiently

---

### Widgets

| Widget | Size | Features |
|--------|------|----------|
| Small  | 2×1  | Balance, monthly income/expenses |
| Medium | 4×2  | Balance summary + last 3 transactions |

Both widgets auto-update after every new SMS and respect privacy mode (amounts hidden with ••••).

---

### Security

- SQLCipher AES-256 database encryption
- EncryptedSharedPreferences for passphrase storage
- Optional PIN (SHA-256 hashed, never stored raw)
- Optional biometric authentication
- Privacy mode toggle (widget + app)
- 100% local — no internet permission, no analytics, no crash reporting
- Database excluded from Android backup

---

### Export

- Export transactions to **Excel (XLSX)** via SheetJS
- Export transactions to **PDF** via iText
- Date-range filtering before export

---

### AI Spending Predictions

- Statistical model analyses spending patterns from transaction history
- Predicts likely end-of-month spend per category
- Surfaces as a card on the Dashboard

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android device/emulator API 26+

### Stable Dependency Matrix

```toml
agp                       = "8.7.3"
kotlin                    = "2.1.21"
ksp                       = "2.1.21-2.0.2"
hilt                      = "2.55"
hiltNavigation            = "1.2.0"
room                      = "2.7.1"
composeBom                = "2025.05.01"
navigation                = "2.8.9"
workManager               = "2.11.1"
datastore                 = "1.1.4"
lifecycleViewmodelCompose = "2.10.0"
desugarJdkLibs            = "2.1.5"
coil                      = "2.7.0"
splashscreen              = "1.2.0"
```

> ⚠️ These versions are confirmed compatible. Upgrading AGP, Kotlin, KSP, or Hilt independently **will** cause build failures.

### Setup
1. Clone project into Android Studio
2. Sync Gradle with the exact versions above
3. Run on a real device for SMS testing (emulator SMS via ADB works for basic tests)

### Testing SMS Parsing via ADB

```bash
adb emu sms send NMB "Kiasi cha TZS 50000 kimewekwa kwenye akaunti yako inayoishia na 34147 tarehe 04-03-2026."
adb emu sms send M-PESA "DC22N8FK524 Imethibitishwa. Umepokea Tsh100.00 kutoka 255792892289 - ESTHER BALADIGA mnamo 2026-01-01 10:00"
adb emu sms send SONGESHA "Umelipa Tsh 100 kama makato ya deni la  SONGESHA. Salio la deni lako kwa sasa ni Tsh 32,058."
adb emu sms send NALA "Mambo! John Doe sent you TSh50,000.00 to your M-Pesa wallet using NALA nala.com"
```

---

## 📦 Dependencies

| Library | Purpose |
|---------|---------|
| Room 2.7.1 | Local database with Flow |
| Hilt 2.55 | Dependency injection |
| Jetpack Compose (BOM 2025.05.01) | Modern UI |
| SQLCipher | AES-256 database encryption |
| Security Crypto | EncryptedSharedPreferences |
| Biometric | Fingerprint / face auth |
| DataStore 1.1.4 | Preferences + opening balances |
| WorkManager 2.11.1 | Background jobs (reminders, recurring) |
| Coil 2.7.0 | Profile photo loading |
| Coroutines + Flow | Async / reactive streams |
| Material3 | Design system |

---

## 🔒 Privacy

All processing is **100% local**. SMS data never leaves the device. The app:
- Does NOT require internet permission
- Does NOT use analytics or crash reporting
- Excludes the database from Android backup

---

## 🗺 Remaining Future Enhancements
- Cloud backup (Google Drive — opt-in)
- Bank API direct integration
- Budget alerts push notifications (partially implemented)
- Tablet two-pane adaptive layout
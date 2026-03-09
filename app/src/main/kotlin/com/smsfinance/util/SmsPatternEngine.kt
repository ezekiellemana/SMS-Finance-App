package com.smsfinance.util

import android.util.Log
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType

/**
 * SMS Pattern Recognition Engine — v3.2
 *
 * Key rule: ONLY SMS where money has actually moved are stored as transactions.
 * Reminder / alert messages (loan due, low balance, OTP, promo) are detected
 * by isReminderSms() and silently ignored so they never corrupt the balance.
 *
 * Tested against real SMS samples from:
 * ── Tanzania ──────────────────────────────────────────────────────
 *  HaloPesa         — Deposit (kutoka NAME(phone)), Withdraw (kwa NAME(phone,Network),
 *                     kwa PHONE_ONLY, kwenye akaunti bank, kwenda wallet),
 *                     SONGESHA loan deduction, English bill payment
 *  Vodacom M-Pesa   — Deposit (kutoka NUMBER-NAME mnamo, M-PESA FAIDA tarehe),
 *                     Withdraw (AMT imetumwa kwa ... kwenye akaunti,
 *                     NAME has received Tsh [English], Umenunua airtime,
 *                     AMT imetolewa [agent])
 *  CRDB Bank        — Deposit, Withdraw (agent), SimBanking transfers
 *  NMB Bank         — Deposit (kimewekwa), Withdrawal (kimetolewa/kimetumwa/kikamilifu),
 *                     Mshiko Fasta reminders filtered
 *  Mixx by Yas      — Deposit, Withdraw (kupokea/kutuma + umefanikiwa)
 *  Airtel Money     — Deposit, Withdraw (Swahili + English confirmed)
 *  Tigo Pesa        — Deposit, Withdraw (Swahili + Kiasi format, now Mixx by Yas)
 *  T-Pesa (TTCL)    — Deposit, Withdraw (Swahili)
 *  AzamPesa         — Deposit, Withdraw (Swahili + English)
 *  SelcomPesa       — Deposit, Withdraw (Swahili + English)
 *  EzyPesa (EITC)   — Deposit, Withdraw (Swahili + English)
 *  NALA             — Deposit (international transfer → M-Pesa wallet)
 * ─────────────────────────────────────────────────────────────────
 */
object SmsPatternEngine {

    private const val TAG = "SmsPatternEngine"

    // ── Recognised sender IDs ─────────────────────────────────────────────────
    val FINANCIAL_SENDERS = setOf(
        // Tanzania Banks
        "NMB", "NMBTZ", "NMB-BANK", "NMBBANK",
        "CRDB", "CRDBBANK", "CRDB-BANK", "CRDB BANK",
        "NBC", "NBCBANK",
        "EQUITY", "EQUITYTZ",
        "STANBIC", "STANBICTZ",
        "AZANIA", "AZANIABANK",
        "EXIM", "EXIMBANK",
        "BOA", "BOATZ",
        "DTB", "DTBTZ",
        "ABSA", "ABSATZ",
        "NCBA", "NCBATZ",
        // Tanzania Mobile Money — real sender IDs as they appear on device
        "M-PESA", "MPESA", "VODACOM", "VODACOM-TZ",   // Vodacom sends as "M-PESA"
        "MIXX", "MIXXBYYAS", "MIXX-YAS", "YAS",
        "TIGOPESA", "TIGO", "TIGO-PESA", "TigoPesa",  // legacy — rebranded to Mixx by Yas
        "AIRTELMONEY", "AIRTEL", "AIRTEL-MONEY", "AIRTEL-TZ",
        "HALOPESA", "HALOTEL", "HALO", "HaloPesa",    // Halotel sends as "HaloPesa"
        "T-PESA", "TPESA", "TTCL",                    // TTCL T-Pesa
        "AZAMPESA", "AZAM", "AzamPesa",               // Azam Media AzamPesa
        "SELCOMPESA", "SELCOM", "SelcomPesa",          // Selcom SelcomPesa
        "EZYPESA", "EZY-PESA", "EzyPesa", "EITC",     // EITC EzyPesa (formerly Zantel)
        "NALA",                                        // NALA international transfers → M-Pesa
        // Loans — Songesha messages come from both Vodacom (M-PESA) and Halotel (HaloPesa)
        // but SONGESHA also appears as its own standalone sender ID
        "SONGESHA",
        // Kenya
        "SAFARICOM", "MPESA-KE",
        "KCB", "KCBBANK",
        "COOPERATIVE", "CO-OPBANK",
        // Uganda
        "MTNMOBILE", "MTN", "MTN-MF",
        "AIRTELUG"
    )

    // ── Amount regex — handles all TZS/TSH/Tsh/KES/UGX formats ──────────────
    private const val AMT =
        """(?:TZS\s?|TSH\s?|Tsh\s?|TSh\s?|KES\s?|Ksh\s?|UGX\s?|USh\s?)?([\d,]+(?:\.\d{1,2})?)"""

    private data class SmsPattern(
        val senderMatch: String,
        val bodyPattern: Regex,
        val type: TransactionType,
        val amountGroup: Int = 1,
        val sourceLabel: String,
        val category: String = ""
    )

    private val PATTERNS: List<SmsPattern> = listOf(

        // ══════════════════════════════════════════════════════════════════════
        // HALOPESA
        // ══════════════════════════════════════════════════════════════════════
        //
        // Confirmed real formats (sender ID: HaloPesa):
        //
        // DEPOSIT:
        //   (a) "Umepokea 20,000 TZS kutoka STEWART ERNEST MASIMA (0626233330)"
        //
        // WITHDRAWAL (send money to wallet / person):
        //   (a) "Tnx 6061531825522767. Umetuma 22,000 TZS kwa ABEL AUGUSTINO
        //        LAIZA (0745870024, M-Pesa) tarehe 03/03/2026 14:46:26.
        //        Maudhui: ... Ada: 520 TZS. Salio jipya: 2,691.87 TZS."
        //       → Amount BEFORE TZS, recipient has phone+network in parens
        //   (b) "IMEFANIKIWA! Tnx 6065369528091332. Umetuma 2,100 TZS kwa
        //        ABEL AUGUSTINO LAIZA (0745870024, M-Pesa) tarehe ..."
        //       → Same format, prefixed with "IMEFANIKIWA!"
        //
        // WITHDRAWAL (bill payment / airtime — phone number only, no name):
        //   (a) "IMEFANIKIWA! Tnx 6065627700451729. Umelipa 500 TZS kwa
        //        692524898 tarehe 07/03/2026 17:26:25.
        //        Ada: O TZS. Salio jipya: 51.87 TZS."
        //       → Phone number only after "kwa"
        //
        // WITHDRAWAL (bank transfer):
        //   (a) "Umetuma TSH 540,000.00 kwenye akaunti namba ... ya NMB"
        //
        // WITHDRAWAL (wallet-to-wallet, named destination):
        //   (a) "Umetuma TSH 9,000.00 kwenda Mixx by Yas, jina ROBERT TAIRO"
        //
        // WITHDRAWAL (SONGESHA loan deduction):
        //   (a) "Umelipa Tsh 100 kama makato ya deni la  SONGESHA."
        //
        // WITHDRAWAL (English bill payment):
        //   (a) "Transaction ID: 6060... You have paid 1,000 TZS for phone..."

        // ── DEPOSIT: Umepokea AMT TZS kutoka NAME (phone) ────────────────────
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]mepokea\s+$AMT\s*(?:TZS|TSH|Tsh)\s+kutoka\s+.+?\([+\d]+\)""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "HaloPesa", category = "Mobile Money"
        ),
        // ── WITHDRAWAL: Umetuma AMT TZS kwa NAME (phone, Network) ───────────
        // "Umetuma 22,000 TZS kwa ABEL AUGUSTINO LAIZA (0745870024, M-Pesa)"
        // "IMEFANIKIWA! Tnx ... Umetuma 2,100 TZS kwa NAME (phone, Network)"
        // Amount is plain digits before TZS (no Tsh/TSH prefix here)
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]metuma\s+$AMT\s*(?:TZS|TSH|Tsh)\s+kwa\s+.+?\(\d+.*?\)""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa", category = "Mobile Money"
        ),
        // ── WITHDRAWAL: Umelipa AMT TZS kwa PHONE_NUMBER (bill/airtime) ──────
        // "Umelipa 500 TZS kwa 692524898 tarehe ..."
        // Phone-number-only after "kwa" (no name, no parens)
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]melipa\s+$AMT\s*(?:TZS|TSH|Tsh)\s+kwa\s+\d{9,13}(?:\s|$)""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa Payment", category = "Bill Payment"
        ),
        // ── WITHDRAWAL: Umetuma TSH AMT kwenye akaunti (bank transfer) ───────
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TSH|TZS|Tsh)\s*$AMT\s+kwenye\s+akaunti""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa → Bank", category = "Transfer"
        ),
        // ── WITHDRAWAL: Umetuma TSH AMT kwenda WALLET, jina NAME ─────────────
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TSH|TZS|Tsh)\s*$AMT\s+kwenda\s+.+?,""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa → Wallet", category = "Transfer"
        ),
        // ── WITHDRAWAL: SONGESHA loan deduction ──────────────────────────────
        // "Umelipa Tsh 100 kama makato ya deni la  SONGESHA."
        // \s+ handles the double-space before SONGESHA seen in real messages.
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]melipa\s+(?:Tsh|TSH|TZS)\s*$AMT\s+kama\s+makato\s+ya\s+deni\s+la\s+SONGESHA""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Loan", category = "Loan Repayment"
        ),
        // ── WITHDRAWAL: English "You have paid AMT TZS for phone" ───────────
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Yy]ou\s+have\s+paid\s+$AMT\s+TZS""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa Payment", category = "Bill Payment"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // VODACOM M-PESA
        // ══════════════════════════════════════════════════════════════════════
        //
        // Confirmed real formats (sender ID: M-PESA):
        //
        // DEPOSIT:
        //   (a) "DC22N8FK524 Imethibitishwa. Umepokea Tsh100.00 kutoka
        //        255792892289 - ESTHER BALADIGA mnamo ..."
        //   (b) "DC9ONDODMFA Imethibitishwa.Umepokea Tsh14.00 kutoka
        //        219777 - M-PESA FAIDA tarehe 9/3/26 saa 12:21 AM
        //        Salio lako la M-Pesa ni Tsh1,473.60."
        //       → M-PESA FAIDA (interest credit) — income to balance
        //
        // WITHDRAWAL (send money):
        //   (a) "DC76NC9YC1A Imethibitishwa. Tsh5,000.00 imetumwa kwa
        //        TIPS-AIRTELMONEY kwenye akaunti namba 255789452917 tarehe..."
        //       → Amount BEFORE keyword, sent to another wallet
        //   (b) "DC76NC9YC1A Confirmed. SAMWEL TARIMO has received Tsh..."
        //       → English format: someone "has received" = you sent = withdrawal
        //
        // WITHDRAWAL (airtime / bundle purchase):
        //   (a) "DC79NCMOEUX Imethibitishwa. Umenunua Tsh500.00 muda wa
        //        maongezi kwa AIRTEL-255692524898, tarehe 7/3/26 5:31 PM.
        //        Salio lako la ni Tsh1,459.60."
        //       → Airtime purchase = outgoing money
        //
        // WITHDRAWAL (agent / legacy):
        //   (a) "Tsh100.00 imetolewa kwenye akaunti yako ya M-Pesa"

        // ── DEPOSIT: Umepokea ... kutoka NUMBER - NAME mnamo ─────────────────
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:Tsh|TZS|TSH)\s*$AMT\s+kutoka\s+[\d+]+\s*-\s*.+?\s+mnamo""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "M-Pesa", category = "Mobile Money"
        ),
        // ── DEPOSIT: Umepokea ... kutoka NUMBER - NAME tarehe (M-PESA FAIDA) ─
        // "DC9ONDODMFA Imethibitishwa.Umepokea Tsh14.00 kutoka 219777 - M-PESA FAIDA tarehe..."
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:Tsh|TZS|TSH)\s*$AMT\s+kutoka\s+[\d]+\s*-\s*.+?(?:tarehe|saa|\d{1,2}/\d{1,2}/\d{2,4})""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "M-Pesa Faida", category = "Mobile Money"
        ),
        // ── WITHDRAWAL: AMT imetumwa kwa NAME/NETWORK kwenye akaunti ─────────
        // "Tsh5,000.00 imetumwa kwa TIPS-AIRTELMONEY kwenye akaunti namba..."
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""$AMT\s+imetumwa\s+kwa\s+.+?\s+kwenye\s+akaunti""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "M-Pesa", category = "Mobile Money"
        ),
        // ── WITHDRAWAL: English "has received Tsh..." (you sent = withdrawal) ─
        // "DC76NC9YC1A Confirmed. SAMWEL TARIMO has received Tsh..."
        // Captures amount after "has received Tsh/TZS"
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex(""".+?\s+has\s+received\s+(?:Tsh|TZS|TSH)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "M-Pesa", category = "Mobile Money"
        ),
        // ── WITHDRAWAL: Umenunua ... muda wa maongezi (airtime purchase) ─────
        // "Imethibitishwa. Umenunua Tsh500.00 muda wa maongezi kwa AIRTEL-..."
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""[Uu]menunua\s+(?:Tsh|TZS|TSH)\s*$AMT\s+muda\s+wa\s+maongezi""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "M-Pesa Airtime", category = "Airtime"
        ),
        // ── WITHDRAWAL: AMT imetolewa kwenye akaunti yako ya M-Pesa ──────────
        // "Tsh100.00 imetolewa kwenye akaunti yako ya M-Pesa"
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""$AMT\s+imetolewa\s+kwenye\s+akaunti\s+yako\s+ya\s+M-Pesa""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "M-Pesa", category = "Mobile Money"
        ),
        // NOTE: "Unakumbushwa kurejesha deni la SONGESHA" (reminder) intentionally excluded.

        // ══════════════════════════════════════════════════════════════════════
        // CRDB BANK
        // ══════════════════════════════════════════════════════════════════════

        // "you have received TZS250,000.00 in your account number: ..."
        SmsPattern(
            senderMatch = "crdb",
            bodyPattern = Regex("""you\s+have\s+received\s+(?:TZS|TSH|Tsh)\s*$AMT\s+in\s+your\s+account""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "CRDB Bank", category = "Bank"
        ),
        // "Umefanikiwa Kutoa TSh15,000 kwa Wakala"
        SmsPattern(
            senderMatch = "crdb",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]utoa\s+(?:TSh|TZS|TSH)\s*$AMT\s+kwa\s+[Ww]akala""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "CRDB Agent", category = "Cash Out"
        ),
        // "Muamala umefanikiwa TZS1000 HALOTEL kwenda NAME 0..."
        SmsPattern(
            senderMatch = "crdb",
            bodyPattern = Regex("""[Mm]uamala\s+umefanikiwa\s+(?:TZS|TSH|Tsh)\s*$AMT\s+HALOTEL\s+kwenda""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "CRDB → HaloPesa", category = "Transfer"
        ),
        SmsPattern(
            senderMatch = "crdb",
            bodyPattern = Regex("""[Mm]uamala\s+umefanikiwa\s+(?:TZS|TSH|Tsh)\s*$AMT\s+VODACOM\s+kwenda""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "CRDB → M-Pesa", category = "Transfer"
        ),
        SmsPattern(
            senderMatch = "crdb",
            bodyPattern = Regex("""[Mm]uamala\s+umefanikiwa\s+(?:TZS|TSH|Tsh)\s*$AMT\s+(?:YAS/ZANTEL|YAS|ZANTEL|MIXX)\s+kwenda""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "CRDB → Mixx by Yas", category = "Transfer"
        ),
        // Generic CRDB SimBanking transfer
        SmsPattern(
            senderMatch = "crdb",
            bodyPattern = Regex("""[Mm]uamala\s+umefanikiwa\s+(?:TZS|TSH|Tsh)\s*$AMT\s+\w+\s+kwenda\s+""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "CRDB SimBanking", category = "Transfer"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // NMB BANK  (sender ID: "NMB")
        // ══════════════════════════════════════════════════════════════════════
        //
        // REMINDERS — no money moved, caught by isReminderSms() before here:
        //   (a) "520NDGL260460008 Mpendwa Mteja.  umebakiwa na Siku 1 kurejesha
        //        TZS 39,900.00 uliyopokea kupitia Mshiko Fasta …"
        //   (b) "801NDGL260240019 Mpendwa Mteja,  Muda wa kurejesha  Kiasi cha
        //        TZS 3,097.52 ulichopokea kupitia Mshiko Fasta umefika. …"
        //
        // WITHDRAWAL — loan repayment deducted from account:
        //   (a) "801NDGL253340510. Hongera umefanikiwa kurejesha kikamilifu
        //        TZS 289,500.00 ya Mshiko Fasta …"
        //   (b) "801NDGL260270012. Kiasi cha TZS TZS 19,300.00 kimetolewa kwenye
        //        akaunti yako inayoishia na 34147 kurejesha Mshiko Fasta …"
        //
        // DEPOSIT — money received into account:
        //   (a) "Kiasi cha TZS 50000 kimewekwa kwenye akaunti yako inayoishia
        //        na 34147 tarehe 04-03-2026. …"
        //
        // WITHDRAWAL — money sent out:
        //   (a) "Kumb: GWX101931762870 Imethibitishwa.
        //        Kiasi cha TSH66000 kimetumwa kutoka katika akaunti inayoishia
        //        na 5389 kwenda EZEKIEL AUGUSTINO LEMANA 255752772587. …"

        // ── DEPOSIT: "Kiasi cha TZS 50000 kimewekwa kwenye akaunti yako" ──────
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""[Kk]iasi\s+cha\s+(?:TZS|TSH|Tsh)\s*$AMT\s+kimewekwa\s+kwenye\s+akaunti""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "NMB Bank", category = "Bank"
        ),

        // ── WITHDRAWAL: "Kiasi cha TZS TZS 19,300.00 kimetolewa kwenye akaunti" ─
        // NMB sometimes sends "TZS TZS" double prefix — (?:TZS\s+)? handles both
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""[Kk]iasi\s+cha\s+(?:TZS\s+)?(?:TZS|TSH|Tsh)\s*$AMT\s+kimetolewa\s+kwenye\s+akaunti""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "NMB Bank", category = "Bank"
        ),

        // ── WITHDRAWAL: loan fully repaid "umefanikiwa kurejesha kikamilifu TZS 289,500.00" ─
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+kurejesha\s+kikamilifu\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "NMB Mshiko Fasta", category = "Loan Repayment"
        ),

        // ── WITHDRAWAL: "Kiasi cha TSH66000 kimetumwa kutoka katika akaunti" ────
        // Amount and currency run together (TSH66000) — $AMT handles no-space format
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""[Kk]iasi\s+cha\s+(?:TZS|TSH|Tsh)\s*$AMT\s+kimetumwa\s+kutoka\s+katika\s+akaunti""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "NMB Bank", category = "Bank"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // MIXX BY YAS — all confirmed formats
        // ══════════════════════════════════════════════════════════════════════

        // "Umefanikiwa kupokea TZS 15,000.00 kutoka ..."  ← NEW
        SmsPattern(
            senderMatch = "mixx",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]upokea\s+$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        // "Umefanikiwa kutuma TZS 5,000.00 kwenda ..."  ← NEW
        SmsPattern(
            senderMatch = "mixx",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]utuma\s+$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        // Generic Swahili receive/send
        SmsPattern(
            senderMatch = "mixx",
            bodyPattern = Regex("""(?:[Uu]mepokea|received|imeingia|[Kk]upokea)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "mixx",
            bodyPattern = Regex("""(?:[Uu]metuma|sent|imetoka|[Kk]utuma)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "yas",
            bodyPattern = Regex("""(?:[Uu]mepokea|received|[Kk]upokea|imeingia)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "yas",
            bodyPattern = Regex("""(?:[Uu]metuma|sent|[Kk]utuma|imetoka)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // AIRTEL MONEY — English confirmed + Swahili
        // ══════════════════════════════════════════════════════════════════════

        // "Airtel Money: Confirmed. You have received TZS5,000.00 from ..."
        SmsPattern(
            senderMatch = "airtel",
            bodyPattern = Regex("""(?:you\s+have\s+received|[Uu]mepokea|credited|received)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Airtel Money", category = "Mobile Money"
        ),
        // "TZS3,000.00 sent to ..." ← NEW — amount comes BEFORE "sent"
        SmsPattern(
            senderMatch = "airtel",
            bodyPattern = Regex("""$AMT\s+sent\s+to\s+\d""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Airtel Money", category = "Mobile Money"
        ),
        // Generic Swahili send
        SmsPattern(
            senderMatch = "airtel",
            bodyPattern = Regex("""(?:[Uu]metuma|withdrawn|payment)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Airtel Money", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // TIGO PESA — Kiasi format + Swahili keywords
        // ══════════════════════════════════════════════════════════════════════

        // "Pesa zimeingizwa kwenye akaunti yako. Kiasi: TZS 20,000. Kutoka: ..."
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""[Pp]esa\s+zimeingizwa[\s\S]+?[Kk]iasi[:\s]+$AMT""", setOf(RegexOption.IGNORE_CASE)),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        // "Pesa zimetumwa. Kiasi: TZS 7,000. Kwenda: ..."
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""[Pp]esa\s+zimetumwa[\s\S]+?[Kk]iasi[:\s]+$AMT""", setOf(RegexOption.IGNORE_CASE)),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        // Generic Swahili receive
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""(?:[Uu]mepokea|received|pesa\s+zimeingizwa)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""(?:[Uu]metuma|sent|pesa\s+zimetumwa)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Mixx by Yas", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // T-PESA (TTCL)
        // ══════════════════════════════════════════════════════════════════════

        // "Umepokea TZS 10,000.00 kutoka JOHN DOE 0765XXXXXX"
        // "T-PESA: Umefanikiwa kupokea TZS 20,000 kutoka..."
        SmsPattern(
            senderMatch = "tpesa",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]upokea\s+$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "T-Pesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "tpesa",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "T-Pesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "tpesa",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]utuma\s+$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "T-Pesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "tpesa",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "T-Pesa", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // AZAMPESA
        // ══════════════════════════════════════════════════════════════════════

        // "AzamPesa: Umepokea TZS 5,000.00 kutoka PETER JAMES(0712XXXXXX)"
        // "AzamPesa: Confirmed. You received TZS 10,000 from..."
        // "Akaunti yako imeongezwa TZS 5,000 ..."
        SmsPattern(
            senderMatch = "azam",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "AzamPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "azam",
            bodyPattern = Regex("""[Yy]ou\s+received\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "AzamPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "azam",
            bodyPattern = Regex("""[Aa]kaunti\s+yako\s+imeongezwa\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "AzamPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "azam",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "AzamPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "azam",
            bodyPattern = Regex("""[Yy]ou\s+sent\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "AzamPesa", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // SELCOMPESA
        // ══════════════════════════════════════════════════════════════════════

        // "SelcomPesa: Umefanikiwa kupokea TZS 10,000.00 kutoka..."
        // "SelcomPesa: You have received TZS 15,000 from..."
        SmsPattern(
            senderMatch = "selcom",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]upokea\s+$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "SelcomPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "selcom",
            bodyPattern = Regex("""[Yy]ou\s+have\s+received\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "SelcomPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "selcom",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "SelcomPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "selcom",
            bodyPattern = Regex("""[Uu]mefanikiwa\s+[Kk]utuma\s+$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SelcomPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "selcom",
            bodyPattern = Regex("""[Yy]ou\s+have\s+sent\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SelcomPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "selcom",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SelcomPesa", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // EZYPESA (EITC — formerly Zantel)
        // ══════════════════════════════════════════════════════════════════════

        // "EzyPesa: Umepokea TZS 8,000.00 kutoka HASSAN ALI 0777XXXXXX"
        // "EzyPesa: Confirmed. Received TZS 25,000 from..."
        SmsPattern(
            senderMatch = "ezy",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "EzyPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "ezy",
            bodyPattern = Regex("""[Cc]onfirmed\.\s+[Rr]eceived\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "EzyPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "ezy",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "EzyPesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "ezy",
            bodyPattern = Regex("""[Cc]onfirmed\.\s+[Ss]ent\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "EzyPesa", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // NALA  (sender ID: "NALA")
        // ══════════════════════════════════════════════════════════════════════
        //
        // DEPOSIT — international transfer received into M-Pesa wallet:
        //   "Mambo! Beatha Massawe sent you TSh253,584.00 to your M-Pesa wallet using NALA nala.com"
        //
        SmsPattern(
            senderMatch = "nala",
            bodyPattern = Regex(""".+\s+sent\s+you\s+(?:TZS|TSH|Tsh|TSh)\s*$AMT\s+to\s+your\s+.+\s+wallet\s+using\s+NALA""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "NALA", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // SONGESHA (standalone sender ID: "SONGESHA")
        // ══════════════════════════════════════════════════════════════════════
        //
        // REMINDER (no money moved — isReminderSms() returns null before this runs):
        //   "Unakumbushwa kurejesha deni lako la Tsh 28507 la SONGESHA.
        //    Ili kulipa deni weka pesa kwenye akaunti yako au
        //    Piga *150*00#>Huduma za Kifedha>SONGESHA>LIPA DENI."
        //
        // DEDUCTION (real money moved — store as WITHDRAWAL):
        //   "Umelipa Tsh 100 kama makato ya deni la  SONGESHA.
        //    Salio la deni lako kwa sasa ni Tsh 32,058."
        //   (\s+ in pattern handles the double-space before SONGESHA in real messages)
        //
        SmsPattern(
            senderMatch = "songesha",
            bodyPattern = Regex("""[Uu]melipa\s+(?:Tsh|TSH|TZS)\s*$AMT\s+kama\s+makato\s+ya\s+deni\s+la\s+SONGESHA""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Loan", category = "Loan Repayment"
        )
        // Reminder messages are silently dropped by isReminderSms() — never reach here.
    )

    // ── Balance extraction ─────────────────────────────────────────────────────
    // Ordered most-specific first to avoid grabbing transaction amount instead of balance.
    // Real formats from Tanzania SMS:
    //   "Salio jipya: 30,666.00 TZS"                          ← HaloPesa deposit
    //   "Salio jipya ni TSH 1,689.00"                         ← HaloPesa bank transfer
    //   "Salio lako jipya ni TSH 2,269.00"                    ← HaloPesa wallet transfer
    //   "Salio lako jipya la M-Pesa ni Tsh0.00"               ← M-Pesa withdrawal
    //   "Salio lako la M-Pesa ni Tsh100.00"                   ← M-Pesa deposit
    //   "Balance: TZS 30,000.00"                              ← Airtel English
    private val BALANCE_PATTERNS = listOf(
        // "Salio jipya: 30,666.00 TZS" — colon format
        Regex("""[Ss]alio\s+jipya\s*:\s*([\d,]+(?:\.\d{1,2})?)"""),
        // "Salio lako jipya la M-Pesa ni Tsh0.00"
        Regex("""[Ss]alio\s+lako\s+jipya\s+(?:la\s+\S+(?:\s+\S+)*?\s+)?ni\s+(?:TZS|TSH|Tsh|TSh)\s*([\d,]+(?:\.\d{1,2})?)"""),
        // "Salio jipya ni TSH 1,689.00"
        Regex("""[Ss]alio\s+jipya\s+ni\s+(?:TZS|TSH|Tsh|TSh)\s*([\d,]+(?:\.\d{1,2})?)"""),
        // "Salio lako la M-Pesa ni Tsh100.00"
        Regex("""[Ss]alio\s+lako\s+la\s+\S+(?:\s+\S+)?\s+ni\s+(?:TZS|TSH|Tsh|TSh)\s*([\d,]+(?:\.\d{1,2})?)"""),
        // English: "Balance: TZS 30,000" / "new balance is TZS 250,000"
        Regex("""(?:new\s+)?[Bb]alance\s*(?:is|:)\s*(?:TZS|TSH|Tsh|KES|Ksh)?\s*([\d,]+(?:\.\d{1,2})?)""")
    )

    // ── Reference extraction ──────────────────────────────────────────────────
    private val REFERENCE_PATTERNS = listOf(
        // "Kumb: GWX101931762870" (NMB), "REF: ABC123", "Ref: 456789", "TxID: XYZ"
        Regex("""(?:KUMB|Kumb|REF|Ref|Tnx|TxID)[:\s]+([A-Za-z0-9]{6,30})"""),
        // "DC22N8FK524 Imethibitishwa" — ref code at start of M-Pesa SMS
        Regex("""^([A-Z]{2,4}[A-Z0-9]{6,15})\s+[Ii]methibitishwa"""),
        // "Utambulisho wa Muamala: 123456789"
        Regex("""[Uu]tambulisho\s+wa\s+[Mm]uamala[:\s]*(\d{6,20})""")
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns true when the SMS body is a reminder or alert — NOT a transaction.
     * No money has moved; recording it would corrupt the user's balance.
     *
     * Covers:
     *  - SONGESHA / loan repayment reminders ("Unakumbushwa kurejesha deni…")
     *  - Generic "please pay" / "overdue" / "due date" alerts from any sender
     *  - Low-balance alerts ("Salio lako ni chini ya…")
     *  - Promotional / marketing messages ("Dear customer, enjoy…")
     *  - OTP / PIN messages ("Your OTP is…", "Nambari yako ya siri…")
     */
    fun isReminderSms(body: String): Boolean {
        val b = body.lowercase()
        return REMINDER_PHRASES.any { b.contains(it) }
    }

    // Phrases that unambiguously signal no money moved.
    // Ordered: most specific first, then generic.
    private val REMINDER_PHRASES = listOf(
        // SONGESHA / loan reminders (Swahili)
        "unakumbushwa kurejesha",       // "You are reminded to repay"
        "kumbushwa kulipa",             // "reminded to pay"
        "kumbushwa kuhusu deni",        // "reminded about your debt"
        "deni lako bado",               // "your debt is still outstanding"
        "tafadhali lipa deni",          // "please pay your debt"
        "rejesha deni lako",            // "repay your debt"
        "bado haujalipia",              // "you have not yet paid"
        "malipo ya deni yako yanakaribia", // "your debt payment is due soon"
        // NMB Mshiko Fasta reminders — no money moved:
        // (a) "…umebakiwa na Siku 1 kurejesha TZS 39,900.00 uliyopokea kupitia Mshiko Fasta…"
        // (b) "…Muda wa kurejesha Kiasi cha TZS 3,097.52 ulichopokea kupitia Mshiko Fasta umefika…"
        "umebakiwa na siku",                        // "X days remaining to repay"
        "muda wa kurejesha",                        // "repayment period"
        "ulichopokea kupitia mshiko fasta umefika", // "Mshiko Fasta repayment due"
        "rejesha kwa wakati uweze kukopa",          // "repay on time to borrow more" — NMB reminder signature
        // Generic English reminders / alerts
        "this is a reminder",
        "please pay",
        "payment is due",
        "payment due on",
        "your bill is due",
        "overdue payment",
        "kindly pay",
        "your loan repayment is due",
        "your repayment of",
        "you are yet to pay",
        "settle your",
        // Low-balance / account alerts (no money moved)
        "salio lako ni chini",          // "your balance is below"
        "your balance is low",
        "low balance alert",
        "insufficient balance",
        "akaunti yako imezuiwa",        // "your account has been blocked"
        "account has been suspended",
        // OTP / security — definitely not a transaction
        "your otp is",
        "your pin is",
        "nambari yako ya siri",         // "your secret number"
        "verification code",
        "msimbo wa uthibitisho",
        // Marketing / promotional
        "dear customer, enjoy",
        "special offer",
        "you have won",
        "umeshinda",                    // "you have won"
        "bonyeza namba"                 // "press the number" (promo)
    )

    fun isFinancialSender(sender: String): Boolean {
        val up = sender.uppercase().trim()
        // Exact match first (fastest + safest)
        if (FINANCIAL_SENDERS.contains(up)) return true
        // Partial match — only when the sender or known ID is long enough (≥4 chars)
        // to avoid false matches like "TZ" matching "CRDBTZ"
        return FINANCIAL_SENDERS.any { known ->
            if (up.length >= 4 && known.length >= 4) {
                up.contains(known) || known.contains(up)
            } else {
                up == known
            }
        }
    }

    fun parse(sender: String, body: String, timestamp: Long): Transaction? {
        // Reminders and alerts never represent a completed transaction.
        // Return null immediately so nothing is stored and the balance stays correct.
        if (isReminderSms(body)) {
            Log.d(TAG, "Skipped reminder SMS from $sender: ${body.take(60)}")
            return null
        }
        return try {
            val match = findMatch(sender, body)
            if (match == null) {
                Log.w(TAG, "No pattern matched [$sender]: ${body.take(80)}")
                return null
            }
            val (pattern, result) = match
            val rawAmt = result.groupValues.getOrNull(pattern.amountGroup) ?: ""
            val amount = rawAmt.replace(",", "").toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                Log.w(TAG, "Bad amount '$rawAmt' from: ${body.take(80)}")
                return null
            }
            Transaction(
                amount      = amount,
                type        = pattern.type,
                source      = pattern.sourceLabel,
                date        = timestamp,
                description = body.take(200),
                reference   = extractReference(body),
                category    = pattern.category
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS from $sender: ${e.message}", e)
            null
        }
    }

    fun extractBalance(body: String): Double? {
        for (regex in BALANCE_PATTERNS) {
            val v = regex.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            if (v != null) return v
        }
        return null
    }

    fun getCategory(source: String): String = when {
        source.contains("Bank", ignoreCase = true)     -> "Banking"
        source.contains("Loan", ignoreCase = true) ||
                source.contains("SONGESHA", ignoreCase = true) -> "Loan"
        source.contains("Agent", ignoreCase = true)    -> "Cash"
        else                                           -> "Mobile Money"
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    // Maps real-world sender ID variants to internal pattern keys
    // Real sender IDs exactly as they appear in the SMS inbox, mapped to internal keys.
    // Source: confirmed from real device screenshots (Tanzania).
    private val SENDER_ALIAS = mapOf(
        // Vodacom M-Pesa — sender appears as "M-PESA" on device
        "m-pesa"       to "mpesa",
        "mpesa"        to "mpesa",
        "vodacom"      to "mpesa",
        "vodacom-tz"   to "mpesa",
        // Halotel — sender appears as "HaloPesa" on device
        "halopesa"     to "halo",
        "halotel"      to "halo",
        // SONGESHA — standalone sender, but also embedded in M-PESA and HaloPesa messages
        "songesha"     to "songesha",
        // CRDB — sender appears as "CRDB BANK" on device
        "crdb bank"    to "crdb",
        "crdb-bank"    to "crdb",
        "crdbbank"     to "crdb",
        // NMB
        "nmbbank"      to "nmb",
        "nmb-bank"     to "nmb",
        "nmbtz"        to "nmb",
        // Tigo
        "tigopesa"     to "tigo",
        "tigo-pesa"    to "tigo",
        // Mixx by Yas
        "mixx-yas"     to "mixx",
        "mixxbyyas"    to "mixx",
        // Airtel
        "airtel-tz"    to "airtel",
        "airtel-money" to "airtel",
        "airtelmoney"  to "airtel",
        // T-Pesa (TTCL)
        "t-pesa"       to "tpesa",
        "ttcl"         to "tpesa",
        // AzamPesa
        "azampesa"     to "azam",
        "azampesa"     to "azam",
        // SelcomPesa
        "selcompesa"   to "selcom",
        "selcom"       to "selcom",
        // EzyPesa (EITC)
        "ezy-pesa"     to "ezy",
        "ezypesa"      to "ezy",
        "eitc"         to "ezy",
        // NALA
        "nala"         to "nala",
    )

    private fun findMatch(sender: String, body: String): Pair<SmsPattern, MatchResult>? {
        val sl = sender.lowercase().trim()
        // Resolve alias: "VODACOM" → "mpesa", "CRDB BANK" → "crdb", etc.
        val key = SENDER_ALIAS[sl] ?: SENDER_ALIAS.entries
            .firstOrNull { sl.contains(it.key) }?.value ?: sl
        // 1. Sender-specific patterns first (most precise)
        for (p in PATTERNS) {
            if (!key.contains(p.senderMatch.lowercase()) &&
                !p.senderMatch.lowercase().contains(key)) continue
            val r = p.bodyPattern.find(body) ?: continue
            return p to r
        }
        // 2. Fallback: body-only match (handles any unrecognised sender ID variant)
        for (p in PATTERNS) {
            val r = p.bodyPattern.find(body) ?: continue
            return p to r
        }
        return null
    }

    fun extractReference(body: String): String {
        for (regex in REFERENCE_PATTERNS) {
            val ref = regex.find(body)?.groupValues?.get(1)
            if (!ref.isNullOrBlank()) return ref
        }
        return ""
    }
}
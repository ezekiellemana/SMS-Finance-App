package com.smsfinance.util

import android.util.Log
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType

/**
 * SMS Pattern Recognition Engine — v3.0
 *
 * Tested against real SMS samples from:
 * ── Tanzania ──────────────────────────────────────────────────────
 *  HaloPesa         — Deposit, Withdraw (bank/wallet), SONGESHA loan
 *  Vodacom M-Pesa   — Deposit, Withdraw, SONGESHA loan repayment
 *  CRDB Bank        — Deposit, Withdraw (agent), SimBanking transfers
 *  NMB Bank         — Deposit, Withdrawal (English format)
 *  Mixx by Yas      — Deposit, Withdraw (kupokea/kutuma + umefanikiwa)
 *  Airtel Money     — Deposit, Withdraw (Swahili + English confirmed)
 *  Tigo Pesa        — Deposit, Withdraw (Swahili + Kiasi format)
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
        "TIGOPESA", "TIGO", "TIGO-PESA", "TigoPesa",
        "AIRTELMONEY", "AIRTEL", "AIRTEL-MONEY", "AIRTEL-TZ",
        "HALOPESA", "HALOTEL", "HALO", "HaloPesa",    // Halotel sends as "HaloPesa"
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

        // "Umepokea 20,000 TZS kutoka STEWART ERNEST MASIMA (0626233330)"
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]mepokea\s+$AMT\s*(?:TZS|TSH|Tsh)\s+kutoka\s+.+?\([+\d]+\)""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "HaloPesa", category = "Mobile Money"
        ),
        // "Umetuma TSH 540,000.00 kwenye akaunti namba ... ya NMB"
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TSH|TZS|Tsh)\s*$AMT\s+kwenye\s+akaunti""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa → Bank", category = "Transfer"
        ),
        // "Umetuma TSH 9,000.00 kwenda Mixx by Yas, jina ROBERT TAIRO"
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]metuma\s+(?:TSH|TZS|Tsh)\s*$AMT\s+kwenda\s+.+?,""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa → Wallet", category = "Transfer"
        ),
        // "Umelipa Tsh 100 kama makato ya deni la SONGESHA"
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]melipa\s+(?:Tsh|TSH|TZS)\s*$AMT\s+kama\s+makato\s+ya\s+deni\s+la\s+SONGESHA""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Loan", category = "Loan Repayment"
        ),
        // "Unakumbushwa kurejesha deni lako la Tsh 27330 la SONGESHA"
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Uu]nakumbushwa\s+kurejesha\s+deni\s+lako\s+la\s+(?:Tsh|TSH|TZS)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Reminder", category = "Loan"
        ),
        // "Transaction ID: 6060... You have paid 1,000 TZS for phone number ... of Buy Bundle"
        // HaloPesa English bill payment / bundle purchase
        SmsPattern(
            senderMatch = "halo",
            bodyPattern = Regex("""[Yy]ou\s+have\s+paid\s+$AMT\s+TZS""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "HaloPesa Payment", category = "Bill Payment"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // VODACOM M-PESA
        // ══════════════════════════════════════════════════════════════════════

        // "DC22N8FK524 Imethibitishwa. Umepokea Tsh100.00 kutoka 255792892289 - ESTHER BALADIGA mnamo ..."
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""[Uu]mepokea\s+(?:Tsh|TZS|TSH)\s*$AMT\s+kutoka\s+[\d+]+\s*-\s*.+?\s+mnamo""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "M-Pesa", category = "Mobile Money"
        ),
        // "Tsh100.00 imetolewa kwenye akaunti yako ya M-Pesa"
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""$AMT\s+imetolewa\s+kwenye\s+akaunti\s+yako\s+ya\s+M-Pesa""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "M-Pesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "mpesa",
            bodyPattern = Regex("""[Uu]nakumbushwa\s+kurejesha\s+deni\s+lako\s+la\s+(?:Tsh|TSH|TZS)\s*$AMT\s+la\s+SONGESHA""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Reminder", category = "Loan"
        ),

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
        // NMB BANK
        // ══════════════════════════════════════════════════════════════════════

        // "Kiasi cha TZS TZS 19,300.00 kimetolewa kwenye akaunti yako inayoishia na ..."
        // Note: NMB sometimes sends "TZS TZS" double prefix — pattern handles both
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""[Kk]iasi\s+cha\s+(?:TZS\s+)?$AMT\s+kimetolewa""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "NMB Bank", category = "Bank"
        ),
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""(?:you\s+have\s+received|credited)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "NMB Bank", category = "Bank"
        ),
        // "Payment of TZS 50,000 has been debited"
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""(?:withdrawn|debited|payment\s+of)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "NMB Bank", category = "Bank"
        ),
        // "TZS 80,000 withdrawn from your NMB account"  ← amount-first format
        SmsPattern(
            senderMatch = "nmb",
            bodyPattern = Regex("""$AMT\s+(?:withdrawn|debited)\s+from""", RegexOption.IGNORE_CASE),
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

        // "Pesa zimeingizwa kwenye akaunti yako. Kiasi: TZS 20,000. Kutoka: ..."  ← NEW
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""[Pp]esa\s+zimeingizwa[\s\S]+?[Kk]iasi[:\s]+$AMT""", setOf(RegexOption.IGNORE_CASE)),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Tigo Pesa", category = "Mobile Money"
        ),
        // "Pesa zimetumwa. Kiasi: TZS 7,000. Kwenda: ..."  ← NEW
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""[Pp]esa\s+zimetumwa[\s\S]+?[Kk]iasi[:\s]+$AMT""", setOf(RegexOption.IGNORE_CASE)),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Tigo Pesa", category = "Mobile Money"
        ),
        // Generic Swahili receive
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""(?:[Uu]mepokea|received|pesa\s+zimeingizwa)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.DEPOSIT, amountGroup = 1,
            sourceLabel = "Tigo Pesa", category = "Mobile Money"
        ),
        SmsPattern(
            senderMatch = "tigo",
            bodyPattern = Regex("""(?:[Uu]metuma|sent|pesa\s+zimetumwa)\s+(?:TZS|TSH|Tsh)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "Tigo Pesa", category = "Mobile Money"
        ),

        // ══════════════════════════════════════════════════════════════════════
        // SONGESHA (standalone sender)
        // ══════════════════════════════════════════════════════════════════════

        SmsPattern(
            senderMatch = "songesha",
            bodyPattern = Regex("""[Uu]melipa\s+(?:Tsh|TSH|TZS)\s*$AMT\s+kama\s+makato\s+ya\s+deni""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Loan", category = "Loan Repayment"
        ),
        SmsPattern(
            senderMatch = "songesha",
            bodyPattern = Regex("""[Uu]nakumbushwa\s+kurejesha\s+deni\s+lako\s+la\s+(?:Tsh|TSH|TZS)\s*$AMT""", RegexOption.IGNORE_CASE),
            type = TransactionType.WITHDRAWAL, amountGroup = 1,
            sourceLabel = "SONGESHA Reminder", category = "Loan"
        )
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
        Regex("""(?:KUMB|REF|Ref|Tnx|TxID)[:\s]+([A-Za-z0-9]{6,30})"""),
        Regex("""^([A-Z]{2,4}[A-Z0-9]{6,15})\s+[Ii]methibitishwa"""),
        Regex("""[Uu]tambulisho\s+wa\s+[Mm]uamala[:\s]*(\d{6,20})""")
    )

    // ── Public API ────────────────────────────────────────────────────────────

    fun isFinancialSender(sender: String): Boolean {
        val up = sender.uppercase().trim()
        return FINANCIAL_SENDERS.any { known ->
            up == known || up.contains(known) || known.contains(up)
        }
    }

    fun parse(sender: String, body: String, timestamp: Long): Transaction? {
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
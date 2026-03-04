package com.smsfinance.util

import com.smsfinance.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPatternEngineTest {

    private val ts = System.currentTimeMillis()
    private fun parse(sender: String, body: String) = SmsPatternEngine.parse(sender, body, ts)

    private fun assertDeposit(sender: String, body: String, amount: Double, source: String? = null, id: String = "") {
        val tx = parse(sender, body)
        assertNotNull("[$id] parse() returned null", tx)
        assertEquals("[$id] type", TransactionType.DEPOSIT, tx!!.type)
        assertEquals("[$id] amount", amount, tx.amount, 0.01)
        if (source != null) assertEquals("[$id] source", source, tx.source)
    }

    private fun assertWithdrawal(sender: String, body: String, amount: Double, source: String? = null, id: String = "") {
        val tx = parse(sender, body)
        assertNotNull("[$id] parse() returned null", tx)
        assertEquals("[$id] type", TransactionType.WITHDRAWAL, tx!!.type)
        assertEquals("[$id] amount", amount, tx.amount, 0.01)
        if (source != null) assertEquals("[$id] source", source, tx.source)
    }

    private fun assertNoMatch(sender: String, body: String, id: String = "") =
        assertNull("[$id] expected null but got a match", parse(sender, body))

    // ── HALOPESA ──────────────────────────────────────────────────────────────

    @Test fun hp01_deposit_umepokea() = assertDeposit("HaloPesa",
        "Umepokea 20,000 TZS kutoka STEWART ERNEST MASIMA (0626233330). Salio jipya: 30,666.00 TZS. Utambulisho wa Muamala:3887545254",
        20_000.0, "HaloPesa", "HP-01")

    @Test fun hp02_withdrawal_umetuma_bank() = assertWithdrawal("HaloPesa",
        "Umetuma TSH 540,000.00 kwenye akaunti namba 12345678 ya NMB BANK. Salio lako jipya ni TSH 1,689.00. Tnx 6055509123110519",
        540_000.0, "HaloPesa \u2192 Bank", "HP-02")

    @Test fun hp03_withdrawal_umetuma_wallet() = assertWithdrawal("HaloPesa",
        "Umetuma TSH 9,000.00 kwenda Mixx by Yas, jina ROBERT TAIRO (+255712345678). Salio lako jipya ni TSH 2,269.00",
        9_000.0, "HaloPesa \u2192 Wallet", "HP-03")

    @Test fun hp04_songesha_repayment() = assertWithdrawal("HaloPesa",
        "Umelipa Tsh 100 kama makato ya deni la SONGESHA. Salio jipya: 500.00 TZS",
        100.0, "SONGESHA Loan", "HP-04")

    @Test fun hp05_songesha_reminder() = assertWithdrawal("HaloPesa",
        "Unakumbushwa kurejesha deni lako la Tsh 27330 la SONGESHA. Tafadhali lipa leo.",
        27_330.0, "SONGESHA Reminder", "HP-05")

    @Test fun hp06_english_bill_payment() = assertWithdrawal("HaloPesa",
        "Transaction ID: 6060123456. You have paid 1,000 TZS for phone number 0712345678 of Buy Bundle",
        1_000.0, "HaloPesa Payment", "HP-06")

    // ── M-PESA ────────────────────────────────────────────────────────────────

    @Test fun mp01_deposit_imethibitishwa() = assertDeposit("M-PESA",
        "DC22N8FK524 Imethibitishwa. Umepokea Tsh100.00 kutoka 255792892289 - ESTHER BALADIGA mnamo 04/03/26 14:30. Salio lako la M-Pesa ni Tsh100.00",
        100.0, "M-Pesa", "MP-01")

    @Test fun mp02_withdrawal_imetolewa() = assertWithdrawal("M-PESA",
        "Tsh5,000.00 imetolewa kwenye akaunti yako ya M-Pesa. Salio lako jipya la M-Pesa ni Tsh0.00. REF:FT26061MPESA",
        5_000.0, "M-Pesa", "MP-02")

    @Test fun mp03_songesha_reminder() = assertWithdrawal("M-PESA",
        "Unakumbushwa kurejesha deni lako la Tsh 15,000 la SONGESHA. Lipa leo.",
        15_000.0, "SONGESHA Reminder", "MP-03")

    @Test fun mp04_large_deposit() = assertDeposit("M-PESA",
        "AB12CD34EF56 Imethibitishwa. Umepokea Tsh250,000.00 kutoka 255741234567 - JOHN WILLIAM mnamo 04/03/26 09:15. Salio lako la M-Pesa ni Tsh250,100.00",
        250_000.0, id = "MP-04")

    @Test fun mp05_via_vodacom_sender() = assertDeposit("VODACOM",
        "XY99ZQAB Imethibitishwa. Umepokea Tsh50,000.00 kutoka 255612345678 - GRACE ANN mnamo 04/03/26. Salio lako la M-Pesa ni Tsh50,000.00",
        50_000.0, id = "MP-05")

    // ── CRDB BANK ─────────────────────────────────────────────────────────────

    @Test fun cr01_deposit_received() = assertDeposit("CRDB BANK",
        "Dear Customer, you have received TZS250,000.00 in your account number: 0150123456789. Balance: TZS 450,000.00. REF:FT26061CY5SQ",
        250_000.0, "CRDB Bank", "CR-01")

    @Test fun cr02_withdrawal_wakala() = assertWithdrawal("CRDB BANK",
        "Umefanikiwa Kutoa TSh15,000 kwa Wakala. Balance: TZS 35,000.00",
        15_000.0, "CRDB Agent", "CR-02")

    @Test fun cr03_simbanking_to_halopesa() = assertWithdrawal("CRDB BANK",
        "Muamala umefanikiwa TZS1000 HALOTEL kwenda GRACE MWAMBA 0621234567. KUMB:19cae4b002e59a5e",
        1_000.0, "CRDB \u2192 HaloPesa", "CR-03")

    @Test fun cr04_simbanking_to_mpesa() = assertWithdrawal("CRDB BANK",
        "Muamala umefanikiwa TZS5,000 VODACOM kwenda PETER JOHN 0712345678",
        5_000.0, "CRDB \u2192 M-Pesa", "CR-04")

    @Test fun cr05_simbanking_to_mixx_zantel() = assertWithdrawal("CRDB BANK",
        "Muamala umefanikiwa TZS3,500 YAS/ZANTEL kwenda ALICE PETER 0779123456",
        3_500.0, "CRDB \u2192 Mixx by Yas", "CR-05")

    @Test fun cr06_simbanking_to_mixx_variant() = assertWithdrawal("CRDB BANK",
        "Muamala umefanikiwa TZS2,000 MIXX kwenda ANNA PAUL 0779999999",
        2_000.0, "CRDB \u2192 Mixx by Yas", "CR-06")

    @Test fun cr07_simbanking_generic() = assertWithdrawal("CRDB BANK",
        "Muamala umefanikiwa TZS20,000 AIRTEL kwenda DAVID MWANGI 0689123456",
        20_000.0, "CRDB SimBanking", "CR-07")

    // ── NMB BANK ──────────────────────────────────────────────────────────────

    @Test fun nm01_swahili_withdrawal_double_tzs() = assertWithdrawal("NMB",
        "Kiasi cha TZS TZS 19,300.00 kimetolewa kwenye akaunti yako inayoishia na 1234. Salio jipya ni TSH 45,700.00",
        19_300.0, "NMB Bank", "NM-01")

    @Test fun nm02_swahili_withdrawal_single_tzs() = assertWithdrawal("NMB",
        "Kiasi cha TZS 50,000.00 kimetolewa kwenye akaunti yako. new balance is TZS 120,000",
        50_000.0, id = "NM-02")

    @Test fun nm03_english_deposit() = assertDeposit("NMB",
        "Dear customer, you have received TZS 80,000.00 in your account. Balance: TZS 180,000.00",
        80_000.0, "NMB Bank", "NM-03")

    @Test fun nm04_payment_debited() = assertWithdrawal("NMB",
        "Payment of TZS 12,500 has been debited from your account. Balance: TZS 67,500",
        12_500.0, "NMB Bank", "NM-04")

    @Test fun nm05_amount_first_withdrawn() = assertWithdrawal("NMB",
        "TZS 80,000 withdrawn from your NMB account ending 5678. Balance: TZS 20,000",
        80_000.0, "NMB Bank", "NM-05")

    // ── MIXX BY YAS ───────────────────────────────────────────────────────────

    @Test fun mx01_umefanikiwa_kupokea() = assertDeposit("MIXX-YAS",
        "Umefanikiwa kupokea TZS 15,000.00 kutoka ANNA JOSEPH.",
        15_000.0, "Mixx by Yas", "MX-01")

    @Test fun mx02_umefanikiwa_kutuma() = assertWithdrawal("MIXX-YAS",
        "Umefanikiwa kutuma TZS 5,000.00 kwenda GRACE PETER 0779123456",
        5_000.0, "Mixx by Yas", "MX-02")

    @Test fun mx03_generic_umepokea() = assertDeposit("MIXX-YAS",
        "Umepokea TZS 10,000 kutoka JAMES PAUL. Salio jipya: 30,000.00 TZS",
        10_000.0, id = "MX-03")

    @Test fun mx04_generic_umetuma() = assertWithdrawal("MIXX-YAS",
        "Umetuma TZS 7,500 kwenda SARAH MIKE 0719123456",
        7_500.0, id = "MX-04")

    @Test fun mx05_via_yas_sender() = assertDeposit("YAS",
        "Umepokea TZS 18,000 kutoka ROSE PETER.",
        18_000.0, id = "MX-05")

    // ── AIRTEL MONEY ──────────────────────────────────────────────────────────

    @Test fun at01_english_deposit() = assertDeposit("AIRTEL-MONEY",
        "Airtel Money: Confirmed. You have received TZS5,000.00 from JOHN DOE 0689123456. Balance: TZS 12,000.00",
        5_000.0, "Airtel Money", "AT-01")

    @Test fun at02_amount_first_sent() = assertWithdrawal("AIRTEL-MONEY",
        "TZS3,000.00 sent to 0689987654 GRACE HENRY. Balance: TZS 9,000",
        3_000.0, "Airtel Money", "AT-02")

    @Test fun at03_withdrawn() = assertWithdrawal("AIRTEL-MONEY",
        "Airtel Money: withdrawn TZS 8,000 from your account. Balance: TZS 2,000",
        8_000.0, id = "AT-03")

    @Test fun at04_swahili_umepokea() = assertDeposit("AIRTEL",
        "Umepokea TZS 20,000 kutoka PETER JOHN. Balance: TZS 22,000",
        20_000.0, id = "AT-04")

    @Test fun at05_via_airtelmoney_sender() = assertDeposit("AIRTELMONEY",
        "Confirmed. You have received TZS35,000.00 from MAMA GRACE 0689000001. Balance: TZS 40,000",
        35_000.0, id = "AT-05")

    // ── TIGO PESA ─────────────────────────────────────────────────────────────

    @Test fun tp01_kiasi_deposit() = assertDeposit("TigoPesa",
        "Pesa zimeingizwa kwenye akaunti yako. Kiasi: TZS 20,000. Kutoka: MARY ANN 0657123456",
        20_000.0, "Tigo Pesa", "TP-01")

    @Test fun tp02_kiasi_withdrawal() = assertWithdrawal("TigoPesa",
        "Pesa zimetumwa. Kiasi: TZS 7,000. Kwenda: ALEX JAMES 0657987654",
        7_000.0, "Tigo Pesa", "TP-02")

    @Test fun tp03_generic_umepokea() = assertDeposit("TIGO",
        "Umepokea TZS 45,000 kutoka ROSE PETER. Salio jipya: 50,000.00 TZS",
        45_000.0, id = "TP-03")

    @Test fun tp04_generic_umetuma() = assertWithdrawal("TIGO",
        "Umetuma TZS 12,000 kwenda JOHN PAUL 0716123456",
        12_000.0, id = "TP-04")

    @Test fun tp05_via_tigopesa_sender() = assertDeposit("TIGO-PESA",
        "Umepokea TZS 60,000 kutoka BABA JOHN. Salio jipya: 65,000.00 TZS",
        60_000.0, id = "TP-05")

    // ── SONGESHA ──────────────────────────────────────────────────────────────

    @Test fun sg01_repayment_umelipa() = assertWithdrawal("SONGESHA",
        "Umelipa Tsh 5,000 kama makato ya deni la SONGESHA. Deni lako lililobaki ni Tsh 25,000.",
        5_000.0, "SONGESHA Loan", "SG-01")

    @Test fun sg02_reminder_unakumbushwa() = assertWithdrawal("SONGESHA",
        "Unakumbushwa kurejesha deni lako la Tsh 30,000 la SONGESHA. Muda wa kulipa umekwisha leo.",
        30_000.0, "SONGESHA Reminder", "SG-02")

    @Test fun sg03_full_repayment_via_mpesa() = assertWithdrawal("SONGESHA",
        "Umelipa Tsh 10,500 kama makato ya deni la SONGESHA kupitia M-PESA.",
        10_500.0, "SONGESHA Loan", "SG-03")

    // ── BALANCE EXTRACTION ────────────────────────────────────────────────────

    @Test fun bal01_halopesa_colon_format() {
        assertEquals("BAL-01", 30_666.0, SmsPatternEngine.extractBalance("Umepokea 5,000 TZS. Salio jipya: 30,666.00 TZS")!!, 0.01)
    }
    @Test fun bal02_halopesa_jipya_ni() {
        assertEquals("BAL-02", 1_689.0, SmsPatternEngine.extractBalance("Umetuma TSH 540,000. Salio jipya ni TSH 1,689.00")!!, 0.01)
    }
    @Test fun bal03_halopesa_lako_jipya_ni() {
        assertEquals("BAL-03", 2_269.0, SmsPatternEngine.extractBalance("Umetuma TSH 9,000. Salio lako jipya ni TSH 2,269.00")!!, 0.01)
    }
    @Test fun bal04_mpesa_jipya_la_mpesa() {
        assertEquals("BAL-04", 0.0, SmsPatternEngine.extractBalance("Tsh5,000 imetolewa. Salio lako jipya la M-Pesa ni Tsh0.00")!!, 0.01)
    }
    @Test fun bal05_mpesa_lako_la_mpesa() {
        assertEquals("BAL-05", 100.0, SmsPatternEngine.extractBalance("Umepokea Tsh100. Salio lako la M-Pesa ni Tsh100.00")!!, 0.01)
    }
    @Test fun bal06_english_balance_colon() {
        assertEquals("BAL-06", 75_000.0, SmsPatternEngine.extractBalance("You have received TZS50,000. Balance: TZS 75,000.00")!!, 0.01)
    }
    @Test fun bal07_english_new_balance_is() {
        assertEquals("BAL-07", 120_000.0, SmsPatternEngine.extractBalance("Kiasi kimetolewa. new balance is TZS 120,000")!!, 0.01)
    }

    // ── REFERENCE EXTRACTION ──────────────────────────────────────────────────

    @Test fun ref01_tnx_format() {
        assertEquals("REF-01", "6055509123110519", SmsPatternEngine.extractReference("Umetuma TSH 540,000. Tnx 6055509123110519"))
    }
    @Test fun ref02_utambulisho_wa_muamala() {
        assertEquals("REF-02", "3887545254", SmsPatternEngine.extractReference("Umepokea 20,000 TZS. Utambulisho wa Muamala:3887545254"))
    }
    @Test fun ref03_mpesa_code_at_start() {
        assertEquals("REF-03", "DC22N8FK524", SmsPatternEngine.extractReference("DC22N8FK524 Imethibitishwa. Umepokea Tsh100.00"))
    }
    @Test fun ref04_crdb_ref_colon() {
        assertEquals("REF-04", "FT26061CY5SQ", SmsPatternEngine.extractReference("You have received TZS250,000. REF:FT26061CY5SQ"))
    }
    @Test fun ref05_crdb_kumb_format() {
        assertEquals("REF-05", "19cae4b002e59a5e", SmsPatternEngine.extractReference("Muamala umefanikiwa TZS1000 HALOTEL kwenda GRACE. KUMB:19cae4b002e59a5e"))
    }
    @Test fun ref06_empty_when_no_reference() {
        assertEquals("REF-06", "", SmsPatternEngine.extractReference("Umepokea TZS 5,000 kutoka JOHN."))
    }

    // ── SENDER ID RECOGNITION ─────────────────────────────────────────────────

    @Test fun sid01_halopesa() = assertTrue(SmsPatternEngine.isFinancialSender("HaloPesa"))
    @Test fun sid02_mpesa() = assertTrue(SmsPatternEngine.isFinancialSender("M-PESA"))
    @Test fun sid03_crdb() = assertTrue(SmsPatternEngine.isFinancialSender("CRDB BANK"))
    @Test fun sid04_nmb() = assertTrue(SmsPatternEngine.isFinancialSender("NMB"))
    @Test fun sid05_mixx() = assertTrue(SmsPatternEngine.isFinancialSender("MIXX"))
    @Test fun sid06_airtel() = assertTrue(SmsPatternEngine.isFinancialSender("AIRTEL"))
    @Test fun sid07_tigopesa() = assertTrue(SmsPatternEngine.isFinancialSender("TigoPesa"))
    @Test fun sid08_songesha() = assertTrue(SmsPatternEngine.isFinancialSender("SONGESHA"))

    // ── EDGE CASES ────────────────────────────────────────────────────────────

    @Test fun eg01_non_financial_returns_null() = assertNoMatch("VODACOM",
        "Your Vodacom bundle has been activated. 1GB valid for 7 days. Dial *150#", "EG-01")

    @Test fun eg02_promotional_returns_null() = assertNoMatch("NMB",
        "Habari! Jiunge na NMB Wakala karibu nawe. Piga simu 0800110011.", "EG-02")

    @Test fun eg03_decimal_amount() = assertDeposit("M-PESA",
        "DC99ABCD Imethibitishwa. Umepokea Tsh750.50 kutoka 255712345678 - TEST USER mnamo 04/03/26",
        750.50, id = "EG-03")

    @Test fun eg04_very_large_amount() = assertDeposit("CRDB BANK",
        "Dear Customer, you have received TZS1,500,000.00 in your account number: 0150999999.",
        1_500_000.0, id = "EG-04")

    @Test fun eg05_unknown_sender_body_fallback() = assertDeposit("UNKNOWN-BANK",
        "Dear Customer, you have received TZS50,000.00 in your account number: 9999.",
        50_000.0, id = "EG-05")

    @Test fun eg06_tsh_no_space_before_amount() = assertDeposit("M-PESA",
        "DC11XXYZ Imethibitishwa. Umepokea Tsh2500.00 kutoka 255611111111 - SOMEONE mnamo 04/03/26",
        2_500.0, id = "EG-06")

    @Test fun eg07_zero_balance() {
        assertEquals("EG-07", 0.0,
            SmsPatternEngine.extractBalance("Tsh5,000 imetolewa kwenye akaunti yako ya M-Pesa. Salio lako jipya la M-Pesa ni Tsh0.00")!!, 0.01)
    }
}
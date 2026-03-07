@file:Suppress("DEPRECATION")
package com.smsfinance.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages opt-in Google Drive cloud backup.
 *
 * Backup format: JSON file stored in the user's Drive app-data folder
 * (appDataFolder — invisible to user, only accessible by this app).
 *
 * Privacy: Data never passes through Anthropic/our servers.
 * It goes directly from the device to the user's own Google Drive.
 */
@Suppress("DEPRECATION")
@Singleton
class CloudBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context  // explicit use-site target
) {
    companion object {
        private const val BACKUP_FILE_NAME = "smart_money_backup.json"  // fixed typo: was 'smsfinance'
        private const val APP_NAME = "Smart Money"
        private const val DRIVE_FOLDER = "appDataFolder"
    }

    /** Google Sign-In intent for the backup screen to launch */
    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /** Currently signed-in account (null if not signed in) */
    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(): Boolean = getSignedInAccount() != null

    fun getSignedInEmail(): String? = getSignedInAccount()?.email

    /** Sign out of Google */
    fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }

    /**
     * Upload transactions as a JSON backup to Google Drive appDataFolder.
     * Returns true on success.
     */
    suspend fun backupToDrive(transactions: List<Transaction>): BackupResult =
        withContext(Dispatchers.IO) {
            try {
                val account = getSignedInAccount()
                    ?: return@withContext BackupResult.Error("Not signed in to Google")

                val drive = buildDriveService(account)

                // Serialize to JSON
                val json = transactionsToJson(transactions)
                val content = ByteArrayContent("application/json", json.toByteArray())

                // Check if backup file already exists — if so, update it
                val existingId = findExistingBackupFileId(drive)
                if (existingId != null) {
                    drive.files().update(existingId, null, content).execute()
                } else {
                    val metadata = File().apply {
                        name = BACKUP_FILE_NAME
                        parents = listOf(DRIVE_FOLDER)
                        mimeType = "application/json"
                    }
                    drive.files().create(metadata, content).execute()
                }
                BackupResult.Success(transactions.size)
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "Unknown error")
            }
        }

    /**
     * Download and parse the backup JSON from Google Drive.
     * Returns list of transactions to restore.
     */
    suspend fun restoreFromDrive(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext RestoreResult.Error("Not signed in to Google")

            val drive = buildDriveService(account)
            val fileId = findExistingBackupFileId(drive)
                ?: return@withContext RestoreResult.Error("No backup found on Google Drive")

            val bytes = drive.files().get(fileId).executeMediaAsInputStream().readBytes()
            val transactions = jsonToTransactions(String(bytes))
            RestoreResult.Success(transactions)
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get metadata about the existing backup (date, count) without downloading.
     */
    suspend fun getBackupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: return@withContext null
            val drive = buildDriveService(account)
            val result = drive.files().list()
                .setSpaces(DRIVE_FOLDER)
                .setFields("files(id, name, modifiedTime, size)")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .execute()
            val file = result.files.firstOrNull() ?: return@withContext null
            BackupInfo(
                fileId = file.id,
                lastModified = file.modifiedTime?.value ?: 0L,
                sizeBytes = file.size.toLong()
            )
        } catch (_: Exception) { null }  // "e" was never used — replaced with _
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    private fun findExistingBackupFileId(drive: Drive): String? {
        return drive.files().list()
            .setSpaces(DRIVE_FOLDER)
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id)")
            .execute()
            .files
            .firstOrNull()
            ?.id
    }

    private fun transactionsToJson(transactions: List<Transaction>): String {
        val arr = JSONArray()
        transactions.forEach { tx ->
            arr.put(JSONObject().apply {
                put("id", tx.id)
                put("amount", tx.amount)
                put("type", tx.type.name)
                put("source", tx.source)
                put("date", tx.date)
                put("description", tx.description)
                put("isManual", tx.isManual)
                put("reference", tx.reference)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("count", transactions.size)
            put("transactions", arr)
        }.toString(2)
    }

    private fun jsonToTransactions(json: String): List<Transaction> {
        val root = JSONObject(json)
        val arr = root.getJSONArray("transactions")
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Transaction(
                id = obj.getLong("id"),
                amount = obj.getDouble("amount"),
                type = TransactionType.fromString(obj.getString("type")),
                source = obj.getString("source"),
                date = obj.getLong("date"),
                description = obj.optString("description", ""),
                isManual = obj.optBoolean("isManual", false),
                reference = obj.optString("reference", "")
            )
        }
    }

    sealed class BackupResult {
        data class Success(val count: Int) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val transactions: List<Transaction>) : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }

    data class BackupInfo(val fileId: String, val lastModified: Long, val sizeBytes: Long)
}
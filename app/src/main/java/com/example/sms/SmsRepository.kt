package com.example.sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.PhoneNumbers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class DeviceSms(val id: Long, val thread: Long, val address: String, val body: String, val time: Long, val type: Int, val read: Boolean = true) {
    val incoming get() = type == Telephony.Sms.MESSAGE_TYPE_INBOX
}
data class SmsSend(val id: String, val address: String, val parts: Int, val completed: Int, val failed: Int, val started: Long, val error: String) {
    fun status(now: Long = System.currentTimeMillis()): String = when {
        error.isNotBlank() -> error
        failed > 0 -> "Błąd wysyłki. Część wiadomości mogła zostać wysłana."
        completed == parts -> "Operator potwierdził wysłanie. Doręczenie nie jest potwierdzone."
        now - started > 120_000 -> "Brak pełnego potwierdzenia. Sprawdź wysłane SMS-y przed ponowieniem."
        else -> "Wysyłanie: potwierdzono $completed z $parts części."
    }
}
object SmsChanges {
    val revision = MutableStateFlow(0L)
    fun changed() { revision.update { it + 1 } }
}

/** Only local transmission receipts. The system SMS provider owns message history. */
class SmsSendStore(context: Context) : SQLiteOpenHelper(context, "sms-transmission.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sends(id TEXT PRIMARY KEY,address TEXT NOT NULL,parts INTEGER NOT NULL,started INTEGER NOT NULL,error TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE receipts(id TEXT NOT NULL,part INTEGER NOT NULL,result INTEGER NOT NULL,PRIMARY KEY(id,part))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun create(id: String, address: String, parts: Int) {
        writableDatabase.insertOrThrow("sends", null, ContentValues().apply {
            put("id", id); put("address", address); put("parts", parts); put("started", System.currentTimeMillis())
        })
    }
    fun record(id: String, part: Int, result: Int) {
        val valid = readableDatabase.rawQuery("SELECT parts FROM sends WHERE id=?", arrayOf(id)).use { it.moveToFirst() && part in 0 until it.getInt(0) }
        if (!valid) return
        writableDatabase.insertWithOnConflict("receipts", null, ContentValues().apply {
            put("id", id); put("part", part); put("result", result)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }
    fun fail(id: String, error: String) { writableDatabase.update("sends", ContentValues().apply { put("error", error) }, "id=?", arrayOf(id)) }
    fun recent(): List<SmsSend> = readableDatabase.rawQuery(
        "SELECT s.id,s.address,s.parts,COUNT(r.part),COALESCE(SUM(CASE WHEN r.result != -1 THEN 1 ELSE 0 END),0),s.started,s.error FROM sends s LEFT JOIN receipts r ON r.id=s.id GROUP BY s.id ORDER BY s.started DESC LIMIT 10", null
    ).use { c -> buildList { while(c.moveToNext()) add(SmsSend(c.getString(0),c.getString(1),c.getInt(2),c.getInt(3),c.getInt(4),c.getLong(5),c.getString(6))) } }
}

class SmsRepository(private val context: Context) {
    fun canRead() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    fun canSend() = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    fun read(limit: Int = 500, thread: Long? = null): List<DeviceSms> {
        check(canRead()) { "Nadaj dostęp do SMS-ów w systemowym oknie uprawnień." }
        val result = ArrayList<DeviceSms>()
        val projection = arrayOf("_id", "thread_id", "address", "body", "date", "type", "read")
        // Selection is parameterized; traversal is bounded even on providers rejecting SQL LIMIT.
        context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection,
            if (thread == null) null else "thread_id=?", thread?.let { arrayOf(it.toString()) }, "date DESC")?.use { c ->
            while (c.moveToNext() && result.size < limit.coerceIn(1, 5000)) {
                result.add(DeviceSms(c.getLong(0),c.getLong(1),c.getString(2).orEmpty(),c.getString(3).orEmpty(),c.getLong(4),c.getInt(5),c.getInt(6) != 0))
            }
        } ?: error("System nie udostępnił skrzynki SMS.")
        return result
    }
    fun send(address: String, text: String): String {
        check(canSend()) { "Brak uprawnienia do wysyłania SMS." }
        val phone = PhoneNumbers.parse(address)?.e164 ?: error("Wpisz pełny numer odbiorcy z prefiksem kraju.")
        require(text.isNotBlank() && text.length <= 2000) { "Wiadomość musi mieć od 1 do 2000 znaków." }
        val sub = SubscriptionManager.getDefaultSmsSubscriptionId()
        // Avoid API-29-only validation: reject negative IDs and the unresolved default sentinel.
        check(sub >= 0 && sub < Int.MAX_VALUE) { "Wybierz domyślną kartę SIM do wysyłania SMS w ustawieniach telefonu." }
        @Suppress("DEPRECATION")
        val manager = SmsManager.getSmsManagerForSubscriptionId(sub)
        val parts = manager.divideMessage(text)
        val id = UUID.randomUUID().toString()
        SmsSendStore(context).use { it.create(id, phone, parts.size) }
        val callbacks = ArrayList(parts.indices.map { part ->
            val intent = Intent(context, SmsSentReceiver::class.java).apply {
                action = context.packageName + ".SMS_SENT"
                data = Uri.parse("czytoty://sent/$id/$part")
                putExtra("send_id", id); putExtra("part", part)
            }
            PendingIntent.getBroadcast(context, part, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        })
        try {
            manager.sendMultipartTextMessage(phone, null, parts, callbacks, null)
        } catch (e: Exception) {
            callbacks.forEach { it.cancel() }
            SmsSendStore(context).use { it.fail(id, "Telefon nie przyjął zlecenia wysyłki. Sprawdź kartę SIM i uprawnienia.") }
            throw e
        } finally { SmsChanges.changed() }
        return id
    }
}

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != context.packageName + ".SMS_SENT") return
        val id = intent.getStringExtra("send_id") ?: return
        val part = intent.getIntExtra("part", -1)
        SmsSendStore(context).use { it.record(id, part, resultCode) }
        SmsChanges.changed()
    }
}

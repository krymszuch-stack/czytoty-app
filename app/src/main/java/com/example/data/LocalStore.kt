package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.Serializable

@Serializable
data class PhoneNote(val phone: String, val label: String = "", val note: String = "", val blocked: Boolean = false)
data class ScreeningEvent(val id: Long, val phone: String, val blocked: Boolean, val time: Long)

/** Device-only data. Separate database from the old presentation build; no fabricated seed rows. */
class LocalStore private constructor(context: Context) : SQLiteOpenHelper(context, "czytoty_prototype.db", null, 1) {
    private val prefs = context.getSharedPreferences("prototype_settings", Context.MODE_PRIVATE)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE phone_notes(phone TEXT PRIMARY KEY, label TEXT NOT NULL, note TEXT NOT NULL, blocked INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE screening_events(id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT NOT NULL, blocked INTEGER NOT NULL, time INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    var protectionEnabled: Boolean
        get() = prefs.getBoolean("protection_enabled", true)
        set(value) { check(prefs.edit().putBoolean("protection_enabled", value).commit()) }

    fun notes(): List<PhoneNote> = readableDatabase.rawQuery("SELECT phone,label,note,blocked FROM phone_notes ORDER BY label,phone", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(PhoneNote(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) == 1)) }
    }
    fun isBlocked(phone: String): Boolean = readableDatabase.rawQuery("SELECT blocked FROM phone_notes WHERE phone=?", arrayOf(phone)).use {
        it.moveToFirst() && it.getInt(0) == 1
    }
    fun save(note: PhoneNote) {
        require(PhoneNumbers.parse(note.phone)?.e164 == note.phone)
        require(note.label.length <= 80 && note.note.length <= 500)
        val values = ContentValues().apply {
            put("phone", note.phone); put("label", note.label); put("note", note.note); put("blocked", if (note.blocked) 1 else 0)
        }
        check(writableDatabase.insertWithOnConflict("phone_notes", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    fun delete(phone: String) { writableDatabase.delete("phone_notes", "phone=?", arrayOf(phone)) }
    fun record(phone: String, blocked: Boolean) {
        val values = ContentValues().apply {
            put("phone", phone); put("blocked", if (blocked) 1 else 0); put("time", System.currentTimeMillis())
        }
        check(writableDatabase.insertOrThrow("screening_events", null, values) != -1L)
        writableDatabase.execSQL("DELETE FROM screening_events WHERE id NOT IN (SELECT id FROM screening_events ORDER BY id DESC LIMIT 200)")
    }
    fun events(): List<ScreeningEvent> = readableDatabase.rawQuery("SELECT id,phone,blocked,time FROM screening_events ORDER BY id DESC LIMIT 200", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(ScreeningEvent(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) == 1, cursor.getLong(3))) }
    }
    fun clearEvents() { writableDatabase.delete("screening_events", null, null) }
    companion object {
        @Volatile private var instance: LocalStore? = null
        fun get(context: Context): LocalStore = instance ?: synchronized(this) {
            instance ?: LocalStore(context.applicationContext).also { instance = it }
        }
    }
}

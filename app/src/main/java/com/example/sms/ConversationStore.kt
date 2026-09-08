package com.example.sms

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.PhoneNumbers

data class SmsDraft(val address: String = "", val body: String = "")

/** Local app metadata only. Never writes to Android's SMS provider or to the cloud. */
class ConversationStore(context: Context) : SQLiteOpenHelper(context, "sms-conversations.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE drafts(thread_key TEXT PRIMARY KEY, address TEXT NOT NULL, body TEXT NOT NULL CHECK(length(body)<=2000))")
        db.execSQL("CREATE TABLE pinned_threads(thread_id INTEGER PRIMARY KEY)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun drafts(): Map<String, SmsDraft> = readableDatabase.rawQuery("SELECT thread_key,address,body FROM drafts", null).use { c ->
        buildMap { while (c.moveToNext()) put(c.getString(0), SmsDraft(c.getString(1), c.getString(2))) }
    }
    fun saveDraft(key: String, value: SmsDraft) {
        require(key == "new" || key.toLongOrNull()?.let { it >= 0 } == true)
        require(value.address.length <= 40 && value.body.length <= 2000)
        if (value.body.isEmpty() && value.address.isEmpty()) writableDatabase.delete("drafts", "thread_key=?", arrayOf(key))
        else check(writableDatabase.insertWithOnConflict("drafts", null, ContentValues().apply {
            put("thread_key", key); put("address", value.address); put("body", value.body)
        }, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    fun pins(): Set<Long> = readableDatabase.rawQuery("SELECT thread_id FROM pinned_threads", null).use { c ->
        buildSet { while (c.moveToNext()) add(c.getLong(0)) }
    }
    fun pin(thread: Long, pinned: Boolean) {
        require(thread >= 0)
        if (pinned) writableDatabase.insertWithOnConflict("pinned_threads", null, ContentValues().apply { put("thread_id", thread) }, SQLiteDatabase.CONFLICT_IGNORE)
        else writableDatabase.delete("pinned_threads", "thread_id=?", arrayOf(thread.toString()))
    }
}

/** Search includes every loaded message, not just each thread's latest preview. */
fun visibleThreads(messages: List<DeviceSms>, query: String, unreadOnly: Boolean, pinsOnly: Boolean,
    pins: Set<Long>, contacts: Map<String, String>): List<DeviceSms> = messages.groupBy { it.thread }.values
    .filter { group ->
        val latest = group.maxBy { it.time }
        val name = contacts[PhoneNumbers.parse(latest.address)?.e164 ?: latest.address].orEmpty()
        (!unreadOnly || group.any { it.incoming && !it.read }) && (!pinsOnly || latest.thread in pins) &&
            (query.isBlank() || name.contains(query, true) || group.any { it.address.contains(query, true) || it.body.contains(query, true) })
    }.map { group -> group.maxBy { it.time } }
    .sortedWith(compareByDescending<DeviceSms> { it.thread in pins }.thenByDescending { it.time })

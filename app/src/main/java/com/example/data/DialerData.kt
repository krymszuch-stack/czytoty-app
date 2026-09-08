package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class CallHistoryEntry(
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val durationSec: Long
) {
    fun typeLabel(): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Połączenie przychodzące"
        CallLog.Calls.OUTGOING_TYPE -> "Połączenie wychodzące"
        CallLog.Calls.MISSED_TYPE -> "Nieodebrane"
        CallLog.Calls.REJECTED_TYPE -> "Odrzucone"
        CallLog.Calls.BLOCKED_TYPE -> "Zablokowane przez system"
        else -> "Połączenie"
    }
}

data class DeviceContact(val name: String, val phone: String)

/** Reads system call log and contacts. Returns empty lists when permission is missing. */
object DialerData {
    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun recentCalls(context: Context, limit: Int = 100): List<CallHistoryEntry> {
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) return emptyList()
        return run {
            val result = mutableListOf<CallHistoryEntry>()
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                val iNumber = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val iName = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val iType = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val iDate = cursor.getColumnIndex(CallLog.Calls.DATE)
                val iDuration = cursor.getColumnIndex(CallLog.Calls.DURATION)
                while (cursor.moveToNext() && result.size < limit.coerceIn(1, 200)) {
                    result.add(
                        CallHistoryEntry(
                            number = if (iNumber >= 0) cursor.getString(iNumber).orEmpty() else "",
                            name = if (iName >= 0) cursor.getString(iName) else null,
                            type = if (iType >= 0) cursor.getInt(iType) else 0,
                            date = if (iDate >= 0) cursor.getLong(iDate) else 0L,
                            durationSec = if (iDuration >= 0) cursor.getLong(iDuration) else 0L
                        )
                    )
                }
            }
            result
        }
    }

    fun contacts(context: Context, filter: String = "", limit: Int = 200): List<DeviceContact> {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) return emptyList()
        return run {
            val result = mutableListOf<DeviceContact>()
            val selection = if (filter.isBlank()) null else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val args = if (filter.isBlank()) null else arrayOf("%$filter%")
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                selection, args, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val iName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val iNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = HashSet<String>()
                while (cursor.moveToNext() && result.size < limit.coerceIn(1, 500)) {
                    val phone = if (iNumber >= 0) cursor.getString(iNumber).orEmpty().trim() else ""
                    if (phone.isBlank() || !seen.add(phone)) continue
                    val name = if (iName >= 0) cursor.getString(iName).orEmpty() else ""
                    result.add(DeviceContact(name.ifBlank { phone }, phone))
                }
            }
            result
        }
    }

    fun contactName(context: Context, phone: String): String? {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) return null
        return run {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }
    }
}


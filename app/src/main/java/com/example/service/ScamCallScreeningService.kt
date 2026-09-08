package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.LocalStore
import com.example.data.PhoneNumbers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScamCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onScreenCall(details: Call.Details) {
        if (Build.VERSION.SDK_INT >= 29 && details.callDirection != Call.Details.DIRECTION_INCOMING) return
        val raw = details.handle?.schemeSpecificPart.orEmpty()
        val phone = PhoneNumbers.parse(raw)?.e164
        val store = LocalStore.get(this)
        // No network request on this path. Fail open; respond before logging to stay within Android's 5s limit.
        val blocked = phone != null && runCatching { store.protectionEnabled && store.isBlocked(phone) }.getOrDefault(false)
        respondToCall(details, CallResponse.Builder()
            .setDisallowCall(blocked).setRejectCall(blocked)
            .setSkipCallLog(false).setSkipNotification(false).build())
        scope.launch { runCatching { store.record(phone ?: "Numer niedostępny", blocked) } }
    }
}

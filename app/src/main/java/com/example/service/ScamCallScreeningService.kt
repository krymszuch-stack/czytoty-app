package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.example.data.DefaultCallerRepository
import com.example.model.RiskSeverity

/**
 * Minimalna produkcyjna implementacja CallScreeningService systemu Android.
 * Odpowiada za bezpośrednie filtrowanie przychodzących połączeń w czasie rzeczywistym
 * z poziomu telekomunikacyjnego stosu Androida (gdy użytkownik nada uprawnienie w systemie).
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ScamCallScreeningService : CallScreeningService() {

  private val repository = DefaultCallerRepository()

  override fun onScreenCall(callDetails: Call.Details) {
    val handle = callDetails.handle
    val incomingNumber = handle?.schemeSpecificPart ?: "Nieznany"

    // Szybki lokalny lookup (Offline-First)
    val callerInfo = repository.lookupNumber(incomingNumber)
    val isHighRisk = callerInfo.severity == RiskSeverity.HIGH
    val settings = repository.protectionSettings.value

    val shouldBlock = isHighRisk && settings.isProtectionActive && settings.blockHighRisk

    // Rejestracja zdarzenia w lokalnym dzienniku bez konieczności uprawnienia READ_CALL_LOG
    repository.recordScreeningEvent(
      phoneNumber = incomingNumber,
      action = if (shouldBlock) "BLOCKED" else "ALLOWED"
    )

    // Przekazanie do aktywnego UI jeśli aplikacja jest na pierwszym planie
    CallScreeningBridge.postIncomingCall(
      IncomingCallEvent(
        callerInfo = callerInfo,
        isRealScreeningServiceCall = true
      )
    )

    val response = CallResponse.Builder()
      .setDisallowCall(shouldBlock)
      .setRejectCall(shouldBlock)
      .setSkipCallLog(false)
      .setSkipNotification(shouldBlock)
      .build()

    respondToCall(callDetails, response)
  }
}

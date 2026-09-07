package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.util.Log

/**
 * Główny BroadcastReceiver dla przychodzących wiadomości SMS.
 * Spełnia oficjalne wymagania roli Default SMS App (android.provider.Telephony.SMS_DELIVER)
 * oraz obsługuje standardowy fallback SMS_RECEIVED w razie braku uprawnienia domyślnej aplikacji.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
      val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
      if (messages.isNullOrEmpty()) return

      val sender = messages[0].displayOriginatingAddress ?: "Nieznany"
      val body = messages.joinToString("") { it.displayMessageBody ?: "" }

      Log.d("SmsDeliverReceiver", "Odebrano SMS (DELIVER) od: $sender (${body.length} znaków)")
      SmsBridge.postIncomingSms(sender, body)
    }
  }
}

/**
 * Fallback BroadcastReceiver dla standardowego odbioru SMS_RECEIVED
 */
class SmsReceivedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
      val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
      if (messages.isNullOrEmpty()) return

      val sender = messages[0].displayOriginatingAddress ?: "Nieznany"
      val body = messages.joinToString("") { it.displayMessageBody ?: "" }

      Log.d("SmsReceivedReceiver", "Odebrano SMS (RECEIVED) od: $sender (${body.length} znaków)")
      SmsBridge.postIncomingSms(sender, body)
    }
  }
}

/**
 * BroadcastReceiver dla MMS wymagany przez Androida dla domyślnej aplikacji SMS
 */
class MmsDeliverReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d("MmsDeliverReceiver", "Odebrano zdarzenie WAP PUSH MMS")
  }
}

/**
 * Serwis wymagany przez system Android dla domyślnej aplikacji SMS do obsługi
 * szybkich odpowiedzi (Respond via message z powiadomień / połączeń).
 */
class HeadlessSmsSendService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Prosty mostek zdarzeń SMS dla aktywnego UI
 */
object SmsBridge {
  data class IncomingSmsEvent(val sender: String, val body: String, val timestamp: Long = System.currentTimeMillis())

  private val listeners = mutableListOf<(IncomingSmsEvent) -> Unit>()

  fun addListener(listener: (IncomingSmsEvent) -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: (IncomingSmsEvent) -> Unit) {
    listeners.remove(listener)
  }

  fun postIncomingSms(sender: String, body: String) {
    val event = IncomingSmsEvent(sender, body)
    listeners.forEach { it.invoke(event) }
  }
}

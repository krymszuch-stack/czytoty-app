package com.example.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CertRepository
import com.example.data.DomainChecker
import kotlinx.coroutines.*

/** SMS_RECEIVED observes incoming SMS while the current default messaging app persists them. */
class SmsFraudReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return
        val body = messages.joinToString("") { it.messageBody.orEmpty() }.take(20_000)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8000) {
                    val snapshot = CertRepository(context).load()
                    val analysis = DomainChecker.analyze(body, snapshot)
                    val listed = analysis.findings.any { it.matchedDomain != null }
                    if (!listed && analysis.localSignals.isEmpty()) return@withTimeout
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@withTimeout
                    val manager = context.getSystemService(NotificationManager::class.java)
                    manager.createNotificationChannel(NotificationChannel("sms_safety", "Sygnały ryzyka w SMS-ach", NotificationManager.IMPORTANCE_HIGH))
                    val open = Intent(context, MainActivity::class.java).apply { putExtra("open_sms", true) }
                    val action = PendingIntent.getActivity(context, 701, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val description = if (listed && snapshot?.isFresh() == true) "Domena z listy CERT w nowym SMS-ie."
                        else if (listed) "Domena z zapisanej kopii CERT. Sprawdź aktualność listy."
                        else "W nowym SMS-ie są sformułowania wymagające ostrożności."
                    manager.notify(701, NotificationCompat.Builder(context, "sms_safety")
                        .setSmallIcon(R.drawable.ic_protection_shield).setContentTitle("CzyToTy? • Sprawdź SMS")
                        .setContentText(description).setContentIntent(action).setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
                }
            } catch (_: Exception) {
                // No message contents or senders in logs; the default SMS app still receives and saves the message.
            } finally { SmsChanges.changed(); pending.finish() }
        }
    }
}

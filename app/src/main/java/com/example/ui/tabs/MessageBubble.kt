package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.CertSnapshot
import com.example.data.DomainChecker
import com.example.sms.DeviceSms
import android.provider.Telephony
import java.text.DateFormat
import java.util.Date

@Composable
fun MessageBubble(sms: DeviceSms, snapshot: CertSnapshot?, onCheck: (String) -> Unit) {
    val analysis = remember(sms.body, snapshot) { DomainChecker.analyze(sms.body, snapshot) }
    val hit = analysis.findings.any { it.matchedDomain != null }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (sms.incoming) Arrangement.Start else Arrangement.End) {
        Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = if (sms.incoming) 6.dp else 24.dp,
            bottomEnd = if (sms.incoming) 24.dp else 6.dp), color = if (sms.incoming) Color(0xFF1D1D2D) else Color(0xFF453064), contentColor = Color(0xFFFAF4FF),
            modifier = Modifier.fillMaxWidth(.87f).widthIn(max = 380.dp)) {
            Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectionContainer { Text(sms.body, style = MaterialTheme.typography.bodyLarge) }
                if (hit) Text(if (snapshot?.isFresh() == true) "Uwaga · domena z listy CERT" else "Uwaga · domena z zapisanej kopii CERT", color = Color(0xFFFFD089), style = MaterialTheme.typography.labelMedium)
                else if (analysis.localSignals.isNotEmpty()) Text("Warto sprawdzić tę treść", color = Color(0xFFFFD089), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(sms.time)) + when (sms.type) {
                        Telephony.Sms.MESSAGE_TYPE_SENT -> " · wysłano"
                        Telephony.Sms.MESSAGE_TYPE_FAILED -> " · błąd"
                        Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> " · oczekuje"
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> " · szkic"
                        else -> ""
                    }, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC2BCCC))
                    IconButton(onClick = { onCheck(sms.body) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Shield, "Sprawdź treść wiadomości", modifier = Modifier.size(17.dp), tint = Color(0xFFC2BCCC))
                    }
                }
            }
        }
    }
}

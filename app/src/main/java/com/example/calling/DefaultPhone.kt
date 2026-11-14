package com.example.calling

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

object DefaultPhone {
    fun openDialer(context: Context, value: String) {
        val target = DialTarget.normalize(value) ?: return
        context.startActivity(Intent(context, com.example.MainActivity::class.java).apply {
            action = Intent.ACTION_DIAL; data = Uri.fromParts("tel", target, null)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }
    fun isDefault(context: Context) = context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == context.packageName
    fun call(context: Context, value: String) {
        val target = DialTarget.normalize(value) ?: return
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return
        try {
            if (isDefault(context)) {
                check(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                telecom.placeCall(Uri.fromParts("tel", target, null), Bundle())
            } else {
                val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", target, null))
                telecom.defaultDialerPackage?.let { intent.setPackage(it) }
                context.startActivity(intent)
            }
        } catch (_: Exception) { Toast.makeText(context, "Nie udało się rozpocząć połączenia. Sprawdź rolę Telefon i uprawnienia.", Toast.LENGTH_LONG).show() }
    }
}

@Composable
fun DefaultPhoneSetup() {
    val context = LocalContext.current
    var held by remember { mutableStateOf(DefaultPhone.isDefault(context)) }
    val role = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { held = DefaultPhone.isDefault(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { held = DefaultPhone.isDefault(context) }
    var fullScreen by remember { mutableStateOf(Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) }
    val fullScreenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        fullScreen = Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (held) "CzyToTy? obsługuje ekran rozmów" else "Twój domyślny Telefon", style = MaterialTheme.typography.titleMedium)
        Text("Własny ekran odbierania i prowadzenia rozmowy. To wersja testowa: przed codziennym użyciem sprawdź połączenie z drugim telefonem.", style = MaterialTheme.typography.bodySmall)
        if (!held) FilledTonalButton(onClick = {
            if (Build.VERSION.SDK_INT >= 29) {
                val manager = context.getSystemService(RoleManager::class.java)
                if (manager.isRoleAvailable(RoleManager.ROLE_DIALER)) role.launch(manager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            } else {
                @Suppress("DEPRECATION")
                role.launch(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName))
            }
        }, modifier = Modifier.testTag("default_phone_setup")) { Text("Ustaw jako domyślny Telefon") }
        TextButton(onClick = { permission.launch(buildList { add(Manifest.permission.CALL_PHONE); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray()) }) { Text("Uprawnienia połączeń i powiadomień") }
        if (!fullScreen && Build.VERSION.SDK_INT >= 34) TextButton(onClick = {
            fullScreenLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
        }) { Text("Zezwól na ekran połączenia przy blokadzie") }
    }
}

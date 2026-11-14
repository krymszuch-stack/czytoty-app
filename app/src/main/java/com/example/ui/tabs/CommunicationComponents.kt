package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline

@Composable
fun ContactAvatar(name: String, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFF6743A5), Color(0xFF375E83), Color(0xFF466A62), Color(0xFF83516D))
    val color = colors[(name.hashCode().toLong().let { if (it < 0) -it else it } % colors.size).toInt()]
    BoxWithConstraints(modifier.size(48.dp).background(Brush.linearGradient(listOf(color, color.copy(alpha = .45f))), CircleShape), contentAlignment = Alignment.Center) {
        if (name.any(Char::isLetter)) Text(name.trim().firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (maxWidth.value * .34f).sp)
        else Icon(Icons.Outlined.PersonOutline, null, tint = Color(0xFFEAE2F4), modifier = Modifier.size(maxWidth * .45f))
    }
}

@Composable
fun PermissionPanel(title: String, description: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onClick) { Text(action) }
        }
    }
}

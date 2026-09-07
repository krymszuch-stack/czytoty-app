package com.example.ui.screens.incall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.incall.model.AudioRoute
import com.example.incall.model.NetworkStatusInfo
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppTextSecondary

/**
 * Minimalistyczny wskaźnik typu i jakości sieci oraz trasy audio (Sekcje 80, 81, 82, 94, 95).
 */
@Composable
fun InCallNetworkIndicator(
  networkStatus: NetworkStatusInfo?,
  audioRoute: AudioRoute,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
  ) {
    // 1. Dyskretna etykieta Bluetooth
    if (audioRoute == AudioRoute.BLUETOOTH) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1B2E),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.padding(end = 8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = "Bluetooth",
            tint = Color(0xFF8E7CFF),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Bluetooth",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFCBC3DE)
          )
        }
      }
    }

    // 2. Minimalistyczny wskaźnik sieci (np. ▂▄▆█ 5G lub Wi-Fi Calling)
    if (networkStatus != null) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF15141D),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.6f))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = if (networkStatus.isWifiCalling) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
            contentDescription = null,
            tint = Color(0xFF9E95B8),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = networkStatus.visualIndicator,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AppTextSecondary
          )
        }
      }
    }
  }
}

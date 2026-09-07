package com.example.safetycheck.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.safetycheck.model.LocationSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Interfejs dostawcy lokalizacji dla modułu Safety Check.
 *
 * ZASADY:
 * 1. Lokalizacja pobierana jest wyłącznie PO wystąpieniu triggera.
 * 2. Asynchroniczny, krótki timeout (max 2.5 s) zapobiega blokowaniu wysyłki alertu.
 * 3. Jeśli lokalizacja jest niedostępna lub brak uprawnień, alert wysyłany jest bez niej.
 */
interface SafetyLocationProvider {
    suspend fun getCurrentLocation(timeoutMs: Long = 2500L): LocationSnapshot?
}

class AndroidSafetyLocationProvider(
    private val context: Context
) : SafetyLocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(timeoutMs: Long): LocationSnapshot? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeoutMs) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    ?: return@withTimeoutOrNull null

                // 1. Sprawdź szybką ostatnią znaną lokalizację
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                var bestLocation: Location? = null

                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                            bestLocation = loc
                        }
                    }
                }

                bestLocation?.let {
                    LocationSnapshot(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracyMeters = it.accuracy,
                        timestamp = it.time
                    )
                }
            } catch (_: Throwable) {
                // Brak uprawnień lub błąd systemowy - kontynuuj bez lokalizacji
                null
            }
        }
    }
}

/**
 * Syntetyczny dostawca lokalizacji dla testów jednostkowych i symulacji deweloperskich.
 */
class SyntheticSafetyLocationProvider(
    private var mockLocation: LocationSnapshot? = LocationSnapshot(52.2297, 21.0122, 12.5f)
) : SafetyLocationProvider {

    fun setMockLocation(location: LocationSnapshot?) {
        this.mockLocation = location
    }

    override suspend fun getCurrentLocation(timeoutMs: Long): LocationSnapshot? {
        return mockLocation
    }
}

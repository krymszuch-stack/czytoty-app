package com.example.incall.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.incall.model.AudioRoute
import com.example.incall.model.ProximityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Interfejs zarządzania sensorem zbliżeniowym i blokadą dotyku (Sekcje 77, 78, 92–94).
 */
interface ProximitySensorManager {
  val proximityState: StateFlow<ProximityState>
  val isTouchLocked: StateFlow<Boolean>
  fun startMonitoring()
  fun stopMonitoring()
  fun setAudioRoute(route: AudioRoute)
  fun simulateSensorEvent(isNear: Boolean) // Pomocne do testów i symulatora
}

/**
 * Produkcyjna implementacja sensora zbliżeniowego z debouncingiem i regułami audio route.
 */
class AndroidProximitySensorManager(
  context: Context,
  private val scope: CoroutineScope,
  private val debounceMs: Long = 250L
) : ProximitySensorManager, SensorEventListener {

  private val sensorManager: SensorManager? =
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  private val proximitySensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

  private val _proximityState = MutableStateFlow(ProximityState.FAR)
  override val proximityState: StateFlow<ProximityState> = _proximityState.asStateFlow()

  private val _isTouchLocked = MutableStateFlow(false)
  override val isTouchLocked: StateFlow<Boolean> = _isTouchLocked.asStateFlow()

  private var currentAudioRoute: AudioRoute = AudioRoute.EARPIECE
  private var debounceJob: Job? = null
  private var isRegistered = false

  override fun startMonitoring() {
    if (isRegistered || proximitySensor == null) return
    sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    isRegistered = true
  }

  override fun stopMonitoring() {
    if (!isRegistered) return
    debounceJob?.cancel()
    sensorManager?.unregisterListener(this)
    isRegistered = false
    _proximityState.value = ProximityState.FAR
    _isTouchLocked.value = false
  }

  override fun setAudioRoute(route: AudioRoute) {
    currentAudioRoute = route
    reevaluateTouchLock(_proximityState.value)
  }

  override fun simulateSensorEvent(isNear: Boolean) {
    handleRawProximityChange(isNear)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return
    val distance = event.values.firstOrNull() ?: return
    val maxRange = proximitySensor?.maximumRange ?: 5f
    // Typowo w Androidzie sensor zwraca 0.0 dla NEAR lub < maximumRange
    val isNear = distance < maxRange && distance < 4f
    handleRawProximityChange(isNear)
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // Nie wymaga reakcji dla sensora proximity
  }

  private fun handleRawProximityChange(isNear: Boolean) {
    val newState = if (isNear) ProximityState.NEAR else ProximityState.FAR

    // Natychmiastowe zablokowanie przy zbliżeniu (Zero Accidental Touch)
    // Debounce przy odsuwaniu lub szybkich zmianach, aby zapobiec migotaniu
    debounceJob?.cancel()
    if (newState == ProximityState.NEAR) {
      _proximityState.value = ProximityState.NEAR
      reevaluateTouchLock(ProximityState.NEAR)
    } else {
      debounceJob = scope.launch {
        delay(debounceMs)
        _proximityState.value = ProximityState.FAR
        reevaluateTouchLock(ProximityState.FAR)
      }
    }
  }

  private fun reevaluateTouchLock(state: ProximityState) {
    // Sekcje 93 i 94: Jeżeli aktywny jest głośnomówiący lub słuchawki Bluetooth,
    // nie blokujemy agresywnie dotyku i nie wygaszamy ekranu przy przypadkowym zasłonięciu sensora.
    val bypassTouchLock = currentAudioRoute == AudioRoute.SPEAKER || currentAudioRoute == AudioRoute.BLUETOOTH
    _isTouchLocked.value = (state == ProximityState.NEAR) && !bypassTouchLock
  }
}

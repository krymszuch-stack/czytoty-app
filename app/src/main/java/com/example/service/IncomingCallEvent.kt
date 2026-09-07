package com.example.service

import com.example.model.CallerInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Zdarzenie przychodzącego połączenia przekazywane z CallScreeningService do warstwy prezentacji.
 */
data class IncomingCallEvent(
  val callerInfo: CallerInfo,
  val isRealScreeningServiceCall: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

object CallScreeningBridge {
  private val _incomingEvents = MutableSharedFlow<IncomingCallEvent>(extraBufferCapacity = 1)
  val incomingEvents: SharedFlow<IncomingCallEvent> = _incomingEvents.asSharedFlow()

  fun postIncomingCall(event: IncomingCallEvent) {
    _incomingEvents.tryEmit(event)
  }
}

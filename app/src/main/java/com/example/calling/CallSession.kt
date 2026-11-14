package com.example.calling

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CallItem(
    val id: String,
    val number: String,
    val state: Int,
    val connectedAt: Long,
    val canHold: Boolean,
    val canMerge: Boolean,
    val conference: Boolean,
    val accountChoices: List<Pair<String, String>> = emptyList()
) {
    val ringing get() = state == Call.STATE_RINGING
    val live get() = state != Call.STATE_DISCONNECTED
    val stateLabel get() = when (state) {
        Call.STATE_RINGING -> "Połączenie przychodzące"
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Łączenie…"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "Wybierz kartę SIM"
        Call.STATE_ACTIVE -> "Rozmowa trwa"
        Call.STATE_HOLDING -> "Połączenie zawieszone"
        Call.STATE_DISCONNECTING -> "Rozłączanie…"
        Call.STATE_DISCONNECTED -> "Połączenie zakończone"
        else -> "Oczekiwanie na telefon"
    }
}
data class AudioChoice(val id: String, val label: String, val nearEar: Boolean = false)
data class CallSessionState(
    val calls: List<CallItem> = emptyList(),
    val muted: Boolean = false,
    val audio: List<AudioChoice> = emptyList(),
    val selectedAudio: String? = null,
    val canAdd: Boolean = false,
    val error: String? = null
)

/** Published exclusively from Telecom callbacks. No synthetic live calls in production. */
object CallSession {
    private val mutable = MutableStateFlow(CallSessionState())
    val state = mutable.asStateFlow()
    internal var service: CzyToTyInCallService? = null
    internal fun update(transform: (CallSessionState) -> CallSessionState) { mutable.update(transform) }
    internal fun clear() { mutable.value = CallSessionState() }
    fun answer(id: String) { service?.answerCall(id) }
    fun end(id: String) { service?.endCall(id) }
    fun hold(id: String) { service?.holdCall(id) }
    fun merge(id: String) { service?.mergeCall(id) }
    fun mute() { service?.changeMute() }
    fun audio(id: String) { service?.changeAudio(id) }
    fun tone(id: String, digit: Char?) { service?.tone(id, digit) }
    fun account(id: String, index: Int) { service?.selectAccount(id, index) }
    fun dismissError() { update { it.copy(error = null) } }
}

object DialTarget {
    fun normalize(value: String): String? {
        val normalized = value.filterNot { it.isWhitespace() || it in "()- ." }
        return normalized.takeIf { it.length in 1..40 && it.matches(Regex("\\+?[0-9*#]+")) && it.any(Char::isDigit) }
    }
}

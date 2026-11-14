package com.example.calling

import android.app.*
import android.content.*
import android.os.*
import android.telecom.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.R
import java.util.UUID

class CzyToTyInCallService : InCallService() {
    private val tracked = linkedMapOf<String, Call>()
    private val callbacks = mutableMapOf<Call, Call.Callback>()
    private val endpoints = mutableMapOf<String, CallEndpoint>()
    private val accounts = mutableMapOf<String, List<PhoneAccountHandle>>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        CallSession.service = this
        val notifications = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel("calls", "Połączenia telefoniczne", NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(null, null) // Telecom owns the ringtone: IN_CALL_SERVICE_RINGING is false.
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notifications.createNotificationChannel(channel)
    }
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        tracked[UUID.randomUUID().toString()] = call
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) { publish() }
            override fun onDetailsChanged(call: Call, details: Call.Details) { publish() }
            override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) { publish() }
            override fun onChildrenChanged(call: Call, children: MutableList<Call>) { publish() }
            override fun onParentChanged(call: Call, parent: Call?) { publish() }
        }
        callbacks[call] = callback
        call.registerCallback(callback, handler)
        publish()
        if (call.state != Call.STATE_RINGING || !getSystemService(NotificationManager::class.java).areNotificationsEnabled()) showScreen()
    }
    override fun onCallRemoved(call: Call) {
        callbacks.remove(call)?.let(call::unregisterCallback)
        tracked.entries.removeAll { it.value === call }
        publish()
        super.onCallRemoved(call)
    }
    override fun onBringToForeground(showDialpad: Boolean) { showScreen(showDialpad) }
    override fun onCanAddCallChanged(canAddCall: Boolean) { CallSession.update { it.copy(canAdd = canAddCall) } }
    private fun showScreen(dialpad: Boolean = false) {
        runCatching { startActivity(Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("dialpad", dialpad)) }
    }
    @Suppress("DEPRECATION")
    private fun publish() {
        accounts.clear()
        val telecom = getSystemService(TelecomManager::class.java)
        val items = tracked.mapNotNull { (id, call) ->
            if (call.parent != null) return@mapNotNull null
            val details = call.details
            val options = if (call.state == Call.STATE_SELECT_PHONE_ACCOUNT) {
                @Suppress("DEPRECATION")
                details.intentExtras?.getParcelableArrayList<PhoneAccountHandle>(Call.AVAILABLE_PHONE_ACCOUNTS).orEmpty()
            } else emptyList()
            accounts[id] = options
            CallItem(id, if (details.handlePresentation == TelecomManager.PRESENTATION_ALLOWED) details.handle?.schemeSpecificPart.orEmpty() else "",
                call.state, details.connectTimeMillis,
                details.can(Call.Details.CAPABILITY_HOLD),
                details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) || call.conferenceableCalls.isNotEmpty(),
                details.hasProperty(Call.Details.PROPERTY_CONFERENCE),
                options.mapIndexed { index, account -> index.toString() to runCatching { telecom.getPhoneAccount(account)?.label?.toString() }.getOrNull().orEmpty().ifBlank { "SIM ${index + 1}" } })
        }
        CallSession.update { it.copy(calls = items, canAdd = canAddCall()) }
        updateNotification(items)
    }
    private fun pending(action: String, id: String): PendingIntent = PendingIntent.getBroadcast(this, action.hashCode(),
        Intent(this, CallActionReceiver::class.java).setAction(action).setData(android.net.Uri.parse("czytoty://call/$id/$action")).putExtra("call_id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun updateNotification(items: List<CallItem>) {
        val manager = getSystemService(NotificationManager::class.java)
        val call = items.firstOrNull { it.ringing } ?: items.firstOrNull { it.live }
        if (call == null) { manager.cancel(800); return }
        val open = PendingIntent.getActivity(this, 800,
            Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val person = Person.Builder().setName(call.number.ifBlank { if (call.conference) "Konferencja" else "Numer prywatny" }).setImportant(true).build()
        val style = if (call.ringing) NotificationCompat.CallStyle.forIncomingCall(person, pending("end", call.id), pending("answer", call.id))
            else NotificationCompat.CallStyle.forOngoingCall(person, pending("end", call.id))
        val builder = NotificationCompat.Builder(this, "calls").setSmallIcon(R.drawable.ic_phone_lookup)
            .setContentTitle(person.name).setContentText(call.stateLabel).setStyle(style).setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_CALL).setOngoing(true).setOnlyAlertOnce(!call.ringing)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        if (call.ringing) builder.setFullScreenIntent(open, true)
        runCatching { manager.notify(800, builder.build()) }
            .onFailure { CallSession.update { it.copy(error = "Android nie pozwolił wyświetlić powiadomienia połączenia.") }; showScreen() }
    }
    private fun operate(id: String, action: (Call) -> Unit) {
        val call = tracked[id] ?: return
        runCatching { action(call) }.onFailure { CallSession.update { it.copy(error = "Nie udało się wykonać czynności. Sprawdź bieżący stan połączenia.") } }
    }
    @Suppress("DEPRECATION")
    fun answerCall(id: String) = operate(id) { if (it.state == Call.STATE_RINGING) it.answer(VideoProfile.STATE_AUDIO_ONLY) }
    @Suppress("DEPRECATION")
    fun endCall(id: String) = operate(id) { if (it.state == Call.STATE_RINGING) it.reject(false, null) else it.disconnect() }
    @Suppress("DEPRECATION")
    fun holdCall(id: String) = operate(id) {
        if (it.details.can(Call.Details.CAPABILITY_HOLD)) {
            if (it.state == Call.STATE_HOLDING) it.unhold() else if (it.state == Call.STATE_ACTIVE) it.hold()
        }
    }
    fun mergeCall(id: String) = operate(id) {
        if (it.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) it.mergeConference()
        else it.conferenceableCalls.firstOrNull()?.let { other -> it.conference(other) }
    }
    fun selectAccount(id: String, index: Int) = operate(id) { call -> accounts[id]?.getOrNull(index)?.let { call.phoneAccountSelected(it, false) } }
    fun tone(id: String, digit: Char?) = operate(id) {
        if (digit == null) it.stopDtmfTone()
        else if (digit in "0123456789*#" && it.state == Call.STATE_ACTIVE) it.playDtmfTone(digit)
    }
    fun changeMute() { setMuted(!CallSession.state.value.muted) }
    @Suppress("DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        if (Build.VERSION.SDK_INT >= 34) return
        val routes = listOf(CallAudioState.ROUTE_EARPIECE to "Słuchawka", CallAudioState.ROUTE_SPEAKER to "Głośnik",
            CallAudioState.ROUTE_BLUETOOTH to "Bluetooth", CallAudioState.ROUTE_WIRED_HEADSET to "Słuchawki")
        CallSession.update { it.copy(muted = audioState.isMuted, selectedAudio = audioState.route.toString(),
            audio = routes.filter { (route, _) -> audioState.supportedRouteMask and route != 0 }.map { (route, label) -> AudioChoice(route.toString(), label, route == CallAudioState.ROUTE_EARPIECE) }) }
    }
    @RequiresApi(34)
    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        endpoints.clear()
        availableEndpoints.forEach { endpoints[it.identifier.toString()] = it }
        CallSession.update { it.copy(audio = availableEndpoints.map { AudioChoice(it.identifier.toString(), it.endpointName.toString(), it.endpointType == CallEndpoint.TYPE_EARPIECE) }) }
    }
    @RequiresApi(34)
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) { CallSession.update { it.copy(selectedAudio = callEndpoint.identifier.toString()) } }
    @RequiresApi(34)
    override fun onMuteStateChanged(isMuted: Boolean) { CallSession.update { it.copy(muted = isMuted) } }
    @Suppress("DEPRECATION")
    fun changeAudio(id: String) {
        if (Build.VERSION.SDK_INT >= 34) {
            val endpoint = endpoints[id] ?: return
            requestCallEndpointChange(endpoint, mainExecutor, object : OutcomeReceiver<Void, CallEndpointException> {
                override fun onResult(result: Void?) = Unit
                override fun onError(error: CallEndpointException) { CallSession.update { it.copy(error = "Nie udało się zmienić wyjścia dźwięku.") } }
            })
        } else if (CallSession.state.value.audio.any { it.id == id }) id.toIntOrNull()?.let(::setAudioRoute)
    }
    override fun onDestroy() {
        callbacks.forEach { (call, callback) -> call.unregisterCallback(callback) }
        callbacks.clear(); tracked.clear(); accounts.clear(); endpoints.clear()
        if (CallSession.service === this) { CallSession.service = null; CallSession.clear() }
        getSystemService(NotificationManager::class.java).cancel(800)
        super.onDestroy()
    }
}

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("call_id") ?: return
        when (intent.action) { "answer" -> CallSession.answer(id); "end" -> CallSession.end(id) }
    }
}

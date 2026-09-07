package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallerRepository
import com.example.data.DefaultCallerRepository
import com.example.data.SmsSimulationScenario
import com.example.model.CallDirection
import com.example.model.CallInteraction
import com.example.model.CallRecord
import com.example.model.CallerInfo
import com.example.model.CommunityReputation
import com.example.model.ContactSortMode
import com.example.model.InteractionSource
import com.example.model.LinkSafetyModalState
import com.example.model.PostCallEvaluation
import com.example.model.ProtectionSettings
import com.example.model.QuickRating
import com.example.model.RiskSeverity
import com.example.model.SmsMessage
import com.example.model.SmsThread
import com.example.model.SubCategory
import com.example.model.UnifiedContact
import com.example.model.UnifiedSenderProfile
import com.example.model.UrlSecurityAnalysis
import com.example.model.UserProfile
import com.example.model.UserRatingSubmission
import com.example.service.CallScreeningBridge
import com.example.service.SmsBridge
import com.example.incall.dtmf.DtmfController
import com.example.incall.model.AudioRoute
import com.example.incall.model.CallScreenState
import com.example.incall.model.InCallContextInfo
import com.example.incall.model.InCallUiState
import com.example.incall.model.NetworkStatusInfo
import com.example.incall.privacy.InCallSmsPrivacyFilter
import com.example.incall.timer.CallTimerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CallerUiState(
  val currentTabIndex: Int = 0, // 0 = Start (Główny), 1 = Telefon, 2 = Wiadomości, 3 = Ochrona, 4 = Więcej
  val recentCalls: List<CallRecord> = emptyList(),
  val smsThreads: List<SmsThread> = emptyList(),
  val selectedThreadId: String? = null,
  val activeThreadMessages: List<SmsMessage> = emptyList(),
  val selectedSenderProfile: UnifiedSenderProfile? = null,
  val activeLinkModal: LinkSafetyModalState? = null,
  val searchQuery: String = "+48 732 123 456",
  val searchResult: CallerInfo? = null,
  val protectionSettings: ProtectionSettings = ProtectionSettings(),
  val userProfile: UserProfile = UserProfile(),
  val selectedCallerForDetails: CallerInfo? = null,
  val activeReportingCall: CallRecord? = null,
  val incomingCallPreview: CallerInfo? = null,
  val isIncomingCallSimulated: Boolean = true, // Jawne oznaczenie DEMO/Symulacja
  val isShareDialogOpen: Boolean = false,
  val feedbackToastMessage: String? = null,
  val isOnboardingVisible: Boolean = false,
  val isRemoteMockMode: Boolean = true,
  val postCallEvaluation: PostCallEvaluation? = null,
  val communityReputations: Map<String, CommunityReputation> = emptyMap(),
  val userRatings: Map<String, UserRatingSubmission> = emptyMap(),
  // Contacts 2.0 State
  val unifiedContacts: List<UnifiedContact> = emptyList(),
  val contactSortMode: ContactSortMode = ContactSortMode.MANUAL,
  val selectedUnifiedContact: UnifiedContact? = null,
  val isAddContactDialogOpen: Boolean = false,
  val isContactsScreenOpen: Boolean = false,
  val isEngineDebugVisible: Boolean = false,
  // Active In-Call State (Sekcje 76–96)
  val activeInCallState: InCallUiState? = null
)

class CallerViewModel(
  private val repository: CallerRepository = DefaultCallerRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    CallerUiState(isRemoteMockMode = repository.isRemoteMockMode)
  )
  val uiState: StateFlow<CallerUiState> = _uiState.asStateFlow()

  init {
    // Obserwacja połączeń
    viewModelScope.launch {
      repository.recentCalls.collect { calls ->
        _uiState.update { it.copy(recentCalls = calls) }
      }
    }

    // Obserwacja wiadomości SMS
    viewModelScope.launch {
      repository.smsThreads.collect { threads ->
        _uiState.update { state ->
          val updatedActiveMessages = state.selectedThreadId?.let {
            repository.getMessagesForThread(it)
          } ?: state.activeThreadMessages
          state.copy(
            smsThreads = threads,
            activeThreadMessages = updatedActiveMessages
          )
        }
      }
    }

    // Obserwacja ustawień ochrony
    viewModelScope.launch {
      repository.protectionSettings.collect { settings ->
        _uiState.update { it.copy(protectionSettings = settings) }
      }
    }

    // Obserwacja profilu użytkownika
    viewModelScope.launch {
      repository.userProfile.collect { profile ->
        _uiState.update { it.copy(userProfile = profile) }
      }
    }

    // Obserwacja dynamicznej reputacji społecznościowej (SEKCJE 19–31)
    viewModelScope.launch {
      repository.communityReputations.collect { reps ->
        _uiState.update { it.copy(communityReputations = reps) }
      }
    }

    // Obserwacja ocen bieżącego użytkownika (One active rating per user per number)
    viewModelScope.launch {
      repository.userRatings.collect { ratings ->
        _uiState.update { it.copy(userRatings = ratings) }
      }
    }

    // Obserwacja Kontaktów 2.0 (People + Organizations) - SEKCJE 33-38
    viewModelScope.launch {
      repository.unifiedContacts.collect { contacts ->
        _uiState.update { state ->
          val updatedSelected = state.selectedUnifiedContact?.let { sel ->
            contacts.firstOrNull { it.id == sel.id }
          }
          state.copy(
            unifiedContacts = contacts,
            selectedUnifiedContact = updatedSelected
          )
        }
      }
    }

    viewModelScope.launch {
      repository.contactSortMode.collect { mode ->
        _uiState.update { it.copy(contactSortMode = mode) }
      }
    }

    // Nasłuchiwanie na zdarzenia z CallScreeningService
    viewModelScope.launch {
      CallScreeningBridge.incomingEvents.collect { event ->
        _uiState.update {
          it.copy(
            incomingCallPreview = event.callerInfo,
            isIncomingCallSimulated = !event.isRealScreeningServiceCall
          )
        }
      }
    }

    // Nasłuchiwanie na zdarzenia z mostka SMS
    SmsBridge.addListener { event ->
      repository.addIncomingSms(event.sender, event.body)
      _uiState.update {
        it.copy(feedbackToastMessage = "Nowa wiadomość od ${event.sender}")
      }
    }

    // Domyślny stan wyszukiwania dla prezentacji
    performSearch("+48 732 123 456")
  }

  fun selectTab(index: Int) {
    _uiState.update {
      it.copy(
        currentTabIndex = index,
        selectedThreadId = null,
        selectedCallerForDetails = null,
        selectedSenderProfile = null
      )
    }
  }

  // =================== OBSŁUGA SMS ===================

  fun openThread(threadId: String) {
    val messages = repository.getMessagesForThread(threadId)
    repository.markThreadAsRead(threadId)
    _uiState.update {
      it.copy(
        selectedThreadId = threadId,
        activeThreadMessages = messages
      )
    }
  }

  fun closeThread() {
    _uiState.update { it.copy(selectedThreadId = null, activeThreadMessages = emptyList()) }
  }

  fun sendSms(text: String) {
    val threadId = _uiState.value.selectedThreadId ?: return
    repository.sendSms(threadId, text)
    val updated = repository.getMessagesForThread(threadId)
    _uiState.update { it.copy(activeThreadMessages = updated) }
  }

  fun triggerSmsSimulation(scenario: SmsSimulationScenario) {
    repository.triggerSmsSimulation(scenario)
    _uiState.update {
      it.copy(feedbackToastMessage = "Zasymulowano odebranie testowej wiadomości SMS")
    }
  }

  fun openSmsWithRecipient(recipient: String) {
    val existing = _uiState.value.smsThreads.firstOrNull { it.senderAddress == recipient }
    val threadId = existing?.threadId ?: run {
      repository.addIncomingSms(recipient, "Rozpoczęto nową konwersację")
      _uiState.value.smsThreads.firstOrNull { it.senderAddress == recipient }?.threadId ?: "thread-${recipient.hashCode()}"
    }
    openThread(threadId)
    _uiState.update { it.copy(currentTabIndex = 1) } // Zakładka Wiadomości (indeks 1)
  }

  fun togglePinThread(threadId: String) {
    repository.togglePinThread(threadId)
  }

  fun reorderPinnedThreads(fromIndex: Int, toIndex: Int) {
    repository.reorderPinnedThreads(fromIndex, toIndex)
  }

  fun toggleMuteThread(threadId: String) {
    repository.toggleMuteThread(threadId)
  }

  fun toggleArchiveThread(threadId: String) {
    repository.toggleArchiveThread(threadId)
  }

  fun deleteThreadLocally(threadId: String) {
    repository.deleteThreadLocally(threadId)
    if (_uiState.value.selectedThreadId == threadId) {
      closeThread()
    }
  }

  fun markThreadAsSpam(threadId: String) {
    repository.markThreadAsSpam(threadId)
  }

  fun openLinkSafetyModal(analysis: UrlSecurityAnalysis, isOfficial: Boolean, brandName: String? = null) {
    _uiState.update {
      it.copy(
        activeLinkModal = LinkSafetyModalState(
          analysis = analysis,
          isOfficialBrandDomain = isOfficial,
          verifiedBrandName = brandName
        )
      )
    }
  }

  fun dismissLinkSafetyModal() {
    _uiState.update { it.copy(activeLinkModal = null) }
  }

  // =================== WSPÓLNY PROFIL NADAWCY ===================

  fun openSenderProfile(senderAddress: String) {
    val profile = repository.getUnifiedSenderProfile(senderAddress)
    _uiState.update { it.copy(selectedSenderProfile = profile) }
  }

  fun closeSenderProfile() {
    _uiState.update { it.copy(selectedSenderProfile = null) }
  }

  // =================== TELEFON & REPUTACJA ===================

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    if (query.length >= 3) {
      performSearch(query)
    }
  }

  fun performSearch(query: String = _uiState.value.searchQuery) {
    val result = repository.lookupNumber(query)
    _uiState.update { it.copy(searchResult = result) }
  }

  fun getCallerForNumber(phoneNumber: String): CallerInfo {
    return repository.lookupNumber(phoneNumber)
  }

  fun openNumberDetails(caller: CallerInfo) {
    _uiState.update { it.copy(selectedCallerForDetails = caller) }
  }

  fun closeNumberDetails() {
    _uiState.update { it.copy(selectedCallerForDetails = null) }
  }

  fun startReporting(call: CallRecord) {
    _uiState.update { it.copy(activeReportingCall = call) }
  }

  fun dismissReporting() {
    _uiState.update { it.copy(activeReportingCall = null) }
  }

  fun submitReport(rating: Int, category: String?) {
    val call = _uiState.value.activeReportingCall
    if (call != null) {
      val success = repository.submitReport(call.id, rating, category)
      _uiState.update {
        it.copy(
          activeReportingCall = null,
          feedbackToastMessage = if (success) {
            "Dziękujemy. Zgłoszenie zostało zapisane w lokalnym rejestrze."
          } else {
            "Osiągnięto limit zgłoszeń na godzinę (ochrona anty-abuse)."
          }
        )
      }
    }
  }

  fun toggleBlock(phoneNumber: String) {
    repository.toggleBlock(phoneNumber)
    val currentSearch = _uiState.value.searchResult
    if (currentSearch != null && currentSearch.phoneNumber == phoneNumber) {
      _uiState.update {
        it.copy(
          searchResult = currentSearch.copy(isBlocked = !currentSearch.isBlocked),
          feedbackToastMessage = if (!currentSearch.isBlocked) "Numer zablokowany lokalnie" else "Numer odblokowany"
        )
      }
    }
    val currentDetails = _uiState.value.selectedCallerForDetails
    if (currentDetails != null && currentDetails.phoneNumber == phoneNumber) {
      _uiState.update {
        it.copy(
          selectedCallerForDetails = currentDetails.copy(isBlocked = !currentDetails.isBlocked)
        )
      }
    }
    val currentSender = _uiState.value.selectedSenderProfile
    if (currentSender != null && currentSender.address == phoneNumber) {
      _uiState.update {
        it.copy(selectedSenderProfile = currentSender.copy(isBlocked = !currentSender.isBlocked))
      }
    }
  }

  fun updateProtectionSettings(transform: (ProtectionSettings) -> ProtectionSettings) {
    val updated = transform(_uiState.value.protectionSettings)
    repository.updateProtectionSettings(updated)
  }

  // Symulacje demonstracyjne dla MVP
  fun triggerIncomingCallSimulation(type: SimulationCallType) {
    val caller = when (type) {
      SimulationCallType.BANK_ZERO_TRUST -> repository.getSampleInboundBankNumber()
      SimulationCallType.SCAM_VISHING -> repository.getSampleScamNumber()
      SimulationCallType.COURIER_NEUTRAL -> repository.getSampleCourierNumber()
    }
    _uiState.update {
      it.copy(
        incomingCallPreview = caller,
        isIncomingCallSimulated = true
      )
    }
  }

  fun dismissIncomingCall(triggerRating: Boolean = true) {
    val caller = _uiState.value.incomingCallPreview
    if (caller != null && triggerRating) {
      val interaction = CallInteraction(
        id = "interaction-${System.currentTimeMillis()}",
        number = caller.phoneNumber,
        timestampEpoch = System.currentTimeMillis(),
        direction = CallDirection.INCOMING,
        answered = true,
        durationSeconds = 18,
        source = if (_uiState.value.isIncomingCallSimulated) InteractionSource.DEMO_SIMULATION else InteractionSource.CALL_SCREENING
      )
      repository.recordCallInteraction(interaction)
      _uiState.update {
        it.copy(
          incomingCallPreview = null,
          postCallEvaluation = PostCallEvaluation(
            callerInfo = caller,
            interaction = interaction
          )
        )
      }
    } else {
      _uiState.update { it.copy(incomingCallPreview = null) }
    }
  }

  // =================== ACTIVE IN-CALL STATE & ACTIONS (SEKCJE 76–96) ===================

  private var inCallTimerJob: Job? = null
  private var activeCallTimerManager: CallTimerManager? = null
  private var activeDtmfController: DtmfController? = null

  fun answerIncomingCall() {
    val caller = _uiState.value.incomingCallPreview ?: return
    _uiState.update { it.copy(incomingCallPreview = null) }

    // Pobierz dane kontekstowe i historię SMS
    val matchingThread = _uiState.value.smsThreads.firstOrNull {
      it.senderAddress == caller.phoneNumber || it.displayName.equals(caller.displayName, ignoreCase = true)
    }
    val rawLastSms = matchingThread?.lastMessageSnippet
    val smsTimestamp = matchingThread?.lastMessageTimestamp

    // Filtr prywatności podglądu SMS (Sekcja 85)
    val filteredSms = InCallSmsPrivacyFilter.filterSnippet(
      rawBody = rawLastSms,
      timestamp = smsTimestamp,
      isEnabled = _uiState.value.protectionSettings.showSmsPreviewInCall
    )

    val matchingContact = repository.getContactForAddress(caller.phoneNumber)

    val contextInfo = InCallContextInfo(
      lastSmsSnippet = filteredSms?.displayText,
      lastSmsTimestamp = filteredSms?.timestamp,
      isSmsSensitive = filteredSms?.isSensitive == true,
      lastCallTimestamp = matchingContact?.lastCallTimestamp,
      organizationName = matchingContact?.primaryDisplayName ?: caller.displayName,
      officialDomains = matchingContact?.officialDomains ?: emptyList(),
      reputationNote = matchingContact?.reputationNote,
      riskScore = caller.riskScore,
      confidence = caller.confidence
    )

    // Inicjalizacja timera i kontrolera DTMF
    activeCallTimerManager = CallTimerManager(viewModelScope)
    activeDtmfController = DtmfController()

    val initialCallState = InCallUiState(
      callState = CallScreenState.ACTIVE,
      caller = caller,
      activeDurationSeconds = 0L,
      formattedDuration = "00:00",
      audioRoute = AudioRoute.EARPIECE,
      isMuted = false,
      isKeypadOpen = false,
      dtmfSequence = "",
      isExpandedContext = false,
      isProximityNear = false,
      isTouchLocked = false,
      networkStatus = NetworkStatusInfo(networkType = "5G", signalBars = 4),
      contextInfo = contextInfo,
      dynamicRiskWarning = if (caller.severity == RiskSeverity.HIGH) "Wykryto sygnały podwyższonego ryzyka" else null
    )

    _uiState.update { it.copy(activeInCallState = initialCallState) }
    activeCallTimerManager?.start()

    inCallTimerJob?.cancel()
    inCallTimerJob = viewModelScope.launch {
      activeCallTimerManager?.durationSeconds?.collect { seconds ->
        _uiState.update { state ->
          val current = state.activeInCallState ?: return@update state
          state.copy(
            activeInCallState = current.copy(
              activeDurationSeconds = seconds,
              formattedDuration = CallTimerManager.formatDuration(seconds)
            )
          )
        }
      }
    }
  }

  fun hangupActiveCall() {
    val active = _uiState.value.activeInCallState ?: return
    val finalDuration = activeCallTimerManager?.stop() ?: active.activeDurationSeconds
    inCallTimerJob?.cancel()
    activeDtmfController?.release()
    activeDtmfController = null
    activeCallTimerManager = null

    // Zapisz interakcję
    val interaction = CallInteraction(
      id = "interaction-${System.currentTimeMillis()}",
      number = active.caller.phoneNumber,
      timestampEpoch = System.currentTimeMillis(),
      direction = CallDirection.INCOMING,
      answered = true,
      durationSeconds = finalDuration.toInt(),
      source = if (_uiState.value.isIncomingCallSimulated) InteractionSource.DEMO_SIMULATION else InteractionSource.CALL_SCREENING
    )
    repository.recordCallInteraction(interaction)

    _uiState.update {
      it.copy(
        activeInCallState = null,
        postCallEvaluation = PostCallEvaluation(
          callerInfo = active.caller,
          interaction = interaction
        )
      )
    }
  }

  fun toggleInCallMute() {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      state.copy(activeInCallState = current.copy(isMuted = !current.isMuted))
    }
  }

  fun toggleInCallSpeaker() {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      val newRoute = if (current.audioRoute == AudioRoute.SPEAKER) AudioRoute.EARPIECE else AudioRoute.SPEAKER
      // Gdy aktywny jest głośnik, odblokowujemy touch lock (Sekcja 93)
      val newTouchLock = if (newRoute == AudioRoute.SPEAKER) false else current.isProximityNear
      state.copy(
        activeInCallState = current.copy(
          audioRoute = newRoute,
          isTouchLocked = newTouchLock
        )
      )
    }
  }

  fun toggleInCallHold() {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      val isNowHeld = current.callState == CallScreenState.ACTIVE
      if (isNowHeld) {
        activeCallTimerManager?.pause()
      } else {
        activeCallTimerManager?.resume()
      }
      state.copy(
        activeInCallState = current.copy(
          callState = if (isNowHeld) CallScreenState.HELD else CallScreenState.ACTIVE
        )
      )
    }
  }

  fun toggleInCallKeypad(open: Boolean) {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      state.copy(activeInCallState = current.copy(isKeypadOpen = open))
    }
  }

  fun sendInCallDtmfDigit(digit: Char) {
    val updatedSeq = activeDtmfController?.sendDigit(digit) ?: ""
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      state.copy(activeInCallState = current.copy(dtmfSequence = updatedSeq))
    }
  }

  fun clearInCallDtmf() {
    activeDtmfController?.clearSequence()
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      state.copy(activeInCallState = current.copy(dtmfSequence = ""))
    }
  }

  fun toggleInCallContextExpansion(expanded: Boolean) {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      state.copy(activeInCallState = current.copy(isExpandedContext = expanded))
    }
  }

  fun onProximitySensorEvent(isNear: Boolean) {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      // Jeśli głośnik lub bluetooth jest aktywny: nie blokujemy agresywnie (Sekcje 93 i 94)
      val bypass = current.audioRoute == AudioRoute.SPEAKER || current.audioRoute == AudioRoute.BLUETOOTH
      val shouldLock = isNear && !bypass
      state.copy(
        activeInCallState = current.copy(
          isProximityNear = isNear,
          isTouchLocked = shouldLock
        )
      )
    }
  }

  fun setInCallDynamicRiskWarning(warning: String?) {
    _uiState.update { state ->
      val current = state.activeInCallState ?: return@update state
      state.copy(activeInCallState = current.copy(dynamicRiskWarning = warning))
    }
  }

  // Crowdsourcing - Ocena po połączeniu (SEKCJE 19–31)
  fun submitQuickRating(phoneNumber: String, rating: QuickRating, subCategory: SubCategory?) {
    val hasInteraction = _uiState.value.recentCalls.any { it.caller.phoneNumber == phoneNumber } ||
        _uiState.value.postCallEvaluation?.callerInfo?.phoneNumber == phoneNumber
    val success = repository.submitQuickRating(phoneNumber, rating, subCategory, hasInteraction)
    
    _uiState.update {
      it.copy(
        postCallEvaluation = null,
        feedbackToastMessage = if (success) "Ocena zapisana. Dziękujemy za budowanie bazy reputacji!" else "Błąd zapisu oceny"
      )
    }

    // Odśwież wynik wyszukiwania jeśli aktualnie wyświetlany jest ten numer
    val currentSearch = _uiState.value.searchResult
    if (currentSearch != null && currentSearch.phoneNumber == phoneNumber) {
      val updatedCaller = repository.lookupNumber(phoneNumber)
      _uiState.update { it.copy(searchResult = updatedCaller) }
    }
  }

  fun dismissPostCallEvaluation() {
    _uiState.update { it.copy(postCallEvaluation = null) }
  }

  fun showPostCallEvaluationForNumber(caller: CallerInfo) {
    val interaction = CallInteraction(
      id = "manual-${System.currentTimeMillis()}",
      number = caller.phoneNumber,
      timestampEpoch = System.currentTimeMillis(),
      direction = CallDirection.INCOMING,
      answered = true,
      durationSeconds = 25,
      source = InteractionSource.INCOMING_CALL_RECEIVER
    )
    _uiState.update {
      it.copy(
        postCallEvaluation = PostCallEvaluation(
          callerInfo = caller,
          interaction = interaction
        )
      )
    }
  }

  fun clearFeedbackMessage() {
    _uiState.update { it.copy(feedbackToastMessage = null) }
  }

  fun toggleShareDialog(open: Boolean) {
    _uiState.update { it.copy(isShareDialogOpen = open) }
  }

  fun showOnboarding() {
    _uiState.update { it.copy(isOnboardingVisible = true) }
  }

  fun dismissOnboarding() {
    _uiState.update { it.copy(isOnboardingVisible = false) }
  }

  // =================== CONTACTS 2.0 METODY ===================

  fun openContactsScreen() {
    _uiState.update { it.copy(isContactsScreenOpen = true) }
  }

  fun closeContactsScreen() {
    _uiState.update { it.copy(isContactsScreenOpen = false) }
  }

  fun openContactDetail(contact: UnifiedContact) {
    _uiState.update { it.copy(selectedUnifiedContact = contact) }
  }

  fun openContactForAddress(address: String) {
    val contact = repository.getContactForAddress(address)
    if (contact != null) {
      openContactDetail(contact)
    } else {
      // Fallback: otwórz ogólny profil nadawcy
      openSenderProfile(address)
    }
  }

  fun closeContactDetail() {
    _uiState.update { it.copy(selectedUnifiedContact = null) }
  }

  fun togglePinContact(contactId: String) {
    repository.togglePinContact(contactId)
  }

  fun toggleQuickContact(contactId: String) {
    repository.toggleQuickContact(contactId)
  }

  fun setContactSortMode(mode: ContactSortMode) {
    repository.setContactSortMode(mode)
  }

  fun reorderContactsManual(fromIndex: Int, toIndex: Int) {
    repository.reorderContactsManual(fromIndex, toIndex)
  }

  fun addCustomContact(contact: UnifiedContact) {
    repository.addCustomContact(contact)
    _uiState.update {
      it.copy(
        isAddContactDialogOpen = false,
        feedbackToastMessage = "Dodano kontakt: ${contact.primaryDisplayName}"
      )
    }
  }

  fun setAddContactDialogOpen(open: Boolean) {
    _uiState.update { it.copy(isAddContactDialogOpen = open) }
  }

  fun acceptSmartSuggestion(contactId: String) {
    repository.togglePinContact(contactId)
    repository.dismissSmartSuggestion(contactId)
    _uiState.update {
      it.copy(feedbackToastMessage = "Przypięto kontakt do szybkiego dostępu")
    }
  }

  fun dismissSmartSuggestion(contactId: String) {
    repository.dismissSmartSuggestion(contactId)
  }

  fun openEngineDebug() {
    _uiState.update { it.copy(isEngineDebugVisible = true) }
  }

  fun closeEngineDebug() {
    _uiState.update { it.copy(isEngineDebugVisible = false) }
  }
}

enum class SimulationCallType {
  BANK_ZERO_TRUST,
  SCAM_VISHING,
  COURIER_NEUTRAL
}

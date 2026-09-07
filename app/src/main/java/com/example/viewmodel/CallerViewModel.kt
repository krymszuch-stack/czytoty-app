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
import com.example.model.PostCallEvaluation
import com.example.model.ProtectionSettings
import com.example.model.QuickRating
import com.example.model.SmsMessage
import com.example.model.SmsThread
import com.example.model.SubCategory
import com.example.model.UnifiedContact
import com.example.model.UnifiedSenderProfile
import com.example.model.UserProfile
import com.example.model.UserRatingSubmission
import com.example.service.CallScreeningBridge
import com.example.service.SmsBridge
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
  val isContactsScreenOpen: Boolean = false
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
    _uiState.update { it.copy(currentTabIndex = 2) } // Przełącz na zakładkę Wiadomości
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
}

enum class SimulationCallType {
  BANK_ZERO_TRUST,
  SCAM_VISHING,
  COURIER_NEUTRAL
}

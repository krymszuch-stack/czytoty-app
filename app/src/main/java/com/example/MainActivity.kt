package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.example.auth.viewmodel.AuthViewModel
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.safetycheck.manager.SafetyCheckManager
import com.example.safetycheck.repository.SafetyCheckRepository
import com.example.ui.components.LinkSafetyDialog
import com.example.ui.components.PostCallRatingCard
import com.example.ui.components.ReportBottomSheet
import com.example.ui.components.SafetyCheckTriggerDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.SafetyCheckSettingsScreen
import com.example.ui.screens.NumberDetailsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProtectionScreen
import com.example.ui.screens.RecentCallsScreen
import com.example.ui.screens.SenderProfileScreen
import com.example.ui.screens.SmsConversationDetailScreen
import com.example.ui.screens.SmsConversationsScreen
import com.example.ui.screens.EngineDebugScreen
import com.example.ui.screens.UnifiedContactDetailScreen
import com.example.ui.screens.VerifyNumberScreen
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CallerViewModel
import com.example.viewmodel.SimulationCallType

class MainActivity : ComponentActivity() {
  private val authViewModel: AuthViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Obsługa powrotu z przeglądarki (PKCE deeplink) przy zimnym starcie
    intent?.let { authViewModel.handleDeeplink(it) }

    setContent {
      MyApplicationTheme {
        MainAppContent(authViewModel = authViewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // Obsługa powrotu z Custom Tabs (czytoty://auth) w trybie singleTop
    authViewModel.handleDeeplink(intent)
  }
}

sealed class NavigationTab(
  val title: String,
  val selectedIcon: ImageVector? = null,
  val unselectedIcon: ImageVector? = null,
  val testTag: String,
  val drawableRes: Int? = null
) {
  object Ostatnie : NavigationTab("Ostatnie", Icons.Default.Home, Icons.Outlined.Home, "tab_recent", R.drawable.ic_phone_lookup)
  object Wiadomosci : NavigationTab("Wiadomości", Icons.Default.ChatBubble, Icons.Outlined.ChatBubbleOutline, "tab_messages", R.drawable.ic_messages_sms)
  object Sprawdz : NavigationTab("Sprawdź", Icons.Default.Search, Icons.Outlined.Search, "tab_check")
  object Ochrona : NavigationTab("Ochrona", Icons.Default.Shield, Icons.Outlined.Shield, "tab_protection", R.drawable.ic_protection_shield)
  object Profil : NavigationTab("Profil", Icons.Default.Person, Icons.Outlined.Person, "tab_profile", R.drawable.ic_everyone_people)
}

@Composable
fun MainAppContent(
  viewModel: CallerViewModel = viewModel(),
  authViewModel: AuthViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  val safetyCheckRepository = remember { SafetyCheckRepository() }
  val safetyCheckManager = remember { SafetyCheckManager(safetyCheckRepository) }
  val safetyCheckState by safetyCheckManager.state.collectAsState()
  var isSafetyCheckSettingsOpen by remember { mutableStateOf(false) }

  val tabs = listOf(
    NavigationTab.Ostatnie,
    NavigationTab.Wiadomosci,
    NavigationTab.Sprawdz,
    NavigationTab.Ochrona,
    NavigationTab.Profil
  )

  LaunchedEffect(uiState.feedbackToastMessage) {
    uiState.feedbackToastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedbackMessage()
    }
  }

  LaunchedEffect(authUiState.errorMessage) {
    authUiState.errorMessage?.let { error ->
      snackbarHostState.showSnackbar(error)
      authViewModel.clearError()
    }
  }

  // 1. Ekran połączenia przychodzącego (Call Screen / Overlay)
  if (uiState.incomingCallPreview != null) {
    IncomingCallScreen(
      caller = uiState.incomingCallPreview!!,
      onAnswer = { viewModel.answerIncomingCall() },
      onReject = { viewModel.dismissIncomingCall(triggerRating = false) },
      isSimulatedPreview = true
    )
    return
  }

  // 1b. Ekran aktywnego połączenia (Interactive In-Call Screen - Sekcje 76–96)
  if (uiState.activeInCallState != null) {
    val inCallScope = rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(Unit) {
      val proximityManager = com.example.incall.sensor.AndroidProximitySensorManager(context, inCallScope)
      proximityManager.setAudioRoute(uiState.activeInCallState!!.audioRoute)
      proximityManager.startMonitoring()
      val job = inCallScope.launch {
        proximityManager.proximityState.collect { proxState ->
          viewModel.onProximitySensorEvent(proxState == com.example.incall.model.ProximityState.NEAR)
        }
      }
      onDispose {
        job.cancel()
        proximityManager.stopMonitoring()
      }
    }

    com.example.ui.screens.incall.ActiveCallScreen(
      uiState = uiState.activeInCallState!!,
      onToggleMute = { viewModel.toggleInCallMute() },
      onToggleSpeaker = { viewModel.toggleInCallSpeaker() },
      onToggleHold = { viewModel.toggleInCallHold() },
      onToggleKeypad = { viewModel.toggleInCallKeypad(it) },
      onDtmfDigit = { viewModel.sendInCallDtmfDigit(it) },
      onClearDtmf = { viewModel.clearInCallDtmf() },
      onToggleContextExpansion = { viewModel.toggleInCallContextExpansion(it) },
      onHangup = { viewModel.hangupActiveCall() },
      onOpenMessages = {
        val number = uiState.activeInCallState!!.caller.phoneNumber
        viewModel.openSenderProfile(number)
      },
      onStartSafetyCheck = {
        viewModel.setInCallDynamicRiskWarning("Aktywna ochrona: analiza bezpieczeństwa w toku")
      }
    )
    return
  }

  // 1b. Ekran logowania Supabase Auth (Google OAuth + PKCE / Guest Mode)
  if (authUiState.isAuthScreenVisible) {
    BackHandler { authViewModel.closeAuthScreen() }
    AuthScreen(
      isLoading = authUiState.isLoading,
      errorMessage = authUiState.errorMessage,
      onSignInWithGoogle = { authViewModel.signInWithGoogle() },
      onContinueAsGuest = { authViewModel.continueAsGuest() },
      onDismiss = { authViewModel.closeAuthScreen() }
    )
    return
  }

  // 2. Ekran Onboarding (Zasady działania)
  if (uiState.isOnboardingVisible) {
    BackHandler { viewModel.dismissOnboarding() }
    OnboardingScreen(onComplete = { viewModel.dismissOnboarding() })
    return
  }

  // 2b. Ekran Diagnostyki Silnika v1.0.0 (Inspector dla developera / audytora)
  if (uiState.isEngineDebugVisible) {
    BackHandler { viewModel.closeEngineDebug() }
    EngineDebugScreen(onBack = { viewModel.closeEngineDebug() })
    return
  }

  // 3. Ekran konwersacji SMS (wątek)
  if (uiState.selectedThreadId != null) {
    val activeThread = uiState.smsThreads.firstOrNull { it.threadId == uiState.selectedThreadId }
    if (activeThread != null) {
      BackHandler { viewModel.closeThread() }
      SmsConversationDetailScreen(
        thread = activeThread,
        messages = uiState.activeThreadMessages,
        onBack = { viewModel.closeThread() },
        onSenderProfileClick = { sender -> viewModel.openSenderProfile(sender) },
        onSendMessage = { text -> viewModel.sendSms(text) },
        onOpenLinkModal = { analysis, isOfficial, brandName ->
          viewModel.openLinkSafetyModal(analysis, isOfficial, brandName)
        }
      )
      return
    }
  }

  // 4. Wspólny profil nadawcy (Karta Nadawcy: Połączenia + SMS)
  if (uiState.selectedSenderProfile != null) {
    BackHandler { viewModel.closeSenderProfile() }
    SenderProfileScreen(
      profile = uiState.selectedSenderProfile!!,
      onBack = { viewModel.closeSenderProfile() },
      onToggleBlock = { viewModel.toggleBlock(it) },
      onReport = { address ->
        val dummyCall = CallRecord(
          id = "report-$address",
          caller = com.example.model.CallerInfo(
            phoneNumber = address,
            formattedNumber = address,
            displayName = uiState.selectedSenderProfile?.displayName,
            category = "Zgłoszenie nadawcy",
            riskScore = uiState.selectedSenderProfile!!.combinedRiskScore,
            confidence = uiState.selectedSenderProfile!!.confidence,
            totalReports = uiState.selectedSenderProfile!!.callsCount + uiState.selectedSenderProfile!!.smsCount,
            reportsLast24h = 1
          ),
          timestamp = "Teraz",
          callType = CallType.INCOMING
        )
        viewModel.startReporting(dummyCall)
      },
      onOpenSmsThread = { address ->
        viewModel.closeSenderProfile()
        viewModel.openSmsWithRecipient(address)
      }
    )
    return
  }

  // 5. Szczegóły numeru z połączenia
  if (uiState.selectedCallerForDetails != null) {
    BackHandler { viewModel.closeNumberDetails() }
    NumberDetailsScreen(
      caller = uiState.selectedCallerForDetails!!,
      onBack = { viewModel.closeNumberDetails() },
      onToggleBlock = { viewModel.toggleBlock(it) },
      onReportClick = { caller ->
        viewModel.startReporting(
          CallRecord(
            id = "custom-report",
            caller = caller,
            timestamp = "Teraz",
            callType = CallType.INCOMING
          )
        )
      },
      onShareClick = { caller ->
        val shareText = "Uwaga na numer: ${caller.formattedNumber} (${caller.category}, ${caller.riskScore}% ryzyka). Sprawdzone w Bezpiecznym Telefonie."
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_TEXT, shareText)
          type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Udostępnij ostrzeżenie")
        context.startActivity(shareIntent)
      }
    )

    if (uiState.activeReportingCall != null) {
      ReportBottomSheet(
        callRecord = uiState.activeReportingCall,
        onDismiss = { viewModel.dismissReporting() },
        onSubmit = { rating, category -> viewModel.submitReport(rating, category) }
      )
    }
    return
  }

  // 6. Szczegóły zunifikowanego kontaktu (Sekcja 34 - Unified Contact Profile)
  if (uiState.selectedUnifiedContact != null) {
    BackHandler { viewModel.closeContactDetail() }
    UnifiedContactDetailScreen(
      contact = uiState.selectedUnifiedContact!!,
      onBack = { viewModel.closeContactDetail() },
      onTogglePin = { id -> viewModel.togglePinContact(id) },
      onToggleQuickContact = { id -> viewModel.toggleQuickContact(id) },
      onOpenSmsThread = { address ->
        viewModel.closeContactDetail()
        viewModel.openSmsWithRecipient(address)
      }
    )
    return
  }

  // 7. Ekran pełnej książki kontaktów 2.0 (Sekcje 33–38)
  if (uiState.isContactsScreenOpen) {
    BackHandler { viewModel.closeContactsScreen() }
    ContactsScreen(
      contacts = uiState.unifiedContacts,
      currentSortMode = uiState.contactSortMode,
      onSelectContact = { contact -> viewModel.openContactDetail(contact) },
      onTogglePin = { id -> viewModel.togglePinContact(id) },
      onToggleQuickContact = { id -> viewModel.toggleQuickContact(id) },
      onSetSortMode = { mode -> viewModel.setContactSortMode(mode) },
      onReorderManual = { from, to -> viewModel.reorderContactsManual(from, to) },
      onAddContact = { contact -> viewModel.addCustomContact(contact) },
      onAcceptSmartSuggestion = { id -> viewModel.acceptSmartSuggestion(id) },
      onDismissSmartSuggestion = { id -> viewModel.dismissSmartSuggestion(id) },
      onOpenSmsThread = { address ->
        viewModel.closeContactsScreen()
        viewModel.openSmsWithRecipient(address)
      },
      onBack = { viewModel.closeContactsScreen() }
    )
    return
  }

  // 8. Ekran konfiguracji Trybu Pomocy (Safety Check - 100% OPT-IN)
  if (isSafetyCheckSettingsOpen) {
    BackHandler { isSafetyCheckSettingsOpen = false }
    SafetyCheckSettingsScreen(
      repository = safetyCheckRepository,
      onBack = { isSafetyCheckSettingsOpen = false }
    )
    return
  }

  // Główny szkielet z dolną nawigacją (Material 3 Dark luxury)
  Scaffold(
    containerColor = AppDarkBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      val borderLineColor = Color(0xFF232034)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF100F17))
          .drawBehind {
            drawLine(
              color = borderLineColor,
              start = Offset(0f, 0f),
              end = Offset(size.width, 0f),
              strokeWidth = 1.dp.toPx()
            )
          }
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(vertical = 8.dp)
          .testTag("bottom_navigation_bar"),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        tabs.forEachIndexed { index, tab ->
          val isSelected = uiState.currentTabIndex == index
          Column(
            modifier = Modifier
              .clip(CircleShape)
              .clickable { viewModel.selectTab(index) }
              .padding(horizontal = 12.dp, vertical = 4.dp)
              .testTag(tab.testTag),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            if (tab.drawableRes != null) {
              Icon(
                painter = androidx.compose.ui.res.painterResource(id = tab.drawableRes),
                contentDescription = tab.title,
                tint = if (isSelected) AppPurple else Color(0xFF827F97),
                modifier = Modifier.size(22.dp)
              )
            } else {
              Icon(
                imageVector = if (isSelected) tab.selectedIcon!! else tab.unselectedIcon!!,
                contentDescription = tab.title,
                tint = if (isSelected) AppPurple else Color(0xFF827F97),
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
              text = tab.title,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
              color = if (isSelected) AppPurple else Color(0xFF827F97)
            )

            if (isSelected) {
              Spacer(modifier = Modifier.height(2.dp))
              Box(
                modifier = Modifier
                  .size(4.dp)
                  .clip(CircleShape)
                  .background(AppPurple)
              )
            } else {
              Spacer(modifier = Modifier.height(6.dp))
            }
          }
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = uiState.currentTabIndex,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "TabContentAnimation"
      ) { tabIndex ->
        when (tabIndex) {
          // 0: Ostatnie (Centrum komunikacji)
          0 -> HomeScreen(
            recentCalls = uiState.recentCalls,
            smsThreads = uiState.smsThreads,
            unifiedContacts = uiState.unifiedContacts,
            onNavigateToCalls = {
              viewModel.openSenderProfile("+48 732 123 456")
            },
            onNavigateToMessages = { viewModel.selectTab(1) },
            onNavigateToCheck = { viewModel.selectTab(2) },
            onNavigateToProtection = { viewModel.selectTab(3) },
            onNavigateToContacts = { viewModel.openContactsScreen() },
            onSelectContact = { contact -> viewModel.openContactDetail(contact) },
            onLookupNumber = { query ->
              viewModel.updateSearchQuery(query)
              viewModel.selectTab(2)
            },
            onOpenSmsThread = { threadId -> viewModel.openThread(threadId) }
          )

          // 1: Wiadomości (SMS)
          1 -> SmsConversationsScreen(
            threads = uiState.smsThreads,
            onThreadClick = { threadId -> viewModel.openThread(threadId) },
            onSenderProfileClick = { sender -> viewModel.openSenderProfile(sender) },
            onNewMessage = {
              viewModel.openSmsWithRecipient("+48 600 000 000")
            },
            onSimulateSms = { scenario -> viewModel.triggerSmsSimulation(scenario) },
            onTogglePin = { threadId -> viewModel.togglePinThread(threadId) },
            onReorderPinned = { from, to -> viewModel.reorderPinnedThreads(from, to) },
            onToggleMute = { threadId -> viewModel.toggleMuteThread(threadId) },
            onToggleArchive = { threadId -> viewModel.toggleArchiveThread(threadId) },
            onDeleteThread = { threadId -> viewModel.deleteThreadLocally(threadId) },
            onMarkSpam = { threadId -> viewModel.markThreadAsSpam(threadId) }
          )

          // 2: Sprawdź (Sprawdź numer - SEKCJE 19–31: Crowdsourcing i Dynamiczna Reputacja)
          2 -> {
            val queryNumber = uiState.searchQuery.ifBlank { "+48 732 123 456" }
            val callerInfo = uiState.searchResult ?: viewModel.getCallerForNumber(queryNumber)
            val communityReputation = uiState.communityReputations[callerInfo.phoneNumber]
            val userRating = uiState.userRatings[callerInfo.phoneNumber]

            VerifyNumberScreen(
              initialNumber = callerInfo.phoneNumber,
              callerInfo = callerInfo,
              communityReputation = communityReputation,
              userRating = userRating,
              onRateNumber = { rating, subCat ->
                viewModel.submitQuickRating(callerInfo.phoneNumber, rating, subCat)
              },
              onBack = { viewModel.selectTab(0) },
              onSelectNumber = { num ->
                viewModel.updateSearchQuery(num)
              }
            )
          }

          // 3: Ochrona (Ustawienia ochrony)
          3 -> ProtectionScreen(
            settings = uiState.protectionSettings,
            onUpdateSettings = { transform -> viewModel.updateProtectionSettings(transform) }
          )

          // 4: Profil (Konto i uprawnienia)
          4 -> MoreSettingsScreen(
            userProfile = uiState.userProfile,
            authUser = authUiState.currentUser,
            isGuestMode = authUiState.isGuestMode,
            onSignInClick = { authViewModel.openAuthScreen() },
            onSignOutClick = { authViewModel.signOut() },
            onOpenOnboarding = { viewModel.showOnboarding() },
            onOpenContacts = { viewModel.openContactsScreen() },
            onOpenEngineDebug = { viewModel.openEngineDebug() },
            onOpenSafetyCheck = { isSafetyCheckSettingsOpen = true }
          )
        }
      }
    }
  }

  // Report Bottom Sheet overlay
  if (uiState.activeReportingCall != null) {
    ReportBottomSheet(
      callRecord = uiState.activeReportingCall,
      onDismiss = { viewModel.dismissReporting() },
      onSubmit = { rating, category -> viewModel.submitReport(rating, category) }
    )
  }

  // Karta oceny po połączeniu (SEKCJE 19–31: Crowdsourcing & Dynamiczna Reputacja)
  if (uiState.postCallEvaluation != null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 76.dp),
      contentAlignment = Alignment.BottomCenter
    ) {
      PostCallRatingCard(
        evaluation = uiState.postCallEvaluation!!,
        onDismiss = { viewModel.dismissPostCallEvaluation() },
        onSubmitRating = { rating, subCategory ->
          viewModel.submitQuickRating(
            phoneNumber = uiState.postCallEvaluation!!.callerInfo.phoneNumber,
            rating = rating,
            subCategory = subCategory
          )
        }
      )
    }
  }

  // Overlay Trybu Pomocy (Safety Check Countdown / Alert Confirmation)
  SafetyCheckTriggerDialog(
    state = safetyCheckState,
    onConfirmedOk = { safetyCheckManager.userAcknowledgedOk() },
    onRequestHelpNow = { safetyCheckManager.userRequestedHelpNow() },
    onDismiss = { safetyCheckManager.cancel() }
  )

  // Modal weryfikacji linku (Link Safety - Sekcja 48)
  uiState.activeLinkModal?.let { modalState ->
    LinkSafetyDialog(
      state = modalState,
      onDismiss = { viewModel.dismissLinkSafetyModal() }
    )
  }
}

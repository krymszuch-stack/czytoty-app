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
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.components.PostCallRatingCard
import com.example.ui.components.ReportBottomSheet
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.NumberDetailsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProtectionScreen
import com.example.ui.screens.RecentCallsScreen
import com.example.ui.screens.SenderProfileScreen
import com.example.ui.screens.SmsConversationDetailScreen
import com.example.ui.screens.SmsConversationsScreen
import com.example.ui.screens.UnifiedContactDetailScreen
import com.example.ui.screens.VerifyNumberScreen
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CallerViewModel
import com.example.viewmodel.SimulationCallType

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContent()
      }
    }
  }
}

sealed class NavigationTab(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  object Ostatnie : NavigationTab("Ostatnie", Icons.Default.Home, Icons.Outlined.Home, "tab_recent")
  object Wiadomosci : NavigationTab("Wiadomości", Icons.Default.ChatBubble, Icons.Outlined.ChatBubbleOutline, "tab_messages")
  object Sprawdz : NavigationTab("Sprawdź", Icons.Default.Search, Icons.Outlined.Search, "tab_check")
  object Ochrona : NavigationTab("Ochrona", Icons.Default.Shield, Icons.Outlined.Shield, "tab_protection")
  object Profil : NavigationTab("Profil", Icons.Default.Person, Icons.Outlined.Person, "tab_profile")
}

@Composable
fun MainAppContent(viewModel: CallerViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

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

  // 1. Ekran połączenia przychodzącego (Call Screen / Overlay)
  if (uiState.incomingCallPreview != null) {
    IncomingCallScreen(
      caller = uiState.incomingCallPreview!!,
      onAnswer = { viewModel.dismissIncomingCall() },
      onReject = { viewModel.dismissIncomingCall() },
      isSimulatedPreview = true
    )
    return
  }

  // 2. Ekran Onboarding (Zasady działania)
  if (uiState.isOnboardingVisible) {
    BackHandler { viewModel.dismissOnboarding() }
    OnboardingScreen(onComplete = { viewModel.dismissOnboarding() })
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
        onSenderProfileClick = { sender -> viewModel.openContactForAddress(sender) },
        onSendMessage = { text -> viewModel.sendSms(text) }
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
            Icon(
              imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
              contentDescription = tab.title,
              tint = if (isSelected) AppPurple else Color(0xFF827F97),
              modifier = Modifier.size(22.dp)
            )

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
            onSimulateSms = { scenario -> viewModel.triggerSmsSimulation(scenario) }
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
            onOpenOnboarding = { viewModel.showOnboarding() },
            onOpenContacts = { viewModel.openContactsScreen() }
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
}

package com.example.data

import com.example.data.remote.MockRemoteCallerDataSource
import com.example.data.remote.RemoteCallerDataSource
import com.example.engine.BrandCatalog
import com.example.engine.ReputationEngine
import com.example.engine.SmsRiskAnalyzer
import com.example.model.BrandIdentity
import com.example.model.CallCategory
import com.example.model.CallInteraction
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.CallerInfo
import com.example.model.CommunityReputation
import com.example.model.ConfidenceLevel
import com.example.model.CrowdReport
import com.example.model.InteractionSource
import com.example.model.NumberIdentity
import com.example.model.ContactEntityType
import com.example.model.ContactSortMode
import com.example.model.OrganizationCategory
import com.example.model.PersonRelationship
import com.example.model.ProtectionSettings
import com.example.model.QuickRating
import com.example.model.RiskSeverity
import com.example.model.ScoringEngine
import com.example.model.SmsMessage
import com.example.model.SmsThread
import com.example.model.SubCategory
import com.example.model.UnifiedContact
import com.example.model.UnifiedSenderProfile
import com.example.model.UserProfile
import com.example.model.UserRatingSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SmsSimulationScenario {
  INPOST_PHISHING,
  GOOGLE_OTP,
  PGE_SCAM,
  MBANK_OTP,
  MOM_PERSONAL
}

interface CallerRepository {
  val recentCalls: StateFlow<List<CallRecord>>
  val smsThreads: StateFlow<List<SmsThread>>
  val protectionSettings: StateFlow<ProtectionSettings>
  val userProfile: StateFlow<UserProfile>
  val isRemoteMockMode: Boolean

  fun lookupNumber(query: String): CallerInfo
  fun submitReport(callId: String, rating: Int, categoryName: String?): Boolean
  fun toggleBlock(phoneNumber: String)
  fun updateProtectionSettings(settings: ProtectionSettings)
  fun recordScreeningEvent(phoneNumber: String, action: String)

  // SMS operations
  fun getMessagesForThread(threadId: String): List<SmsMessage>
  fun markThreadAsRead(threadId: String)
  fun sendSms(threadId: String, text: String): SmsMessage
  fun addIncomingSms(sender: String, body: String)
  fun triggerSmsSimulation(scenario: SmsSimulationScenario)

  // Unified sender profile (Cross-channel: Call + SMS)
  fun getUnifiedSenderProfile(senderAddress: String): UnifiedSenderProfile

  // Crowdsourcing & Dynamic Community Reputation (SEKCJE 19–31)
  val communityReputations: StateFlow<Map<String, CommunityReputation>>
  val userRatings: StateFlow<Map<String, UserRatingSubmission>>
  fun getCommunityReputation(phoneNumber: String): CommunityReputation
  fun getUserRating(phoneNumber: String): UserRatingSubmission?
  fun submitQuickRating(
    phoneNumber: String,
    rating: QuickRating,
    subCategory: SubCategory? = null,
    hasRealInteraction: Boolean = true
  ): Boolean
  fun recordCallInteraction(interaction: CallInteraction)

  fun getSampleInboundBankNumber(): CallerInfo
  fun getSampleScamNumber(): CallerInfo
  fun getSampleCourierNumber(): CallerInfo

  // Contacts 2.0 (People + Organizations) - SEKCJE 33–38
  val unifiedContacts: StateFlow<List<UnifiedContact>>
  val contactSortMode: StateFlow<ContactSortMode>
  fun getContactById(id: String): UnifiedContact?
  fun getContactForAddress(address: String): UnifiedContact?
  fun togglePinContact(contactId: String)
  fun toggleQuickContact(contactId: String)
  fun setContactSortMode(mode: ContactSortMode)
  fun reorderContactsManual(fromIndex: Int, toIndex: Int)
  fun addCustomContact(contact: UnifiedContact)
  fun dismissSmartSuggestion(contactId: String)
}

class DefaultCallerRepository(
  private val remoteDataSource: RemoteCallerDataSource = MockRemoteCallerDataSource()
) : CallerRepository {

  override val isRemoteMockMode: Boolean
    get() = remoteDataSource.isDemonstrationMock

  // 1. Oficjalna infolinia banku (PKO BP) – tożsamość numeru znana, ale rozmówca niepotwierdzony!
  private val pkoInboundHotline = run {
    val identity = NumberIdentity(
      isPublicDirectoryListed = true,
      registeredEntityName = "PKO Bank Polski S.A.",
      entityCategory = "Bankowość i finanse",
      isFinancialInstitution = true,
      isInboundOnlyHotline = true,
      officialSourceUrl = "https://www.pkobp.pl"
    )
    val scoreResult = ScoringEngine.calculateScore(
      category = CallCategory.IMPERSONATION_BANK,
      totalReports = 6,
      reportsLast24h = 4,
      reportsLastHour = 1,
      numberIdentity = identity
    )
    CallerInfo(
      phoneNumber = "+48 22 521 80 00",
      formattedNumber = "+48 22 521 80 00",
      displayName = "PKO Bank Polski",
      numberIdentity = identity,
      callPurpose = "Oficjalna infolinia przychodząca",
      riskScore = scoreResult.riskScore,
      confidence = scoreResult.confidence,
      category = "Infolinia bankowa (ryzyko podszycia)",
      totalReports = 6,
      reportsLast24h = 4,
      reportsLastHour = 1,
      activityTrend = listOf(0, 0, 1, 0, 1, 2, 4),
      firstSeen = "Baza oficjalna",
      lastSeen = "Dzisiaj, 12:48",
      subCategories = listOf("Infolinia przychodząca", "Możliwy CLI Spoofing"),
      riskReasonExplanation = scoreResult.explanation,
      isVerifiedCallerIdentityPossible = false
    )
  }

  // 2. Podejrzenie oszustwa (podszywanie się pod pracowników banku / wyłudzenie kodu BLIK / fałszywe SMS)
  private val scamNumber = run {
    val identity = NumberIdentity(
      isPublicDirectoryListed = false,
      registeredEntityName = null,
      entityCategory = null,
      isFinancialInstitution = false
    )
    val scoreResult = ScoringEngine.calculateScore(
      category = CallCategory.FRAUD_SUSPICION,
      totalReports = 32,
      reportsLast24h = 18,
      reportsLastHour = 8,
      numberIdentity = identity
    )
    CallerInfo(
      phoneNumber = "+48 732 123 456",
      formattedNumber = "+48 732 123 456",
      displayName = null,
      numberIdentity = identity,
      callPurpose = null,
      riskScore = scoreResult.riskScore,
      confidence = scoreResult.confidence,
      category = "Podejrzenie oszustwa (vishing + smishing)",
      totalReports = 32,
      reportsLast24h = 18,
      reportsLastHour = 8,
      activityTrend = listOf(1, 2, 0, 3, 8, 18, 32),
      firstSeen = "15 sie 2024",
      lastSeen = "Dzisiaj, 11:57",
      subCategories = listOf("Fałszywy doradca", "Wyłudzenie kodu BLIK", "Fałszywa dopłata do paczki"),
      riskReasonExplanation = scoreResult.explanation,
      isVerifiedCallerIdentityPossible = false
    )
  }

  // 3. Kurier DPD – oficjalny numer w rejestrze, brak zgłoszeń oszustw
  private val dpdCourier = run {
    val identity = NumberIdentity(
      isPublicDirectoryListed = true,
      registeredEntityName = "DPD Polska Sp. z o.o.",
      entityCategory = "Logistyka i doręczenia",
      isFinancialInstitution = false,
      isInboundOnlyHotline = false,
      officialSourceUrl = "https://www.dpd.com/pl"
    )
    val scoreResult = ScoringEngine.calculateScore(
      category = CallCategory.SAFE_OR_EXPECTED,
      totalReports = 0,
      reportsLast24h = 0,
      reportsLastHour = 0,
      numberIdentity = identity
    )
    CallerInfo(
      phoneNumber = "+48 22 577 55 55",
      formattedNumber = "+48 22 577 55 55",
      displayName = "DPD Polska",
      numberIdentity = identity,
      callPurpose = "Doręczenie przesyłki",
      riskScore = scoreResult.riskScore,
      confidence = scoreResult.confidence,
      category = "Kurier / Logistyka",
      totalReports = 0,
      reportsLast24h = 0,
      reportsLastHour = 0,
      activityTrend = listOf(0, 0, 0, 0, 0, 0, 0),
      firstSeen = "Rejestr publiczny",
      lastSeen = "Dzisiaj, 09:31",
      subCategories = listOf("Numer w rejestrze", "Logistyka"),
      riskReasonExplanation = scoreResult.explanation,
      isVerifiedCallerIdentityPossible = false
    )
  }

  // 4. Telemarketing
  private val telemarketingNumber = run {
    val identity = NumberIdentity(
      isPublicDirectoryListed = false
    )
    val scoreResult = ScoringEngine.calculateScore(
      category = CallCategory.TELEMARKETING,
      totalReports = 48,
      reportsLast24h = 14,
      reportsLastHour = 2,
      numberIdentity = identity
    )
    CallerInfo(
      phoneNumber = "+48 22 307 88 12",
      formattedNumber = "+48 22 307 88 12",
      displayName = "Oferta handlowa",
      numberIdentity = identity,
      riskScore = scoreResult.riskScore,
      confidence = scoreResult.confidence,
      category = "Telemarketing",
      totalReports = 48,
      reportsLast24h = 14,
      reportsLastHour = 2,
      activityTrend = listOf(8, 12, 10, 15, 11, 14, 14),
      firstSeen = "12 maj 2024",
      lastSeen = "Wczoraj, 16:15",
      subCategories = listOf("Połączenia masowe", "Oferta fotowoltaiki"),
      riskReasonExplanation = scoreResult.explanation,
      isVerifiedCallerIdentityPossible = false
    )
  }

  private val _knownCallers = MutableStateFlow(
    listOf(pkoInboundHotline, scamNumber, dpdCourier, telemarketingNumber)
  )

  private val _recentCalls = MutableStateFlow(
    listOf(
      CallRecord(
        id = "call-1",
        caller = pkoInboundHotline,
        timestamp = "Dzisiaj, 12:48",
        callType = CallType.INCOMING,
        durationSeconds = 85,
        isLocalCallVerified = true
      ),
      CallRecord(
        id = "call-2",
        caller = scamNumber,
        timestamp = "Dzisiaj, 11:57",
        callType = CallType.BLOCKED,
        durationSeconds = 0,
        isLocalCallVerified = true
      ),
      CallRecord(
        id = "call-3",
        caller = dpdCourier,
        timestamp = "Dzisiaj, 09:31",
        callType = CallType.INCOMING,
        durationSeconds = 34,
        isLocalCallVerified = true
      ),
      CallRecord(
        id = "call-4",
        caller = telemarketingNumber,
        timestamp = "Wczoraj, 16:15",
        callType = CallType.MISSED,
        durationSeconds = 0,
        isLocalCallVerified = true
      )
    )
  )

  // =================== SMS DATA & STORAGE ===================

  private val _smsMessages = MutableStateFlow<Map<String, List<SmsMessage>>>(emptyMap())
  private val _smsThreads = MutableStateFlow<List<SmsThread>>(emptyList())

  // =================== CROWDSOURCING & REPUTATION (SEKCJE 19–31) ===================

  private val _communitySubmissions = MutableStateFlow<Map<String, List<UserRatingSubmission>>>(emptyMap())
  private val _communityReputations = MutableStateFlow<Map<String, CommunityReputation>>(emptyMap())
  private val _userRatings = MutableStateFlow<Map<String, UserRatingSubmission>>(emptyMap())
  private val _callInteractions = MutableStateFlow<List<CallInteraction>>(emptyList())

  override val communityReputations: StateFlow<Map<String, CommunityReputation>> = _communityReputations.asStateFlow()
  override val userRatings: StateFlow<Map<String, UserRatingSubmission>> = _userRatings.asStateFlow()

  init {
    initDefaultSmsData()
    initDefaultReputations()
  }

  private fun initDefaultReputations() {
    val now = System.currentTimeMillis()
    val submissionsMap = mutableMapOf<String, List<UserRatingSubmission>>()

    // 1. Oszustwo: +48 732 123 456 (32 zgłoszenia w ostatnich 7 dniach, głównie oszustwa i próby wyłudzenia)
    val scamSubs = mutableListOf<UserRatingSubmission>()
    for (i in 1..26) {
      val subCat = if (i % 2 == 0) SubCategory.BLIK_PHISHING else SubCategory.BANK_IMPERSONATION
      val daysAgo = (i % 7)
      scamSubs.add(
        UserRatingSubmission(
          id = "seed-scam-$i",
          phoneNumber = "+48 732 123 456",
          rating = QuickRating.SCAM,
          subCategory = subCat,
          timestampEpoch = now - (daysAgo * 24L * 60L * 60L * 1000L) - (i * 1800000L),
          hasRealInteraction = true,
          userTrustWeight = 1.0f
        )
      )
    }
    for (i in 1..4) {
      scamSubs.add(
        UserRatingSubmission(
          id = "seed-scam-nuis-$i",
          phoneNumber = "+48 732 123 456",
          rating = QuickRating.NUISANCE,
          subCategory = SubCategory.OTHER_NUISANCE,
          timestampEpoch = now - (i * 24L * 60L * 60L * 1000L),
          hasRealInteraction = true,
          userTrustWeight = 0.9f
        )
      )
    }
    for (i in 1..2) {
      scamSubs.add(
        UserRatingSubmission(
          id = "seed-scam-pos-$i",
          phoneNumber = "+48 732 123 456",
          rating = QuickRating.POSITIVE,
          subCategory = SubCategory.OTHER_SAFE,
          timestampEpoch = now - (20L * 24L * 60L * 60L * 1000L),
          hasRealInteraction = false,
          userTrustWeight = 0.6f
        )
      )
    }
    submissionsMap["+48 732 123 456"] = scamSubs

    // 2. Infolinia PKO BP: +48 22 521 80 00 (18 zgłoszeń: 14 pozytywnych, 4 podejrzane zgłoszenia podszywania się)
    val pkoSubs = mutableListOf<UserRatingSubmission>()
    for (i in 1..14) {
      pkoSubs.add(
        UserRatingSubmission(
          id = "seed-pko-pos-$i",
          phoneNumber = "+48 22 521 80 00",
          rating = QuickRating.POSITIVE,
          subCategory = SubCategory.BANK_FINANCE,
          timestampEpoch = now - ((i % 14) * 24L * 60L * 60L * 1000L),
          hasRealInteraction = true,
          userTrustWeight = 1.0f
        )
      )
    }
    for (i in 1..4) {
      pkoSubs.add(
        UserRatingSubmission(
          id = "seed-pko-scam-$i",
          phoneNumber = "+48 22 521 80 00",
          rating = QuickRating.SCAM,
          subCategory = SubCategory.BANK_IMPERSONATION,
          timestampEpoch = now - (i * 12L * 60L * 60L * 1000L),
          hasRealInteraction = true,
          userTrustWeight = 1.1f
        )
      )
    }
    submissionsMap["+48 22 521 80 00"] = pkoSubs

    // 3. Kurier DPD: +48 22 577 55 55 (25 zgłoszeń: 24 pozytywne)
    val dpdSubs = mutableListOf<UserRatingSubmission>()
    for (i in 1..24) {
      dpdSubs.add(
        UserRatingSubmission(
          id = "seed-dpd-pos-$i",
          phoneNumber = "+48 22 577 55 55",
          rating = QuickRating.POSITIVE,
          subCategory = SubCategory.COURIER,
          timestampEpoch = now - ((i % 10) * 24L * 60L * 60L * 1000L),
          hasRealInteraction = true,
          userTrustWeight = 1.0f
        )
      )
    }
    dpdSubs.add(
      UserRatingSubmission(
        id = "seed-dpd-nuis-1",
        phoneNumber = "+48 22 577 55 55",
        rating = QuickRating.NUISANCE,
        subCategory = SubCategory.OTHER_NUISANCE,
        timestampEpoch = now - (40L * 24L * 60L * 60L * 1000L),
        hasRealInteraction = true,
        userTrustWeight = 0.8f
      )
    )
    submissionsMap["+48 22 577 55 55"] = dpdSubs

    // 4. Telemarketing: +48 22 307 88 12 (48 zgłoszeń: 42 natrętne oferty handlowe)
    val teleSubs = mutableListOf<UserRatingSubmission>()
    for (i in 1..42) {
      teleSubs.add(
        UserRatingSubmission(
          id = "seed-tele-nuis-$i",
          phoneNumber = "+48 22 307 88 12",
          rating = QuickRating.NUISANCE,
          subCategory = if (i % 3 == 0) SubCategory.ROBOCALL_BOT else SubCategory.SALES_OFFER,
          timestampEpoch = now - ((i % 14) * 24L * 60L * 60L * 1000L),
          hasRealInteraction = true,
          userTrustWeight = 1.0f
        )
      )
    }
    for (i in 1..6) {
      teleSubs.add(
        UserRatingSubmission(
          id = "seed-tele-pos-$i",
          phoneNumber = "+48 22 307 88 12",
          rating = QuickRating.POSITIVE,
          subCategory = SubCategory.OTHER_SAFE,
          timestampEpoch = now - (50L * 24L * 60L * 60L * 1000L),
          hasRealInteraction = false,
          userTrustWeight = 0.7f
        )
      )
    }
    submissionsMap["+48 22 307 88 12"] = teleSubs

    _communitySubmissions.value = submissionsMap

    val reputationsMap = mutableMapOf<String, CommunityReputation>()
    submissionsMap.forEach { (phone, subs) ->
      reputationsMap[phone] = ReputationEngine.aggregateReputation(phone, subs, now)
    }
    _communityReputations.value = reputationsMap

    // Dołącz zagregowaną reputację do znanych numerów
    _knownCallers.update { callers ->
      callers.map { caller ->
        reputationsMap[caller.phoneNumber]?.let { rep ->
          caller.copy(communityReputation = rep)
        } ?: caller
      }
    }

    _recentCalls.update { calls ->
      calls.map { call ->
        reputationsMap[call.caller.phoneNumber]?.let { rep ->
          call.copy(caller = call.caller.copy(communityReputation = rep))
        } ?: call
      }
    }
  }

  private fun initDefaultSmsData() {
    val initialMessages = mutableMapOf<String, List<SmsMessage>>()
    val initialThreads = mutableListOf<SmsThread>()

    // 1. Google (OTP)
    val threadGoogle = "thread-google"
    val analysisGoogle = SmsRiskAnalyzer.analyzeMessage(
      sender = "Google",
      body = "G-847213 to Twój kod weryfikacyjny Google. Nikomu go nie udostępniaj.",
      recognizedBrand = BrandCatalog.google
    )
    val msgGoogle = SmsMessage(
      id = "msg-g1",
      threadId = threadGoogle,
      senderAddress = "Google",
      body = "G-847213 to Twój kod weryfikacyjny Google. Nikomu go nie udostępniaj.",
      timestamp = "13:20",
      timestampEpoch = System.currentTimeMillis() - 1000 * 60 * 15,
      isOutgoing = false,
      isRead = false,
      otpInfo = analysisGoogle.otpInfo,
      urlAnalysis = analysisGoogle.urlAnalysis,
      riskScore = analysisGoogle.riskScore,
      riskSeverity = analysisGoogle.severity,
      riskReasons = analysisGoogle.reasons
    )
    initialMessages[threadGoogle] = listOf(msgGoogle)
    initialThreads.add(
      SmsThread(
        threadId = threadGoogle,
        senderAddress = "Google",
        displayName = "Google",
        recognizedBrand = BrandCatalog.google,
        lastMessageSnippet = "G-847213 to Twój kod weryfikacyjny Google...",
        lastMessageTimestamp = "13:20",
        lastMessageTimestampEpoch = msgGoogle.timestampEpoch,
        unreadCount = 1,
        highestRiskScore = 0,
        highestSeverity = RiskSeverity.NO_KNOWN_RISK,
        hasOtp = true
      )
    )

    // 2. Scam Smishing (ten sam numer +48 732 123 456 co w vishingu!)
    val threadScam = "thread-scam"
    val scamBody = "INPOST: Twoja przesyłka wymaga dopłaty 1.89 zł ze względu na błąd wagi. Opłać teraz, aby uniknąć zwrotu: http://inpost-doplata-paczka.xyz/pay"
    val analysisScam = SmsRiskAnalyzer.analyzeMessage(
      sender = "+48 732 123 456",
      body = scamBody,
      recognizedBrand = BrandCatalog.inPost // Treść podszywa się pod InPost
    )
    val msgScam = SmsMessage(
      id = "msg-scam1",
      threadId = threadScam,
      senderAddress = "+48 732 123 456",
      body = scamBody,
      timestamp = "11:59",
      timestampEpoch = System.currentTimeMillis() - 1000 * 60 * 95,
      isOutgoing = false,
      isRead = false,
      otpInfo = analysisScam.otpInfo,
      urlAnalysis = analysisScam.urlAnalysis,
      riskScore = analysisScam.riskScore,
      riskSeverity = analysisScam.severity,
      riskReasons = analysisScam.reasons
    )
    initialMessages[threadScam] = listOf(msgScam)
    initialThreads.add(
      SmsThread(
        threadId = threadScam,
        senderAddress = "+48 732 123 456",
        displayName = "Podejrzany numer",
        recognizedBrand = null,
        lastMessageSnippet = "INPOST: Twoja przesyłka wymaga dopłaty 1.89 zł...",
        lastMessageTimestamp = "11:59",
        lastMessageTimestampEpoch = msgScam.timestampEpoch,
        unreadCount = 1,
        highestRiskScore = analysisScam.riskScore,
        highestSeverity = analysisScam.severity,
        hasOtp = false,
        isSpamOrBlocked = true
      )
    )

    // 3. InPost (Autentyczny / Oficjalna domena inpost.pl)
    val threadInpost = "thread-inpost"
    val inpostBody = "Paczka 62349182 czeka w Paczkomacie WAW12A (Warszawa, ul. Marszałkowska 10) do 08.09 14:00. Kod odbioru: 492011. Szczegóły: https://inpost.pl"
    val analysisInpost = SmsRiskAnalyzer.analyzeMessage(
      sender = "InPost",
      body = inpostBody,
      recognizedBrand = BrandCatalog.inPost
    )
    val msgInpost = SmsMessage(
      id = "msg-inpost1",
      threadId = threadInpost,
      senderAddress = "InPost",
      body = inpostBody,
      timestamp = "Wczoraj",
      timestampEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
      isOutgoing = false,
      isRead = true,
      otpInfo = analysisInpost.otpInfo,
      urlAnalysis = analysisInpost.urlAnalysis,
      riskScore = analysisInpost.riskScore,
      riskSeverity = analysisInpost.severity,
      riskReasons = analysisInpost.reasons
    )
    initialMessages[threadInpost] = listOf(msgInpost)
    initialThreads.add(
      SmsThread(
        threadId = threadInpost,
        senderAddress = "InPost",
        displayName = "InPost",
        recognizedBrand = BrandCatalog.inPost,
        lastMessageSnippet = "Paczka 62349182 czeka w Paczkomacie WAW12A. Kod odbioru: 492011...",
        lastMessageTimestamp = "Wczoraj",
        lastMessageTimestampEpoch = msgInpost.timestampEpoch,
        unreadCount = 0,
        highestRiskScore = 0,
        highestSeverity = RiskSeverity.NO_KNOWN_RISK,
        hasOtp = true
      )
    )

    // 4. Mama (prywatny kontakt)
    val threadMom = "thread-mom"
    val msgMom = SmsMessage(
      id = "msg-mom1",
      threadId = threadMom,
      senderAddress = "+48 601 234 567",
      body = "Cześć, czy będziesz w niedzielę na obiedzie? Kupiłam już składniki na szarlotkę.",
      timestamp = "Wczoraj",
      timestampEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
      isOutgoing = false,
      isRead = true,
      otpInfo = null,
      urlAnalysis = null,
      riskScore = 0,
      riskSeverity = RiskSeverity.NO_KNOWN_RISK,
      riskReasons = listOf("Prywatna wiadomość bez linków i żądań")
    )
    initialMessages[threadMom] = listOf(msgMom)
    initialThreads.add(
      SmsThread(
        threadId = threadMom,
        senderAddress = "+48 601 234 567",
        displayName = "Mama",
        recognizedBrand = null,
        lastMessageSnippet = "Cześć, czy będziesz w niedzielę na obiedzie?...",
        lastMessageTimestamp = "Wczoraj",
        lastMessageTimestampEpoch = msgMom.timestampEpoch,
        unreadCount = 0,
        highestRiskScore = 0,
        highestSeverity = RiskSeverity.NO_KNOWN_RISK,
        hasOtp = false
      )
    )

    _smsMessages.value = initialMessages
    _smsThreads.value = initialThreads
  }

  // =================== IMPLEMENTACJA SMS ===================

  override val smsThreads: StateFlow<List<SmsThread>> = _smsThreads.asStateFlow()

  override fun getMessagesForThread(threadId: String): List<SmsMessage> {
    return _smsMessages.value[threadId] ?: emptyList()
  }

  override fun markThreadAsRead(threadId: String) {
    _smsThreads.update { threads ->
      threads.map {
        if (it.threadId == threadId) it.copy(unreadCount = 0) else it
      }
    }
    _smsMessages.update { map ->
      val list = map[threadId] ?: return@update map
      val updated = list.map { it.copy(isRead = true) }
      map + (threadId to updated)
    }
  }

  override fun sendSms(threadId: String, text: String): SmsMessage {
    val newMsg = SmsMessage(
      id = "sent-${System.currentTimeMillis()}",
      threadId = threadId,
      senderAddress = "Ja",
      body = text,
      timestamp = "Teraz",
      timestampEpoch = System.currentTimeMillis(),
      isOutgoing = true,
      isRead = true
    )

    _smsMessages.update { map ->
      val existing = map[threadId] ?: emptyList()
      map + (threadId to (existing + newMsg))
    }

    _smsThreads.update { threads ->
      threads.map {
        if (it.threadId == threadId) {
          it.copy(
            lastMessageSnippet = text,
            lastMessageTimestamp = "Teraz",
            lastMessageTimestampEpoch = System.currentTimeMillis(),
            totalMessagesCount = it.totalMessagesCount + 1
          )
        } else it
      }
    }

    return newMsg
  }

  override fun addIncomingSms(sender: String, body: String) {
    val brand = BrandCatalog.findBrandBySender(sender)
    val analysis = SmsRiskAnalyzer.analyzeMessage(sender, body, brand)

    val existingThread = _smsThreads.value.firstOrNull { it.senderAddress == sender }
    val threadId = existingThread?.threadId ?: "thread-${sender.hashCode()}"

    val newMsg = SmsMessage(
      id = "msg-${System.currentTimeMillis()}",
      threadId = threadId,
      senderAddress = sender,
      body = body,
      timestamp = "Teraz",
      timestampEpoch = System.currentTimeMillis(),
      isOutgoing = false,
      isRead = false,
      otpInfo = analysis.otpInfo,
      urlAnalysis = analysis.urlAnalysis,
      riskScore = analysis.riskScore,
      riskSeverity = analysis.severity,
      riskReasons = analysis.reasons
    )

    _smsMessages.update { map ->
      val existing = map[threadId] ?: emptyList()
      map + (threadId to (existing + newMsg))
    }

    _smsThreads.update { threads ->
      val updatedThread = SmsThread(
        threadId = threadId,
        senderAddress = sender,
        displayName = brand?.displayName ?: existingThread?.displayName,
        recognizedBrand = brand,
        lastMessageSnippet = body.take(60),
        lastMessageTimestamp = "Teraz",
        lastMessageTimestampEpoch = System.currentTimeMillis(),
        unreadCount = (existingThread?.unreadCount ?: 0) + 1,
        totalMessagesCount = (existingThread?.totalMessagesCount ?: 0) + 1,
        highestRiskScore = maxOf(existingThread?.highestRiskScore ?: 0, analysis.riskScore),
        highestSeverity = if (analysis.severity.ordinal > (existingThread?.highestSeverity?.ordinal ?: 0)) analysis.severity else (existingThread?.highestSeverity ?: analysis.severity),
        hasOtp = analysis.otpInfo != null || (existingThread?.hasOtp == true),
        isSpamOrBlocked = analysis.severity == RiskSeverity.HIGH || (existingThread?.isSpamOrBlocked == true)
      )

      val filtered = threads.filter { it.threadId != threadId }
      listOf(updatedThread) + filtered
    }
  }

  override fun triggerSmsSimulation(scenario: SmsSimulationScenario) {
    when (scenario) {
      SmsSimulationScenario.INPOST_PHISHING -> {
        addIncomingSms(
          sender = "+48 732 123 456",
          body = "INPOST: Twoja paczka została wstrzymana z powodu błędnego adresu. Uzupełnij dane i opłać 1.49 zł: http://inpost-paczka-odbierz.xyz/waw"
        )
      }
      SmsSimulationScenario.GOOGLE_OTP -> {
        addIncomingSms(
          sender = "Google",
          body = "G-914285 to Twój kod weryfikacyjny Google. Pamiętaj: nikt z Google nie poprosi Cię o ten kod."
        )
      }
      SmsSimulationScenario.PGE_SCAM -> {
        addIncomingSms(
          sender = "PGE",
          body = "PGE: Przypominamy o nieuregulowanej należności 12.80 zł. Odłączenie prądu nastąpi jutro. Opłać natychmiast: http://bit.ly/pge-rachunek"
        )
      }
      SmsSimulationScenario.MBANK_OTP -> {
        addIncomingSms(
          sender = "mBank",
          body = "Kod do autoryzacji transakcji w mBank: 831 920. Kwota: 250,00 PLN. Jeśli nie zlecałeś tej operacji, zadzwoń natychmiast."
        )
      }
      SmsSimulationScenario.MOM_PERSONAL -> {
        addIncomingSms(
          sender = "+48 601 234 567",
          body = "Pamiętaj o odebraniu leków z apteki. Dobrego dnia!"
        )
      }
    }
  }

  // =================== WSPÓLNY SILNIK REPUTACJI (Cross-channel) ===================

  override fun getUnifiedSenderProfile(senderAddress: String): UnifiedSenderProfile {
    val clean = senderAddress.trim()
    val brand = BrandCatalog.findBrandBySender(clean)

    // Sygnały połączeń
    val matchingCalls = _recentCalls.value.filter {
      it.caller.phoneNumber == clean || it.caller.displayName.equals(clean, ignoreCase = true)
    }
    val knownCaller = _knownCallers.value.firstOrNull { it.phoneNumber == clean }

    val callCount = matchingCalls.size
    val lastCallTime = matchingCalls.firstOrNull()?.timestamp

    // Sygnały SMS
    val matchingThread = _smsThreads.value.firstOrNull {
      it.senderAddress == clean || it.displayName.equals(clean, ignoreCase = true)
    }
    val messages = matchingThread?.let { _smsMessages.value[it.threadId] } ?: emptyList()
    val smsCount = messages.size
    val lastSmsTime = matchingThread?.lastMessageTimestamp

    val suspiciousLinks = messages.count { it.urlAnalysis?.isHighRisk == true }

    // Obliczenie łącznego ryzyka
    val callRisk = knownCaller?.riskScore ?: (if (matchingCalls.any { it.caller.severity == RiskSeverity.HIGH }) 85 else 0)
    val smsRisk = matchingThread?.highestRiskScore ?: 0
    val combinedRisk = maxOf(callRisk, smsRisk)

    val confidence = when {
      brand != null -> ConfidenceLevel.WYSOKA
      knownCaller != null -> knownCaller.confidence
      messages.isNotEmpty() || matchingCalls.isNotEmpty() -> ConfidenceLevel.SREDNIA
      else -> ConfidenceLevel.NISKA
    }

    val dataSources = mutableListOf<String>()
    if (brand != null) dataSources.add("Katalog oficjalnych marek (${brand.verificationSource})")
    if (knownCaller?.numberIdentity?.isPublicDirectoryListed == true) dataSources.add("Rejestr publiczny UKE / KRS")
    if (knownCaller != null && knownCaller.totalReports > 0) dataSources.add("Zgłoszenia społecznościowe (${knownCaller.totalReports} raportów)")
    if (suspiciousLinks > 0) dataSources.add("Lokalny filtr analizy linków (mismatch domeny / podejrzane TLD)")
    if (dataSources.isEmpty()) dataSources.add("Lokalna historia urządzenia")

    val riskFactors = mutableListOf<String>()
    if (knownCaller?.isFinancialInstitution == true) {
      riskFactors.add("Instytucja finansowa – ostrzeżenie Zero Trust przed sfałszowaniem CLI")
    }
    if (suspiciousLinks > 0) {
      riskFactors.add("Wykryto $suspiciousLinks podejrzanych linków w wiadomościach SMS")
    }
    if (knownCaller != null && knownCaller.subCategories.isNotEmpty()) {
      riskFactors.addAll(knownCaller.subCategories)
    }
    if (riskFactors.isEmpty()) {
      riskFactors.add("Brak zarejestrowanych incydentów i skarg")
    }

    return UnifiedSenderProfile(
      address = clean,
      formattedAddress = knownCaller?.formattedNumber ?: clean,
      displayName = brand?.displayName ?: knownCaller?.displayName ?: matchingThread?.displayName,
      recognizedBrand = brand,
      callsCount = callCount,
      lastCallTime = lastCallTime,
      smsCount = smsCount,
      lastSmsTime = lastSmsTime,
      reportsCount = knownCaller?.totalReports ?: 0,
      suspiciousLinksCount = suspiciousLinks,
      combinedRiskScore = combinedRisk,
      confidence = confidence,
      dataSources = dataSources,
      riskFactors = riskFactors,
      isBlocked = knownCaller?.isBlocked == true || matchingThread?.isSpamOrBlocked == true
    )
  }

  // =================== POZOSTAŁE METODY ===================

  private val _protectionSettings = MutableStateFlow(
    ProtectionSettings(
      isProtectionActive = true,
      blockHighRisk = true,
      blockTelemarketing = false,
      blockUnknown = false,
      blockThreshold = 85f,
      lastUpdatedTime = "Przed chwilą (lokalny cache)",
      blockedTodayCount = 1,
      warningsTodayCount = 4,
      isCallScreeningRoleGranted = true
    )
  )

  private val _userProfile = MutableStateFlow(UserProfile())

  override val recentCalls: StateFlow<List<CallRecord>> = _recentCalls.asStateFlow()
  override val protectionSettings: StateFlow<ProtectionSettings> = _protectionSettings.asStateFlow()
  override val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  override fun lookupNumber(query: String): CallerInfo {
    val clean = query.trim().replace(" ", "")
    val existing = _knownCallers.value.firstOrNull {
      it.phoneNumber.replace(" ", "").contains(clean) || clean.contains(it.phoneNumber.replace(" ", ""))
    }
    if (existing != null) return existing

    val brand = BrandCatalog.findBrandBySender(query)
    val rep = getCommunityReputation(query)
    val scoreResult = if (rep.totalRatingsCount > 0) {
      ReputationEngine.calculateCombinedScore(
        reputation = rep,
        numberIdentity = if (brand != null) NumberIdentity(isPublicDirectoryListed = true, registeredEntityName = brand.displayName) else null
      )
    } else {
      ScoringEngine.calculateScore(
        category = CallCategory.SAFE_OR_EXPECTED,
        totalReports = 0,
        reportsLast24h = 0,
        reportsLastHour = 0,
        numberIdentity = if (brand != null) NumberIdentity(isPublicDirectoryListed = true, registeredEntityName = brand.displayName) else null
      )
    }

    return CallerInfo(
      phoneNumber = query,
      formattedNumber = query,
      displayName = brand?.displayName,
      riskScore = scoreResult.riskScore,
      confidence = scoreResult.confidence,
      category = if (brand != null) "Rozpoznana marka" else (if (rep.totalRatingsCount > 0) rep.reputationLabel else "Nieznany numer"),
      totalReports = rep.totalRatingsCount,
      reportsLast24h = rep.ratingsLast7Days,
      firstSeen = if (rep.totalRatingsCount > 0) rep.freshnessSummary else "Brak wpisów",
      lastSeen = "Teraz",
      riskReasonExplanation = scoreResult.explanation,
      isVerifiedCallerIdentityPossible = false,
      communityReputation = rep
    )
  }

  override fun submitQuickRating(
    phoneNumber: String,
    rating: QuickRating,
    subCategory: SubCategory?,
    hasRealInteraction: Boolean
  ): Boolean {
    val cleanNumber = phoneNumber.trim()
    val trustWeight = ReputationEngine.calculateUserTrustWeight(_userProfile.value, hasRealInteraction)

    val existingUserRating = _userRatings.value[cleanNumber]
    val submission = UserRatingSubmission(
      id = "rate-${System.currentTimeMillis()}",
      phoneNumber = cleanNumber,
      rating = rating,
      subCategory = subCategory,
      timestampEpoch = System.currentTimeMillis(),
      hasRealInteraction = hasRealInteraction,
      userTrustWeight = trustWeight,
      isUpdatedRating = existingUserRating != null
    )

    // 1. Zapisz/zaktualizuj unikalną ocenę bieżącego użytkownika (One active rating per user per number)
    _userRatings.update { it + (cleanNumber to submission) }

    // 2. Zaktualizuj zagregowaną listę zgłoszeń (zastąp starą ocenę tego samego użytkownika)
    val currentSubs = _communitySubmissions.value[cleanNumber] ?: emptyList()
    val updatedSubs = listOf(submission) + currentSubs.filter { !it.isUpdatedRating }
    _communitySubmissions.update { it + (cleanNumber to updatedSubs) }

    // 3. Przelicz reputację społecznościową z uwzględnieniem Time Decay
    val updatedReputation = ReputationEngine.aggregateReputation(cleanNumber, updatedSubs)
    _communityReputations.update { it + (cleanNumber to updatedReputation) }

    // 4. Zaktualizuj wpisy w historii połączeń
    _recentCalls.update { calls ->
      calls.map {
        if (it.caller.phoneNumber == cleanNumber) {
          it.copy(
            caller = it.caller.copy(communityReputation = updatedReputation),
            quickRating = rating,
            userRating = when (rating) {
              QuickRating.POSITIVE -> 5
              QuickRating.NUISANCE -> 2
              QuickRating.SCAM -> 1
            },
            reportedCategory = subCategory?.displayName ?: rating.displayName
          )
        } else it
      }
    }

    // 5. Zaktualizuj znane numery
    _knownCallers.update { callers ->
      callers.map {
        if (it.phoneNumber == cleanNumber) {
          val combined = ReputationEngine.calculateCombinedScore(
            reputation = updatedReputation,
            numberIdentity = it.numberIdentity,
            isSpoofSuspectedOnInbound = it.numberIdentity?.isFinancialInstitution == true
          )
          it.copy(
            communityReputation = updatedReputation,
            riskScore = combined.riskScore,
            confidence = combined.confidence,
            riskReasonExplanation = combined.explanation,
            totalReports = updatedReputation.totalRatingsCount
          )
        } else it
      }
    }

    // 6. Zwiększ licznik potwierdzonych zgłoszeń użytkownika w profilu
    _userProfile.update {
      it.copy(
        confirmedReports = it.confirmedReports + (if (existingUserRating == null) 1 else 0),
        credibilityScore = (it.credibilityScore + 1).coerceAtMost(100)
      )
    }

    return true
  }

  override fun getCommunityReputation(phoneNumber: String): CommunityReputation {
    val clean = phoneNumber.trim()
    return _communityReputations.value[clean] ?: run {
      val subs = _communitySubmissions.value[clean] ?: emptyList()
      ReputationEngine.aggregateReputation(clean, subs)
    }
  }

  override fun getUserRating(phoneNumber: String): UserRatingSubmission? {
    return _userRatings.value[phoneNumber.trim()]
  }

  override fun recordCallInteraction(interaction: CallInteraction) {
    _callInteractions.update { listOf(interaction) + it }
  }

  override fun submitReport(callId: String, rating: Int, categoryName: String?): Boolean {
    val category = CallCategory.values().firstOrNull { it.displayName == categoryName }
      ?: CallCategory.OTHER

    val report = CrowdReport(
      id = "rep-${System.currentTimeMillis()}",
      phoneNumber = callId,
      category = category,
      rating = rating
    )

    _recentCalls.update { calls ->
      calls.map {
        if (it.id == callId) it.copy(userRating = rating, reportedCategory = category.displayName)
        else it
      }
    }
    return true
  }

  override fun toggleBlock(phoneNumber: String) {
    _knownCallers.update { callers ->
      callers.map {
        if (it.phoneNumber == phoneNumber) it.copy(isBlocked = !it.isBlocked)
        else it
      }
    }
    _recentCalls.update { calls ->
      calls.map {
        if (it.caller.phoneNumber == phoneNumber) {
          it.copy(caller = it.caller.copy(isBlocked = !it.caller.isBlocked))
        } else it
      }
    }
    _smsThreads.update { threads ->
      threads.map {
        if (it.senderAddress == phoneNumber) it.copy(isSpamOrBlocked = !it.isSpamOrBlocked)
        else it
      }
    }
  }

  override fun updateProtectionSettings(settings: ProtectionSettings) {
    _protectionSettings.value = settings
  }

  override fun recordScreeningEvent(phoneNumber: String, action: String) {
    // rejestracja lokalna
  }

  override fun getSampleInboundBankNumber(): CallerInfo = pkoInboundHotline
  override fun getSampleScamNumber(): CallerInfo = scamNumber
  override fun getSampleCourierNumber(): CallerInfo = dpdCourier

  // =================== CONTACTS 2.0 IMPLEMENTACJA ===================

  private val _contactSortMode = MutableStateFlow(ContactSortMode.MANUAL)
  override val contactSortMode: StateFlow<ContactSortMode> = _contactSortMode.asStateFlow()

  // Inicjalna lista kontaktów (People + Organizations) - SEKCJE 33-38
  private val _rawContacts = MutableStateFlow(
    listOf(
      // 1. Mama (PERSON - Szybki kontakt, przypięta)
      UnifiedContact(
        id = "contact-mom",
        entityType = ContactEntityType.PERSON,
        primaryDisplayName = "Mama",
        avatarInitials = "M",
        isPinned = true,
        manualOrderIndex = 0,
        quickContactSlot = true,
        singleClickToCall = true,
        callsCount = 14,
        lastCallTimestamp = "Dzisiaj, 10:15",
        lastCallEpoch = System.currentTimeMillis() - 1000 * 60 * 120,
        smsCount = 28,
        lastSmsTimestamp = "Wczoraj",
        lastSmsEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
        otpCount = 0,
        reportsCount = 0,
        suspiciousLinksCount = 0,
        riskScore = 0,
        riskSeverity = RiskSeverity.NO_KNOWN_RISK,
        confidence = ConfidenceLevel.WYSOKA,
        firstName = "Barbara",
        lastName = "Kowalska",
        relationship = PersonRelationship.FAMILY,
        personalPhoneNumbers = listOf("+48 601 234 567"),
        email = "mama@rodzina.pl",
        reputationNote = "Zaufany kontakt prywatny (Rodzina)"
      ),

      // 2. Doktor Janusz Nowak (PERSON - Lekarz, przypięty)
      UnifiedContact(
        id = "contact-doctor",
        entityType = ContactEntityType.PERSON,
        primaryDisplayName = "Dr Janusz Nowak",
        avatarInitials = "JN",
        isPinned = true,
        manualOrderIndex = 1,
        quickContactSlot = true,
        singleClickToCall = false,
        callsCount = 4,
        lastCallTimestamp = "3 dni temu",
        lastCallEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 72,
        smsCount = 2,
        lastSmsTimestamp = "1 wrz",
        lastSmsEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 120,
        otpCount = 0,
        reportsCount = 0,
        suspiciousLinksCount = 0,
        riskScore = 0,
        riskSeverity = RiskSeverity.NO_KNOWN_RISK,
        confidence = ConfidenceLevel.WYSOKA,
        firstName = "Janusz",
        lastName = "Nowak",
        relationship = PersonRelationship.DOCTOR,
        personalPhoneNumbers = listOf("+48 602 998 877"),
        email = "przychodnia@med-rejon.pl",
        reputationNote = "Lekarz rodzinny / Kardiolog"
      ),

      // 3. DPD Polska (ORGANIZATION - Zgodnie z przykładem w Sekcji 33)
      UnifiedContact(
        id = "contact-dpd",
        entityType = ContactEntityType.ORGANIZATION,
        primaryDisplayName = "DPD Polska",
        avatarInitials = "DPD",
        isPinned = false,
        manualOrderIndex = 2,
        quickContactSlot = false,
        callsCount = 6,
        lastCallTimestamp = "Dzisiaj, 09:31",
        lastCallEpoch = System.currentTimeMillis() - 1000 * 60 * 180,
        smsCount = 12,
        lastSmsTimestamp = "Wczoraj",
        lastSmsEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 20,
        otpCount = 0,
        reportsCount = 0,
        suspiciousLinksCount = 0,
        riskScore = 10,
        riskSeverity = RiskSeverity.LOW,
        confidence = ConfidenceLevel.WYSOKA,
        organizationCategory = OrganizationCategory.COURIER_LOGISTICS,
        knownPhoneNumbers = listOf("+48 22 577 55 55", "+48 22 577 55 00"),
        smsSenderIds = listOf("DPD", "DPDPL"),
        officialDomains = listOf("dpd.com.pl", "dpd.pl"),
        officialWebsite = "https://www.dpd.com/pl",
        contactHours = "Pn - Pt: 8:00 - 19:00, Sob: 8:00 - 14:00",
        identificationSource = "Rejestr podmiotów KRS / Certyfikacja nadawcy SMS",
        reputationNote = "Oficjalny kurier logistyczny w Polsce. Zweryfikowane Sender ID i infolinia.",
        smartPinSuggestion = "Częsty kontakt kurierski. Czy chcesz przypiąć DPD do szybkiego dostępu?"
      ),

      // 4. InPost (ORGANIZATION)
      UnifiedContact(
        id = "contact-inpost",
        entityType = ContactEntityType.ORGANIZATION,
        primaryDisplayName = "InPost",
        avatarInitials = "INP",
        isPinned = false,
        manualOrderIndex = 3,
        quickContactSlot = false,
        callsCount = 1,
        lastCallTimestamp = "26 sie",
        lastCallEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 240,
        smsCount = 19,
        lastSmsTimestamp = "Wczoraj, 18:40",
        lastSmsEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 22,
        otpCount = 12,
        reportsCount = 0,
        suspiciousLinksCount = 0,
        riskScore = 0,
        riskSeverity = RiskSeverity.NO_KNOWN_RISK,
        confidence = ConfidenceLevel.WYSOKA,
        organizationCategory = OrganizationCategory.COURIER_LOGISTICS,
        knownPhoneNumbers = listOf("+48 722 444 000", "+48 746 600 000"),
        smsSenderIds = listOf("InPost", "Paczkomaty"),
        officialDomains = listOf("inpost.pl"),
        officialWebsite = "https://inpost.pl",
        contactHours = "Całodobowo 24/7 (Infolinia automatyczna i Paczkomaty)",
        identificationSource = "Rejestr Operatorów Pocztowych UKE / Bezpieczna domena .pl",
        brandIdentity = BrandCatalog.inPost,
        reputationNote = "Oficjalny operator Paczkomatów. Zaufane kody odbioru i powiadomienia."
      ),

      // 5. PKO Bank Polski (ORGANIZATION)
      UnifiedContact(
        id = "contact-pkobp",
        entityType = ContactEntityType.ORGANIZATION,
        primaryDisplayName = "PKO Bank Polski",
        avatarInitials = "PKO",
        isPinned = false,
        manualOrderIndex = 4,
        quickContactSlot = false,
        callsCount = 3,
        lastCallTimestamp = "Dzisiaj, 12:48",
        lastCallEpoch = System.currentTimeMillis() - 1000 * 60 * 45,
        smsCount = 8,
        lastSmsTimestamp = "28 sie",
        lastSmsEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 180,
        otpCount = 7,
        reportsCount = 0,
        suspiciousLinksCount = 0,
        riskScore = 5,
        riskSeverity = RiskSeverity.LOW,
        confidence = ConfidenceLevel.WYSOKA,
        organizationCategory = OrganizationCategory.BANKING_FINANCE,
        knownPhoneNumbers = listOf("+48 800 302 302", "+48 81 535 60 60"),
        smsSenderIds = listOf("PKOBP", "IKO"),
        officialDomains = listOf("pkobp.pl"),
        officialWebsite = "https://www.pkobp.pl",
        contactHours = "Infolinia bankowa: 24h / 7 dni w tygodniu",
        identificationSource = "Rejestr Bankowy KNF (Komisja Nadzoru Finansowego)",
        reputationNote = "Oficjalna infolinia przychodząca. Uwaga: bank nigdy nie prosi o hasła ani kody BLIK w rozmowie przychodzącej."
      ),

      // 6. Anna Kowalczyk (PERSON - Praca / Biuro)
      UnifiedContact(
        id = "contact-anna",
        entityType = ContactEntityType.PERSON,
        primaryDisplayName = "Anna Kowalczyk",
        avatarInitials = "AK",
        isPinned = false,
        manualOrderIndex = 5,
        quickContactSlot = false,
        callsCount = 5,
        lastCallTimestamp = "Wczoraj, 15:30",
        lastCallEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 25,
        smsCount = 9,
        lastSmsTimestamp = "Wczoraj, 16:00",
        lastSmsEpoch = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
        otpCount = 0,
        reportsCount = 0,
        suspiciousLinksCount = 0,
        riskScore = 0,
        riskSeverity = RiskSeverity.NO_KNOWN_RISK,
        confidence = ConfidenceLevel.WYSOKA,
        firstName = "Anna",
        lastName = "Kowalczyk",
        relationship = PersonRelationship.WORK,
        personalPhoneNumbers = listOf("+48 501 789 123"),
        email = "anna.kowalczyk@firma.pl",
        customLabels = listOf("Zespół projektowy", "Kierownik"),
        reputationNote = "Współpracownik z biura"
      )
    )
  )

  private val _unifiedContacts = MutableStateFlow<List<UnifiedContact>>(emptyList())
  override val unifiedContacts: StateFlow<List<UnifiedContact>> = _unifiedContacts.asStateFlow()

  init {
    updateComputedContacts()
  }

  private fun updateComputedContacts() {
    val raw = _rawContacts.value
    val mode = _contactSortMode.value

    // ZASADA 35 & 36:
    // Pinned contacts ZAWSZE na samej górze.
    // Manual order ma pierwszeństwo.
    val sorted = when (mode) {
      ContactSortMode.MANUAL -> {
        raw.sortedWith(
          compareByDescending<UnifiedContact> { it.isPinned }
            .thenBy { it.manualOrderIndex }
        )
      }
      ContactSortMode.MOST_FREQUENT -> {
        raw.sortedWith(
          compareByDescending<UnifiedContact> { it.isPinned }
            .thenByDescending { it.interactionFrequency }
        )
      }
      ContactSortMode.LEAST_FREQUENT -> {
        raw.sortedWith(
          compareByDescending<UnifiedContact> { it.isPinned }
            .thenBy { it.interactionFrequency }
        )
      }
      ContactSortMode.RECENT -> {
        raw.sortedWith(
          compareByDescending<UnifiedContact> { it.isPinned }
            .thenByDescending { it.latestActivityEpoch }
        )
      }
      ContactSortMode.ALPHABETICAL -> {
        raw.sortedWith(
          compareByDescending<UnifiedContact> { it.isPinned }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.primaryDisplayName }
        )
      }
      ContactSortMode.ORGANIZATIONS -> {
        raw.filter { it.entityType == ContactEntityType.ORGANIZATION }
          .sortedWith(
            compareByDescending<UnifiedContact> { it.isPinned }
              .thenBy { it.manualOrderIndex }
          )
      }
      ContactSortMode.PEOPLE -> {
        raw.filter { it.entityType == ContactEntityType.PERSON }
          .sortedWith(
            compareByDescending<UnifiedContact> { it.isPinned }
              .thenBy { it.manualOrderIndex }
          )
      }
    }
    _unifiedContacts.value = sorted
  }

  override fun getContactById(id: String): UnifiedContact? {
    return _rawContacts.value.firstOrNull { it.id == id }
  }

  override fun getContactForAddress(address: String): UnifiedContact? {
    val clean = address.trim().replace(" ", "")
    return _rawContacts.value.firstOrNull { contact ->
      contact.allPhoneNumbers.any { it.replace(" ", "").contains(clean) || clean.contains(it.replace(" ", "")) } ||
      contact.smsSenderIds.any { it.equals(address.trim(), ignoreCase = true) } ||
      contact.primaryDisplayName.equals(address.trim(), ignoreCase = true)
    }
  }

  override fun togglePinContact(contactId: String) {
    _rawContacts.update { list ->
      list.map {
        if (it.id == contactId) it.copy(isPinned = !it.isPinned) else it
      }
    }
    updateComputedContacts()
  }

  override fun toggleQuickContact(contactId: String) {
    _rawContacts.update { list ->
      list.map {
        if (it.id == contactId) it.copy(quickContactSlot = !it.quickContactSlot) else it
      }
    }
    updateComputedContacts()
  }

  override fun setContactSortMode(mode: ContactSortMode) {
    _contactSortMode.value = mode
    updateComputedContacts()
  }

  override fun reorderContactsManual(fromIndex: Int, toIndex: Int) {
    val current = _rawContacts.value.toMutableList()
    if (fromIndex in current.indices && toIndex in current.indices) {
      val item = current.removeAt(fromIndex)
      current.add(toIndex, item)
      // Przepisz manualOrderIndex
      val reindexed = current.mapIndexed { index, contact ->
        contact.copy(manualOrderIndex = index)
      }
      _rawContacts.value = reindexed
      _contactSortMode.value = ContactSortMode.MANUAL
      updateComputedContacts()
    }
  }

  override fun addCustomContact(contact: UnifiedContact) {
    _rawContacts.update { it + contact.copy(manualOrderIndex = it.size) }
    updateComputedContacts()
  }

  override fun dismissSmartSuggestion(contactId: String) {
    _rawContacts.update { list ->
      list.map {
        if (it.id == contactId) it.copy(smartPinSuggestion = null) else it
      }
    }
    updateComputedContacts()
  }
}

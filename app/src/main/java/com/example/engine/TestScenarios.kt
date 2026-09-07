package com.example.engine

import com.example.model.ConfidenceLevel
import com.example.model.NumberIdentity

/**
 * Zestaw 10 kontrolowanych scenariuszy testowych (Test Dataset - SEKCJA 12).
 * Każdy scenariusz definiuje dane wejściowe oraz oczekiwane przedziały (Expected Ranges)
 * dla deterministycznej weryfikacji silnika v1.
 */
object TestScenarios {

    data class ScenarioExpectation(
        val scenarioName: String,
        val description: String,
        val expectedRiskMin: Int,
        val expectedRiskMax: Int,
        val expectedConfidenceLevels: List<ConfidenceLevel>,
        val requiredReasonCodes: List<EngineReasonCode> = emptyList(),
        val forbiddenReasonCodes: List<EngineReasonCode> = emptyList()
    )

    // =========================================================================
    // SCENARIUSZ A: Normalny kurier (DPD / InPost ze zweryfikowanym numerem)
    // =========================================================================
    val scenarioA_Courier = ScenarioExpectation(
        scenarioName = "A. Normalny kurier",
        description = "Oficjalny numer kurierski z historią pozytywnych doręczeń.",
        expectedRiskMin = 0,
        expectedRiskMax = 20,
        expectedConfidenceLevels = listOf(ConfidenceLevel.WYSOKA, ConfidenceLevel.SREDNIA),
        requiredReasonCodes = listOf(EngineReasonCode.PUBLIC_REGISTER_LISTED)
    )

    // =========================================================================
    // SCENARIUSZ B: Agresywny telemarketing (Oferty fotowoltaiki / ankiety)
    // WAŻNE: Telemarketing != Scam! Risk nie może przekroczyć 50 punktów.
    // =========================================================================
    val scenarioB_Telemarketing = ScenarioExpectation(
        scenarioName = "B. Agresywny telemarketing",
        description = "Wielokrotne zgłoszenia ofert handlowych bez cech wyłudzenia.",
        expectedRiskMin = 30,
        expectedRiskMax = 50,
        expectedConfidenceLevels = listOf(ConfidenceLevel.WYSOKA, ConfidenceLevel.SREDNIA),
        requiredReasonCodes = listOf(EngineReasonCode.TELEMARKETING_MASS_CALLS)
    )

    // =========================================================================
    // SCENARIUSZ C: Nowy numer z zaledwie 2 raportami
    // =========================================================================
    val scenarioC_NewNumberFewReports = ScenarioExpectation(
        scenarioName = "C. Nowy numer z 2 raportami",
        description = "Pojedyncze zgłoszenia, zbyt mało danych na wysoką pewność.",
        expectedRiskMin = 20,
        expectedRiskMax = 50,
        expectedConfidenceLevels = listOf(ConfidenceLevel.NISKA)
    )

    // =========================================================================
    // SCENARIUSZ D: Kampania vishingowa z 50 świeżymi raportami
    // =========================================================================
    val scenarioD_VishingCampaign = ScenarioExpectation(
        scenarioName = "D. Kampania vishingowa (50 raportów)",
        description = "Zmasowana próba wyłudzenia danych z wysoką zgodnością źródeł.",
        expectedRiskMin = 80,
        expectedRiskMax = 100,
        expectedConfidenceLevels = listOf(ConfidenceLevel.WYSOKA),
        requiredReasonCodes = listOf(EngineReasonCode.SCAM_VISHING_REPORTS)
    )

    // =========================================================================
    // SCENARIUSZ E: Stary numer z raportami sprzed 8 miesięcy (Time Decay)
    // =========================================================================
    val scenarioE_OldDecayedReports = ScenarioExpectation(
        scenarioName = "E. Stary numer (raporty sprzed 8 miesięcy)",
        description = "Wygaszone zgłoszenia w czasie, brak świeżej aktywności.",
        expectedRiskMin = 10,
        expectedRiskMax = 45,
        expectedConfidenceLevels = listOf(ConfidenceLevel.NISKA, ConfidenceLevel.SREDNIA),
        requiredReasonCodes = listOf(EngineReasonCode.HISTORICAL_DATA_DECAYED)
    )

    // =========================================================================
    // SCENARIUSZ F: Oficjalna infolinia banku przy połączeniu przychodzącym (Spoofing)
    // =========================================================================
    val scenarioF_BankInboundSpoof = ScenarioExpectation(
        scenarioName = "F. Infolinia bankowa przy połączeniu przychodzącym",
        description = "Oficjalny numer infolinii bankowej dzwoniący do klienta (CLI Spoofing).",
        expectedRiskMin = 50,
        expectedRiskMax = 95,
        expectedConfidenceLevels = listOf(ConfidenceLevel.WYSOKA, ConfidenceLevel.SREDNIA),
        requiredReasonCodes = listOf(EngineReasonCode.HOTLINE_CLI_SPOOF_RISK)
    )

    // =========================================================================
    // SCENARIUSZ G: Fala review-bombingu (Anomalia zgłoszeń)
    // =========================================================================
    val scenarioG_ReviewBombing = ScenarioExpectation(
        scenarioName = "G. Atak typu Review-Bombing",
        description = "Nienaturalny skok zgłoszeń w krótkim czasie. Confidence zablokowane.",
        expectedRiskMin = 30,
        expectedRiskMax = 75,
        expectedConfidenceLevels = listOf(ConfidenceLevel.SREDNIA),
        requiredReasonCodes = listOf(EngineReasonCode.ANOMALOUS_REPORTING_SURGE)
    )

    // =========================================================================
    // SCENARIUSZ H: Podejrzany SMS z linkiem phishingowym (np. fałszywy InPost)
    // =========================================================================
    val scenarioH_SmishingPhishingLink = ScenarioExpectation(
        scenarioName = "H. Smishing z fałszywym linkiem",
        description = "SMS o dopłacie z linkiem w niespójnej domenie.",
        expectedRiskMin = 75,
        expectedRiskMax = 100,
        expectedConfidenceLevels = listOf(ConfidenceLevel.WYSOKA, ConfidenceLevel.SREDNIA),
        requiredReasonCodes = listOf(EngineReasonCode.SMS_BRAND_DOMAIN_MISMATCH)
    )

    // =========================================================================
    // SCENARIUSZ I: Kod OTP od rozpoznanej marki (Google / mBank)
    // =========================================================================
    val scenarioI_LegitimateOtp = ScenarioExpectation(
        scenarioName = "I. Legalny kod autoryzacyjny OTP",
        description = "Standardowy kod logowania SMS od znanej instytucji.",
        expectedRiskMin = 0,
        expectedRiskMax = 15,
        expectedConfidenceLevels = listOf(ConfidenceLevel.WYSOKA, ConfidenceLevel.SREDNIA)
    )

    // =========================================================================
    // SCENARIUSZ J: Numer bez jakichkolwiek danych (Clean Slate)
    // =========================================================================
    val scenarioJ_UnknownCleanSlate = ScenarioExpectation(
        scenarioName = "J. Numer bez żadnych danych",
        description = "Brak sygnałów i wpisów w bazie. Niskie ryzyko, niska pewność.",
        expectedRiskMin = 0,
        expectedRiskMax = 10,
        expectedConfidenceLevels = listOf(ConfidenceLevel.NISKA),
        requiredReasonCodes = listOf(EngineReasonCode.NO_COMMUNITY_SIGNALS)
    )

    val allScenarios = listOf(
        scenarioA_Courier,
        scenarioB_Telemarketing,
        scenarioC_NewNumberFewReports,
        scenarioD_VishingCampaign,
        scenarioE_OldDecayedReports,
        scenarioF_BankInboundSpoof,
        scenarioG_ReviewBombing,
        scenarioH_SmishingPhishingLink,
        scenarioI_LegitimateOtp,
        scenarioJ_UnknownCleanSlate
    )
}

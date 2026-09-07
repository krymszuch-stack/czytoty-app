package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.conversation.safety.analyzer.ConversationRiskAnalyzer
import com.example.conversation.safety.diarization.SpeakerEstimator
import com.example.conversation.safety.extractor.SafetySignalExtractor
import com.example.conversation.safety.model.TranscriptResult
import com.example.conversation.safety.model.TranscriptStatus
import com.example.conversation.safety.redaction.TranscriptRedactor
import com.example.safetycheck.detector.DistressSignalDetector
import com.example.safetycheck.model.DistressConfidence
import com.example.safetycheck.model.DistressSignalType
import com.example.engine.CommunicationSignal
import com.example.engine.ConfidenceEngineV1
import com.example.engine.EngineConfig
import com.example.engine.IdentityEngine
import com.example.engine.ReputationCategory
import com.example.engine.ReputationEngineV1
import com.example.engine.RiskEngineV1
import com.example.engine.SignalChannel
import com.example.engine.SignalProvenance
import com.example.engine.SourceType
import com.example.engine.TimeDecayEngine
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Ekran diagnostyczny dewelopera (Engine Debug Screen - SEKCJA 22).
 * Dostępny w kompilacji debug. Prezentuje surowe sygnały, wyliczone wagi,
 * decay czasowy, pośrednie kalkulacje i wygenerowane kody przyczyn.
 */
@Composable
fun EngineDebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var queryNumber by remember { mutableStateOf("+48 732 123 456") }
    var selectedScenarioIndex by remember { mutableStateOf(0) }
    var selectedConvTestCase by remember { mutableStateOf(0) }

    val convTestCases = listOf(
        "A: Kurier (Low Risk)" to "Dzień dobry, kurier, będę za pięć minut.",
        "B: Prośba o BLIK (High Risk)" to "Podaj proszę kod BLIK 592 103.",
        "C: Negacja BLIK (Not Scam)" to "Bank nigdy nie poprosi Cię o kod BLIK.",
        "D: AnyDesk (Very High)" to "Zainstaluj AnyDesk, pomogę zabezpieczyć konto.",
        "E: Konto techniczne (Very High)" to "Proszę przelać środki na konto techniczne 12 1020 3040 0000 1234 5678 9012.",
        "F: Hałas / Niezrozumiały (Unknown)" to ""
    )

    var selectedDistressScenarioIndex by remember { mutableStateOf(0) }
    val distressScenarios = listOf(
        "1. TV / Serial" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 4000L, rmsEnergy = 0.45f, peakEnergy = 0.60f, transcriptText = "W poprzednim odcinku naszego programu rozmawialiśmy o pogodzie."
        ),
        "2. Film akcji" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 900L, rmsEnergy = 0.90f, peakEnergy = 0.98f, isSingleImpulse = true, transcriptText = "Bum! Eksplozja w tle."
        ),
        "3. Głośna muzyka" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 5000L, rmsEnergy = 0.80f, peakEnergy = 0.88f, isRhythmicMusic = true, transcriptText = "La la la rytmiczny bas gra głośno."
        ),
        "4. Dziecko (Zabawa)" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 3000L, rmsEnergy = 0.70f, peakEnergy = 0.85f, isChildPlayfulSpeech = true, transcriptText = "Haha, patrz mamo jak wysoko skaczę!"
        ),
        "5. Pies (Szczekanie)" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 2500L, rmsEnergy = 0.75f, peakEnergy = 0.90f, isDogBarkingCadence = true, transcriptText = "Hau hau hau!"
        ),
        "6. Trening (Siłownia)" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 3500L, rmsEnergy = 0.65f, peakEnergy = 0.80f, transcriptText = "Uch, ciężar! Jeszcze jedno powtórzenie, raz, dwa, trzy!"
        ),
        "7. Impreza (Toast)" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 4500L, rmsEnergy = 0.70f, peakEnergy = 0.82f, transcriptText = "Wznieśmy toast, na zdrowie, wspaniała impreza!"
        ),
        "8. Kłótnia" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 3500L, rmsEnergy = 0.75f, peakEnergy = 0.85f, transcriptText = "Nie zgadzam się z tobą, przestań gadać bzdury, kłamiesz!"
        ),
        "9. Pomoc (REAL)" to DistressSignalDetector.AudioEnvironmentContext(
            durationMs = 3200L, rmsEnergy = 0.88f, peakEnergy = 0.96f, transcriptText = "Pomocy! Ratunku! Zostaw mnie, niech ktoś pomoże!"
        )
    )

    val scenarios = listOf(
        "Scam Call Campaign" to listOf(
            CommunicationSignal("1", SignalChannel.CALL, queryNumber, ReputationCategory.SUSPECTED_SCAM, SignalProvenance(SourceType.COMMUNITY, "u-1", System.currentTimeMillis() - 3600000)),
            CommunicationSignal("2", SignalChannel.CALL, queryNumber, ReputationCategory.SUSPECTED_SCAM, SignalProvenance(SourceType.COMMUNITY, "u-2", System.currentTimeMillis() - 7200000)),
            CommunicationSignal("3", SignalChannel.CALL, queryNumber, ReputationCategory.IMPERSONATION, SignalProvenance(SourceType.CALL_EVENT, "d-1", System.currentTimeMillis() - 1800000))
        ),
        "Telemarketing Offer" to listOf(
            CommunicationSignal("1", SignalChannel.CALL, queryNumber, ReputationCategory.TELEMARKETING, SignalProvenance(SourceType.COMMUNITY, "u-1", System.currentTimeMillis() - 10000000)),
            CommunicationSignal("2", SignalChannel.CALL, queryNumber, ReputationCategory.TELEMARKETING, SignalProvenance(SourceType.COMMUNITY, "u-2", System.currentTimeMillis() - 20000000))
        ),
        "Smishing with Phishing Link" to listOf(
            CommunicationSignal("1", SignalChannel.SMS, queryNumber, ReputationCategory.IMPERSONATION, SignalProvenance(SourceType.SMS_EVENT, "sms-1", System.currentTimeMillis()), hasUrgentPaymentKeywords = true, hasBrandMismatch = true, hasShortenerUrl = true, urlThreatScore = 85)
        ),
        "Decayed Historical Data" to listOf(
            CommunicationSignal("1", SignalChannel.CALL, queryNumber, ReputationCategory.SUSPECTED_SCAM, SignalProvenance(SourceType.COMMUNITY, "u-1", System.currentTimeMillis() - (150L * 24 * 3600 * 1000)))
        )
    )

    val currentSignals = scenarios[selectedScenarioIndex].second

    // Obliczenia silnika na żywo
    val now = System.currentTimeMillis()
    val identity = remember(queryNumber) { IdentityEngine.resolveIdentity(queryNumber) }
    val reputation = remember(currentSignals, now) { ReputationEngineV1.evaluateReputation(currentSignals, now) }
    val risk = remember(identity, reputation, currentSignals, now) {
        RiskEngineV1.assessRisk(identity, reputation, historicalSignals = currentSignals, currentEpoch = now)
    }

    Scaffold(
        containerColor = AppDarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = AppTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = AppPurpleLight)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Engine Inspector (Debug)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppTextPrimary)
                    Text(text = "Engine Version: ${EngineConfig.ENGINE_VERSION}", fontSize = 11.sp, color = AppPurpleLight, fontFamily = FontFamily.Monospace)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Pole edycji numeru
            OutlinedTextField(
                value = queryNumber,
                onValueChange = { queryNumber = it },
                label = { Text("Numer do analizy") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPurple,
                    unfocusedBorderColor = Color(0x33A875FF),
                    focusedTextColor = AppTextPrimary,
                    unfocusedTextColor = AppTextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("debug_query_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Przełącznik scenariuszy
            Text(text = "Wybierz scenariusz wejściowy:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.forEachIndexed { idx, (name, _) ->
                    val isSelected = selectedScenarioIndex == idx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AppPurpleContainer else AppSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AppPurple else Color(0x22A875FF)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedScenarioIndex = idx }
                    ) {
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AppPurpleLight else AppTextSecondary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. KARTA WYNIKÓW (IDENTITY, REPUTATION, RISK, CONFIDENCE)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "PODSUMOWANIE 4 FILARÓW", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppPurpleLight, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    DebugMetricRow("1. Identity", "${identity.entityType} | Auth: ${identity.isCallerIdentityAuthenticated}")
                    DebugMetricRow("2. Reputation", "${reputation.dominantCategory} (${reputation.positivePercent}% pos, ${reputation.negativePercent}% neg)")
                    DebugMetricRow("3. Risk Score", "${risk.riskScore} / 100 (${risk.riskLevel})", if (risk.riskScore > 50) AppRed else AppGreen)
                    DebugMetricRow("4. Confidence", "${risk.confidence.level} (internal: ${"%.2f".format(risk.confidence.internalScore)})")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. KODY PRZYCZYN (ENGINE REASONS)
            Text(text = "Wygenerowane kody przyczyn (Engine Reasons):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            risk.reasons.forEach { reason ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = reason.code.name, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AppPurpleLight)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = reason.impact.name, fontSize = 9.sp, color = AppTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = reason.localizedDescription, fontSize = 12.sp, color = AppTextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. PARAMETRY KONFIGURACJI I WAGI
            Text(text = "Wagi i parametry (INITIAL_CALIBRATION):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("RiskWeights.CATEGORY_SEVERITY: ${EngineConfig.RiskWeights.CATEGORY_SEVERITY_WEIGHT}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppTextSecondary)
                    Text("RiskWeights.REPORT_VOLUME: ${EngineConfig.RiskWeights.REPORT_VOLUME_WEIGHT}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppTextSecondary)
                    Text("RiskWeights.RECENCY_24H: ${EngineConfig.RiskWeights.RECENCY_24H_WEIGHT}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppTextSecondary)
                    Text("DecayConfig.HALF_LIFE_DAYS: ${EngineConfig.DecayConfig.HALF_LIFE_DAYS} dni", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppTextSecondary)
                    Text("ThresholdConfig.RISK_HIGH: >= 75", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppTextSecondary)
                    Text("ThresholdConfig.CONFIDENCE_HIGH: > 0.70", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. CONVERSATION SAFETY SNAPSHOT INSPECTOR (Test Cases A–F)
            Text(text = "Conversation Safety Snapshot (Cases A-F):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                convTestCases.forEachIndexed { index, (label, _) ->
                    val isSelected = selectedConvTestCase == index
                    val shortLabel = label.substringBefore(":")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AppPurpleContainer else AppSurface,
                        modifier = Modifier
                            .clickable { selectedConvTestCase = index }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = shortLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AppPurpleLight else AppTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val currentTestCase = convTestCases[selectedConvTestCase]
            val rawTranscript = currentTestCase.second
            val isNoise = selectedConvTestCase == 5
            val transcriptResult = TranscriptResult(
                text = rawTranscript,
                status = if (isNoise) TranscriptStatus.NO_SPEECH else TranscriptStatus.SUCCESS,
                confidence = if (isNoise) 0.15f else 0.95f
            )
            val redactedTranscript = TranscriptRedactor.redact(rawTranscript)
            val detectedSignals = SafetySignalExtractor.extractSignals(rawTranscript)
            val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
            val convRisk = ConversationRiskAnalyzer.analyze(
                transcriptResult = transcriptResult,
                speakerEstimate = speakerEstimate,
                signals = detectedSignals,
                processingTimeMs = 8L
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Wybrany przypadek: ${currentTestCase.first}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppPurpleLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    DebugMetricRow("audio source", "RAM ONLY (PCM 16kHz - Auto Wipe)")
                    DebugMetricRow("raw transcript", if (rawTranscript.isBlank()) "(brak / hałas)" else rawTranscript)
                    DebugMetricRow("redacted transcript", if (redactedTranscript.isBlank()) "(brak)" else redactedTranscript)
                    DebugMetricRow("speaker estimate", "${speakerEstimate.estimatedSpeakers.label} (${speakerEstimate.confidence})")
                    DebugMetricRow(
                        "detected signals",
                        if (detectedSignals.isEmpty()) "brak" else detectedSignals.joinToString { it.code.name },
                        if (detectedSignals.isNotEmpty()) AppRed else AppGreen
                    )
                    if (detectedSignals.isNotEmpty()) {
                        DebugMetricRow("signal weights", detectedSignals.joinToString { "${it.code.name}: ${it.weight}" })
                    }
                    DebugMetricRow(
                        "conversation risk",
                        "${convRisk.riskScore} / 100 (${convRisk.riskLevel})",
                        if (convRisk.riskScore > 50) AppRed else AppGreen
                    )
                    DebugMetricRow("confidence", "${convRisk.confidence.level} (internal: ${"%.2f".format(convRisk.confidence.internalScore)})")
                    DebugMetricRow("processing time", "${convRisk.processingTimeMs} ms (on-device local)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Wyjaśnienie / Powody:", fontSize = 11.sp, color = AppTextSecondary)
                    convRisk.reasons.forEach { r ->
                        Text("• $r", fontSize = 11.sp, color = AppTextPrimary, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. SAFETY CHECK / DISTRESS INSPECTOR (9 Scenariuszy)
            Text(text = "Safety Check / Distress Inspector (9 Scenariuszy):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    distressScenarios.take(5).forEachIndexed { index, (label, _) ->
                        val isSelected = selectedDistressScenarioIndex == index
                        val shortLabel = label.substringBefore(" ")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppPurpleContainer else AppSurface,
                            modifier = Modifier.clickable { selectedDistressScenarioIndex = index }
                        ) {
                            Text(
                                text = shortLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppPurpleLight else AppTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    distressScenarios.drop(5).forEachIndexed { index, (label, _) ->
                        val actualIndex = index + 5
                        val isSelected = selectedDistressScenarioIndex == actualIndex
                        val shortLabel = label.substringBefore(" ")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppPurpleContainer else AppSurface,
                            modifier = Modifier.clickable { selectedDistressScenarioIndex = actualIndex }
                        ) {
                            Text(
                                text = shortLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppPurpleLight else AppTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val currentDistressScenario = distressScenarios[selectedDistressScenarioIndex]
            val distressContext = currentDistressScenario.second
            val detectedDistressSignal = DistressSignalDetector.evaluate(distressContext)
            val isTriggered = detectedDistressSignal != null && detectedDistressSignal.confidence != DistressConfidence.LOW

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Scenariusz: ${currentDistressScenario.first}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppPurpleLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    DebugMetricRow("audio events", if (detectedDistressSignal == null) "Normalne tło akustyczne" else detectedDistressSignal.type.displayName)
                    DebugMetricRow("confidence", detectedDistressSignal?.confidence?.name ?: "LOW (Poniżej progu)")
                    DebugMetricRow("trigger threshold", "Wymaga >= 2.2s oraz Confidence >= MEDIUM")
                    DebugMetricRow(
                        "trigger status",
                        if (isTriggered) "TRIGGER AKTYWNY (Wyświetlono dialog)" else "ODRZUCONY (Brak alarmu)",
                        if (isTriggered) AppRed else AppGreen
                    )
                    DebugMetricRow("countdown", if (isTriggered) "10s -> Auto-alert w razie braku odpowiedzi" else "Bezczynny (Idle)")
                    DebugMetricRow("location status", "Pobierana asynchronicznie (Mock GPS: 52.2297, 21.0122)")
                    DebugMetricRow("alert status", if (isTriggered) "W gotowości do wysyłki (Trusted Contacts)" else "Brak wysyłki alertu")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Gwarancja prywatności: Zero zapisu audio w pamięci i w bazie danych.", fontSize = 10.sp, color = AppTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DebugMetricRow(label: String, value: String, valueColor: Color = AppTextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = AppTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
    }
}

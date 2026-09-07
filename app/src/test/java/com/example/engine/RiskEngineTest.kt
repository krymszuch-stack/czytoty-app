package com.example.engine

import com.example.model.ConfidenceLevel
import com.example.model.NumberIdentity
import com.example.sap.SapBusinessPartner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    private val now = System.currentTimeMillis()

    @Test
    fun `Scenario A - Normal courier has low risk and public directory identity`() {
        val courierNumber = "+48 22 577 55 55"
        val identity = IdentityEngine.resolveIdentity(
            phoneNumber = courierNumber,
            numberIdentity = NumberIdentity(
                isPublicDirectoryListed = true,
                registeredEntityName = "DPD Polska",
                entityCategory = "Kurier / Logistyka",
                isFinancialInstitution = false,
                isInboundOnlyHotline = false
            )
        )

        val signals = listOf(
            CommunicationSignal(
                id = "sig-1",
                channel = SignalChannel.CALL,
                phoneNumber = courierNumber,
                category = ReputationCategory.OK,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-1", now - 3600000)
            ),
            CommunicationSignal(
                id = "sig-2",
                channel = SignalChannel.CALL,
                phoneNumber = courierNumber,
                category = ReputationCategory.OK,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-2", now - 7200000)
            )
        )

        val reputation = ReputationEngineV1.evaluateReputation(signals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = signals, currentEpoch = now)

        assertTrue("Risk score should be <= 20, was ${risk.riskScore}", risk.riskScore <= 20)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.PUBLIC_REGISTER_LISTED })
    }

    @Test
    fun `Scenario B - Aggressive telemarketing does not exceed moderate risk (telemarketing is not scam)`() {
        val telemarketingNumber = "+48 22 307 88 12"
        val identity = IdentityEngine.resolveIdentity(telemarketingNumber)

        val signals = (1..15).map { i ->
            CommunicationSignal(
                id = "tele-$i",
                channel = SignalChannel.CALL,
                phoneNumber = telemarketingNumber,
                category = ReputationCategory.TELEMARKETING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-$i", now - (i * 3600000L))
            )
        }

        val reputation = ReputationEngineV1.evaluateReputation(signals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = signals, currentEpoch = now)

        assertEquals(ReputationCategory.TELEMARKETING, reputation.dominantCategory)
        assertTrue("Telemarketing risk should be between 30 and 50, was ${risk.riskScore}", risk.riskScore in 30..50)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.TELEMARKETING_MASS_CALLS })
    }

    @Test
    fun `Scenario C - New number with few reports has LOW confidence`() {
        val newNumber = "+48 500 111 222"
        val identity = IdentityEngine.resolveIdentity(newNumber)

        val signals = listOf(
            CommunicationSignal(
                id = "sig-1",
                channel = SignalChannel.CALL,
                phoneNumber = newNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "u-1", now - 3600000)
            ),
            CommunicationSignal(
                id = "sig-2",
                channel = SignalChannel.CALL,
                phoneNumber = newNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "u-2", now - 7200000)
            )
        )

        val reputation = ReputationEngineV1.evaluateReputation(signals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = signals, currentEpoch = now)

        assertEquals(ConfidenceLevel.NISKA, risk.confidence.level)
    }

    @Test
    fun `Scenario D - Massive vishing campaign triggers HIGH risk and HIGH confidence`() {
        val scamNumber = "+48 732 123 456"
        val identity = IdentityEngine.resolveIdentity(scamNumber)

        val signals = (1..35).map { i ->
            CommunicationSignal(
                id = "scam-$i",
                channel = SignalChannel.CALL,
                phoneNumber = scamNumber,
                category = ReputationCategory.SUSPECTED_SCAM,
                provenance = SignalProvenance(
                    sourceType = if (i % 2 == 0) SourceType.COMMUNITY else SourceType.CALL_EVENT,
                    sourceId = "reporter-$i",
                    timestampEpoch = now - (i * 4 * 3600000L),
                    reporterTrustScore = 0.8f
                )
            )
        }

        val reputation = ReputationEngineV1.evaluateReputation(signals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = signals, currentEpoch = now)

        assertTrue("Vishing risk should be >= 80, was ${risk.riskScore}", risk.riskScore >= 80)
        assertEquals(ConfidenceLevel.WYSOKA, risk.confidence.level)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.SCAM_VISHING_REPORTS })
    }

    @Test
    fun `Scenario E - Time decay significantly reduces risk of 8-month-old reports`() {
        val oldNumber = "+48 601 222 333"
        val identity = IdentityEngine.resolveIdentity(oldNumber)
        val eightMonthsAgo = now - (240L * 24L * 60L * 60L * 1000L)

        val oldSignals = (1..10).map { i ->
            CommunicationSignal(
                id = "old-$i",
                channel = SignalChannel.CALL,
                phoneNumber = oldNumber,
                category = ReputationCategory.SUSPECTED_SCAM,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-$i", eightMonthsAgo)
            )
        }

        val reputation = ReputationEngineV1.evaluateReputation(oldSignals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = oldSignals, currentEpoch = now)

        assertTrue("Old reports risk should be dampened below 50, was ${risk.riskScore}", risk.riskScore < 50)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.HISTORICAL_DATA_DECAYED })
    }

    @Test
    fun `Scenario F - Bank inbound-only hotline on incoming call triggers CLI spoof risk`() {
        val bankHotline = "+48 22 521 80 00"
        val identity = IdentityEngine.resolveIdentity(
            phoneNumber = bankHotline,
            numberIdentity = NumberIdentity(
                isPublicDirectoryListed = true,
                registeredEntityName = "PKO Bank Polski",
                isFinancialInstitution = true,
                isInboundOnlyHotline = true
            )
        )

        val signals = listOf(
            CommunicationSignal(
                id = "spoof-1",
                channel = SignalChannel.CALL,
                phoneNumber = bankHotline,
                category = ReputationCategory.IMPERSONATION,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-1", now - 1800000)
            )
        )

        val activeInboundCall = CommunicationSignal(
            id = "active-call",
            channel = SignalChannel.CALL,
            phoneNumber = bankHotline,
            category = ReputationCategory.IMPERSONATION,
            provenance = SignalProvenance(SourceType.CALL_EVENT, "device-1", now)
        )

        val reputation = ReputationEngineV1.evaluateReputation(signals, now)
        val risk = RiskEngineV1.assessRisk(
            identity = identity,
            reputation = reputation,
            activeSignal = activeInboundCall,
            historicalSignals = signals,
            currentEpoch = now
        )

        assertTrue("Inbound bank hotline receiving call should have risk >= 50, was ${risk.riskScore}", risk.riskScore >= 50)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.HOTLINE_CLI_SPOOF_RISK })
    }

    @Test
    fun `Scenario G - Review bombing triggers anomaly detection and caps confidence`() {
        val attackedNumber = "+48 512 000 999"
        val identity = IdentityEngine.resolveIdentity(attackedNumber)

        // 12 zgłoszeń w ciągu ostatnich 30 minut bez żadnej wcześniejszej historii
        val spikeSignals = (1..12).map { i ->
            CommunicationSignal(
                id = "spike-$i",
                channel = SignalChannel.CALL,
                phoneNumber = attackedNumber,
                category = ReputationCategory.SUSPECTED_SCAM,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-$i", now - (i * 60000L))
            )
        }

        val reputation = ReputationEngineV1.evaluateReputation(spikeSignals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = spikeSignals, currentEpoch = now)

        assertTrue("Anomaly should be detected", reputation.isAnomalyDetected)
        assertEquals("Confidence must be capped to SREDNIA during anomaly", ConfidenceLevel.SREDNIA, risk.confidence.level)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.ANOMALOUS_REPORTING_SURGE })
    }

    @Test
    fun `Scenario H - Phishing SMS with domain mismatch triggers HIGH risk`() {
        val smishingSender = "InPost"
        val identity = IdentityEngine.resolveIdentity(smishingSender)

        val smsSignal = CommunicationSignal(
            id = "sms-phish",
            channel = SignalChannel.SMS,
            phoneNumber = smishingSender,
            category = ReputationCategory.IMPERSONATION,
            provenance = SignalProvenance(SourceType.SMS_EVENT, "device-sms", now),
            hasUrgentPaymentKeywords = true,
            hasBrandMismatch = true,
            hasShortenerUrl = true,
            urlThreatScore = 90
        )

        val reputation = ReputationEngineV1.evaluateReputation(listOf(smsSignal), now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, activeSignal = smsSignal, currentEpoch = now)

        assertTrue("Smishing risk should be >= 75, was ${risk.riskScore}", risk.riskScore >= 75)
        assertEquals(RiskLevel.HIGH, risk.riskLevel)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.SMS_BRAND_DOMAIN_MISMATCH })
    }

    @Test
    fun `Scenario I - Legitimate OTP SMS has LOW risk`() {
        val otpSender = "mBank"
        val identity = IdentityEngine.resolveIdentity(otpSender)

        val otpSignal = CommunicationSignal(
            id = "sms-otp",
            channel = SignalChannel.SMS,
            phoneNumber = otpSender,
            category = ReputationCategory.OK,
            provenance = SignalProvenance(SourceType.BRAND_DIRECTORY, "brand-mbank", now),
            hasBlikKeywords = false,
            hasUrgentPaymentKeywords = false
        )

        val reputation = ReputationEngineV1.evaluateReputation(listOf(otpSignal), now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, activeSignal = otpSignal, currentEpoch = now)

        assertTrue("Legitimate OTP risk should be <= 15, was ${risk.riskScore}", risk.riskScore <= 15)
        assertEquals(RiskLevel.LOW, risk.riskLevel)
    }

    @Test
    fun `Scenario J - Clean slate number with zero reports has LOW risk and LOW confidence`() {
        val unknownNumber = "+48 699 888 777"
        val identity = IdentityEngine.resolveIdentity(unknownNumber)
        val emptySignals = emptyList<CommunicationSignal>()

        val reputation = ReputationEngineV1.evaluateReputation(emptySignals, now)
        val risk = RiskEngineV1.assessRisk(identity, reputation, historicalSignals = emptySignals, currentEpoch = now)

        assertTrue("Clean slate risk should be <= 10, was ${risk.riskScore}", risk.riskScore <= 10)
        assertEquals(ConfidenceLevel.NISKA, risk.confidence.level)
        assertTrue(risk.reasons.any { it.code == EngineReasonCode.NO_COMMUNITY_SIGNALS })
    }
}

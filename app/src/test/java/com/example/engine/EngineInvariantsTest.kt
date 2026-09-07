package com.example.engine

import com.example.model.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy niezmienników i własności logicznych silnika (Property Tests / Invariants - SEKCJA 14).
 */
class EngineInvariantsTest {

    private val now = System.currentTimeMillis()

    @Test
    fun `Invariant 1 - Adding a fresh confirmed scam report never decreases Risk`() {
        val testNumber = "+48 700 800 900"
        val identity = IdentityEngine.resolveIdentity(testNumber)

        val initialSignals = listOf(
            CommunicationSignal(
                id = "sig-1",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-1", now - 3600000)
            )
        )

        val rep1 = ReputationEngineV1.evaluateReputation(initialSignals, now)
        val risk1 = RiskEngineV1.assessRisk(identity, rep1, historicalSignals = initialSignals, currentEpoch = now)

        val updatedSignals = initialSignals + CommunicationSignal(
            id = "sig-2",
            channel = SignalChannel.CALL,
            phoneNumber = testNumber,
            category = ReputationCategory.SUSPECTED_SCAM,
            provenance = SignalProvenance(SourceType.COMMUNITY, "user-2", now, reporterTrustScore = 0.9f)
        )

        val rep2 = ReputationEngineV1.evaluateReputation(updatedSignals, now)
        val risk2 = RiskEngineV1.assessRisk(identity, rep2, historicalSignals = updatedSignals, currentEpoch = now)

        assertTrue(
            "Risk after adding scam report (${risk2.riskScore}) must be >= initial risk (${risk1.riskScore})",
            risk2.riskScore >= risk1.riskScore
        )
    }

    @Test
    fun `Invariant 2 - Time aging of signals never increases their impact on risk`() {
        val testNumber = "+48 700 111 222"
        val identity = IdentityEngine.resolveIdentity(testNumber)

        val freshSignals = (1..5).map { i ->
            CommunicationSignal(
                id = "sig-$i",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = ReputationCategory.SUSPECTED_SCAM,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-$i", now - (i * 3600000L))
            )
        }

        val repFresh = ReputationEngineV1.evaluateReputation(freshSignals, now)
        val riskFresh = RiskEngineV1.assessRisk(identity, repFresh, historicalSignals = freshSignals, currentEpoch = now)

        val sixMonthsLaterEpoch = now + (180L * 24L * 60L * 60L * 1000L)
        val repAged = ReputationEngineV1.evaluateReputation(freshSignals, sixMonthsLaterEpoch)
        val riskAged = RiskEngineV1.assessRisk(identity, repAged, historicalSignals = freshSignals, currentEpoch = sixMonthsLaterEpoch)

        assertTrue(
            "Risk after 6 months (${riskAged.riskScore}) must be <= fresh risk (${riskFresh.riskScore})",
            riskAged.riskScore <= riskFresh.riskScore
        )
    }

    @Test
    fun `Invariant 3 - Increasing independent sources increases or maintains Confidence`() {
        val testNumber = "+48 600 500 400"
        val identity = IdentityEngine.resolveIdentity(testNumber)

        val singleSourceSignals = (1..6).map { i ->
            CommunicationSignal(
                id = "s-$i",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-$i", now - 3600000)
            )
        }

        val conf1 = ConfidenceEngineV1.evaluateConfidence(singleSourceSignals, identity, false)

        val multiSourceSignals = singleSourceSignals + listOf(
            CommunicationSignal(
                id = "s-call",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.CALL_EVENT, "device-1", now - 1800000)
            ),
            CommunicationSignal(
                id = "s-sms",
                channel = SignalChannel.SMS,
                phoneNumber = testNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.SMS_EVENT, "device-1", now - 900000)
            ),
            CommunicationSignal(
                id = "s-feed",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.SECURITY_FEED, "cert-feed", now - 600000)
            )
        )

        val conf2 = ConfidenceEngineV1.evaluateConfidence(multiSourceSignals, identity, false)

        assertTrue(
            "Confidence with multiple sources (${conf2.internalScore}) must be >= single source (${conf1.internalScore})",
            conf2.internalScore >= conf1.internalScore
        )
    }

    @Test
    fun `Invariant 4 - Pure telemarketing never reaches high risk of vishing`() {
        val telemarketingNumber = "+48 22 111 22 33"
        val vishingNumber = "+48 22 999 88 77"

        val teleSignals = (1..20).map { i ->
            CommunicationSignal(
                id = "t-$i",
                channel = SignalChannel.CALL,
                phoneNumber = telemarketingNumber,
                category = ReputationCategory.TELEMARKETING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "u-$i", now - (i * 3600000L))
            )
        }

        val vishingSignals = (1..20).map { i ->
            CommunicationSignal(
                id = "v-$i",
                channel = SignalChannel.CALL,
                phoneNumber = vishingNumber,
                category = ReputationCategory.SUSPECTED_SCAM,
                provenance = SignalProvenance(SourceType.COMMUNITY, "u-$i", now - (i * 3600000L))
            )
        }

        val repTele = ReputationEngineV1.evaluateReputation(teleSignals, now)
        val riskTele = RiskEngineV1.assessRisk(IdentityEngine.resolveIdentity(telemarketingNumber), repTele, historicalSignals = teleSignals, currentEpoch = now)

        val repVishing = ReputationEngineV1.evaluateReputation(vishingSignals, now)
        val riskVishing = RiskEngineV1.assessRisk(IdentityEngine.resolveIdentity(vishingNumber), repVishing, historicalSignals = vishingSignals, currentEpoch = now)

        assertTrue("Telemarketing risk must be strictly < Vishing risk", riskTele.riskScore < riskVishing.riskScore)
        assertTrue("Telemarketing risk must not exceed 50, was ${riskTele.riskScore}", riskTele.riskScore <= 50)
        assertTrue("Vishing risk must be >= 75, was ${riskVishing.riskScore}", riskVishing.riskScore >= 75)
    }

    @Test
    fun `Invariant 5 - Positive community reputation does NOT guarantee verified caller identity`() {
        val testNumber = "+48 501 234 567"
        val identity = IdentityEngine.resolveIdentity(testNumber)

        val positiveSignals = (1..20).map { i ->
            CommunicationSignal(
                id = "pos-$i",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = ReputationCategory.OK,
                provenance = SignalProvenance(SourceType.COMMUNITY, "u-$i", now - 3600000)
            )
        }

        val rep = ReputationEngineV1.evaluateReputation(positiveSignals, now)

        assertEquals(100, rep.positivePercent)
        // Krytyczna zasada: Zaufana reputacja społeczności != weryfikacja kryptograficzna rozmówcy w sieci GSM
        assertFalse(
            "GSM caller identity must not be marked authenticated simply because community votes OK",
            identity.isCallerIdentityAuthenticated
        )
    }

    @Test
    fun `Invariant 6 - Deduplication mutates previous user vote instead of stacking multiple votes`() {
        val testNumber = "+48 500 000 001"

        // Użytkownik "user-A" wysyła 5 ocen pod rząd dla tego samego numeru
        val userRepeatedSignals = (1..5).map { i ->
            CommunicationSignal(
                id = "userA-vote-$i",
                channel = SignalChannel.CALL,
                phoneNumber = testNumber,
                category = if (i == 5) ReputationCategory.OK else ReputationCategory.ANNOYING,
                provenance = SignalProvenance(SourceType.COMMUNITY, "user-A", now + (i * 1000L))
            )
        }

        val deduplicated = AntiAbuseEngine.deduplicateUserSignals(userRepeatedSignals)

        assertEquals("Should have exactly 1 active signal for user-A", 1, deduplicated.size)
        assertEquals("Latest vote (OK) should have mutated and overridden previous votes", ReputationCategory.OK, deduplicated.first().category)
    }
}

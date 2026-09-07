package com.example.engine

/**
 * Silnik obliczania wagi zaufania zgłaszającego (TrustScoreEngine).
 * Służy wyłącznie jako mechanizm ochrony przed nadużyciami (anti-abuse) –
 * nie tworzy publicznych rankingów użytkowników.
 */
object TrustScoreEngine {

    data class ReporterProfile(
        val reporterId: String,
        val accountAgeDays: Int = 1,
        val totalSubmissions: Int = 0,
        val consensusAgreementCount: Int = 0,
        val conflictingReportsCount: Int = 0,
        val hasPlayIntegrityAttestation: Boolean = true
    )

    /**
     * Wylicza wagę zaufania w przedziale [0.15 .. 1.0].
     * Nowy użytkownik zaczyna z bezpieczną, umiarkowaną wagą (0.5).
     */
    fun calculateTrustScore(
        profile: ReporterProfile,
        hasRealInteractionOnDevice: Boolean
    ): Float {
        var baseScore = 0.50f

        // 1. Wiek konta / instalacji
        if (profile.accountAgeDays >= 90) baseScore += 0.15f
        else if (profile.accountAgeDays >= 30) baseScore += 0.10f
        else if (profile.accountAgeDays < 3) baseScore -= 0.10f // Bardzo świeże instalacje mają niższą początkową wagę

        // 2. Historia zgłoszeń zgodnych z konsensusem
        if (profile.totalSubmissions >= 5) {
            val agreementRate = profile.consensusAgreementCount.toFloat() / profile.totalSubmissions
            if (agreementRate >= 0.80f) baseScore += 0.15f
            else if (agreementRate < 0.40f) baseScore -= 0.20f
        }

        // 3. Potwierdzona realna interakcja telekomunikacyjna na urządzeniu
        if (hasRealInteractionOnDevice) {
            baseScore += 0.20f
        }

        // 4. Play Integrity attestation
        if (!profile.hasPlayIntegrityAttestation) {
            baseScore -= 0.25f
        }

        return baseScore.coerceIn(0.15f, 1.0f)
    }
}

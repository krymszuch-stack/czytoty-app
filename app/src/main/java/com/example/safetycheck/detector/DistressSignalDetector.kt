package com.example.safetycheck.detector

import com.example.safetycheck.model.DistressConfidence
import com.example.safetycheck.model.DistressSignalType
import com.example.safetycheck.model.PossibleDistressSignal
import java.util.Locale

/**
 * Wieloczynnikowy detektor sygnałów stresu (DistressSignalDetector).
 *
 * ZASADA FUNDAMENTALNA:
 * System preferuje FALSE NEGATIVE over CONSTANT FALSE ALARMS.
 * Pojedynczy głośny dźwięk NIGDY nie uruchamia alarmu.
 * Wymagane jest kilka sekund spójnego sygnału i wysoka pewność.
 */
object DistressSignalDetector {

    // Słowa kluczowe wzywania pomocy w języku polskim
    private val HELP_PHRASES = listOf(
        "pomocy",
        "ratunku",
        "niech ktoś pomoże",
        "niech ktoś mi pomoże",
        "zostaw mnie",
        "odejdź ode mnie",
        "nie dotykaj mnie",
        "pomóżcie",
        "ratujcie",
        "dzwońcie po pomoc"
    )

    // Słowa kłótni / dyskusji, które NIE są wzywaniem pomocy (filtr kłótni)
    private val ARGUMENT_EXCLUSIONS = listOf(
        "nie zgadzam się",
        "przestań gadać",
        "bzdury",
        "kłamiesz",
        "daj mi spokój",
        "moja racja",
        "co ty mówisz",
        "zamknij się"
    )

    // Wskaźniki treningu fizycznego (filtr siłowni)
    private val WORKOUT_KEYWORDS = listOf(
        "jeszcze jedno powtórzenie",
        "ciężar",
        "seria",
        "trening",
        "wyciśnij",
        "hantle",
        "raz dwa trzy"
    )

    // Wskaźniki imprezy / rozrywki (filtr imprezy)
    private val PARTY_KEYWORDS = listOf(
        "toast",
        "na zdrowie",
        "muzyka",
        "zatańczmy",
        "impreza",
        "wesoło",
        "drinki"
    )

    data class AudioEnvironmentContext(
        val durationMs: Long,
        val rmsEnergy: Float, // 0.0 .. 1.0
        val peakEnergy: Float, // 0.0 .. 1.0
        val isSingleImpulse: Boolean = false, // np. upadek telefonu, trzaśnięcie drzwiami
        val isRhythmicMusic: Boolean = false, // głośna muzyka / bas
        val isDogBarkingCadence: Boolean = false, // krótkie impulsy zwierzęce
        val isChildPlayfulSpeech: Boolean = false, // śmiech / zabawa dziecięca
        val transcriptText: String = ""
    )

    /**
     * Ocenia sytuację akustyczną i leksykalną.
     * Zwraca PossibleDistressSignal tylko wtedy, gdy spełnione są rygorystyczne kryteria ciągłości.
     */
    fun evaluate(context: AudioEnvironmentContext): PossibleDistressSignal? {
        // 1. ZASADA POJEDYNCZEGO DŹWIĘKU: Pojedynczy impuls natychmiast odrzucany
        if (context.isSingleImpulse || context.durationMs < 1800L) {
            return null
        }

        // 2. FILTR MUZYKI I PSÓW
        if (context.isRhythmicMusic || context.isDogBarkingCadence) {
            return null
        }

        val normalizedText = context.transcriptText.lowercase(Locale.forLanguageTag("pl"))

        // 3. FILTR ZABAWY DZIECKA (brak leksykalnych sygnałów pomocy)
        if (context.isChildPlayfulSpeech && !containsHelpKeywords(normalizedText)) {
            return null
        }

        // 4. FILTR SIŁOWNI / TRENINGU
        if (WORKOUT_KEYWORDS.any { normalizedText.contains(it) } && !containsHelpKeywords(normalizedText)) {
            return null
        }

        // 5. FILTR IMPREZY / ROZRYWKI
        if (PARTY_KEYWORDS.any { normalizedText.contains(it) } && !containsHelpKeywords(normalizedText)) {
            return null
        }

        // 6. FILTR KŁÓTNI (Podniesiony głos, ale brak wzywania pomocy)
        if (ARGUMENT_EXCLUSIONS.any { normalizedText.contains(it) } && !containsHelpKeywords(normalizedText)) {
            return null
        }

        // 7. ANALIZA POWTARZAJĄCYCH SIĘ SŁÓW POMOCY (Najsilniejszy sygnał)
        val matchedHelpPhrases = HELP_PHRASES.filter { normalizedText.contains(it) }
        val keywordCount = countHelpOccurrences(normalizedText)

        if (matchedHelpPhrases.isNotEmpty()) {
            // Jeśli pada wołanie o pomoc i trwa przez wymagany czas
            val isSustained = context.durationMs >= 2200L || keywordCount >= 2

            if (isSustained) {
                return PossibleDistressSignal(
                    type = DistressSignalType.SUSTAINED_CRY_FOR_HELP,
                    confidence = if (keywordCount >= 2 || context.rmsEnergy > 0.65f) DistressConfidence.HIGH else DistressConfidence.MEDIUM,
                    durationMs = context.durationMs,
                    acousticScore = context.rmsEnergy,
                    detectedKeywords = matchedHelpPhrases
                )
            }
        }

        // 8. DŁUGOTRWAŁY CIĄGŁY KRZYK (Czysto akustyczny, trwający >= 3 sekundy o wysokiej energii)
        if (context.durationMs >= 3000L && context.rmsEnergy >= 0.85f && context.peakEnergy >= 0.92f) {
            return PossibleDistressSignal(
                type = DistressSignalType.SUSTAINED_HIGH_ENERGY_VOCALIZATION,
                confidence = DistressConfidence.MEDIUM,
                durationMs = context.durationMs,
                acousticScore = context.rmsEnergy,
                detectedKeywords = emptyList()
            )
        }

        // Brak spełnienia progów pewności -> preferujemy False Negative nad nieustannym budzeniem użytkownika
        return null
    }

    private fun containsHelpKeywords(text: String): Boolean {
        return HELP_PHRASES.any { text.contains(it) }
    }

    private fun countHelpOccurrences(text: String): Int {
        var count = 0
        for (phrase in HELP_PHRASES) {
            var index = 0
            while (true) {
                index = text.indexOf(phrase, index)
                if (index != -1) {
                    count++
                    index += phrase.length
                } else {
                    break
                }
            }
        }
        return count
    }
}

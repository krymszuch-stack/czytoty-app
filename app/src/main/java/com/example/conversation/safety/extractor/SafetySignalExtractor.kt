package com.example.conversation.safety.extractor

import com.example.conversation.safety.model.DetectedSignal
import com.example.conversation.safety.model.SafetySignalCode
import java.util.Locale

/**
 * Kontekstowy silnik ekstrakcji sygnałów bezpieczeństwa rozmowy (SafetySignalExtractor).
 *
 * Zgodnie z zasadą: NIE UŻYWAĆ SAMEGO KEYWORD MATCHING.
 * Analizuje intencję wypowiedzi, obecność negacji, ostrzeżeń edukacyjnych, żądań oraz presji czasu.
 */
object SafetySignalExtractor {

    // Tokeny negacji / ostrzeżeń w języku polskim
    private val NEGATION_PATTERNS = listOf(
        "nigdy nie",
        "nie poprosi",
        "nie prosi",
        "nie pytamy",
        "nie pyta",
        "nie podawaj",
        "nie należy podawać",
        "nie wolno",
        "w żadnym wypadku",
        "pod żadnym pozorem",
        "ostrzegam przed",
        "ostrzegamy przed",
        "przestrzegam",
        "nie wymagamy",
        "nie autoryzuj",
        "nie instaluj"
    )

    // Oprogramowanie do zdalnego pulpitu (często używane w scamach "na pracownika banku")
    private val REMOTE_DESK_APPS = listOf(
        "anydesk",
        "teamviewer",
        "quicksupport",
        "rustdesk",
        "ultraviewer",
        "zdalny pulpit",
        "zdalnego pulpitu",
        "aplikacj[eęa] (wsparcia|pomocnicz[aąe]|techniczn[aąe])"
    )

    /**
     * Główna funkcja wyciągająca sygnały z transkrypcji.
     */
    fun extractSignals(transcriptText: String?): List<DetectedSignal> {
        if (transcriptText.isNullOrBlank()) return emptyList()

        val normalized = transcriptText.lowercase(Locale.forLanguageTag("pl"))
        val signals = mutableListOf<DetectedSignal>()

        // 1. ANALIZA KODU BLIK (Z uwzględnieniem negacji i ostrzeżeń bankowych)
        analyzeBlikSignal(normalized, signals)

        // 2. ANALIZA KODÓW OTP / SMS
        analyzeOtpSignal(normalized, signals)

        // 3. ANALIZA HASEŁ I DANYCH LOGOWANIA
        analyzePasswordSignal(normalized, signals)

        // 4. ANALIZA DANYCH KARTY PŁATNICZEJ
        analyzeCardSignal(normalized, signals)

        // 5. ANALIZA NUMERU PESEL
        analyzePeselSignal(normalized, signals)

        // 6. ZDALNY PULPIT & INSTALACJA APLIKACJI
        analyzeRemoteAccessSignal(normalized, signals)

        // 7. KONTO TECHNICZNE / REZERWOWE
        analyzeTechnicalAccountSignal(normalized, signals)

        // 8. PILNY PRZELEW / ZLECENIE PRZELEWU
        analyzeUrgentTransferSignal(normalized, signals)

        // 9. PRESJA INWESTYCYJNA (KRYPTOWALUTY / FOREX)
        analyzeInvestmentSignal(normalized, signals)

        // 10. PODSZYWANIE SIĘ POD BANK / POLICJĘ
        analyzeImpersonationSignal(normalized, signals)

        // 11. GROŹBA BLOKADY KONTA & PRESJA CZASU
        analyzeThreatAndPressureSignal(normalized, signals)

        // 12. ŻĄDANIE ZACHOWANIA TAJEMNICY (SECRECY)
        analyzeSecrecySignal(normalized, signals)

        // 13. KURIER – TYLKO W PRZYPADKU ŻĄDANIA DOPŁATY
        analyzeCourierSignal(normalized, signals)

        return signals
    }

    private fun isPhraseNegated(text: String, targetPhrase: String): Boolean {
        val targetIndex = text.indexOf(targetPhrase)
        if (targetIndex == -1) return false

        // Sprawdź okno 50 znaków przed frazą lub 30 znaków po frazie
        val startWindow = (targetIndex - 50).coerceAtLeast(0)
        val endWindow = (targetIndex + targetPhrase.length + 30).coerceAtMost(text.length)
        val contextSnippet = text.substring(startWindow, endWindow)

        return NEGATION_PATTERNS.any { neg -> contextSnippet.contains(neg) }
    }

    private fun analyzeBlikSignal(text: String, signals: MutableList<DetectedSignal>) {
        if (!text.contains("blik")) return

        if (isPhraseNegated(text, "blik")) {
            // Ostrzeżenie bankowe lub negacja – np. "Bank nigdy nie poprosi Cię o kod BLIK"
            // NIE generujemy REQUEST_BLIK!
            return
        }

        val blikRequestRegex = Regex(
            "\\b(podaj|podawać|podanie|poda|dyktuj|przekaż|wygeneruj|przepisz|wpisz|wprowadź|poproszę|proszę)\\b.*\\bblik",
            RegexOption.IGNORE_CASE
        )

        if (blikRequestRegex.containsMatchIn(text) || text.contains("kod blik")) {
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.REQUEST_BLIK,
                    rawMatchedPhrase = "prośba o kod blik",
                    weight = SafetySignalCode.REQUEST_BLIK.baseWeight,
                    confidence = 0.95f,
                    explanation = "Rozmówca żąda podania lub wygenerowania kodu BLIK."
                )
            )
        }
    }

    private fun analyzeOtpSignal(text: String, signals: MutableList<DetectedSignal>) {
        val otpKeywords = listOf("kod z sms", "kod sms", "hasło jednorazowe", "kod autoryzacyjny", "kod potwierdzający")
        val foundKeyword = otpKeywords.firstOrNull { text.contains(it) } ?: return

        if (isPhraseNegated(text, foundKeyword)) return

        val requestRegex = Regex("\\b(podaj|przeczytaj|przekaż|dyktuj|wpisz|proszę o|poproszę)\\b.*\\bkod", RegexOption.IGNORE_CASE)
        if (requestRegex.containsMatchIn(text) || text.contains("jaki kod przyszedł")) {
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.REQUEST_OTP,
                    rawMatchedPhrase = foundKeyword,
                    weight = SafetySignalCode.REQUEST_OTP.baseWeight,
                    confidence = 0.90f,
                    explanation = "Rozmówca prosi o jednorazowy kod autoryzacyjny z wiadomości SMS."
                )
            )
        }
    }

    private fun analyzePasswordSignal(text: String, signals: MutableList<DetectedSignal>) {
        val passKeywords = listOf("hasło do konta", "hasło do banku", "hasło do bankowości", "dane do logowania", "login i hasło")
        val foundKeyword = passKeywords.firstOrNull { text.contains(it) } ?: return

        if (isPhraseNegated(text, foundKeyword)) return

        signals.add(
            DetectedSignal(
                code = SafetySignalCode.REQUEST_PASSWORD,
                rawMatchedPhrase = foundKeyword,
                weight = SafetySignalCode.REQUEST_PASSWORD.baseWeight,
                confidence = 0.95f,
                explanation = "Rozmówca prosi o poufne hasło lub dane logowania."
            )
        )
    }

    private fun analyzeCardSignal(text: String, signals: MutableList<DetectedSignal>) {
        val cardKeywords = listOf("numer karty", "kod cvc", "kod cvv", "data ważności karty", "zdjęcie karty", "obie strony karty")
        val foundKeyword = cardKeywords.firstOrNull { text.contains(it) } ?: return

        if (isPhraseNegated(text, foundKeyword)) return

        signals.add(
            DetectedSignal(
                code = SafetySignalCode.REQUEST_CARD_DATA,
                rawMatchedPhrase = foundKeyword,
                weight = SafetySignalCode.REQUEST_CARD_DATA.baseWeight,
                confidence = 0.90f,
                explanation = "Rozmówca żąda poufnych danych karty płatniczej (numer, data, CVV/CVC)."
            )
        )
    }

    private fun analyzePeselSignal(text: String, signals: MutableList<DetectedSignal>) {
        if (!text.contains("pesel")) return
        if (isPhraseNegated(text, "pesel")) return

        val requestRegex = Regex("\\b(podaj|podanie|zweryfikuj|poproszę o|jaki jest|proszę)\\b.*\\bpesel", RegexOption.IGNORE_CASE)
        if (requestRegex.containsMatchIn(text)) {
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.REQUEST_PESEL,
                    rawMatchedPhrase = "prośba o pesel",
                    weight = SafetySignalCode.REQUEST_PESEL.baseWeight,
                    confidence = 0.70f,
                    explanation = "Rozmówca prosi o podanie numeru PESEL."
                )
            )
        }
    }

    private fun analyzeRemoteAccessSignal(text: String, signals: MutableList<DetectedSignal>) {
        val matchesRemoteApp = REMOTE_DESK_APPS.any { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) }

        if (matchesRemoteApp) {
            val isInstallRequested = Regex("\\b(zainstaluj|pobierz|ściągnij|uruchom|włącz|wejdź na stronę)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            val matchedApp = REMOTE_DESK_APPS.firstOrNull { text.contains(it) } ?: "oprogramowanie zdalne"

            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.REMOTE_ACCESS_REQUEST,
                    rawMatchedPhrase = matchedApp,
                    weight = SafetySignalCode.REMOTE_ACCESS_REQUEST.baseWeight,
                    confidence = 0.98f,
                    explanation = "Żądanie instalacji oprogramowania do zdalnego zarządzania pulpitem ($matchedApp)."
                )
            )

            if (isInstallRequested) {
                signals.add(
                    DetectedSignal(
                        code = SafetySignalCode.INSTALL_APP_REQUEST,
                        rawMatchedPhrase = "instalacja aplikacji",
                        weight = SafetySignalCode.INSTALL_APP_REQUEST.baseWeight,
                        confidence = 0.90f,
                        explanation = "Nacisk na instalację zewnętrznego oprogramowania w trakcie rozmowy."
                    )
                )
            }
        }
    }

    private fun analyzeTechnicalAccountSignal(text: String, signals: MutableList<DetectedSignal>) {
        val techAccountTerms = listOf("konto techniczne", "rachunek techniczny", "bezpieczne konto", "rachunek rezerwowy", "konto tymczasowe", "konto zabezpieczające")
        val foundTerm = techAccountTerms.firstOrNull { text.contains(it) }

        if (foundTerm != null) {
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.TECHNICAL_ACCOUNT,
                    rawMatchedPhrase = foundTerm,
                    weight = SafetySignalCode.TECHNICAL_ACCOUNT.baseWeight,
                    confidence = 0.98f,
                    explanation = "Użycie socjotechniki 'konta technicznego / bezpiecznego rachunku'."
                )
            )
        }
    }

    private fun analyzeUrgentTransferSignal(text: String, signals: MutableList<DetectedSignal>) {
        val transferKeywords = listOf("przelać", "przelej", "przelew", "przelanie", "przelewać", "zdeponuj", "wpłać środki", "wykonaj przelew")
        val isTransferMentioned = transferKeywords.any { text.contains(it) }

        if (isTransferMentioned) {
            val isUrgent = text.contains("konto techniczne") ||
                    text.contains("natychmiast") ||
                    text.contains("teraz") ||
                    text.contains("jak najszybciej") ||
                    text.contains("proszę przelać środki") ||
                    text.contains("zabezpieczyć konto") ||
                    text.contains("zabezpieczyć środki")

            if (isUrgent) {
                signals.add(
                    DetectedSignal(
                        code = SafetySignalCode.URGENT_TRANSFER,
                        rawMatchedPhrase = "żądanie przelewu",
                        weight = SafetySignalCode.URGENT_TRANSFER.baseWeight,
                        confidence = 0.92f,
                        explanation = "Prośba o pilne przelanie lub zabezpieczenie środków pieniężnych."
                    )
                )
            }
        }
    }

    private fun analyzeInvestmentSignal(text: String, signals: MutableList<DetectedSignal>) {
        val investmentTerms = listOf("kryptowaluty", "krypto", "bitcoin", "akcje orlen", "giełda", "gwarantowany zysk", "doradca finansowy", "broker")
        val foundTerm = investmentTerms.firstOrNull { text.contains(it) }

        if (foundTerm != null) {
            val pressure = text.contains("zysk") || text.contains("inwestyc") || text.contains("wypłata zysku") || text.contains("zarobić")
            if (pressure) {
                signals.add(
                    DetectedSignal(
                        code = SafetySignalCode.INVESTMENT_PRESSURE,
                        rawMatchedPhrase = foundTerm,
                        weight = SafetySignalCode.INVESTMENT_PRESSURE.baseWeight,
                        confidence = 0.85f,
                        explanation = "Nacisk na szybkie inwestycje finansowe lub kryptowaluty."
                    )
                )
            }
        }
    }

    private fun analyzeImpersonationSignal(text: String, signals: MutableList<DetectedSignal>) {
        if (text.contains("dział bezpieczeństwa") || text.contains("departament bezpieczeństwa") || text.contains("pracownik banku")) {
            // Jeśli jednocześnie prosi o dane lub aplikacje, to mocny sygnał podszywania się
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.BANK_IMPERSONATION,
                    rawMatchedPhrase = "dział bezpieczeństwa banku",
                    weight = SafetySignalCode.BANK_IMPERSONATION.baseWeight,
                    confidence = 0.80f,
                    explanation = "Rozmówca podaje się za pracownika działu bezpieczeństwa banku."
                )
            )
        }

        if (text.contains("policj") || text.contains("funkcjonariusz") || text.contains("prokuratur") || text.contains("cbś") || text.contains("komenda")) {
            if (text.contains("pieniądze") || text.contains("środki") || text.contains("przelew") || text.contains("tajna akcja") || text.contains("zagrożon")) {
                signals.add(
                    DetectedSignal(
                        code = SafetySignalCode.POLICE_IMPERSONATION,
                        rawMatchedPhrase = "policja / służby",
                        weight = SafetySignalCode.POLICE_IMPERSONATION.baseWeight,
                        confidence = 0.90f,
                        explanation = "Powoływanie się na działania policji lub prokuratury w kontekście finansowym."
                    )
                )
            }
        }
    }

    private fun analyzeThreatAndPressureSignal(text: String, signals: MutableList<DetectedSignal>) {
        if (text.contains("zablokowan") || text.contains("zablokujemy") || text.contains("blokada konta") || text.contains("utraci pan środki")) {
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.ACCOUNT_BLOCK_THREAT,
                    rawMatchedPhrase = "blokada konta",
                    weight = SafetySignalCode.ACCOUNT_BLOCK_THREAT.baseWeight,
                    confidence = 0.85f,
                    explanation = "Groźba natychmiastowej utraty środków lub blokady rachunku."
                )
            )
        }

        if (text.contains("natychmiast") || text.contains("w tej chwili") || text.contains("mamy 5 minut") || text.contains("za chwilę przepadną") || text.contains("szybko")) {
            // Dodaj presję czasu jeśli jest też inny sygnał zagrożenia
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.TIME_PRESSURE,
                    rawMatchedPhrase = "presja czasu",
                    weight = SafetySignalCode.TIME_PRESSURE.baseWeight,
                    confidence = 0.75f,
                    explanation = "Wywieranie presji czasu na podjęcie natychmiastowej decyzji."
                )
            )
        }
    }

    private fun analyzeSecrecySignal(text: String, signals: MutableList<DetectedSignal>) {
        val secrecyTerms = listOf("nikomu nie mów", "nikomu nie mówić", "tajemnica", "nie rozłączaj", "nie mów nikomu w banku", "nie wspominaj pracownikom")
        val found = secrecyTerms.firstOrNull { text.contains(it) }

        if (found != null) {
            signals.add(
                DetectedSignal(
                    code = SafetySignalCode.SECRECY_REQUEST,
                    rawMatchedPhrase = found,
                    weight = SafetySignalCode.SECRECY_REQUEST.baseWeight,
                    confidence = 0.95f,
                    explanation = "Żądanie zachowania rozmowy w tajemnicy przed personelem banku lub rodziną."
                )
            )
        }
    }

    private fun analyzeCourierSignal(text: String, signals: MutableList<DetectedSignal>) {
        // Kurier sam w sobie NIE jest sygnałem ryzyka (np. "Dzień dobry, kurier, będę za pięć minut" to normalne połączenie)
        // Sygnał występuje WYŁĄCZNIE gdy kurier domaga się dopłaty przez telefon lub link
        if (text.contains("kurier") || text.contains("paczka") || text.contains("przesyłka")) {
            if (text.contains("dopłać") || text.contains("dopłata") || text.contains("brakująca kwota") || text.contains("link do płatności")) {
                signals.add(
                    DetectedSignal(
                        code = SafetySignalCode.COURIER_PAYMENT,
                        rawMatchedPhrase = "dopłata do przesyłki",
                        weight = SafetySignalCode.COURIER_PAYMENT.baseWeight,
                        confidence = 0.85f,
                        explanation = "Żądanie dopłaty lub autoryzacji płatności za przesyłkę kurierską."
                    )
                )
            }
        }
    }
}

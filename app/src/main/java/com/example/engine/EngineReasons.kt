package com.example.engine

/**
 * Ustandaryzowane kody i opisy przyczyn ocen silnika CzyToTy? (Engine Reasons).
 * UI korzysta wyłącznie ze stabilnych kodów, eliminując niespójne teksty generowane ad-hoc.
 */
enum class EngineReasonCode {
    HOTLINE_CLI_SPOOF_RISK,
    SCAM_VISHING_REPORTS,
    TELEMARKETING_MASS_CALLS,
    ROBOCALL_AUTOMATED,
    SMS_BLIK_OR_CREDENTIALS,
    SMS_BRAND_DOMAIN_MISMATCH,
    SMS_SUSPICIOUS_SHORTENER,
    SMS_URGENT_PAYMENT_DEMAND,
    ANOMALOUS_REPORTING_SURGE,
    HIGH_ACTIVITY_24H,
    PUBLIC_REGISTER_LISTED,
    SAP_VERIFIED_ENTERPRISE,
    COMMUNITY_HIGHLY_POSITIVE,
    HISTORICAL_DATA_DECAYED,
    NO_COMMUNITY_SIGNALS,
    CONVERSATION_HIGH_RISK_SIGNALS,
    CONVERSATION_SAFE_NO_SIGNALS
}

enum class ReasonImpact {
    CRITICAL_RISK,
    ELEVATED_RISK,
    MODERATE_RISK,
    NEUTRAL,
    POSITIVE_REPUTATION,
    TRUSTED_VERIFICATION
}

data class EngineReason(
    val code: EngineReasonCode,
    val localizedDescription: String,
    val impact: ReasonImpact
) {
    companion object {
        fun create(code: EngineReasonCode, customParam: String? = null): EngineReason {
            return when (code) {
                EngineReasonCode.HOTLINE_CLI_SPOOF_RISK -> EngineReason(
                    code,
                    "Numer to oficjalna infolinia, ale w technologii GSM rozmówca może fałszować numer dzwoniący (spoofing).",
                    ReasonImpact.CRITICAL_RISK
                )
                EngineReasonCode.SCAM_VISHING_REPORTS -> EngineReason(
                    code,
                    customParam?.let { "$it zgłoszeń oznaczono jako próbę wyłudzenia lub oszustwa." }
                        ?: "Zgłoszenia społeczności wskazują na próby wyłudzenia danych lub oszustwa.",
                    ReasonImpact.CRITICAL_RISK
                )
                EngineReasonCode.TELEMARKETING_MASS_CALLS -> EngineReason(
                    code,
                    "Zgłoszenia wskazują na ofertę handlową lub telemarketing (brak cech oszustwa).",
                    ReasonImpact.MODERATE_RISK
                )
                EngineReasonCode.ROBOCALL_AUTOMATED -> EngineReason(
                    code,
                    "Użytkownicy zgłaszają powtarzalne, automatyczne połączenia od bota.",
                    ReasonImpact.MODERATE_RISK
                )
                EngineReasonCode.SMS_BLIK_OR_CREDENTIALS -> EngineReason(
                    code,
                    "Wiadomość SMS żąda podania kodu BLIK, hasła lub poufnych danych logowania.",
                    ReasonImpact.CRITICAL_RISK
                )
                EngineReasonCode.SMS_BRAND_DOMAIN_MISMATCH -> EngineReason(
                    code,
                    customParam?.let { "Link w SMS prowadzi do fałszywej domeny podszywającej się pod $it." }
                        ?: "Adres strony w SMS nie należy do oficjalnej domeny nadawcy.",
                    ReasonImpact.CRITICAL_RISK
                )
                EngineReasonCode.SMS_SUSPICIOUS_SHORTENER -> EngineReason(
                    code,
                    "Wiadomość zawiera skrócony link maskujący docelowy adres docelowy.",
                    ReasonImpact.ELEVATED_RISK
                )
                EngineReasonCode.SMS_URGENT_PAYMENT_DEMAND -> EngineReason(
                    code,
                    "Wiadomość wywiera presję czasu (np. 'natychmiastowa dopłata do paczki lub odłączenie mediów').",
                    ReasonImpact.CRITICAL_RISK
                )
                EngineReasonCode.ANOMALOUS_REPORTING_SURGE -> EngineReason(
                    code,
                    "Wykryto nagły, nienaturalny skok zgłoszeń w krótkim czasie. Wiarygodność danych jest sprawdzana.",
                    ReasonImpact.MODERATE_RISK
                )
                EngineReasonCode.HIGH_ACTIVITY_24H -> EngineReason(
                    code,
                    customParam?.let { "$it nowych zgłoszeń w ciągu ostatnich 24 godzin." }
                        ?: "Wysoki wzrost aktywności zgłoszeń w ostatnich 24 godzinach.",
                    ReasonImpact.ELEVATED_RISK
                )
                EngineReasonCode.PUBLIC_REGISTER_LISTED -> EngineReason(
                    code,
                    customParam?.let { "Numer figuruje w oficjalnym wykazie: $it." }
                        ?: "Numer zarejestrowany w publicznym rejestrze instytucji.",
                    ReasonImpact.POSITIVE_REPUTATION
                )
                EngineReasonCode.SAP_VERIFIED_ENTERPRISE -> EngineReason(
                    code,
                    customParam?.let { "Zweryfikowany kontrahent w systemie SAP: $it." }
                        ?: "Numer zweryfikowany w korporacyjnej kartotece SAP S/4HANA.",
                    ReasonImpact.TRUSTED_VERIFICATION
                )
                EngineReasonCode.COMMUNITY_HIGHLY_POSITIVE -> EngineReason(
                    code,
                    "Zdecydowana większość użytkowników ocenia ten numer pozytywnie.",
                    ReasonImpact.POSITIVE_REPUTATION
                )
                EngineReasonCode.HISTORICAL_DATA_DECAYED -> EngineReason(
                    code,
                    "Zgłoszenia dotyczące tego numeru pochodzą sprzed wielu miesięcy (wpływ wygaszony w czasie).",
                    ReasonImpact.NEUTRAL
                )
                EngineReasonCode.NO_COMMUNITY_SIGNALS -> EngineReason(
                    code,
                    "Brak zgłoszeń społeczności dla tego numeru w bazie CzyToTy?.",
                    ReasonImpact.NEUTRAL
                )
                EngineReasonCode.CONVERSATION_HIGH_RISK_SIGNALS -> EngineReason(
                    code,
                    customParam?.let { "Analiza rozmowy wykryła krytyczne sygnały: $it." }
                        ?: "Wykryto sygnały podwyższonego ryzyka w treści rozmowy.",
                    ReasonImpact.CRITICAL_RISK
                )
                EngineReasonCode.CONVERSATION_SAFE_NO_SIGNALS -> EngineReason(
                    code,
                    "Nie wykryliśmy typowych sygnałów oszustwa w analizowanym fragmencie.",
                    ReasonImpact.NEUTRAL
                )
            }
        }
    }
}

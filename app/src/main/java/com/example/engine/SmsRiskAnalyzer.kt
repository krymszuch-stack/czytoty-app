package com.example.engine

import com.example.model.BrandIdentity
import com.example.model.ConfidenceLevel
import com.example.model.OtpCodeInfo
import com.example.model.RiskSeverity
import com.example.model.UrlSecurityAnalysis
import java.net.URI
import java.util.regex.Pattern

data class SmsAnalysisResult(
  val riskScore: Int,
  val confidence: ConfidenceLevel,
  val severity: RiskSeverity,
  val reasons: List<String>,
  val otpInfo: OtpCodeInfo?,
  val urlAnalysis: UrlSecurityAnalysis?
)

/**
 * Lokalny, transparentny analizator ryzyka wiadomości SMS (Offline, bez ML/LLM).
 * Realizuje heurystyki polskiego krajobrazu zagrożeń smishingowych:
 * - niedopasowanie domeny do marki (brand domain mismatch),
 * - skracacze linków,
 * - próby wyłudzenia kodu BLIK, hasła lub instalacji nieznanych plików (.apk),
 * - żądania natychmiastowych mikropłatności / dopłat do paczek / odcięć mediów,
 * - lokalna detekcja kodów jednorazowych (OTP).
 */
object SmsRiskAnalyzer {

  // URL extraction pattern
  private val URL_PATTERN = Pattern.compile(
    "(https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)",
    Pattern.CASE_INSENSITIVE
  )

  // Skracacze linków
  private val KNOWN_SHORTENERS = setOf(
    "bit.ly", "tinyurl.com", "t.co", "is.gd", "buff.ly", "ow.ly", "cutt.ly", "rb.gy", "linktr.ee"
  )

  // Podejrzane TLD często nadużywane w tanich domenach phishingowych
  private val SUSPICIOUS_TLDS = setOf(
    "xyz", "top", "vip", "work", "cfd", "online", "club", "site", "buzz", "click"
  )

  // Słowa kluczowe wysokiego ryzyka
  private val KEYWORDS_BLIK = listOf("kod blik", "podaj blik", "przelew blik", "potwierdź blik")
  private val KEYWORDS_URGENT_PAYMENT = listOf(
    "dopłać", "niedopłata", "doplata", "zawieszona paczka", "odcięcie prądu",
    "odlaczenie", "ostatnie wezwanie", "windykacja natychmiastowa", "kaucja"
  )
  private val KEYWORDS_CREDENTIALS = listOf(
    "hasło", "haslo", "login", "zaloguj się natychmiast", "odblokuj konto", "weryfikacja konta"
  )
  private val KEYWORDS_APP_INSTALL = listOf(
    "zainstaluj aplikację", "pobierz aplikację", "plik apk", "aktualizacja certyfikatu", "zdalny pulpit"
  )

  // Wzorce OTP (kody jednorazowe)
  private val OTP_KEYWORDS = listOf("kod", "code", "otp", "weryfikac", "hasło jednorazowe", "twoj kod")
  private val DIGIT_CODE_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b")
  private val GOOGLE_PREFIX_CODE_PATTERN = Pattern.compile("\\b[Gg]-(\\d{4,8})\\b")

  /**
   * Główna funkcja analizy pojedynczej wiadomości SMS
   */
  fun analyzeMessage(
    sender: String,
    body: String,
    recognizedBrand: BrandIdentity? = BrandCatalog.findBrandBySender(sender)
  ): SmsAnalysisResult {
    val reasons = mutableListOf<String>()
    var score = 0
    val lowerBody = body.lowercase()

    // 1. Detekcja kodu OTP (lokalna)
    val otpInfo = detectOtp(body, recognizedBrand)

    // 2. Analiza linku (jeśli występuje)
    val urlAnalysis = analyzeUrl(body, recognizedBrand)
    if (urlAnalysis != null) {
      if (urlAnalysis.isShortener) {
        score += 35
        reasons.add("Wiadomość zawiera skrócony link maskujący docelowy adres (${urlAnalysis.domain})")
      }

      if (urlAnalysis.isSuspiciousTld) {
        score += 30
        reasons.add("Link używa domeny z końcówką .${urlAnalysis.domain.substringAfterLast('.')} często spotykanej w kampaniach masowych")
      }

      if (urlAnalysis.mismatchExplanation != null) {
        score += 55
        reasons.add(urlAnalysis.mismatchExplanation)
      } else if (!urlAnalysis.isRecognizedBrandDomain && recognizedBrand != null) {
        score += 45
        reasons.add("Domena linku (${urlAnalysis.domain}) nie należy do oficjalnych domen marki ${recognizedBrand.displayName}")
      }
    }

    // 3. Analiza treści pod kątem inżynierii społecznej (social engineering)
    if (KEYWORDS_BLIK.any { lowerBody.contains(it) }) {
      score += 50
      reasons.add("Wiadomość żąda podania lub potwierdzenia kodu BLIK")
    }

    if (KEYWORDS_URGENT_PAYMENT.any { lowerBody.contains(it) }) {
      score += 40
      reasons.add("Prośba o pilną dopłatę lub groźba natychmiastowych konsekwencji (np. odcięcie usługi/paczka)")
    }

    if (KEYWORDS_CREDENTIALS.any { lowerBody.contains(it) }) {
      score += 45
      reasons.add("Żądanie wprowadzenia loginu, hasła lub natychmiastowego logowania")
    }

    if (KEYWORDS_APP_INSTALL.any { lowerBody.contains(it) }) {
      score += 60
      reasons.add("Nakłanianie do instalacji zewnętrznej aplikacji lub pliku .apk")
    }

    // Wiadomości OTP od znanej marki bez podejrzanych linków są standardem
    if (otpInfo != null && urlAnalysis == null && score == 0) {
      score = 0
      reasons.add("Standardowy jednorazowy kod weryfikacyjny")
    }

    val finalScore = score.coerceIn(0, 100)
    val severity = RiskSeverity.fromScore(finalScore)

    val confidence = when {
      urlAnalysis?.mismatchExplanation != null -> ConfidenceLevel.WYSOKA
      finalScore >= 70 -> ConfidenceLevel.WYSOKA
      finalScore >= 30 -> ConfidenceLevel.SREDNIA
      else -> ConfidenceLevel.WYSOKA
    }

    return SmsAnalysisResult(
      riskScore = finalScore,
      confidence = confidence,
      severity = severity,
      reasons = if (reasons.isEmpty()) listOf("Brak znanych reguł ryzyka w treści wiadomości") else reasons,
      otpInfo = otpInfo,
      urlAnalysis = urlAnalysis
    )
  }

  /**
   * Lokalna heurystyka wykrywania kodów jednorazowych (OTP)
   */
  fun detectOtp(body: String, brand: BrandIdentity?): OtpCodeInfo? {
    val lower = body.lowercase()
    val hasOtpKeyword = OTP_KEYWORDS.any { lower.contains(it) }

    // Wzorzec dla Google np. "G-123456"
    val googleMatcher = GOOGLE_PREFIX_CODE_PATTERN.matcher(body)
    if (googleMatcher.find()) {
      val digits = googleMatcher.group(1) ?: ""
      return OtpCodeInfo(
        code = digits,
        issuerName = brand?.displayName ?: "Google",
        codeType = "Kod weryfikacyjny Google"
      )
    }

    // Standardowe kody 4-8 cyfr
    if (hasOtpKeyword) {
      val digitMatcher = DIGIT_CODE_PATTERN.matcher(body)
      while (digitMatcher.find()) {
        val candidate = digitMatcher.group(1) ?: continue
        // Odrzucamy typowe lata (np. 2024, 2026) jeśli występują w kontekście daty
        if (candidate.length == 4 && candidate.startsWith("202")) continue

        return OtpCodeInfo(
          code = candidate,
          issuerName = brand?.displayName,
          codeType = when {
            lower.contains("transakcj") || lower.contains("przelew") -> "Autoryzacja transakcji"
            lower.contains("logow") -> "Logowanie do konta"
            else -> "Kod weryfikacyjny"
          }
        )
      }
    }

    return null
  }

  /**
   * Wyciągnięcie i weryfikacja linku
   */
  fun analyzeUrl(body: String, brand: BrandIdentity?): UrlSecurityAnalysis? {
    val matcher = URL_PATTERN.matcher(body)
    if (!matcher.find()) return null

    val fullUrl = matcher.group(1) ?: return null
    val domain = extractDomain(fullUrl) ?: return null

    val isShortener = KNOWN_SHORTENERS.any { domain.equals(it, ignoreCase = true) }
    val tld = domain.substringAfterLast('.', "")
    val isSuspiciousTld = SUSPICIOUS_TLDS.contains(tld.lowercase())

    val isRecognizedBrandDomain = brand?.isOfficialDomain(domain) == true

    val mismatchExplanation = if (brand != null && !isRecognizedBrandDomain) {
      "Wiadomość podaje się za markę ${brand.displayName}, lecz link prowadzi do zewnętrznej domeny: $domain"
    } else null

    val isHighRisk = isShortener || isSuspiciousTld || (mismatchExplanation != null)

    return UrlSecurityAnalysis(
      fullUrl = fullUrl,
      domain = domain,
      isRecognizedBrandDomain = isRecognizedBrandDomain,
      isSuspiciousTld = isSuspiciousTld,
      isShortener = isShortener,
      mismatchExplanation = mismatchExplanation,
      isHighRisk = isHighRisk
    )
  }

  private fun extractDomain(url: String): String? {
    return try {
      val uri = URI(if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url)
      val host = uri.host ?: return null
      host.removePrefix("www.")
    } catch (_: Exception) {
      null
    }
  }
}

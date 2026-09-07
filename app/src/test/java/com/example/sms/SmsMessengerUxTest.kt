package com.example.sms

import com.example.data.DefaultCallerRepository
import com.example.engine.BrandCatalog
import com.example.engine.SmsRiskAnalyzer
import com.example.model.RiskSeverity
import com.example.model.SmsMessageType
import com.example.model.SmsThread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zestaw testów jednostkowych nowoczesnego modułu Wiadomości (SMS / Messenger UX - Sekcje 40–58).
 *
 * WERYFIKUJE:
 * 1. Semantyczną kategoryzację wiadomości (OTP, DELIVERY, BANK_FINANCE, PROMOTION, SYSTEM, SUSPICIOUS, NORMAL).
 * 2. Przypinanie wątków i gwarancję obecności na szczycie listy.
 * 3. Manualne porządkowanie przypiętych wątków (Manual order > automatyka).
 * 4. Archiwizację, wyciszanie i lokalne usuwanie konwersacji.
 * 5. Oznaczanie wątku jako spam i blokowanie.
 * 6. Lokalną detekcję kodów jednorazowych (OTP) bez połączenia z chmurą.
 * 7. Weryfikację bezpieczeństwa linków (Link Safety - oficjalna domena vs phishing).
 * 8. Niezmiennik tożsamości: rozpoznana marka != zweryfikowany nadawca.
 */
class SmsMessengerUxTest {

    // =========================================================================
    // 1. KLASYFIKACJA TYPÓW WIADOMOŚCI (SEKCJA 44)
    // =========================================================================

    @Test
    fun `otp message is classified as OTP type`() {
        val body = "G-847213 to Twój kod weryfikacyjny Google. Nikomu go nie udostępniaj."
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "Google",
            body = body,
            recognizedBrand = BrandCatalog.google
        )

        assertEquals(SmsMessageType.OTP, analysis.messageType)
        assertNotNull(analysis.otpInfo)
        assertEquals("847213", analysis.otpInfo?.code)
    }

    @Test
    fun `delivery notification is classified as DELIVERY type`() {
        val body = "Paczka 62349182 czeka w Paczkomacie WAW12A. Kod odbioru: 492011. Szczegóły: https://inpost.pl"
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "InPost",
            body = body,
            recognizedBrand = BrandCatalog.inPost
        )

        assertEquals(SmsMessageType.DELIVERY, analysis.messageType)
    }

    @Test
    fun `bank transaction message is classified as BANK_FINANCE type`() {
        val body = "Zalogowano się na Twoje konto w mBanku z nowego urządzenia. Jeśli to nie Ty, zmień hasło."
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "mBank",
            body = body,
            recognizedBrand = BrandCatalog.mBank
        )

        assertEquals(SmsMessageType.BANK_FINANCE, analysis.messageType)
    }

    @Test
    fun `phishing message with fake link is classified as SUSPICIOUS type`() {
        val body = "INPOST: Twoja przesyłka wymaga dopłaty 1.89 zł. Opłać teraz: http://inpost-doplata-paczka.xyz/pay"
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "+48 732 123 456",
            body = body,
            recognizedBrand = BrandCatalog.inPost
        )

        assertEquals(SmsMessageType.SUSPICIOUS, analysis.messageType)
        assertEquals(RiskSeverity.HIGH, analysis.severity)
        assertTrue(analysis.riskScore >= 70)
    }

    @Test
    fun `promotional message is classified as PROMOTION type`() {
        val body = "Tylko dzisiaj super rabat -30% na wszystkie smartfony! Wykorzystaj kod rabatowy w koszyku."
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "Sklep",
            body = body,
            recognizedBrand = null
        )

        assertEquals(SmsMessageType.PROMOTION, analysis.messageType)
    }

    @Test
    fun `system carrier message is classified as SYSTEM type`() {
        val body = "Pakiet internetowy został aktywowany. Twój operator Orange."
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "Orange",
            body = body,
            recognizedBrand = null
        )

        assertEquals(SmsMessageType.SYSTEM, analysis.messageType)
    }

    @Test
    fun `personal family text is classified as NORMAL type`() {
        val body = "Cześć mamo, będę w domu koło 18:00."
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "+48 601 234 567",
            body = body,
            recognizedBrand = null
        )

        assertEquals(SmsMessageType.NORMAL, analysis.messageType)
    }

    // =========================================================================
    // 2. PRZYPINANIE WĄTKÓW I MANUAL ORDER (SEKCJA 50)
    // =========================================================================

    @Test
    fun `pinned threads are always placed on top of the conversation list`() {
        val repo = DefaultCallerRepository()
        val initialThreads = repo.smsThreads.value

        // Przypnij wątek Mamy (który domyślnie nie był przypięty)
        val momThread = initialThreads.first { it.displayName == "Mama" }
        assertFalse(momThread.isPinned)

        repo.togglePinThread(momThread.threadId)

        val updatedThreads = repo.smsThreads.value
        val updatedMom = updatedThreads.first { it.threadId == momThread.threadId }
        assertTrue(updatedMom.isPinned)

        // Sprawdź, czy wszystkie przypięte są na początku listy
        val firstUnpinnedIndex = updatedThreads.indexOfFirst { !it.isPinned }
        val lastPinnedIndex = updatedThreads.indexOfLast { it.isPinned }

        assertTrue("Wszystkie przypięte wątki muszą znajdować się przed nieprzypiętymi",
            lastPinnedIndex < firstUnpinnedIndex)
    }

    @Test
    fun `manual reorder of pinned threads updates pinOrder correctly`() {
        val repo = DefaultCallerRepository()

        val pinnedBefore = repo.smsThreads.value.filter { it.isPinned }
        assertTrue("Oczekiwano co najmniej 2 przypiętych wątków w demo", pinnedBefore.size >= 2)

        val firstId = pinnedBefore[0].threadId
        val secondId = pinnedBefore[1].threadId

        // Zamień kolejność pierwszego z drugim (reorder: 0 -> 1)
        repo.reorderPinnedThreads(0, 1)

        val pinnedAfter = repo.smsThreads.value.filter { it.isPinned }
        assertEquals(secondId, pinnedAfter[0].threadId)
        assertEquals(firstId, pinnedAfter[1].threadId)
        assertEquals(0, pinnedAfter[0].pinOrder)
        assertEquals(1, pinnedAfter[1].pinOrder)
    }

    // =========================================================================
    // 3. ARCHIWIZACJA, WYCISZANIE I USUWANIE (SEKCJA 41 & 51)
    // =========================================================================

    @Test
    fun `toggle mute thread updates isMuted state`() {
        val repo = DefaultCallerRepository()
        val thread = repo.smsThreads.value.first()
        assertFalse(thread.isMuted)

        repo.toggleMuteThread(thread.threadId)
        assertTrue(repo.smsThreads.value.first { it.threadId == thread.threadId }.isMuted)

        repo.toggleMuteThread(thread.threadId)
        assertFalse(repo.smsThreads.value.first { it.threadId == thread.threadId }.isMuted)
    }

    @Test
    fun `toggle archive thread moves it into archived state`() {
        val repo = DefaultCallerRepository()
        val thread = repo.smsThreads.value.first()
        assertFalse(thread.isArchived)

        repo.toggleArchiveThread(thread.threadId)
        assertTrue(repo.smsThreads.value.first { it.threadId == thread.threadId }.isArchived)

        repo.toggleArchiveThread(thread.threadId)
        assertFalse(repo.smsThreads.value.first { it.threadId == thread.threadId }.isArchived)
    }

    @Test
    fun `delete thread removes thread and messages locally`() {
        val repo = DefaultCallerRepository()
        val thread = repo.smsThreads.value.first()

        assertTrue(repo.getMessagesForThread(thread.threadId).isNotEmpty())

        repo.deleteThreadLocally(thread.threadId)

        assertFalse(repo.smsThreads.value.any { it.threadId == thread.threadId })
        assertTrue(repo.getMessagesForThread(thread.threadId).isEmpty())
    }

    @Test
    fun `mark thread as spam updates severity to HIGH and tags as spam`() {
        val repo = DefaultCallerRepository()
        val momThread = repo.smsThreads.value.first { it.displayName == "Mama" }

        repo.markThreadAsSpam(momThread.threadId)

        val updated = repo.smsThreads.value.first { it.threadId == momThread.threadId }
        assertTrue(updated.isSpamOrBlocked)
        assertEquals(RiskSeverity.HIGH, updated.highestSeverity)
        assertEquals("Podejrzane", updated.categoryTag)
    }

    // =========================================================================
    // 4. LINK SAFETY & IDENTYFIKACJA MAREK (SEKCJA 46 & 48)
    // =========================================================================

    @Test
    fun `official brand domain is verified against brand catalog`() {
        val body = "Szczegóły odbioru paczki: https://inpost.pl/sledzenie"
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "InPost",
            body = body,
            recognizedBrand = BrandCatalog.inPost
        )

        assertNotNull(analysis.urlAnalysis)
        assertTrue(analysis.urlAnalysis!!.isRecognizedBrandDomain)
        assertFalse(analysis.urlAnalysis!!.isHighRisk)
        assertNull(analysis.urlAnalysis!!.mismatchExplanation)
    }

    @Test
    fun `spoofed phishing domain generates domain mismatch warning`() {
        val body = "Dopłać do paczki tutaj: http://inpost-paczka-odbierz.xyz/waw"
        val analysis = SmsRiskAnalyzer.analyzeMessage(
            sender = "InPost",
            body = body,
            recognizedBrand = BrandCatalog.inPost
        )

        assertNotNull(analysis.urlAnalysis)
        assertFalse(analysis.urlAnalysis!!.isRecognizedBrandDomain)
        assertTrue(analysis.urlAnalysis!!.isHighRisk || analysis.riskScore >= 70)
        assertNotNull(analysis.urlAnalysis!!.mismatchExplanation)
    }

    @Test
    fun `brand recognition distinction invariant - recognized brand is not blindly verified sender`() {
        val repo = DefaultCallerRepository()
        val threads = repo.smsThreads.value

        // InPost autentyczny (alfanumeryczny zarejestrowany Sender ID)
        val inpostThread = threads.firstOrNull { it.displayName == "InPost" }
        assertNotNull(inpostThread)
        assertTrue(inpostThread!!.isBrandVerified)

        // Phishing podszywający się pod InPost z losowego numeru komórkowego (+48 732 123 456)
        val scamThread = threads.firstOrNull { it.senderAddress == "+48 732 123 456" }
        assertNotNull(scamThread)
        assertFalse("Numer komórkowy podszywający się pod InPost NIE MOŻE mieć flagi isBrandVerified",
            scamThread!!.isBrandVerified)
    }

    @Test
    fun `unified sender profile aggregates phone numbers email domains and reputation`() {
        val repo = DefaultCallerRepository()
        val inpostProfile = repo.getUnifiedSenderProfile("InPost")

        assertEquals("InPost", inpostProfile.displayName)
        assertNotNull(inpostProfile.recognizedBrand)
        assertTrue(inpostProfile.domains.contains("inpost.pl"))
        assertTrue(inpostProfile.reputationStatus.isNotEmpty())

        val momProfile = repo.getUnifiedSenderProfile("+48 601 234 567")
        assertEquals("Mama", momProfile.displayName)
        assertEquals("mama@rodzina.pl", momProfile.email)
        assertTrue(momProfile.phoneNumbers.contains("+48 601 234 567"))
    }
}

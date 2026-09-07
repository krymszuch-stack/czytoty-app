package com.example.data.remote

import com.example.model.CallerInfo
import com.example.model.CrowdReport

/**
 * Kontrakt zdalnego API reputacji numerów (Remote Data Source).
 * W fazie MVP backend nie jest jeszcze zdeployowany na serwerze produkcyjnym,
 * dlatego implementacja [MockRemoteCallerDataSource] służy jako jawna makieta / demo
 * do testów architektury przed wdrożeniem produkcyjnego mikroserwisu.
 */
interface RemoteCallerDataSource {

  /**
   * Sprawdzenie reputacji numeru w centralnej bazie.
   * Zwraca wynik lub null w przypadku braku połączenia / przekroczenia limitu czasu (timeout).
   */
  suspend fun fetchNumberReputation(phoneNumber: String): CallerInfo?

  /**
   * Wysłanie raportu crowdsourcingowego.
   * Zwraca true jeśli zgłoszenie zostało pomyślnie przyjęte.
   */
  suspend fun submitReport(report: CrowdReport): Boolean

  /**
   * Pobranie przyrostowej paczki aktualizacji (delta-update) dla lokalnego cache.
   */
  suspend fun fetchDeltaUpdates(sinceTimestamp: Long): List<CallerInfo>

  /**
   * Flaga wskazująca, czy to źródło jest rzeczywistym serwerem, czy makietą demonstracyjną.
   */
  val isDemonstrationMock: Boolean
}

/**
 * Jawna implementacja demonstracyjna (DEMO / MOCK).
 * Kod i UI wprost komunikują ten stan – brak symulowania gotowego API.
 */
class MockRemoteCallerDataSource : RemoteCallerDataSource {

  override val isDemonstrationMock: Boolean = true

  override suspend fun fetchNumberReputation(phoneNumber: String): CallerInfo? {
    // Symulacja opóźnienia sieciowego w trybie demo
    return null
  }

  override suspend fun submitReport(report: CrowdReport): Boolean {
    // W trybie demo raport jest akceptowany lokalnie
    return true
  }

  override suspend fun fetchDeltaUpdates(sinceTimestamp: Long): List<CallerInfo> {
    return emptyList()
  }
}

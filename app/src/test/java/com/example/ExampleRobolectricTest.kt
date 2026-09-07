package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SmsSimulationScenario
import com.example.engine.BrandCatalog
import com.example.engine.SmsRiskAnalyzer
import com.example.viewmodel.CallerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bezpieczny Telefon", appName)
  }

  @Test
  fun `test onboarding toggle in ViewModel`() {
    val viewModel = CallerViewModel()
    assertEquals(false, viewModel.uiState.value.isOnboardingVisible)
    viewModel.showOnboarding()
    assertEquals(true, viewModel.uiState.value.isOnboardingVisible)
    viewModel.dismissOnboarding()
    assertEquals(false, viewModel.uiState.value.isOnboardingVisible)
  }

  @Test
  fun `test OTP detection in SmsRiskAnalyzer`() {
    val brand = BrandCatalog.findBrandBySender("Google")
    val sms = "Twój kod weryfikacyjny Google to: 847213. Nikomu go nie udostępniaj."
    val otpInfo = SmsRiskAnalyzer.detectOtp(sms, brand)
    assertNotNull(otpInfo)
    assertEquals("847213", otpInfo?.code)
  }

  @Test
  fun `test InPost domain mismatch phishing detection`() {
    val brand = BrandCatalog.findBrandBySender("InPost")
    assertNotNull(brand)
    val sms = "Paczka wstrzymana z powodu niedoplaty 1.49 zl: https://inpost-doplata-paczka.xyz/pay"
    val analysis = SmsRiskAnalyzer.analyzeUrl(sms, brand)
    assertNotNull(analysis)
    assertTrue("Domena spoza oficjalnej listy powinna zostać oznaczona", analysis?.isRecognizedBrandDomain == false)
    assertEquals("inpost-doplata-paczka.xyz", analysis?.domain)
  }

  @Test
  fun `test SMS simulation in ViewModel`() {
    val viewModel = CallerViewModel()
    viewModel.triggerSmsSimulation(SmsSimulationScenario.INPOST_PHISHING)
    val updatedThreads = viewModel.uiState.value.smsThreads
    assertTrue(updatedThreads.isNotEmpty())
  }
}

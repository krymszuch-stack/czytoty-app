package com.example.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stan sesji autentykacji w CzyToTy?.
 *
 * Supabase jest jedynym źródłem prawdy dla sesji użytkownika.
 */
sealed interface AuthSessionState {
    /** Sprawdzanie sesji przy starcie aplikacji (Splash / Bootstrap). */
    object Bootstrapping : AuthSessionState

    /** Użytkownik zalogowany przez konto Google. */
    data class Authenticated(val user: AuthUserProfile) : AuthSessionState

    /** Użytkownik korzysta z aplikacji w trybie lokalnym bez konta (Guest Mode). */
    data class GuestMode(val guestId: String = "local-guest") : AuthSessionState

    /** Błąd autentykacji (komunikat bezpieczny dla użytkownika). */
    data class Error(val userFriendlyMessage: String, val technicalDetail: String? = null) : AuthSessionState
}

/**
 * Zunifikowany profil zalogowanego użytkownika CzyToTy?.
 *
 * Bezpieczeństwo i prywatność:
 * - Brak ekspozycji tokenów, kluczy czy wewnętrznych identyfikatorów Google.
 * - Posiada pole [initials] jako bezpieczny fallback, gdy awatar nie może się załadować.
 */
data class AuthUserProfile(
    val id: String,
    val email: String?,
    val displayName: String,
    val avatarUrl: String?,
    val provider: String = "google",
    val createdAt: Long = System.currentTimeMillis(),
    val isEmailVerified: Boolean = true
) {
    val initials: String
        get() = displayName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { email?.firstOrNull()?.uppercase() ?: "U" }
}

/**
 * Model danych profilu w tabeli PostgreSQL public.profiles w Supabase.
 */
@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("country_code")
    val countryCode: String? = "PL",
    @SerialName("locale")
    val locale: String? = "pl_PL",
    @SerialName("onboarding_completed")
    val onboardingCompleted: Boolean = true,
    @SerialName("reputation_contribution_enabled")
    val reputationContributionEnabled: Boolean = true,
    @SerialName("analytics_consent")
    val analyticsConsent: Boolean = false
)

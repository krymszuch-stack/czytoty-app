package com.example.auth.supabase

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton konfiguracji i dostępu do klienta Supabase dla CzyToTy?.
 *
 * Bezpieczeństwo:
 * - PKCE FlowType.PKCE
 * - Systemowe Chrome Custom Tabs (brak podatnych WebView)
 * - Deeplink czytoty://auth
 * - Brak hardcodowanych kluczy w kodzie (odczyt z BuildConfig wstrzykiwanego z local.properties)
 */
object SupabaseClientProvider {

    /**
     * Czy deweloper skonfigurował realny projekt w local.properties?
     */
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
                !BuildConfig.SUPABASE_URL.contains("placeholder") &&
                !BuildConfig.SUPABASE_URL.contains("czytoty-project.supabase.co") &&
                BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank() &&
                !BuildConfig.SUPABASE_PUBLISHABLE_KEY.contains("placeholder")

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "czytoty"
                host = "auth"
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Postgrest)
        }
    }
}

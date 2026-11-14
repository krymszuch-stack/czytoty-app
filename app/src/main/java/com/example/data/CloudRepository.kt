package com.example.data

import com.example.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudNote(@SerialName("user_id") val userId: String, val phone: String, val label: String, val note: String)
@Serializable
data class DataSource(val id: String, val name: String, val url: String, val purpose: String,
    @SerialName("refresh_seconds") val refreshSeconds: Int)

class CloudRepository {
    val configured = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()
    private val client by lazy {
        check(configured) { "Brak konfiguracji Supabase w tej kompilacji." }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }
    suspend fun email(): String? {
        if (!configured) return null
        client.auth.awaitInitialization()
        return client.auth.currentUserOrNull()?.email
    }
    suspend fun checkConnection(): List<DataSource> = client.from("data_sources").select().decodeList()
    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) { this.email = email; this.password = password }
    }
    suspend fun signUp(email: String, password: String): Boolean {
        client.auth.signUpWith(Email) { this.email = email; this.password = password }
        return client.auth.currentSessionOrNull() != null
    }
    suspend fun signOut() = client.auth.signOut()
    suspend fun save(note: PhoneNote): CloudNote {
        val user = client.auth.currentUserOrNull() ?: error("Zaloguj się, aby zapisać notatkę w chmurze.")
        val saved = CloudNote(user.id, note.phone, note.label, note.note)
        client.from("private_notes").upsert(saved)
        return saved
    }
    suspend fun notes(): List<CloudNote> {
        val user = client.auth.currentUserOrNull() ?: error("Brak zalogowanego użytkownika.")
        return client.from("private_notes").select { filter { eq("user_id", user.id) }; limit(100) }.decodeList()
    }
    suspend fun delete(phone: String) {
        val user = client.auth.currentUserOrNull() ?: error("Brak zalogowanego użytkownika.")
        client.from("private_notes").delete { filter { eq("user_id", user.id); eq("phone", phone) } }
    }
}

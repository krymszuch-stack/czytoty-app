package com.example.safetycheck.repository

import com.example.safetycheck.model.SafetyCheckConfig
import com.example.safetycheck.model.SafetyCheckIncident
import com.example.safetycheck.model.TrustedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Repozytorium konfiguracji i historii incydentów Safety Check.
 *
 * ZASADY:
 * 1. Domyślnie moduł jest w 100% WYŁĄCZONY (isEnabled = false).
 * 2. Przechowuje wyłącznie metadane zdarzeń (zero surowego audio).
 */
class SafetyCheckRepository(
    initialConfig: SafetyCheckConfig = SafetyCheckConfig(isEnabled = false)
) {
    private val _config = MutableStateFlow(initialConfig)
    val config: StateFlow<SafetyCheckConfig> = _config.asStateFlow()

    private val incidents = CopyOnWriteArrayList<SafetyCheckIncident>()

    fun updateConfig(update: (SafetyCheckConfig) -> SafetyCheckConfig) {
        _config.value = update(_config.value)
    }

    fun setEnabled(enabled: Boolean) {
        updateConfig { it.copy(isEnabled = enabled) }
    }

    fun setUserName(name: String) {
        updateConfig { it.copy(userName = name) }
    }

    fun addTrustedContact(contact: TrustedContact) {
        updateConfig { current ->
            current.copy(trustedContacts = current.trustedContacts + contact)
        }
    }

    fun removeTrustedContact(contactId: String) {
        updateConfig { current ->
            current.copy(trustedContacts = current.trustedContacts.filterNot { it.id == contactId })
        }
    }

    fun logIncident(incident: SafetyCheckIncident) {
        incidents.add(incident)
    }

    fun getIncidents(): List<SafetyCheckIncident> {
        return incidents.toList()
    }

    fun clearIncidents() {
        incidents.clear()
    }
}

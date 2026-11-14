package com.example.data

/** The same local decision used by CallScreeningService. Unknown numbers are never rated safe. */
object PhoneProtection {
    fun shouldReject(normalizedPhone: String?, enabled: Boolean, isBlocked: (String) -> Boolean): Boolean =
        normalizedPhone != null && enabled && isBlocked(normalizedPhone)
}

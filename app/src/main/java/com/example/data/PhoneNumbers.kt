package com.example.data

import com.google.i18n.phonenumbers.PhoneNumberUtil

data class ParsedPhone(val e164: String, val formatted: String, val country: String)

object PhoneNumbers {
    private val util = PhoneNumberUtil.getInstance()
    fun parse(input: String): ParsedPhone? = runCatching {
        require(input.trim().matches(Regex("[+0-9() .-]{5,40}")))
        val number = util.parse(input.trim(), "PL")
        require(util.isValidNumber(number))
        ParsedPhone(
            util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164),
            util.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
            util.getRegionCodeForNumber(number) ?: "Nieznany"
        )
    }.getOrNull()
}

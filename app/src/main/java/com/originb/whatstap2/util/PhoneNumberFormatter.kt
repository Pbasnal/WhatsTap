package com.originb.whatstap2.util

import com.originb.whatstap2.BuildConfig
import com.originb.whatstap2.util.Logger
import com.originb.whatstap2.util.AppConstants

object PhoneNumberFormatter {
    private const val MIN_PHONE_NUMBER_LENGTH = 7
    private const val MAX_PHONE_NUMBER_LENGTH = 15

    data class CountryCodeRule(
        val countryCode: String,
        val prefix: String,
        val removeLeadingZero: Boolean = false
    )

    private val countryCodeRules = listOf(
        CountryCodeRule("+1", "1", true),   // US/Canada
        CountryCodeRule("+44", "44", true), // UK
        CountryCodeRule("+91", "91", true)  // India
    )

    fun normalizePhoneNumber(phoneNumber: String): String {
        var cleanedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        if (cleanedNumber.length < MIN_PHONE_NUMBER_LENGTH || cleanedNumber.length > MAX_PHONE_NUMBER_LENGTH) {
            if (BuildConfig.DEBUG) {
                Logger.w("PhoneNumberFormatter", "Invalid phone number length: ${cleanedNumber.length}")
            }
            return cleanedNumber
        }

        // Apply country code rules
        for (rule in countryCodeRules) {
            if (cleanedNumber.startsWith(rule.countryCode)) {
                cleanedNumber = if (rule.removeLeadingZero && cleanedNumber.startsWith("${rule.countryCode}0")) {
                    rule.countryCode + cleanedNumber.substring(rule.countryCode.length + 1)
                } else {
                    cleanedNumber
                }
                break
            }
        }

        return cleanedNumber
    }

    fun formatForWhatsApp(phoneNumber: String): String {
        val normalized = normalizePhoneNumber(phoneNumber)
        
        // WhatsApp typically requires international format without '+' and spaces
        return normalized.replace("+", "").replace(" ", "")
    }
}

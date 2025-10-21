package com.originb.whatstap2.util

import com.originb.whatstap2.BuildConfig
import com.originb.whatstap2.util.Logger
import com.originb.whatstap2.util.AppConstants

object PhoneNumberFormatter {

    data class CountryCodeRule(
        val countryCode: String,
        val minLength: Int,
        val startsWithPattern: String? = null
    )

    private val countryCodeRules = listOf(
        CountryCodeRule("1", 11, "1"),      // US/Canada
        CountryCodeRule("91", 12, "91"),    // India
        CountryCodeRule("44", 11, "44"),    // UK
        CountryCodeRule("49", 11, "49"),    // Germany
        CountryCodeRule("33", 10, "33"),    // France
        CountryCodeRule("39", 10, "39"),    // Italy
        CountryCodeRule("81", 10, "81"),    // Japan
        CountryCodeRule("86", 11, "86")     // China
    )

    fun normalizePhoneNumber(phoneNumber: String): String {
        var normalized = phoneNumber.replace(Regex("[^\\d]"), "")
        when {
            normalized.length == 11 && normalized.startsWith("1") -> {
                normalized = normalized.substring(1)
            }
        }
        Logger.d("PhoneNumberFormatter", "Normalized \'$phoneNumber\' -> \'$normalized\'")
        return normalized
    }

    fun formatForWhatsApp(phoneNumber: String): String {
        var cleanNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
        val hasPlus = cleanNumber.startsWith("+")
        if (hasPlus) {
            cleanNumber = cleanNumber.substring(1)
        }
        cleanNumber = cleanNumber.filter { it.isDigit() }

        Logger.d("PhoneNumberFormatter", "Formatting: \'$phoneNumber\' -> \'$cleanNumber\' (had +: $hasPlus)")

        if (cleanNumber.length < AppConstants.MIN_PHONE_NUMBER_LENGTH) {
            Logger.w("PhoneNumberFormatter", "Number too short: $cleanNumber")
            return cleanNumber
        }

        return if (cleanNumber.isNotEmpty()) {
            when {
                hasPlus || cleanNumber.length > 11 -> {
                    cleanNumber
                }
                cleanNumber.startsWith("1") && cleanNumber.length == 11 -> cleanNumber
                cleanNumber.startsWith("91") && cleanNumber.length == 12 -> cleanNumber
                cleanNumber.startsWith("44") && cleanNumber.length >= 11 -> cleanNumber
                cleanNumber.startsWith("49") && cleanNumber.length >= 11 -> cleanNumber
                cleanNumber.startsWith("33") && cleanNumber.length >= 10 -> cleanNumber
                cleanNumber.startsWith("39") && cleanNumber.length >= 10 -> cleanNumber
                cleanNumber.startsWith("81") && cleanNumber.length >= 10 -> cleanNumber
                cleanNumber.startsWith("86") && cleanNumber.length >= 11 -> cleanNumber
                cleanNumber.length == 10 && cleanNumber[0] in '2'..'9' -> {
                    "1$cleanNumber"
                }
                cleanNumber.length == 10 && cleanNumber[0] in '6'..'9' -> {
                    "91$cleanNumber"
                }
                cleanNumber.startsWith("0") && cleanNumber.length >= 10 -> {
                    "44${cleanNumber.substring(1)}"
                }
                cleanNumber.length >= 10 -> {
                    cleanNumber
                }
                else -> {
                    Logger.w("PhoneNumberFormatter", "Unclear format for number: $cleanNumber")
                    cleanNumber
                }
            }
        } else {
            ""
        }
    }
}

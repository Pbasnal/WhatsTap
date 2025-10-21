package com.originb.whatstap2.integration

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.originb.whatstap2.util.Logger
import com.originb.whatstap2.util.PhoneNumberFormatter

class WhatsAppIntegrationHandler(private val context: Context) {

    fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun tryWhatsAppChat(phoneNumber: String): Boolean {
        return try {
            val formattedNumber = PhoneNumberFormatter.formatForWhatsApp(phoneNumber)
            Logger.d("WhatsAppIntegrationHandler", "Opening WhatsApp chat with number: $formattedNumber")

            val whatsappIntents = listOf(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber")
                    setPackage("com.whatsapp")
                },
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("whatsapp://send?phone=$formattedNumber")
                    setPackage("com.whatsapp")
                },
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber")
                    setPackage("com.whatsapp.w4b")
                }
            )

            for ((index, intent) in whatsappIntents.withIndex()) {
                try {
                    Logger.d("WhatsAppIntegrationHandler", "Trying WhatsApp method ${index + 1}: ${intent.data}")
                    val resolveInfo = context.packageManager.resolveActivity(intent, 0)
                    if (resolveInfo != null) {
                        context.startActivity(intent)
                        Logger.d("WhatsAppIntegrationHandler", "WhatsApp chat opened successfully: ${intent.data}")
                        return true
                    }
                } catch (e: Exception) {
                    Logger.w("WhatsAppIntegrationHandler", "WhatsApp intent failed: ${intent.data}", e)
                    continue
                }
            }

            Logger.w("WhatsAppIntegrationHandler", "All WhatsApp intents failed for number: $formattedNumber")
            false
        } catch (e: Exception) {
            Logger.e("WhatsAppIntegrationHandler", "Error opening WhatsApp chat", e)
            false
        }
    }
}


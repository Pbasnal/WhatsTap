package com.originb.whatstap2.integration

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.originb.whatstap2.domain.model.Contact
import com.originb.whatstap2.domain.model.Result
import com.originb.whatstap2.util.Logger
import com.originb.whatstap2.util.PhoneTypeUtils

class CallHandler(private val context: Context) {

    private val whatsAppIntegrationHandler = WhatsAppIntegrationHandler(context)

    fun handleContactCall(contact: Contact): Result<Unit> {
        return if (contact.isWhatsAppContact) {
            if (!whatsAppIntegrationHandler.isWhatsAppInstalled()) {
                Toast.makeText(context, "WhatsApp not installed, making regular call", Toast.LENGTH_SHORT).show()
                makeVoiceCall(contact.phoneNumber)
            } else {
                Logger.d("CallHandler", "Opening WhatsApp for: ${contact.name}")
                if (!whatsAppIntegrationHandler.tryWhatsAppChat(contact.phoneNumber)) {
                    Toast.makeText(context, "WhatsApp not available, making regular call", Toast.LENGTH_SHORT).show()
                    makeVoiceCall(contact.phoneNumber)
                } else {
                    Result.Success(Unit)
                }
            }
        } else {
            makeVoiceCall(contact.phoneNumber)
        }
    }

    private fun makeVoiceCall(phoneNumber: String): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(intent)
                Result.Success(Unit)
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                context.startActivity(dialIntent)
                Toast.makeText(context, "CALL_PHONE permission not granted, opening dialer", Toast.LENGTH_SHORT).show()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to make call", Toast.LENGTH_SHORT).show()
            Logger.e("CallHandler", "Error making voice call", e)
            Result.Error("Failed to make call: ${e.message}", e)
        }
    }
}


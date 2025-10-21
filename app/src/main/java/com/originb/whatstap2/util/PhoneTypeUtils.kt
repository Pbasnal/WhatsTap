package com.originb.whatstap2.util

import android.provider.ContactsContract

object PhoneTypeUtils {

    fun getPhoneTypeLabel(type: Int, customLabel: String?): String {
        val result = when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Pager"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "Work Pager"
            ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> "Assistant"
            ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "MMS"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> {
                val label = customLabel?.lowercase() ?: "custom"
                if (label.contains("whatsapp") || label.contains("wa")) {
                    customLabel ?: "WhatsApp"
                } else {
                    customLabel ?: "Custom"
                }
            }
            else -> "Phone"
        }
        return result
    }
    
    fun isWhatsAppLabel(label: String?): Boolean {
        val lowerLabel = label?.lowercase() ?: return false
        return lowerLabel.contains("whatsapp") || lowerLabel.contains("wa")
    }
}


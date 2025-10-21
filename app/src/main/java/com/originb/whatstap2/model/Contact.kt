package com.originb.whatstap2.domain.model

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val phoneLabel: String? = null,
    val photoUri: String? = null,
    val isWhatsAppContact: Boolean = false
) 
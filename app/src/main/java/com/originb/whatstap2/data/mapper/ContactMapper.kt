package com.originb.whatstap2.data.mapper

import com.originb.whatstap2.data.local.ContactEntity
import com.originb.whatstap2.domain.model.Contact
import com.originb.whatstap2.util.PhoneTypeUtils

fun ContactEntity.toDomain(): Contact {
    val isWhatsApp = PhoneTypeUtils.isWhatsAppLabel(this.phoneLabel)
    return Contact(
        id = this.id,
        name = this.name,
        phoneNumber = this.phoneNumber,
        phoneLabel = this.phoneLabel,
        photoUri = this.photoUri,
        isWhatsAppContact = isWhatsApp
    )
}

fun Contact.toEntity(): ContactEntity {
    return ContactEntity(
        id = this.id,
        name = this.name,
        phoneNumber = this.phoneNumber,
        phoneLabel = this.phoneLabel,
        photoUri = this.photoUri
    )
}


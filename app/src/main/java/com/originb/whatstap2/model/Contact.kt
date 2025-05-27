package com.originb.whatstap2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val phoneLabel: String? = null,
    val photoUri: String? = null,
    val whatsappNumber: String? = null
) 
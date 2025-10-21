package com.originb.whatstap2.domain.repository

import androidx.lifecycle.LiveData
import com.originb.whatstap2.model.Contact

interface ContactRepository {
    fun getAllContacts(): LiveData<List<Contact>>
    suspend fun insert(contact: Contact): Long
    suspend fun update(contact: Contact)
    suspend fun delete(contact: Contact)
    suspend fun getContactById(id: Long): Contact?
    suspend fun getAllContactsSync(): List<Contact>
    suspend fun insertSync(contact: Contact): Long
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact?
    suspend fun insertOrReplaceSync(contact: Contact): Long
    suspend fun deleteAllContacts()
    suspend fun getAllPhoneNumbers(): List<String>
}


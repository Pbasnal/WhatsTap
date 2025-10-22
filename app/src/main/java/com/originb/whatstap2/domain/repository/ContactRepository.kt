package com.originb.whatstap2.domain.repository

import androidx.lifecycle.LiveData
import com.originb.whatstap2.domain.model.Contact

interface ContactRepository {
    fun getAllContacts(): LiveData<List<Contact>>
    suspend fun insert(contact: Contact): Long
    suspend fun update(contact: Contact)
    suspend fun delete(contact: Contact)
    suspend fun getContactById(id: Long): Contact?
    suspend fun insertSync(contact: Contact): Long
    suspend fun updateSync(contact: Contact)
    suspend fun deleteSync(contact: Contact)
}


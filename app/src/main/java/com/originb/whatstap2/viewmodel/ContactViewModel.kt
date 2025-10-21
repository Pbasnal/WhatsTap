package com.originb.whatstap2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.originb.whatstap2.data.ContactDatabase
import com.originb.whatstap2.domain.repository.ContactRepository
import com.originb.whatstap2.model.Contact
import com.originb.whatstap2.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactRepository

    init {
        repository = ContactDatabase.getContactRepository(application)
    }

    val allContacts: LiveData<List<Contact>> = repository.getAllContacts()

    fun insert(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertedId = repository.insert(contact)
            Logger.d("ContactViewModel", "Contact inserted with ID: $insertedId")
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(contact)
        }
    }

    fun update(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(contact)
            Logger.d("ContactViewModel", "Contact updated: ${contact.name} with ID: ${contact.id}")
        }
    }

    suspend fun getContactById(id: Long): Contact? {
        return repository.getContactById(id)
    }
    
    suspend fun getAllContactsSync(): List<Contact> {
        return repository.getAllContactsSync()
    }
    
    suspend fun insertSync(contact: Contact): Long {
        return repository.insertSync(contact)
    }
    
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        return repository.getContactByPhoneNumber(phoneNumber)
    }
    
    suspend fun insertOrReplaceSync(contact: Contact): Long {
        return repository.insertOrReplaceSync(contact)
    }
    
    suspend fun deleteAllContacts() {
        repository.deleteAllContacts()
    }
    
    suspend fun getAllPhoneNumbers(): List<String> {
        return repository.getAllPhoneNumbers()
    }
} 
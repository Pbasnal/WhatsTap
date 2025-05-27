package com.originb.whatstap2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.originb.whatstap2.data.ContactDatabase
import com.originb.whatstap2.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ContactDatabase.getDatabase(application)
    private val contactDao = database.contactDao()

    val allContacts: LiveData<List<Contact>> = contactDao.getAllContacts()

    fun insert(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertedId = contactDao.insert(contact)
            android.util.Log.d("ContactViewModel", "Contact inserted with ID: $insertedId")
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactDao.delete(contact)
        }
    }

    fun update(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactDao.update(contact)
            android.util.Log.d("ContactViewModel", "Contact updated: ${contact.name} with ID: ${contact.id}")
        }
    }

    suspend fun getContactById(id: Long): Contact? {
        return contactDao.getContactById(id)
    }
} 
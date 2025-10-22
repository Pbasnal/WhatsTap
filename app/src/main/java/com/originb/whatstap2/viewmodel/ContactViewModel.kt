package com.originb.whatstap2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.originb.whatstap2.data.ContactDatabase
import com.originb.whatstap2.domain.model.Contact
import com.originb.whatstap2.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactRepository = ContactDatabase.getContactRepository(application)

    val allContacts: LiveData<List<Contact>> = repository.getAllContacts()

    fun insert(contact: Contact) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(contact)
    }

    fun update(contact: Contact) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(contact)
    }

    fun delete(contact: Contact) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(contact)
    }

    suspend fun getContactById(id: Long): Contact? {
        return repository.getContactById(id)
    }
} 
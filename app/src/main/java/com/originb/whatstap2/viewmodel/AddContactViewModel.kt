package com.originb.whatstap2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.originb.whatstap2.data.ContactDatabase
import com.originb.whatstap2.domain.model.Contact
import com.originb.whatstap2.domain.repository.ContactRepository
import com.originb.whatstap2.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddContactViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContactRepository = ContactDatabase.getContactRepository(application)

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            if (contact.id == 0L) {
                val insertedId = repository.insert(contact)
                Logger.d("AddContactViewModel", "New contact inserted with ID: $insertedId")
            } else {
                repository.update(contact)
                Logger.d("AddContactViewModel", "Contact updated with ID: ${contact.id}")
            }
        }
    }

    suspend fun getContactById(id: Long): Contact? {
        return repository.getContactById(id)
    }
}


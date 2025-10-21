package com.originb.whatstap2.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.originb.whatstap2.data.ContactDao
import com.originb.whatstap2.data.mapper.toDomain
import com.originb.whatstap2.data.mapper.toEntity
import com.originb.whatstap2.domain.repository.ContactRepository
import com.originb.whatstap2.domain.model.Contact

class ContactRepositoryImpl(private val contactDao: ContactDao) : ContactRepository {
    override fun getAllContacts(): LiveData<List<Contact>> = contactDao.getAllContacts().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun insert(contact: Contact): Long = contactDao.insert(contact.toEntity())

    override suspend fun update(contact: Contact) = contactDao.update(contact.toEntity())

    override suspend fun delete(contact: Contact) = contactDao.delete(contact.toEntity())

    override suspend fun getContactById(id: Long): Contact? = contactDao.getContactById(id)?.toDomain()

    override suspend fun getAllContactsSync(): List<Contact> = contactDao.getAllContactsSync().map { it.toDomain() }

    override suspend fun insertSync(contact: Contact): Long = contactDao.insert(contact.toEntity())

    override suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? = contactDao.getContactByPhoneNumber(phoneNumber)?.toDomain()

    override suspend fun insertOrReplaceSync(contact: Contact): Long = contactDao.insertOrReplace(contact.toEntity())

    override suspend fun deleteAllContacts() = contactDao.deleteAllContacts()

    override suspend fun getAllPhoneNumbers(): List<String> = contactDao.getAllPhoneNumbers()
}

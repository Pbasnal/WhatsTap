package com.originb.whatstap2.domain.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.originb.whatstap2.data.mapper.toDomain
import com.originb.whatstap2.data.mapper.toEntity
import com.originb.whatstap2.domain.model.Contact
import com.originb.whatstap2.domain.model.Result
import com.originb.whatstap2.domain.model.SyncResult
import com.originb.whatstap2.util.Logger
import com.originb.whatstap2.util.PhoneNumberFormatter
import com.originb.whatstap2.util.PhoneTypeUtils
import com.originb.whatstap2.viewmodel.ContactViewModel

class ContactSyncRepository(private val context: Context, private val viewModel: ContactViewModel) {

    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun syncStarredContacts(): Result<SyncResult> {
        return try {
            val systemContacts = fetchStarredContactsFromSystem()
            val existingContactsMap = getExistingContactsMap()

            var updatedCount = 0
            var insertedCount = 0

            systemContacts.forEach { contact ->
                val normalizedNumber = PhoneNumberFormatter.normalizePhoneNumber(contact.phoneNumber)
                val existingContact = existingContactsMap[normalizedNumber]

                if (existingContact != null) {
                    val nameChanged = existingContact.name != contact.name
                    val labelChanged = existingContact.phoneLabel != contact.phoneLabel
                    val photoChanged = contact.photoUri != null && existingContact.photoUri != contact.photoUri

                    if (nameChanged || labelChanged || photoChanged) {
                        val updatedContact = existingContact.copy(
                            name = contact.name,
                            phoneLabel = contact.phoneLabel,
                            photoUri = contact.photoUri ?: existingContact.photoUri
                        )
                        viewModel.update(updatedContact)
                        updatedCount++
                    }
                } else {
                    viewModel.insert(contact)
                    insertedCount++
                }
            }
            Logger.d("ContactSyncRepository", "Sync completed: $insertedCount new, $updatedCount updated, ${systemContacts.size} total starred contacts processed")
            Result.Success(SyncResult(insertedCount, updatedCount, insertedCount > 0 || updatedCount > 0))
        } catch (e: Exception) {
            Logger.e("ContactSyncRepository", "Error during contact sync", e)
            Result.Error("Error during contact sync: ${e.message}", e)
        }
    }

    private suspend fun fetchStarredContactsFromSystem(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = ?"
        val selectionArgs = arrayOf("1")

        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )?.use { cursor ->
            Logger.d("ContactSyncRepository", "Found ${cursor.count} starred contacts in system")

            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                val customLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))

                val phoneLabel = PhoneTypeUtils.getPhoneTypeLabel(type, customLabel)

                val contact = Contact(
                    id = 0, // Will be ignored by Room, but kept for domain model consistency
                    name = name ?: "Unknown",
                    phoneNumber = number ?: "",
                    phoneLabel = phoneLabel,
                    photoUri = photoUri,
                    isWhatsAppContact = PhoneTypeUtils.isWhatsAppLabel(phoneLabel)
                )
                contacts.add(contact)
                Logger.d("ContactSyncRepository", "Added starred contact: ${contact.name} - ${contact.phoneNumber} (${phoneLabel})")
            }
        }
        return contacts
    }

    private suspend fun getExistingContactsMap(): Map<String, Contact> {
        return viewModel.getAllContactsSync().associateBy { PhoneNumberFormatter.normalizePhoneNumber(it.phoneNumber) }
    }
}

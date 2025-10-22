package com.originb.whatstap2.domain.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
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
            val starredContacts = fetchStarredContactsFromSystem()
            val existingContacts = getExistingContactsMap()

            var insertedCount = 0
            var updatedCount = 0

            starredContacts.forEach { contact ->
                val existingContact = existingContacts[contact.phoneNumber]
                
                if (existingContact == null) {
                    viewModel.insert(contact)
                    insertedCount++
                } else if (needsUpdate(existingContact, contact)) {
                    val updatedContact = contact.copy(id = existingContact.id)
                    viewModel.update(updatedContact)
                    updatedCount++
                }
            }

            val syncResult = SyncResult(
                inserted = insertedCount,
                updated = updatedCount,
                hasChanges = insertedCount > 0 || updatedCount > 0
            )

            Logger.logContactSync(insertedCount, updatedCount)
            Result.Success(syncResult)
        } catch (e: Exception) {
            Logger.e("ContactSyncRepository", "Error syncing contacts", e)
            Result.Error("Failed to sync contacts: ${e.message}")
        }
    }

    private suspend fun fetchStarredContactsFromSystem(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.STARRED
        )

        val selection = "${ContactsContract.Contacts.STARRED} = ?"
        val selectionArgs = arrayOf("1")

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { contactsCursor ->
            val contactIdIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)
            val contactNameIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (contactsCursor.moveToNext()) {
                if (contactIdIndex == -1 || contactNameIndex == -1) {
                    Logger.w("ContactSyncRepository", "Invalid column indices")
                    continue
                }

                val contactId = contactsCursor.getLong(contactIdIndex)
                val contactName = contactsCursor.getString(contactNameIndex) ?: "Unknown"

                // Get phone details
                val phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )

                phoneCursor?.use { phonesForContact ->
                    val phoneNumberIndex = phonesForContact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val phoneTypeIndex = phonesForContact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val phoneLabelIndex = phonesForContact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                    while (phonesForContact.moveToNext()) {
                        if (phoneNumberIndex == -1 || phoneTypeIndex == -1) {
                            Logger.w("ContactSyncRepository", "Invalid phone column indices")
                            continue
                        }

                        val phoneNumber = phonesForContact.getString(phoneNumberIndex)
                        val phoneType = phonesForContact.getInt(phoneTypeIndex)
                        val phoneLabel = if (phoneLabelIndex != -1) phonesForContact.getString(phoneLabelIndex) else null

                        if (phoneNumber.isNullOrBlank()) {
                            continue
                        }

                        val normalizedNumber = PhoneNumberFormatter.normalizePhoneNumber(phoneNumber)
                        val formattedLabel = PhoneTypeUtils.getPhoneTypeLabel(phoneType, phoneLabel)
                        val isWhatsAppContact = PhoneTypeUtils.isWhatsAppLabel(formattedLabel)

                        contacts.add(
                            Contact(
                                id = 0, // Room will generate the ID
                                name = contactName,
                                phoneNumber = normalizedNumber,
                                phoneLabel = formattedLabel,
                                isWhatsAppContact = isWhatsAppContact
                            )
                        )
                    }
                }
            }
        }

        return contacts
    }

    private suspend fun getExistingContactsMap(): Map<String, Contact> {
        return viewModel.allContacts.value?.associateBy { it.phoneNumber } ?: emptyMap()
    }

    private fun needsUpdate(existingContact: Contact, newContact: Contact): Boolean {
        return existingContact.name != newContact.name ||
               existingContact.phoneLabel != newContact.phoneLabel ||
               existingContact.isWhatsAppContact != newContact.isWhatsAppContact
    }
}

package com.originb.whatstap2.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.originb.whatstap2.model.Contact

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): LiveData<List<Contact>>

    @Insert
    suspend fun insert(contact: Contact): Long

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): Contact?
    
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsSync(): List<Contact>
} 
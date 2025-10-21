package com.originb.whatstap2.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.originb.whatstap2.data.local.ContactEntity

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): LiveData<List<ContactEntity>>

    @Insert
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?
    
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsSync(): List<ContactEntity>
    
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getContactByPhoneNumber(phoneNumber: String): ContactEntity?
    
    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(contact: ContactEntity): Long
    
    @Query("SELECT phoneNumber FROM contacts")
    suspend fun getAllPhoneNumbers(): List<String>
} 
package com.originb.whatstap2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.originb.whatstap2.data.local.ContactEntity
import com.originb.whatstap2.domain.repository.ContactRepository
import com.originb.whatstap2.data.local.ContactRepositoryImpl

@Database(entities = [ContactEntity::class], version = 2)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: ContactDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN phoneLabel TEXT")
            }
        }

        fun getDatabase(context: Context): ContactDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contact_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getContactRepository(context: Context): ContactRepository {
            return ContactRepositoryImpl(getDatabase(context).contactDao())
        }
    }
} 
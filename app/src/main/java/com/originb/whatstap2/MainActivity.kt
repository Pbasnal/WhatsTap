package com.originb.whatstap2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.originb.whatstap2.adapter.ContactAdapter
import com.originb.whatstap2.databinding.ActivityMainBinding
import com.originb.whatstap2.model.Contact
import com.originb.whatstap2.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ContactViewModel by viewModels()
    private lateinit var adapter: ContactAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadFavoriteContacts()
        } else {
            showPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        observeContacts()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter(
            onContactClick = { contact ->
                openWhatsApp(contact.phoneNumber)
            },
            onEditClick = { contact ->
                editContact(contact)
            }
        )
        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addContactFab.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }
    }

    private fun editContact(contact: Contact) {
        val intent = Intent(this, AddContactActivity::class.java).apply {
            putExtra("contact_id", contact.id)
            putExtra("contact_name", contact.name)
            putExtra("contact_number", contact.phoneNumber)
            contact.photoUri?.let { uriString ->
                val uri = Uri.parse(uriString)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("contact_photo", uriString)
            }
        }
        startActivity(intent)
    }

    private fun observeContacts() {
        viewModel.allContacts.observe(this) { contacts ->
            adapter.submitList(contacts)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            loadFavoriteContacts()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs permissions to function as a launcher. Please grant the permissions in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun loadFavoriteContacts() {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
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
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))

                contacts.add(Contact(id, name, number, photoUri))
            }
        }

        // Insert contacts into the database
        lifecycleScope.launch {
            contacts.forEach { contact ->
                viewModel.insert(contact)
            }
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=${phoneNumber.filter { it.isDigit() }}")
                setPackage("com.whatsapp")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        // Do nothing to prevent exiting the launcher
    }
} 
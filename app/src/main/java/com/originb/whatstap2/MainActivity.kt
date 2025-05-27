package com.originb.whatstap2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.originb.whatstap2.model.Contact
import com.originb.whatstap2.viewmodel.ContactViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    private val viewModel: ContactViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            checkStarredContactsInSystem()
            loadFavoriteContacts()
        } else {
            showPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onAddContact = { startActivity(Intent(this, AddContactActivity::class.java)) },
                        onEditContact = { contact -> editContact(contact) },
                        onContactClick = { contact -> openWhatsApp(contact.phoneNumber) },
                        onSyncContacts = { syncStarredContacts() }
                    )
                }
            }
        }

        checkPermissions()
    }

    private fun editContact(contact: Contact) {
        val intent = Intent(this, AddContactActivity::class.java).apply {
            putExtra("contact_id", contact.id)
            putExtra("contact_name", contact.name)
            putExtra("contact_number", contact.phoneNumber)
            contact.photoUri?.let { uriString ->
                // Only grant URI permission for non-contact URIs
                val uri = Uri.parse(uriString)
                if (!uri.authority.equals("com.android.contacts")) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                putExtra("contact_photo", uriString)
            }
        }
        startActivity(intent)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            checkStarredContactsInSystem()
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
        android.util.Log.d("MainActivity", "Loading favorite contacts from system...")
        
        lifecycleScope.launch(Dispatchers.IO) {
            // First check if we already have contacts in the database
            val existingContacts = viewModel.getAllContactsSync()
            android.util.Log.d("MainActivity", "Existing contacts in database: ${existingContacts.size}")
            
            // Only load from system if database is empty or we want to sync
            if (existingContacts.isEmpty()) {
                loadStarredContactsFromSystem()
            }
        }
    }
    
    private suspend fun loadStarredContactsFromSystem() {
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

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )?.use { cursor ->
                android.util.Log.d("MainActivity", "Found ${cursor.count} starred contacts in system")
                
                while (cursor.moveToNext()) {
                    val systemContactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))

                    // Create contact with Room auto-generated ID (0) but store system ID for reference
                    val contact = Contact(
                        id = 0, // Let Room auto-generate
                        name = name ?: "Unknown",
                        phoneNumber = number ?: "",
                        photoUri = photoUri
                    )
                    
                    contacts.add(contact)
                    android.util.Log.d("MainActivity", "Added starred contact: ${contact.name} - ${contact.phoneNumber}")
                }
            }

            // Insert all contacts into Room database
            contacts.forEach { contact ->
                val insertedId = viewModel.insertSync(contact)
                android.util.Log.d("MainActivity", "Inserted contact ${contact.name} with ID: $insertedId")
            }
            
            android.util.Log.d("MainActivity", "Finished loading ${contacts.size} starred contacts")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading starred contacts", e)
        }
    }
    
    // Method to check if there are any starred contacts in the system
    private fun checkStarredContactsInSystem() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = ?"
                val selectionArgs = arrayOf("1")

                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    android.util.Log.d("MainActivity", "Total starred contacts in system: ${cursor.count}")
                    if (cursor.count == 0) {
                        android.util.Log.w("MainActivity", "No starred contacts found in system. Please star some contacts first.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error checking starred contacts", e)
            }
        }
    }
    
    private fun syncStarredContacts() {
        android.util.Log.d("MainActivity", "Manual sync of starred contacts requested")
        lifecycleScope.launch(Dispatchers.IO) {
            // Force sync regardless of existing contacts
            loadStarredContactsFromSystem()
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
        super.onBackPressed()
        // Do nothing to prevent exiting the launcher
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ContactViewModel,
    onAddContact: () -> Unit,
    onEditContact: (Contact) -> Unit,
    onContactClick: (Contact) -> Unit,
    onSyncContacts: () -> Unit
) {
    val contacts by viewModel.allContacts.observeAsState(initial = emptyList())
    val context = LocalContext.current
    
    // Debug observer - you can place a breakpoint here
    LaunchedEffect(contacts) {
        android.util.Log.d("MainActivity", "Contacts list updated. Count: ${contacts.size}")
        contacts.forEachIndexed { index, contact ->
            android.util.Log.d("MainActivity", "Contact $index: ${contact.name} (ID: ${contact.id})")
        }
    }

    Scaffold(
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = onSyncContacts,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Contacts",
                        tint = Color.White
                    )
                }
                FloatingActionButton(
                    onClick = onAddContact,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_contact),
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    onEditClick = { onEditContact(contact) },
                    onClick = { onContactClick(contact) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactItem(
    contact: Contact,
    onEditClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = contact.photoUri ?: R.drawable.circle_background,
                contentDescription = stringResource(R.string.contact_photo),
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp),
//                error = R.drawable.circle_background
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_contact),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 
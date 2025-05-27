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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
                        onContactClick = { contact -> handleContactCall(contact) },
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
            putExtra("contact_label", contact.phoneLabel)
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CALL_PHONE
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
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
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
                    val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                    val customLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))

                    // Get the phone label based on type or custom label
                    val phoneLabel = getPhoneTypeLabel(type, customLabel)

                    // Create contact with Room auto-generated ID (0) but store system ID for reference
                    val contact = Contact(
                        id = 0, // Let Room auto-generate
                        name = name ?: "Unknown",
                        phoneNumber = number ?: "",
                        phoneLabel = phoneLabel,
                        photoUri = photoUri
                    )
                    
                    contacts.add(contact)
                    android.util.Log.d("MainActivity", "Added starred contact: ${contact.name} - ${contact.phoneNumber} (${phoneLabel})")
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

    private fun getPhoneTypeLabel(type: Int, customLabel: String?): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Pager"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "Work Pager"
            ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> "Assistant"
            ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "MMS"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> {
                // Check if custom label contains WhatsApp-related terms
                val label = customLabel?.lowercase() ?: "custom"
                if (label.contains("whatsapp") || label.contains("wa")) {
                    customLabel ?: "WhatsApp"
                } else {
                    customLabel ?: "Custom"
                }
            }
            else -> "Phone"
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

    private fun handleContactCall(contact: Contact) {
        val phoneLabel = contact.phoneLabel?.lowercase() ?: ""
        
        if (phoneLabel.contains("whatsapp") || phoneLabel.contains("wa")) {
            // Check if WhatsApp is installed first
            if (!isWhatsAppInstalled()) {
                Toast.makeText(this, "WhatsApp not installed, making regular call", Toast.LENGTH_SHORT).show()
                makeVoiceCall(contact.phoneNumber)
                return
            }
            
            // Show the formatted number for debugging
            val formattedNumber = formatPhoneNumberForWhatsApp(contact.phoneNumber)
            android.util.Log.d("MainActivity", "Opening WhatsApp for: ${contact.name}")
            android.util.Log.d("MainActivity", "Original: ${contact.phoneNumber}, Formatted: $formattedNumber")
            
            // WhatsApp doesn't allow direct video call triggering from third-party apps
            // Open WhatsApp chat and provide clear instructions
            if (!tryWhatsAppChat(contact.phoneNumber)) {
                // If WhatsApp chat fails, fallback to normal call
                Toast.makeText(this, "WhatsApp not available, making regular call", Toast.LENGTH_SHORT).show()
                makeVoiceCall(contact.phoneNumber)
            } else {
                // Show a more helpful message with specific instructions
                showWhatsAppCallInstructions(contact.name)
            }
        } else {
            // Trigger normal voice call
            makeVoiceCall(contact.phoneNumber)
        }
    }

    private fun showWhatsAppCallInstructions(contactName: String) {
        // Create a more informative dialog instead of just a toast
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("WhatsApp Video Call")
            .setMessage("WhatsApp chat with $contactName is now open.\n\nTo start a video call:\n• Tap the video call button (📹) at the top\n• Or tap the phone icon (📞) for voice call")
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                packageManager.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun formatPhoneNumberForWhatsApp(phoneNumber: String): String {
        // Remove all non-digit characters except +
        var cleanNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        // Remove + if present and handle it separately
        val hasPlus = cleanNumber.startsWith("+")
        if (hasPlus) {
            cleanNumber = cleanNumber.substring(1)
        }
        
        // Only keep digits now
        cleanNumber = cleanNumber.filter { it.isDigit() }
        
        android.util.Log.d("MainActivity", "Formatting: '$phoneNumber' -> '$cleanNumber' (had +: $hasPlus)")
        
        // If number is too short, return as-is
        if (cleanNumber.length < 7) {
            android.util.Log.w("MainActivity", "Number too short: $cleanNumber")
            return cleanNumber
        }
        
        // If the number doesn't start with a country code, try to add one
        return if (cleanNumber.isNotEmpty()) {
            when {
                // If it already had a + or looks like it has a country code, use as-is
                hasPlus || cleanNumber.length > 11 -> {
                    cleanNumber
                }
                
                // Common country code patterns (more conservative)
                cleanNumber.startsWith("1") && cleanNumber.length == 11 -> cleanNumber // US/Canada
                cleanNumber.startsWith("91") && cleanNumber.length == 12 -> cleanNumber // India
                cleanNumber.startsWith("44") && cleanNumber.length >= 11 -> cleanNumber // UK
                cleanNumber.startsWith("49") && cleanNumber.length >= 11 -> cleanNumber // Germany
                cleanNumber.startsWith("33") && cleanNumber.length >= 10 -> cleanNumber // France
                cleanNumber.startsWith("39") && cleanNumber.length >= 10 -> cleanNumber // Italy
                cleanNumber.startsWith("81") && cleanNumber.length >= 10 -> cleanNumber // Japan
                cleanNumber.startsWith("86") && cleanNumber.length >= 11 -> cleanNumber // China
                
                // US/Canada number without country code (10 digits, doesn't start with 0 or 1)
                cleanNumber.length == 10 && cleanNumber[0] in '2'..'9' -> {
                    "1$cleanNumber"
                }
                
                // India number without country code (10 digits starting with 6-9)
                cleanNumber.length == 10 && cleanNumber[0] in '6'..'9' -> {
                    "91$cleanNumber"
                }
                
                // UK number without country code (starts with 0, remove 0 and add 44)
                cleanNumber.startsWith("0") && cleanNumber.length >= 10 -> {
                    "44${cleanNumber.substring(1)}"
                }
                
                // For other cases, if it looks like it might already have a country code, use as-is
                cleanNumber.length >= 10 -> {
                    cleanNumber
                }
                
                // Too short, return as-is
                else -> {
                    android.util.Log.w("MainActivity", "Unclear format for number: $cleanNumber")
                    cleanNumber
                }
            }
        } else {
            ""
        }
    }

    private fun tryWhatsAppDirectCall(phoneNumber: String): Boolean {
        return try {
            val formattedNumber = formatPhoneNumberForWhatsApp(phoneNumber)
            android.util.Log.d("MainActivity", "Trying WhatsApp direct call with number: $formattedNumber")
            
            // Try various direct call methods
            val callIntents = listOf(
                // Method 1: WhatsApp call intent (most direct)
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("whatsapp://call?phone=$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 2: WhatsApp video call intent
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("whatsapp://videocall?phone=$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 3: WhatsApp internal call action
                Intent().apply {
                    action = "android.intent.action.CALL"
                    data = Uri.parse("whatsapp://call/$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 4: Direct JID call (WhatsApp internal)
                Intent().apply {
                    action = "com.whatsapp.intent.action.CALL"
                    putExtra("jid", "$formattedNumber@s.whatsapp.net")
                    putExtra("video", true)
                    setPackage("com.whatsapp")
                },
                // Method 5: WhatsApp contact with call action
                Intent().apply {
                    action = Intent.ACTION_CALL
                    data = Uri.parse("whatsapp://contact/$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 6: Try with wa.me but with call parameter
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("https://wa.me/$formattedNumber?action=call")
                    setPackage("com.whatsapp")
                },
                // Method 6b: Try wa.me with video call parameter
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("https://wa.me/$formattedNumber?call=video")
                    setPackage("com.whatsapp")
                },
                // Method 6c: Try wa.me with call type
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("https://wa.me/$formattedNumber?type=video_call")
                    setPackage("com.whatsapp")
                },
                // Method 7: WhatsApp with tel: scheme
                Intent().apply {
                    action = Intent.ACTION_CALL
                    data = Uri.parse("tel:whatsapp:$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 8: Try opening WhatsApp with specific call activity
                Intent().apply {
                    setClassName("com.whatsapp", "com.whatsapp.voipcalling.VoipCallingActivity")
                    putExtra("jid", "$formattedNumber@s.whatsapp.net")
                    putExtra("video_call", true)
                },
                // Method 9: Try WhatsApp's main activity with call extras
                Intent().apply {
                    setClassName("com.whatsapp", "com.whatsapp.Main")
                    putExtra("jid", "$formattedNumber@s.whatsapp.net")
                    putExtra("call_type", "video")
                    action = Intent.ACTION_VIEW
                },
                // Method 10: Try using Android's call intent with WhatsApp scheme
                Intent().apply {
                    action = Intent.ACTION_CALL
                    data = Uri.parse("whatsapp:$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 11: Try WhatsApp protocol with video parameter
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("whatsapp://video/$formattedNumber")
                    setPackage("com.whatsapp")
                }
            )
            
            for ((index, intent) in callIntents.withIndex()) {
                try {
                    android.util.Log.d("MainActivity", "Trying call method ${index + 1}: ${intent.action} - ${intent.data}")
                    
                    // Check if the intent can be resolved
                    val resolveInfo = packageManager.resolveActivity(intent, 0)
                    if (resolveInfo != null) {
                        startActivity(intent)
                        android.util.Log.d("MainActivity", "WhatsApp call intent succeeded: ${intent.data}")
                        return true
                    } else {
                        android.util.Log.d("MainActivity", "WhatsApp call intent cannot be resolved: ${intent.data}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "WhatsApp call intent failed: ${intent.data}", e)
                    continue
                }
            }
            
            android.util.Log.w("MainActivity", "All WhatsApp call intents failed for number: $formattedNumber")
            false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in tryWhatsAppDirectCall", e)
            false
        }
    }

    private fun tryWhatsAppVideoCall(phoneNumber: String): Boolean {
        return try {
            val formattedNumber = formatPhoneNumberForWhatsApp(phoneNumber)
            android.util.Log.d("MainActivity", "Trying WhatsApp video call with number: $formattedNumber")
            
            // Try multiple WhatsApp video call approaches
            val videoCallIntents = listOf(
                // Method 1: Direct video call intent (newer WhatsApp versions)
                Intent().apply {
                    action = "android.intent.action.VIEW"
                    setPackage("com.whatsapp")
                    data = Uri.parse("whatsapp://video_call?phone=$formattedNumber")
                },
                // Method 2: WhatsApp call intent with video flag
                Intent().apply {
                    action = "android.intent.action.VIEW"
                    setPackage("com.whatsapp")
                    data = Uri.parse("whatsapp://call?phone=$formattedNumber&video=true")
                },
                // Method 3: Internal WhatsApp video call intent
                Intent().apply {
                    action = "com.whatsapp.intent.action.CALL"
                    setPackage("com.whatsapp")
                    putExtra("jid", "$formattedNumber@s.whatsapp.net")
                    putExtra("video", true)
                },
                // Method 4: WhatsApp contact intent with call extra
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    setPackage("com.whatsapp")
                    data = Uri.parse("whatsapp://send?phone=$formattedNumber")
                    putExtra("call_type", "video")
                    putExtra("video_call", true)
                }
            )
            
            for (intent in videoCallIntents) {
                try {
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        android.util.Log.d("MainActivity", "WhatsApp video call intent succeeded: ${intent.data}")
                        return true
                    }
                } catch (e: Exception) {
                    android.util.Log.d("MainActivity", "WhatsApp video call intent failed: ${intent.data}", e)
                    continue
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "All WhatsApp video call attempts failed", e)
            false
        }
    }

    private fun tryWhatsAppChat(phoneNumber: String): Boolean {
        return try {
            val formattedNumber = formatPhoneNumberForWhatsApp(phoneNumber)
            android.util.Log.d("MainActivity", "Opening WhatsApp chat with number: $formattedNumber")
            
            // Use only the most reliable WhatsApp methods
            val whatsappIntents = listOf(
                // Method 1: Standard wa.me link (most reliable)
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 2: WhatsApp protocol with send
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("whatsapp://send?phone=$formattedNumber")
                    setPackage("com.whatsapp")
                },
                // Method 3: Try WhatsApp Business if regular WhatsApp fails
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber")
                    setPackage("com.whatsapp.w4b")
                }
            )
            
            for ((index, intent) in whatsappIntents.withIndex()) {
                try {
                    android.util.Log.d("MainActivity", "Trying WhatsApp method ${index + 1}: ${intent.data}")
                    
                    // Check if the intent can be resolved
                    val resolveInfo = packageManager.resolveActivity(intent, 0)
                    if (resolveInfo != null) {
                        startActivity(intent)
                        android.util.Log.d("MainActivity", "WhatsApp chat opened successfully: ${intent.data}")
                        return true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "WhatsApp intent failed: ${intent.data}", e)
                    continue
                }
            }
            
            android.util.Log.w("MainActivity", "All WhatsApp intents failed for number: $formattedNumber")
            false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error opening WhatsApp chat", e)
            false
        }
    }

    private fun makeVoiceCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            
            // Check if we have CALL_PHONE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent)
            } else {
                // Request permission or use ACTION_DIAL instead
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(dialIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to make call", Toast.LENGTH_SHORT).show()
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 2 columns for card layout
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f), // Makes the card taller than it is wide
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Contact photo taking maximum space
                AsyncImage(
                    model = contact.photoUri ?: R.drawable.circle_background,
                    contentDescription = stringResource(R.string.contact_photo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes maximum available space
                        .aspectRatio(1f) // Keeps it square
                        .padding(8.dp)
                        .clip(CircleShape), // Makes the image circular
                    contentScale = ContentScale.Crop
                )
                
                // Name below the image
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                
                // Phone number below the name
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Phone label below the number (if available)
                contact.phoneLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Edit button positioned at top-right corner
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_contact),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 
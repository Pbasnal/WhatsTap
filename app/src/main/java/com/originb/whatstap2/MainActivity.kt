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
            android.util.Log.d("MainActivity", "Permissions granted - ready for manual sync")
        } else {
            // Permissions denied - just continue without the popup
            // The app can still function in limited capacity
            android.util.Log.d("MainActivity", "Some permissions denied, continuing with limited functionality")
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
            android.util.Log.d("MainActivity", "All permissions already granted - ready for manual sync")
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    // loadFavoriteContacts function removed to prevent automatic syncing
    
    private suspend fun loadStarredContactsFromSystem(): Pair<Int, Int> {
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
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                    val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                    val customLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))

                    // Get the phone label based on type or custom label
                    val phoneLabel = getPhoneTypeLabel(type, customLabel)
                    
                    android.util.Log.d("MainActivity", "Contact: $name - Type: $type, Custom Label: '$customLabel' -> Final Label: '$phoneLabel'")

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

            // Insert or update contacts in Room database, avoiding duplicates
            // Get all existing contacts once for efficiency
            val existingContacts = viewModel.getAllContactsSync()
            val existingPhoneNumbers = existingContacts.map { normalizePhoneNumber(it.phoneNumber) to it }.toMap().toMutableMap()
            
            var updatedCount = 0
            var insertedCount = 0
            
            contacts.forEach { contact ->
                // Check for existing contact by normalized phone number
                val normalizedNumber = normalizePhoneNumber(contact.phoneNumber)
                val existingContact = existingPhoneNumbers[normalizedNumber]
                
                if (existingContact != null) {
                    // Check if any fields actually changed before updating
                    val nameChanged = existingContact.name != contact.name
                    val labelChanged = existingContact.phoneLabel != contact.phoneLabel
                    val photoChanged = contact.photoUri != null && existingContact.photoUri != contact.photoUri
                    
                    android.util.Log.d("MainActivity", "=== Contact Update Check ===")
                    android.util.Log.d("MainActivity", "Contact: ${contact.name} (${contact.phoneNumber})")
                    android.util.Log.d("MainActivity", "Name changed: $nameChanged (${existingContact.name} -> ${contact.name})")
                    android.util.Log.d("MainActivity", "Label changed: $labelChanged (${existingContact.phoneLabel} -> ${contact.phoneLabel})")
                    android.util.Log.d("MainActivity", "Photo changed: $photoChanged (${existingContact.photoUri} -> ${contact.photoUri})")
                    
                    if (nameChanged || labelChanged || photoChanged) {
                        // Update existing contact with new information
                        val updatedContact = existingContact.copy(
                            name = contact.name,
                            phoneLabel = contact.phoneLabel,
                            photoUri = contact.photoUri ?: existingContact.photoUri
                        )
                        viewModel.update(updatedContact)
                        updatedCount++
                        android.util.Log.d("MainActivity", "Updated existing contact ${contact.name} with ID: ${existingContact.id}")
                        android.util.Log.d("MainActivity", "  Phone label updated: ${existingContact.phoneLabel} -> ${contact.phoneLabel}")
                        
                        // Update the map with the updated contact
                        existingPhoneNumbers[normalizedNumber] = updatedContact
                    } else {
                        android.util.Log.d("MainActivity", "No changes detected for contact: ${contact.name}")
                    }
                } else {
                    // Insert new contact
                    val insertedId = viewModel.insertSync(contact)
                    val insertedContact = contact.copy(id = insertedId)
                    insertedCount++
                    android.util.Log.d("MainActivity", "Inserted new contact ${contact.name} (${contact.phoneNumber} -> $normalizedNumber) with ID: $insertedId")
                    
                    // Add the newly inserted contact to the map to prevent duplicates within this sync
                    existingPhoneNumbers[normalizedNumber] = insertedContact
                }
            }
            
            android.util.Log.d("MainActivity", "Sync completed: $insertedCount new, $updatedCount updated, ${contacts.size} total starred contacts processed")
            
            return Pair(insertedCount, updatedCount)
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading starred contacts", e)
            return Pair(0, 0)
        }
    }

    private fun syncStarredContacts() {
        android.util.Log.d("MainActivity", "Manual sync of starred contacts requested")
        
        // Show a toast to indicate sync is starting
        runOnUiThread {
            Toast.makeText(this, "Syncing starred contacts...", Toast.LENGTH_SHORT).show()
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Force sync regardless of existing contacts
                val (insertedCount, updatedCount) = loadStarredContactsFromSystem()

                // Show completion message with statistics
                runOnUiThread {
                    val message = if (insertedCount > 0 || updatedCount > 0) {
                        "Sync completed: $insertedCount new, $updatedCount updated"
                    } else {
                        "Sync completed: No changes needed"
                    }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error during sync", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters (spaces, dashes, parentheses, plus signs, etc.)
        var normalized = phoneNumber.replace(Regex("[^\\d]"), "")
        
        // Handle common country code patterns
        when {
            // US/Canada numbers: Remove leading 1 if the number is 11 digits
            normalized.length == 11 && normalized.startsWith("1") -> {
                normalized = normalized.substring(1)
            }
            // Keep other country codes as-is for now
            // You can add more specific country code handling here if needed
        }
        
        android.util.Log.d("MainActivity", "Normalized '$phoneNumber' -> '$normalized'")
        return normalized
    }
    
    // Test function to verify normalization works correctly
    private fun testPhoneNormalization() {
        val testNumbers = listOf(
            "+1234567890",
            "1234567890", 
            "+1 234 567 890",
            "1 234 567 890",
            "(234) 567-890",
            "+1 (234) 567-890",
            "234-567-890",
            "234.567.890",
            "+1-234-567-890",
            "1 (234) 567-890",
            "+91 98765 43210",
            "91 98765 43210",
            "+44 20 1234 5678",
            "020 1234 5678"
        )
        
        android.util.Log.d("MainActivity", "=== Phone Normalization Test ===")
        testNumbers.forEach { number ->
            val normalized = normalizePhoneNumber(number)
            android.util.Log.d("MainActivity", "Test: '$number' -> '$normalized'")
        }
        
        // Test duplicate detection
        android.util.Log.d("MainActivity", "=== Duplicate Detection Test ===")
        val duplicateGroups = listOf(
            listOf("+1234567890", "1234567890", "+1 234 567 890", "1 (234) 567-890"),
            listOf("+91 98765 43210", "91 98765 43210", "91-98765-43210")
        )
        
        duplicateGroups.forEachIndexed { groupIndex, group ->
            android.util.Log.d("MainActivity", "Group ${groupIndex + 1}:")
            val normalizedNumbers = group.map { normalizePhoneNumber(it) }
            group.forEachIndexed { index, original ->
                android.util.Log.d("MainActivity", "  '$original' -> '${normalizedNumbers[index]}'")
            }
            val allSame = normalizedNumbers.all { it == normalizedNumbers.first() }
            android.util.Log.d("MainActivity", "  All normalized to same: $allSame")
        }
        android.util.Log.d("MainActivity", "=== End Test ===")
    }

    private fun getPhoneTypeLabel(type: Int, customLabel: String?): String {
        val result = when (type) {
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
                android.util.Log.d("MainActivity", "Processing custom label: '$customLabel' -> lowercase: '$label'")
                if (label.contains("whatsapp") || label.contains("wa")) {
                    android.util.Log.d("MainActivity", "Detected WhatsApp label!")
                    customLabel ?: "WhatsApp"
                } else {
                    android.util.Log.d("MainActivity", "Not a WhatsApp label, using: ${customLabel ?: "Custom"}")
                    customLabel ?: "Custom"
                }
            }
            else -> "Phone"
        }
        
        android.util.Log.d("MainActivity", "getPhoneTypeLabel: type=$type, customLabel='$customLabel' -> result='$result'")
        return result
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
            }
        } else {
            // Trigger normal voice call
            makeVoiceCall(contact.phoneNumber)
        }
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
    // Check if this is a WhatsApp contact
    val isWhatsAppContact = contact.phoneLabel?.lowercase()?.contains("whatsapp") == true ||
                           contact.phoneLabel?.lowercase()?.contains("wa") == true
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f), // Makes the card taller than it is wide
        colors = CardDefaults.cardColors(
            containerColor = if (isWhatsAppContact) {
                Color(0xFF005947) // Custom green color: #005947, RGB(0,89,71)
            } else {
                MaterialTheme.colorScheme.surface
            }
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
                    textAlign = TextAlign.Center,
                    color = if (isWhatsAppContact) Color.White else MaterialTheme.colorScheme.onSurface
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
                    color = if (isWhatsAppContact) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = if (isWhatsAppContact) Color.White else MaterialTheme.colorScheme.primary
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
                    tint = if (isWhatsAppContact) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 
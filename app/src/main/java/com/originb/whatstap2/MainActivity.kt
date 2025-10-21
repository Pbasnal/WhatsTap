package com.originb.whatstap2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
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
import com.originb.whatstap2.domain.model.Contact
import com.originb.whatstap2.util.AppConstants
import com.originb.whatstap2.util.Logger
import com.originb.whatstap2.util.PhoneNumberFormatter
import com.originb.whatstap2.util.PhoneTypeUtils
import com.originb.whatstap2.viewmodel.ContactViewModel
import com.originb.whatstap2.integration.WhatsAppIntegrationHandler
import com.originb.whatstap2.domain.repository.ContactSyncRepository
import com.originb.whatstap2.util.PermissionManager
import com.originb.whatstap2.integration.CallHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.originb.whatstap2.domain.model.Result

class MainActivity : ComponentActivity() {
    private val viewModel: ContactViewModel by viewModels()
    private val whatsAppIntegrationHandler by lazy { WhatsAppIntegrationHandler(this) }
    private val contactSyncRepository by lazy { ContactSyncRepository(this, viewModel) }
    private val permissionManager by lazy { PermissionManager(this) }
    private val callHandler by lazy { CallHandler(this) }

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
        
        permissionManager.checkAndRequestPermissions()
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

    private fun syncStarredContacts() {
        Logger.d("MainActivity", "Manual sync of starred contacts requested")
        
        // Show a toast to indicate sync is starting
        runOnUiThread {
            Toast.makeText(this, "Syncing starred contacts...", Toast.LENGTH_SHORT).show()
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Force sync regardless of existing contacts
                val syncResult = contactSyncRepository.syncStarredContacts()

                // Show completion message with statistics
                runOnUiThread {
                    when (syncResult) {
                        is Result.Success -> {
                            val message = if (syncResult.data.hasChanges) {
                                "Sync completed: ${syncResult.data.inserted} new, ${syncResult.data.updated} updated"
                            } else {
                                "Sync completed: No changes needed"
                            }
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }
                        is Result.Error -> {
                            Toast.makeText(this@MainActivity, "Sync failed: ${syncResult.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error during sync", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleContactCall(contact: Contact) {
        callHandler.handleContactCall(contact)
            .onError { message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
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
        Logger.d("MainActivity", "Contacts list updated. Count: ${contacts.size}")
        contacts.forEachIndexed { index, contact ->
            Logger.d("MainActivity", "Contact $index: ${contact.name} (ID: ${contact.id})")
        }
    }

    Scaffold(
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = onSyncContacts,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = AppConstants.FabPadding)
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
            columns = GridCells.Fixed(AppConstants.GridColumnCount), // 2 columns for card layout
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppConstants.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(AppConstants.CardSpacing),
            horizontalArrangement = Arrangement.spacedBy(AppConstants.CardSpacing)
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
    val isWhatsAppContact = contact.isWhatsAppContact
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f), // Makes the card taller than it is wide
        colors = CardDefaults.cardColors(
            containerColor = if (isWhatsAppContact) {
                AppConstants.WhatsAppGreen // Custom green color: #005947, RGB(0,89,71)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppConstants.CardElevation)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppConstants.CardSpacing),
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
                        .padding(AppConstants.ContactPhotoPadding)
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
                        .padding(horizontal = AppConstants.ContactNamePaddingHorizontal),
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
                        .padding(horizontal = AppConstants.ContactNamePaddingHorizontal, vertical = AppConstants.ContactNumberPaddingVertical),
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
                            .padding(horizontal = AppConstants.ContactNamePaddingHorizontal),
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
                    .size(AppConstants.IconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_contact),
                    tint = if (isWhatsAppContact) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppConstants.IconSize)
                )
            }
        }
    }
} 
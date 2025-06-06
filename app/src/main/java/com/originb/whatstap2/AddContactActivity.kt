package com.originb.whatstap2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.originb.whatstap2.databinding.ActivityAddContactBinding
import com.originb.whatstap2.model.Contact
import com.originb.whatstap2.viewmodel.ContactViewModel

class AddContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddContactBinding
    private val viewModel: ContactViewModel by viewModels()
    private var selectedPhotoUri: String? = null
    private var editingContactId: Long? = null
    private var phoneLabel: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                // Only take persistable URI permission for media content, not contacts
                if (it.scheme == "content" && !it.authority.equals("com.android.contacts")) {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                selectedPhotoUri = it.toString()
                loadImage(it)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { contactUri ->
                loadContactDetails(contactUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadContactData()
    }

    private fun setupClickListeners() {
        binding.contactPhoto.setOnClickListener {
            openImagePicker()
        }

        binding.saveButton.setOnClickListener {
            saveContact()
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

        binding.importContactButton.setOnClickListener {
            openContactPicker()
        }
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContact.launch(intent)
    }

    private fun loadContactDetails(contactUri: Uri) {
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            )

            contentResolver.query(
                contactUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI)
                    val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else ""
                    val number = if (numberIndex != -1) cursor.getString(numberIndex) else ""
                    val photoUri = if (photoIndex != -1) cursor.getString(photoIndex) else null
                    val type = if (typeIndex != -1) cursor.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    val customLabel = if (labelIndex != -1) cursor.getString(labelIndex) else null

                    // Get the phone label based on type or custom label
                    phoneLabel = getPhoneTypeLabel(type, customLabel)

                    binding.nameInput.setText(name)
                    binding.phoneInput.setText(number)

                    if (!photoUri.isNullOrEmpty()) {
                        val uri = Uri.parse(photoUri)
                        // Don't take persistable permission for contact URIs
                        selectedPhotoUri = photoUri
                        loadImage(uri)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load contact details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContactData() {
        // Check if we're in edit mode
        val contactId = intent.getLongExtra("contact_id", -1)
        editingContactId = if (contactId != -1L) contactId else null
        android.util.Log.d("AddContactActivity", "loadContactData - editingContactId: $editingContactId")
        if (editingContactId != null) {
            // Load existing contact data
            val name = intent.getStringExtra("contact_name")
            val number = intent.getStringExtra("contact_number")
            val photoUriString = intent.getStringExtra("contact_photo")
            phoneLabel = intent.getStringExtra("contact_label")

            binding.nameInput.setText(name)
            binding.phoneInput.setText(number)
            selectedPhotoUri = photoUriString

            // Load photo if available
            if (!photoUriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(photoUriString)
                    // Don't take persistable permission for contact URIs
                    loadImage(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.contactPhoto.setImageResource(R.drawable.circle_background)
                }
            } else {
                binding.contactPhoto.setImageResource(R.drawable.circle_background)
            }
        }
    }

    private fun loadImage(uri: Uri) {
        try {
            // For Google Photos URIs, we need to handle them differently
            if (uri.authority?.contains("google.android.apps.photos") == true) {
                // Use MediaStore to get the actual content URI
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val selection = "${MediaStore.Images.Media._ID} = ?"
                val selectionArgs = arrayOf(uri.lastPathSegment)
                
                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                        loadImageWithGlide(contentUri)
                    } else {
                        loadImageWithGlide(uri)
                    }
                }
            } else {
                loadImageWithGlide(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.contactPhoto.setImageResource(R.drawable.circle_background)
        }
    }

    private fun loadImageWithGlide(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.circle_background)
            .error(R.drawable.circle_background)
            .circleCrop()
            .into(binding.contactPhoto)
    }

    private fun openImagePicker() {
        pickImage.launch("image/*")
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

    private fun saveContact() {
        val name = binding.nameInput.text.toString()
        val phoneNumber = binding.phoneInput.text.toString()

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("AddContactActivity", "Saving contact - editingContactId: $editingContactId")
        android.util.Log.d("AddContactActivity", "Contact details - Name: $name, Phone: $phoneNumber, Photo: $selectedPhotoUri")

        val contact = if (editingContactId != null) {
            // For editing existing contact, use the existing ID
            Contact(
                id = editingContactId!!,
                name = name,
                phoneNumber = phoneNumber,
                phoneLabel = phoneLabel,
                photoUri = selectedPhotoUri
            )
        } else {
            // For new contact, let Room auto-generate the ID
            Contact(
                id = 0, // Room will auto-generate this
                name = name,
                phoneNumber = phoneNumber,
                phoneLabel = phoneLabel,
                photoUri = selectedPhotoUri
            )
        }

        android.util.Log.d("AddContactActivity", "Final contact object: $contact")

        if (editingContactId != null) {
            viewModel.update(contact)
            Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(contact)
            Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
} 
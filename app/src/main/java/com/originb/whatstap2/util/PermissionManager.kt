package com.originb.whatstap2.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.originb.whatstap2.util.Logger

class PermissionManager(private val activity: ComponentActivity) {

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Logger.d("PermissionManager", "Permissions granted - ready for manual sync")
        } else {
            Logger.d("PermissionManager", "Some permissions denied, continuing with limited functionality")
        }
    }

    fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CALL_PHONE
        )

        if (permissions.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }) {
            Logger.d("PermissionManager", "All permissions already granted - ready for manual sync")
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }
}

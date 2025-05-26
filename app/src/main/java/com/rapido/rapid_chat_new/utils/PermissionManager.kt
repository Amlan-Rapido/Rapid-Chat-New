package com.rapido.rapid_chat_new.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    fun areRecordingPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun getRequiredPermissions(): Array<String> = requiredPermissions
    
    fun getPermissionsToRequest(): Array<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.also { permissionsToRequest ->
            // Log current permission status
            requiredPermissions.forEach { permission ->
                val isGranted = ContextCompat.checkSelfPermission(context, permission) == 
                    PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "Permission status for $permission: ${if (isGranted) "GRANTED" else "DENIED"}")
            }
        }.toTypedArray()
    }
    
    companion object {
        private const val TAG = "PermissionManager"
    }
} 
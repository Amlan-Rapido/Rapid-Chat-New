package com.rapido.rapid_chat_new.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    // Since we're using cache directory storage, we only need RECORD_AUDIO permission
    private val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    
    fun areRecordingPermissionsGranted(): Boolean {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        Log.d(TAG, "Checking recording permissions - All granted: $allGranted")
        if (!allGranted) {
            logPermissionStatus()
        }
        
        return allGranted
    }
    
    fun getRequiredPermissions(): Array<String> = requiredPermissions
    
    fun getPermissionsToRequest(): Array<String> {
        val permissionsToRequest = requiredPermissions.filter {
            val isGranted = ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                Log.d(TAG, "Permission not granted: $it")
            }
            !isGranted
        }.toTypedArray()
        
        Log.d(TAG, "Permissions to request: ${permissionsToRequest.joinToString()}")
        return permissionsToRequest
    }
    
    private fun logPermissionStatus() {
        Log.d(TAG, "Current permission status:")
        requiredPermissions.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "$permission: ${if (isGranted) "GRANTED" else "DENIED"}")
        }
    }
    
    companion object {
        private const val TAG = "PermissionManager"
    }
} 
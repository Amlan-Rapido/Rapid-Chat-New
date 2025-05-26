package com.rapido.rapid_chat_new

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rapido.chat.integration.screens.ChatScreen
import com.rapido.rapid_chat_new.presentation.AndroidChatViewModel
import com.rapido.rapid_chat_new.ui.theme.RapidChatNewTheme
import com.rapido.rapid_chat_new.utils.PermissionManager
import com.rapido.voice_recorder.VoiceRecorderState
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    
    private val chatViewModel: AndroidChatViewModel by viewModel()
    private val permissionManager: PermissionManager by inject()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.entries.all { it.value }
        
        Log.d(TAG, "Permissions result: $permissionsMap")
        
        if (!allGranted) {
            Log.w(TAG, "Some permissions were denied, voice messages may not work properly")
            Toast.makeText(
                this,
                "Audio recording permission is required for voice messages",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d(TAG, "All permissions granted successfully")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "Starting RapidChat application")
        
        // Request permissions immediately
        if (!permissionManager.areRecordingPermissionsGranted()) {
            Log.d(TAG, "Requesting recording permissions at startup")
            requestRecordingPermissions()
        } else {
            Log.d(TAG, "Recording permissions already granted")
        }
        
        setContent {
            RapidChatNewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Monitor voice recorder state changes
                    val voiceRecorderState by chatViewModel.voiceRecorderState.collectAsState()
                    
                    // Handle voice recorder errors
                    when (val state = voiceRecorderState) {
                        is VoiceRecorderState.Error -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Recording error: ${state.exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> { /* Other states are handled in ChatScreen */ }
                    }
                    
                    ChatScreen(chatViewModel)
                }
            }
        }
    }
    
    private fun requestRecordingPermissions() {
        val permissionsToRequest = permissionManager.getPermissionsToRequest()
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
}

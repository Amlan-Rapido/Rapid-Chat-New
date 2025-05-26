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
import com.rapido.voice_recorder.PlatformContextProvider
import com.rapido.voice_recorder.VoiceRecorderState
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    
    private val chatViewModel: AndroidChatViewModel by viewModel()
    private val permissionManager: PermissionManager by inject()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.entries.all { it.value }
        
        Log.d(TAG, "Permission results received: $permissionsMap")
        
        if (!allGranted) {
            Log.w(TAG, "Some permissions were denied")
            val deniedPermissions = permissionsMap.filterValues { !it }.keys
            Log.w(TAG, "Denied permissions: $deniedPermissions")
            
            Toast.makeText(
                this,
                "Voice messages require audio recording permission. Some features may not work properly.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d(TAG, "All permissions granted, initializing voice recorder")
            initializeVoiceRecorder()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "Starting RapidChat application")
        
        // Initialize PlatformContextProvider first
        try {
            PlatformContextProvider.initialize(this)
            Log.d(TAG, "PlatformContextProvider initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PlatformContextProvider", e)
            Toast.makeText(
                this,
                "Failed to initialize voice recorder. Voice messages may not work properly.",
                Toast.LENGTH_LONG
            ).show()
        }
        
        // Check and request permissions
        if (!permissionManager.areRecordingPermissionsGranted()) {
            Log.d(TAG, "Requesting recording permissions")
            requestRecordingPermissions()
        } else {
            Log.d(TAG, "Recording permissions already granted")
            initializeVoiceRecorder()
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
                            Log.e(TAG, "Voice recorder error: ${state.exception.message}", state.exception)
                            Toast.makeText(
                                this@MainActivity,
                                "Recording error: ${state.exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // If it's a permission error, request permissions again
                            if (state.exception.message?.contains("permission", ignoreCase = true) == true) {
                                requestRecordingPermissions()
                            }
                        }
                        is VoiceRecorderState.Recording -> {
                            Log.d(TAG, "Recording in progress: ${state.durationMs}ms")
                        }
                        is VoiceRecorderState.Preview -> {
                            if (state.playing) {
                                Log.d(TAG, "Playing voice message: ${state.currentPositionMs}/${state.audio.durationMs}ms")
                            }
                        }
                        else -> { /* Other states don't need special handling */ }
                    }
                    
                    ChatScreen(chatViewModel)
                }
            }
        }
    }
    
    private fun requestRecordingPermissions() {
        val permissionsToRequest = permissionManager.getPermissionsToRequest()
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Launching permission request for: ${permissionsToRequest.joinToString()}")
            permissionLauncher.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "No permissions to request")
        }
    }
    
    private fun initializeVoiceRecorder() {
        try {
            // The voice recorder should be automatically initialized by Koin
            // We just need to ensure the context is properly set
            if (!PlatformContextProvider.isInitialized()) {
                Log.d(TAG, "Re-initializing PlatformContextProvider")
                PlatformContextProvider.initialize(this)
            }
            Log.d(TAG, "Voice recorder ready to use")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize voice recorder", e)
            Toast.makeText(
                this,
                "Failed to initialize voice recorder. Voice messages may not work properly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}

package com.rapido.rapid_chat_new

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.rapido.chat.data.VoiceRecorderIntegrationImpl
import com.rapido.chat.data.repository.repositoryimpl.ChatRepositoryImpl
import com.rapido.chat.integration.screens.ChatScreen
import com.rapido.chat.integration.viewmodel.ChatViewModelFactory
import com.rapido.rapid_chat_new.ui.theme.RapidChatNewTheme
import com.rapido.voice_recorder.PlatformVoiceRecorder
import com.rapido.voice_recorder.PlatformContextProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.entries.all { it.value }
        
        // Log permissions status for debugging
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
        
        // Initialize the platform context provider for voice recorder
        try {
            PlatformContextProvider.initialize(this)
            Log.d(TAG, "PlatformContextProvider initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PlatformContextProvider", e)
        }
        
        // Request permissions
        checkAndRequestPermissions()
        
        setContent {
            RapidChatNewTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                        // Create voice recorder integration
                        val voiceRecorder = remember {
                            Log.d(TAG, "Creating platform voice recorder")
                            // Create the platform voice recorder
                            val platformVoiceRecorder = PlatformVoiceRecorder()
                            // Create the voice recorder implementation
                            com.rapido.voice_recorder.VoiceRecorderImpl(platformVoiceRecorder)
                        }
                        
                        // Create voice recorder integration for chat module
                        val voiceRecorderIntegration = remember {
                            Log.d(TAG, "Creating voice recorder integration")
                            VoiceRecorderIntegrationImpl(voiceRecorder)
                        }
                        
                        // Create chat repository
                        val chatRepository = remember {
                            Log.d(TAG, "Creating chat repository")
                            ChatRepositoryImpl(voiceRecorderIntegration)
                        }
                        
                        // Create chat view model
                        val viewModelFactory = remember {
                            ChatViewModelFactory(chatRepository)
                        }
                        
                        val viewModel = remember {
                            viewModelFactory.create()
                        }
                        
                        // Request permissions again when the screen is first displayed
                        LaunchedEffect(Unit) {
                            Log.d(TAG, "LaunchedEffect: Checking permissions")
                            checkAndRequestPermissions()
                        }
                        
                        // Release resources when the activity is destroyed
                        DisposableEffect(Unit) {
                            onDispose {
                                Log.d(TAG, "Disposing resources")
                                voiceRecorderIntegration.release()
                                viewModel.onCleared()
                            }
                        }
                        
                        // Set the chat screen as the content
                        Log.d(TAG, "Setting up ChatScreen")
                        ChatScreen(viewModel)
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        // Log current permission status
        requiredPermissions.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == 
                PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permission status for $permission: ${if (isGranted) "GRANTED" else "DENIED"}")
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            permissionLauncher.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }
}
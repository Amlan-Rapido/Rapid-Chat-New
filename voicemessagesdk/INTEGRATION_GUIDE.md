# Voice Message SDK Integration Guide

**For Chat SDK Developers**

This guide shows how to integrate the Rapido Voice Message SDK into an existing chat system.

## 🎯 Integration Overview

The Voice Message SDK provides a **single Compose component** (`VoiceMessageButton`) that handles the complete voice message lifecycle:

```
User Interaction → Recording → Preview → Send → File Transfer
```

### **What Your Chat SDK Gets:**

- ✅ **Complete UI Component** - Drop-in replacement for text input
- ✅ **Voice Message Data** - File path, duration, metadata
- ✅ **Automatic File Management** - Recording, storage, cleanup
- ✅ **Cross-platform Support** - Android & iOS with single codebase

## 🏗️ Integration Architecture

### **Integration (With Voice Messages):**
```
ChatInputBar → VoiceMessageButton → ChatViewModel → ChatRepository → Backend
         ↑                     ↓
    VoiceMessageManager ← VoiceMessageData
```

## 📋 Step-by-Step Integration

### **Step 1: Add Dependency**

```kotlin
// In your chat SDK's build.gradle.kts
dependencies {
    implementation("com.rapido.sdk:voicemessage:1.0.0")
}
```

### **Step 2: Update Dependency Injection**

```kotlin
// In your chat module's DI configuration
fun chatModule() = module {
    // Include the voice message SDK module
    includes(voiceRecorderModule())
    
    // Your existing dependencies
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }
    single<ChatViewModel> { ChatViewModel(get()) }
}
```

### **Step 3: Expose VoiceMessageManager**

```kotlin
// In your ChatRepository interface
interface ChatRepository {
    // Your existing methods...
    
    // Add this property to expose voice message functionality
    val voiceMessageManager: VoiceMessageManager
}

// In your ChatRepositoryImpl
class ChatRepositoryImpl(
    // Your existing dependencies...
    override val voiceMessageManager: VoiceMessageManager // ← Injected automatically
) : ChatRepository {
    // Your implementation...
}

// In your ChatViewModel
class ChatViewModel(
    private val chatRepository: ChatRepository
) {
    // Expose VoiceMessageManager for UI components
    val voiceMessageManager: VoiceMessageManager = chatRepository.voiceMessageManager
    
    // Add method to handle voice messages
    fun sendVoiceMessage(data: VoiceMessageData) {
        viewModelScope.launch {
            val message = createChatMessage(data)
            chatRepository.addMessage(message)
            uploadToServer(data.filePath) // Your server upload logic
        }
    }
}
```

### **Step 4: Update ChatInputBar**

```kotlin
@Composable
fun ChatInputBar(
    viewModel: ChatViewModel,
    onSendMessage: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val showVoiceButton = textInput.isBlank()

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showVoiceButton) {
            // Replace text input with voice message button when empty
            VoiceMessageButton(
                voiceMessageManager = viewModel.voiceMessageManager,
                callbacks = object : VoiceMessageButtonCallbacks {
                    override fun onVoiceMessageReady(data: VoiceMessageData) {
                        viewModel.sendVoiceMessage(data)
                    }
                    
                    override fun onError(error: String) {
                        // Handle error (show toast, etc.)
                        showErrorToUser(error)
                    }
                }
            )
        } else {
            // Your existing text input
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Type a message...") }
            )
            
            Button(
                onClick = { 
                    onSendMessage(textInput)
                    textInput = ""
                }
            ) {
                Text("Send")
            }
        }
    }
}
```

### **Step 5: Handle Voice Message Data**

```kotlin
// In your data model
data class ChatMessage(
    val id: String,
    val senderId: String,
    val timestamp: Long,
    val type: MessageType, // TEXT or VOICE
    val content: String,   // Empty for voice messages
    val audioUrl: String?, // File path for voice messages
    val audioDuration: Long? // Duration in milliseconds
)

// In your ChatRepository
suspend fun sendVoiceMessage(data: VoiceMessageData): ChatMessage {
    val message = ChatMessage(
        id = generateId(),
        senderId = getCurrentUserId(),
        timestamp = System.currentTimeMillis(),
        type = MessageType.VOICE,
        content = "",
        audioUrl = data.filePath,
        audioDuration = data.durationMs
    )
    
    // Add to local storage
    addMessage(message)
    
    // Upload file to your backend
    uploadAudioFile(data.filePath, message.id)
    
    return message
}
```

## 🎨 UI Integration Examples

### **Conditional Voice Button (Recommended)**

```kotlin
@Composable
fun SmartChatInput(viewModel: ChatViewModel) {
    var textInput by remember { mutableStateOf("") }
    
    if (textInput.isBlank()) {
        // Show voice button when no text
        VoiceMessageButton(
            voiceMessageManager = viewModel.voiceMessageManager,
            callbacks = VoiceMessageCallbacks(viewModel)
        )
    } else {
        // Show text input and send button
        Row {
            TextField(
                value = textInput,
                onValueChange = { textInput = it }
            )
            SendButton(
                onClick = { 
                    viewModel.sendTextMessage(textInput)
                    textInput = ""
                }
            )
        }
    }
}
```

### **Side-by-Side Layout**

```kotlin
@Composable
fun ChatInputWithBothOptions(viewModel: ChatViewModel) {
    Row {
        TextField(
            modifier = Modifier.weight(1f),
            // ... text input properties
        )
        
        VoiceMessageButton(
            voiceMessageManager = viewModel.voiceMessageManager,
            callbacks = VoiceMessageCallbacks(viewModel)
        )
    }
}
```

## 🔄 Backend Integration

### **File Upload Strategy**

```kotlin
class VoiceMessageUploader(
    private val apiClient: ApiClient
) {
    suspend fun uploadVoiceMessage(filePath: String, messageId: String): String {
        val file = File(filePath)
        val response = apiClient.uploadAudio(
            file = file,
            messageId = messageId,
            format = "m4a"
        )
        return response.downloadUrl
    }
}

// In your ChatRepository
suspend fun sendVoiceMessage(data: VoiceMessageData) {
    // 1. Create local message immediately
    val localMessage = createLocalMessage(data)
    addMessage(localMessage)
    
    // 2. Upload in background
    try {
        val remoteUrl = uploader.uploadVoiceMessage(data.filePath, localMessage.id)
        updateMessageUrl(localMessage.id, remoteUrl)
    } catch (e: Exception) {
        markMessageAsFailed(localMessage.id)
    }
}
```

## 🎵 Playback Integration

### **Playing Sent Voice Messages**

```kotlin
// In your message list composable
@Composable
fun VoiceMessageItem(
    message: ChatMessage,
    viewModel: ChatViewModel
) {
    Row {
        IconButton(
            onClick = { 
                viewModel.playVoiceMessage(message.audioUrl ?: "") 
            }
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
        
        Text("Voice message (${formatDuration(message.audioDuration)})")
    }
}

// In your ChatViewModel
fun playVoiceMessage(audioUrl: String) {
    viewModelScope.launch {
        if (audioUrl.startsWith("http")) {
            // Download and play remote file
            playRemoteAudio(audioUrl)
        } else {
            // Play local file
            chatRepository.playLocalVoiceMessage(audioUrl)
        }
    }
}
```

## 🚀 Migration Guide

### **For Existing Chat SDKs**

1. **Add the dependency** to your chat module
2. **Include `voiceRecorderModule()`** in your DI configuration
3. **Add `VoiceMessageManager`** parameter to your repository constructor
4. **Expose `voiceMessageManager`** in your ChatViewModel
5. **Replace or augment** your chat input with `VoiceMessageButton`
6. **Add voice message handling** to your data layer

### **Backward Compatibility**

The integration is **completely additive** - existing functionality remains unchanged:

- ✅ Existing text messaging works exactly the same
- ✅ No breaking changes to your public API
- ✅ Voice messages are optional feature addition
- ✅ Gradual rollout possible (feature flags)

## 🔧 Configuration Options

### **Custom File Storage**

```kotlin
// Override default storage location
fun customChatModule() = module {
    includes(voiceRecorderModule())
    
    // Override with custom file manager
    single<AudioFileManager> { 
        CustomAudioFileManager(
            baseDirectory = getCustomVoiceDirectory(),
            maxFileSizeMB = 10,
            compressionQuality = 0.8f
        )
    }
}
```

### **Custom UI Styling**

```kotlin
// Create themed voice button
@Composable
fun ThemedVoiceMessageButton(
    voiceMessageManager: VoiceMessageManager,
    callbacks: VoiceMessageButtonCallbacks
) {
    VoiceMessageButton(
        voiceMessageManager = voiceMessageManager,
        callbacks = callbacks,
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(24.dp)
        )
    )
}
```

## 📊 Analytics Integration

```kotlin
// Track voice message usage
class VoiceMessageAnalytics(private val analytics: Analytics) {
    fun trackVoiceMessageSent(durationMs: Long) {
        analytics.track("voice_message_sent", mapOf(
            "duration_ms" to durationMs,
            "duration_seconds" to (durationMs / 1000.0)
        ))
    }
    
    fun trackVoiceMessageError(error: String) {
        analytics.track("voice_message_error", mapOf("error" to error))
    }
}

// In your ChatViewModel
override fun onVoiceMessageReady(data: VoiceMessageData) {
    analytics.trackVoiceMessageSent(data.durationMs)
    sendVoiceMessage(data)
}
```

## 🐛 Common Integration Issues

### **Issue: DI Not Found**
```kotlin
// ❌ Wrong: Manual creation
val manager = VoiceMessageManagerImpl()

// ✅ Correct: DI injection
class ChatRepository(
    private val voiceMessageManager: VoiceMessageManager // Injected
)
```

### **Issue: File Not Found on Playback**
```kotlin
// ❌ Wrong: Calling reset() after sending
voiceMessageManager.reset() // Deletes the file!

// ✅ Correct: Let SDK handle state transitions
// File automatically transferred to your chat system
```

### **Issue: Permission Denied**
```kotlin
// ✅ Add to AndroidManifest.xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## 📞 Support & Next Steps

**Ready to integrate?** 

1. **Start with Step 1** above
2. **Test with sample project** (provided separately)
3. **Contact us** for integration support: sdk-support@rapido.com

**Questions from your team lead?**

- **Performance**: Minimal overhead, voice files typically 50-200KB
- **Bundle size**: ~2.5MB addition to your chat SDK
- **Maintenance**: We handle all voice recording complexity
- **Updates**: Semantic versioning, backward compatible updates 
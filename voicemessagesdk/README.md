# Rapido Voice Message SDK

A cross-platform (Android/iOS) voice messaging SDK for Kotlin Multiplatform with Compose UI.

## 🎯 Features

- ✅ **Cross-platform** - Works on Android and iOS
- ✅ **High-quality M4A recording** - Optimized compression
- ✅ **Material 3 UI** - Beautiful, consistent design
- ✅ **Self-contained** - Complete recording → preview → send flow
- ✅ **Compose integration** - Native Compose UI components
- ✅ **Simple integration** - Just 2 lines of code
- ✅ **No permissions required** - Uses cache directory storage
- ✅ **Real-time preview** - Play/pause recorded messages
- ✅ **Automatic file management** - Handles cleanup and storage

## 📦 Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.rapido.sdk:voicemessage:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>com.rapido.sdk</groupId>
    <artifactId>voicemessage</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🚀 Quick Start

### 1. Add to Dependency Injection

```kotlin
// In your DI module
fun yourModule() = module {
    includes(voiceRecorderModule()) // Include SDK module
}
```

### 2. Use in Compose UI

```kotlin
@Composable
fun YourChatInput(viewModel: YourChatViewModel) {
    VoiceMessageButton(
        voiceMessageManager = viewModel.voiceMessageManager,
        callbacks = object : VoiceMessageButtonCallbacks {
            override fun onVoiceMessageReady(data: VoiceMessageData) {
                // Handle completed voice message
                viewModel.sendVoiceMessage(data)
            }
            
            override fun onError(error: String) {
                // Handle errors
                showErrorToUser(error)
            }
        }
    )
}
```

### 3. Handle Voice Message Data

```kotlin
class YourChatViewModel(
    private val voiceMessageManager: VoiceMessageManager // Injected
) {
    fun sendVoiceMessage(data: VoiceMessageData) {
        // data.voiceMessage - Complete voice message object
        // data.durationMs - Duration in milliseconds
        // data.filePath - Path to M4A file
        
        // Save to your chat system
        saveToChatHistory(data)
        
        // Upload to server
        uploadToServer(data.filePath)
    }
}
```

## 🏗️ Architecture Integration

### For Chat SDK Integration

```kotlin
// 1. Include in your chat module
implementation("com.rapido.sdk:voicemessage:1.0.0")

// 2. Add to your DI
fun chatModule() = module {
    includes(voiceRecorderModule())
    
    // Your existing chat dependencies
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }
}

// 3. Expose VoiceMessageManager in your chat ViewModel
class ChatViewModel(
    private val chatRepository: ChatRepository
) {
    val voiceMessageManager: VoiceMessageManager = chatRepository.voiceMessageManager
}

// 4. Use VoiceMessageButton in your chat input
@Composable
fun ChatInputBar(chatViewModel: ChatViewModel) {
    VoiceMessageButton(
        voiceMessageManager = chatViewModel.voiceMessageManager,
        callbacks = object : VoiceMessageButtonCallbacks {
            override fun onVoiceMessageReady(data: VoiceMessageData) {
                chatViewModel.sendVoiceMessage(data)
            }
            override fun onError(error: String) {
                chatViewModel.showError(error)
            }
        }
    )
}
```

## 📱 Platform Requirements

### Android
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35
- **Permissions:** `RECORD_AUDIO` (automatically requested)
- **Dependencies:** Compose BOM 2024.x+

### iOS
- **Min iOS:** 14.0
- **Xcode:** 15.0+
- **Swift:** 5.9+
- **Dependencies:** None (self-contained framework)

## 📊 API Reference

### VoiceMessageButton

Main UI component for voice message recording.

```kotlin
@Composable
fun VoiceMessageButton(
    voiceMessageManager: VoiceMessageManager,
    callbacks: VoiceMessageButtonCallbacks,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
)
```

### VoiceMessageData

Data class containing completed voice message information.

```kotlin
data class VoiceMessageData(
    val voiceMessage: VoiceMessage,  // Complete voice message object
    val durationMs: Long,            // Duration in milliseconds
    val filePath: String             // Path to M4A file
)
```

### VoiceMessageButtonCallbacks

Interface for handling voice message events.

```kotlin
interface VoiceMessageButtonCallbacks {
    fun onVoiceMessageReady(data: VoiceMessageData)
    fun onError(error: String)
}
```

### VoiceMessageManager

Core manager for voice message operations (injected automatically).

```kotlin
interface VoiceMessageManager {
    val state: StateFlow<VoiceRecorderState>
    suspend fun startRecording()
    suspend fun stopRecording(): VoiceMessage
    suspend fun cancelRecording()
    // ... other methods
}
```

## 🎨 UI States

The `VoiceMessageButton` automatically handles three UI states:

1. **Default State** - Shows microphone button
2. **Recording State** - Shows animated recording indicator with cancel/stop
3. **Preview State** - Shows play/pause controls with delete/send options

## 🔧 Configuration

### Custom File Storage (Optional)

```kotlin
// Override default cache directory storage
fun customVoiceRecorderModule() = module {
    single<AudioFileManager> { 
        CustomAudioFileManager(customDirectory = "/your/path") 
    }
}
```

### Custom UI Styling (Override Composables)

```kotlin
// Create your own VoiceMessageButton with custom styling
@Composable
fun CustomVoiceMessageButton(
    voiceMessageManager: VoiceMessageManager,
    callbacks: VoiceMessageButtonCallbacks
) {
    // Your custom implementation
}
```

## 🐛 Troubleshooting

### Common Issues

1. **Recording Permission Denied**
   - Ensure `RECORD_AUDIO` permission is added to AndroidManifest.xml
   - SDK automatically requests permission when needed

2. **File Not Found During Playback**
   - Don't call `voiceMessageManager.reset()` after sending
   - Use `voiceMessageManager.transitionToIdle()` instead

3. **DI Issues**
   - Ensure `voiceRecorderModule()` is included in your DI configuration
   - VoiceMessageManager should be injected, not manually created

## 📄 License

MIT License - see LICENSE file for details.

## 🤝 Contributing

Contributions welcome! Please read CONTRIBUTING.md for guidelines.

## 📞 Support

- **Documentation:** [Full API Docs](https://docs.rapido.com/voice-sdk)
- **Issues:** [GitHub Issues](https://github.com/rapido/voicemessage-sdk/issues)
- **Email:** sdk-support@rapido.com 
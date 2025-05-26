package com.rapido.rapid_chat_new

import android.app.Application
import android.util.Log
import com.rapido.voice_recorder.PlatformContextProvider
import com.rapido.rapid_chat_new.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RapidChatApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PlatformContextProvider
        try {
            PlatformContextProvider.initialize(this)
            Log.d(TAG, "PlatformContextProvider initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PlatformContextProvider", e)
        }
        
        // Initialize Koin
        startKoin {
            androidLogger()
            androidContext(this@RapidChatApp)
            modules(appModule)
        }
    }
    
    companion object {
        private const val TAG = "RapidChatApp"
    }
} 
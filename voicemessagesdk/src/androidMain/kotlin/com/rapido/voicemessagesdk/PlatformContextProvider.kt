package com.rapido.voicemessagesdk

import android.content.Context

object PlatformContextProvider {

    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    fun isInitialized(): Boolean {
        return ::appContext.isInitialized
    }
}
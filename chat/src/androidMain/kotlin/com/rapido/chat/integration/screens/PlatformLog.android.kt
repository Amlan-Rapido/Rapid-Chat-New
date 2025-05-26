package com.rapido.chat.integration.screens

import android.util.Log

actual fun platformLogD(tag: String, message: String) {
    Log.d(tag, message)
} 
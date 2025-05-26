package com.rapido.chat.integration.screens

import platform.Foundation.NSLog

actual fun platformLogD(tag: String, message: String) {
    NSLog("[$tag] $message")
} 
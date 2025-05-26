package com.rapido.chat.ios

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import com.rapido.chat.integration.screens.ChatScreen
import com.rapido.chat.integration.viewmodel.ChatViewModelInterface
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

@Composable
private fun ChatScreenWithDI() {
    val viewModel: ChatViewModelInterface = koinInject()
    ChatScreen(viewModel = viewModel)
}

fun MainViewController(): UIViewController = ComposeUIViewController {
    ChatScreenWithDI()
}

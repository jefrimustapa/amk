package com.amk.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Touchpad : Screen("touchpad", "Touchpad", Icons.Default.Mouse)
    object Keyboard : Screen("keyboard", "Keyboard", Icons.Default.Keyboard)
    object Remote : Screen("remote", "Remote", Icons.Default.Tv)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val all = listOf(Touchpad, Keyboard, Remote, Settings)
    }
}

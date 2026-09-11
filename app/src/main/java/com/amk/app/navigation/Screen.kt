package com.amk.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Touchpad : Screen("touchpad", "Touchpad", Icons.Default.Mouse)
    data object Keyboard : Screen("keyboard", "Keyboard", Icons.Default.Keyboard)
    data object Remote : Screen("remote", "Remote", Icons.Default.Tv)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val all: List<Screen> by lazy {
            listOf(Touchpad, Keyboard, Remote, Settings)
        }
    }
}

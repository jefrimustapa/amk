package com.amk.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Touchpad : Screen("touchpad", "Touchpad", Icons.Rounded.Mouse)
    data object Keyboard : Screen("keyboard", "Keyboard", Icons.Rounded.Keyboard)
    data object Remote : Screen("remote", "Remote", Icons.Rounded.Tv)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)

    companion object {
        val all: List<Screen> by lazy {
            listOf(Touchpad, Keyboard, Remote, Settings)
        }
    }
}

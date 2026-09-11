package com.amk.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.amk.app.hid.HidDeviceManager
import com.amk.app.model.AppSettings
import com.amk.app.navigation.Screen
import com.amk.app.ui.components.ConnectionBadge
import com.amk.app.ui.screens.KeyboardScreen
import com.amk.app.ui.screens.RemoteScreen
import com.amk.app.ui.screens.SettingsScreen
import com.amk.app.ui.screens.TouchpadScreen
import com.amk.app.ui.theme.AMKTheme
import com.amk.app.ui.theme.AccentCyan
import com.amk.app.ui.theme.DarkBg
import com.amk.app.ui.theme.DarkSurface
import com.amk.app.ui.theme.TextMuted
import com.amk.app.ui.theme.TextPrimary
import com.amk.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    private lateinit var hidManager: HidDeviceManager
    private var appSettings by mutableStateOf(AppSettings())

    private val bluetoothPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            hidManager.start()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required for AMK controller", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load settings with auto-recovery from external persistent storage
        appSettings = AppSettings.load(this)

        hidManager = HidDeviceManager(this)
        checkAndRequestPermissions()

        setContent {
            AMKTheme {
                MainAppScaffold(
                    hidManager = hidManager,
                    settings = appSettings,
                    onSettingsChanged = { newSettings ->
                        appSettings = newSettings
                        AppSettings.save(this, newSettings)
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            bluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            hidManager.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    hidManager: HidDeviceManager,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Touchpad) }
    val connectionState by hidManager.connectionState.collectAsState()
    val connectedDeviceName by hidManager.connectedDeviceName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AMK",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    ConnectionBadge(
                        connectionState = connectionState,
                        deviceName = connectedDeviceName,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 0.dp
            ) {
                Screen.all.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBg,
                            selectedTextColor = AccentCyan,
                            indicatorColor = AccentCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            when (currentScreen) {
                Screen.Touchpad -> TouchpadScreen(
                    hidManager = hidManager,
                    settings = settings
                )
                Screen.Keyboard -> KeyboardScreen(
                    hidManager = hidManager,
                    settings = settings
                )
                Screen.Remote -> RemoteScreen(
                    hidManager = hidManager,
                    settings = settings
                )
                Screen.Settings -> SettingsScreen(
                    hidManager = hidManager,
                    settings = settings,
                    onSettingsChanged = onSettingsChanged
                )
            }
        }
    }
}

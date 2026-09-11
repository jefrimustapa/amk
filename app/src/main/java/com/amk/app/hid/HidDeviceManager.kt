package com.amk.app.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@SuppressLint("MissingPermission")
class HidDeviceManager(private val context: Context) {
    private val tag = "AMK_HID"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private var isAppRegistered = false

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.i(tag, "Bluetooth HID Device Profile connected")
                hidDevice = proxy as? BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.w(tag, "Bluetooth HID Device Profile disconnected")
                hidDevice = null
                isAppRegistered = false
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectedDeviceName.value = null
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isAppRegistered = registered
            Log.i(tag, "HID App Status Changed: registered=$registered, device=${pluggedDevice?.name}")
            if (pluggedDevice != null) {
                connectedDevice = pluggedDevice
                _connectedDeviceName.value = pluggedDevice.name ?: pluggedDevice.address
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val deviceName = device?.name ?: device?.address ?: "Unknown"
            Log.i(tag, "HID Connection State changed: $deviceName -> state $state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _connectionState.value = ConnectionState.CONNECTED
                    _connectedDeviceName.value = deviceName
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    connectedDevice = device
                    _connectionState.value = ConnectionState.CONNECTING
                    _connectedDeviceName.value = deviceName
                }
                else -> {
                    if (device == connectedDevice) {
                        connectedDevice = null
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _connectedDeviceName.value = null
                    }
                }
            }
        }
    }

    fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(tag, "Bluetooth is disabled or not supported")
            return
        }
        bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
    }

    fun stop() {
        try {
            if (isAppRegistered && hidDevice != null) {
                hidDevice?.unregisterApp()
            }
            if (hidDevice != null && bluetoothAdapter != null) {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error closing HID service: ${e.message}", e)
        }
    }

    private fun registerHidApp() {
        val hid = hidDevice ?: return
        if (isAppRegistered) return

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "AMK Remote",
            "Air Mouse & Keyboard Controller",
            "AMK",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidConstants.HID_REPORT_DESCRIPTOR
        )

        val qos = null // Default QoS
        val success = hid.registerApp(sdpSettings, qos, qos, executor, hidCallback)
        Log.i(tag, "registerApp result: $success")
    }

    fun connect(device: BluetoothDevice) {
        val hid = hidDevice ?: return
        Log.i(tag, "Attempting connection to ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = device.name ?: device.address
        hid.connect(device)
    }

    fun disconnect() {
        val hid = hidDevice ?: return
        val target = connectedDevice ?: return
        Log.i(tag, "Disconnecting from ${target.name}")
        hid.disconnect(target)
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // Mouse Reports
    // -------------------------------------------------------------
    fun sendMouseReport(buttons: Byte, dx: Int, dy: Int, wheel: Int) {
        val hid = hidDevice ?: return
        val dev = connectedDevice ?: return

        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()
        val clampedWheel = wheel.coerceIn(-127, 127).toByte()

        val report = byteArrayOf(buttons, clampedDx, clampedDy, clampedWheel)
        hid.sendReport(dev, HidConstants.REPORT_ID_MOUSE.toInt(), report)
    }

    // -------------------------------------------------------------
    // Keyboard Reports (Modifiers + 6 key bytes)
    // -------------------------------------------------------------
    fun sendKeyboardReport(modifier: Byte, keycode: Byte) {
        val hid = hidDevice ?: return
        val dev = connectedDevice ?: return

        // Key Press
        val pressReport = byteArrayOf(modifier, 0, keycode, 0, 0, 0, 0, 0)
        hid.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), pressReport)

        // Key Release after small delay
        mainHandler.postDelayed({
            val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
            hid.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), releaseReport)
        }, 30)
    }

    // -------------------------------------------------------------
    // Consumer Reports (D-Pad, Home, Back, Volume, Media)
    // -------------------------------------------------------------
    fun sendConsumerKey(usageCode: Short) {
        val hid = hidDevice ?: return
        val dev = connectedDevice ?: return

        val low = (usageCode.toInt() and 0xFF).toByte()
        val high = ((usageCode.toInt() shr 8) and 0xFF).toByte()

        // Press
        val pressReport = byteArrayOf(low, high)
        hid.sendReport(dev, HidConstants.REPORT_ID_CONSUMER.toInt(), pressReport)

        // Release
        mainHandler.postDelayed({
            val releaseReport = byteArrayOf(0, 0)
            hid.sendReport(dev, HidConstants.REPORT_ID_CONSUMER.toInt(), releaseReport)
        }, 50)
    }
}

package com.amk.app.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@SuppressLint("MissingPermission")
class HidDeviceManager private constructor(private val context: Context) {
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

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private var isBondReceiverRegistered = false
    private val bondReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                Log.i(tag, "Bond state changed for ${device?.name ?: device?.address}: $bondState")
                refreshPairedDevices()
            }
        }
    }

    private var isAppRegistered = false
    private var connectionStartTime = 0L
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 2

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

    var onDeviceConnectedListener: ((BluetoothDevice) -> Unit)? = null
    private var lastTargetDevice: BluetoothDevice? = null

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isAppRegistered = registered
            Log.i(tag, "HID App Status Changed: registered=$registered, device=${pluggedDevice?.name}")
            if (pluggedDevice != null) {
                connectedDevice = pluggedDevice
                _connectedDeviceName.value = pluggedDevice.name ?: pluggedDevice.address
                onDeviceConnectedListener?.invoke(pluggedDevice)
            } else if (registered && connectedDevice == null && lastTargetDevice != null) {
                // Auto-reconnect to last known TV target once app re-registers, but ONLY if still bonded
                val target = lastTargetDevice
                if (target != null) {
                    val isBonded = bluetoothAdapter?.bondedDevices?.any { it.address == target.address } == true
                    if (isBonded) {
                        mainHandler.postDelayed({
                            if (connectedDevice == null) {
                                Log.i(tag, "Auto-reconnecting to ${target.name ?: target.address} after registration")
                                connect(target)
                            }
                        }, 1000)
                    } else {
                        Log.w(tag, "Skipping auto-reconnect: ${target.name ?: target.address} is not bonded")
                        lastTargetDevice = null
                    }
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val deviceName = device?.name ?: device?.address ?: "Unknown"
            Log.i(tag, "HID Connection State changed: $deviceName -> state $state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    stopBleAdvertising()
                    stopDiscovery()
                    connectedDevice = device
                    lastTargetDevice = device
                    connectionStartTime = System.currentTimeMillis()
                    reconnectAttempts = 0
                    _connectionState.value = ConnectionState.CONNECTED
                    _connectedDeviceName.value = deviceName
                    device?.let { onDeviceConnectedListener?.invoke(it) }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    connectedDevice = device
                    _connectionState.value = ConnectionState.CONNECTING
                    _connectedDeviceName.value = deviceName
                }
                else -> {
                    val duration = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0L
                    connectionStartTime = 0L

                    if (device == connectedDevice || connectedDevice == null) {
                        connectedDevice = null
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _connectedDeviceName.value = null

                        // Only auto-reconnect if it was an active connection that lasted > 3 seconds,
                        // device is still bonded, and we haven't exceeded retry attempts
                        val isBonded = device != null && bluetoothAdapter?.bondedDevices?.any { it.address == device.address } == true
                        if (isBonded && duration > 3000 && reconnectAttempts < maxReconnectAttempts) {
                            reconnectAttempts++
                            device?.let { disconnectedTarget ->
                                lastTargetDevice = disconnectedTarget
                                mainHandler.postDelayed({
                                    if (connectedDevice == null && isAppRegistered) {
                                        Log.i(tag, "Attempting reconnect to ${disconnectedTarget.name ?: disconnectedTarget.address} (attempt $reconnectAttempts)")
                                        connect(disconnectedTarget)
                                    }
                                }, 2000)
                            }
                        } else {
                            if (duration in 1..3000) {
                                Log.w(tag, "Target $deviceName disconnected immediately (${duration}ms) or not bonded. Aborting auto-reconnect.")
                            }
                            reconnectAttempts = 0
                        }
                    }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: HidDeviceManager? = null

        fun getInstance(context: Context): HidDeviceManager {
            return instance ?: synchronized(this) {
                instance ?: HidDeviceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(tag, "Bluetooth is disabled or not supported")
            return
        }
        if (!isBondReceiverRegistered) {
            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            ContextCompat.registerReceiver(context, bondReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            isBondReceiverRegistered = true
        }
        refreshPairedDevices()
        bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
    }

    fun stop() {
        stopBleAdvertising()
        stopDiscovery()
        if (isBondReceiverRegistered) {
            try {
                context.unregisterReceiver(bondReceiver)
            } catch (e: Exception) {
                Log.w(tag, "Error unregistering bond receiver: ${e.message}")
            }
            isBondReceiverRegistered = false
        }
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

    fun reconnectIfPossible() {
        if (connectedDevice != null) return
        if (!isAppRegistered) {
            registerHidApp()
            return
        }
        val target = lastTargetDevice ?: return
        val isBonded = bluetoothAdapter?.bondedDevices?.any { it.address == target.address } == true
        if (!isBonded) {
            Log.w(tag, "reconnectIfPossible: ${target.name} is no longer bonded, clearing")
            lastTargetDevice = null
            return
        }
        Log.i(tag, "reconnectIfPossible: connecting to ${target.name}")
        connect(target)
    }

    fun setLastTarget(address: String) {
        try {
            val dev = bluetoothAdapter?.getRemoteDevice(address)
            if (dev != null) {
                lastTargetDevice = dev
            }
        } catch (e: Exception) {
            Log.e(tag, "Invalid device address: $address")
        }
    }

    fun clearLastTarget() {
        Log.i(tag, "clearLastTarget: clearing remembered target (was ${lastTargetDevice?.name ?: lastTargetDevice?.address})")
        lastTargetDevice = null
        reconnectAttempts = 0
        mainHandler.removeCallbacksAndMessages(null)
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
        lastTargetDevice = device
        Log.i(tag, "Attempting connection to ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = device.name ?: device.address
        hid.connect(device)
    }

    fun disconnect() {
        val hid = hidDevice ?: return
        val target = connectedDevice ?: return
        Log.i(tag, "Disconnecting from ${target.name}")
        lastTargetDevice = null // Explicit user disconnect: don't auto-reconnect
        hid.disconnect(target)
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = getPairedDevices()
    }

    fun unpairDevice(device: BluetoothDevice): Boolean {
        return try {
            if (connectedDevice?.address == device.address) {
                disconnect()
            }
            if (lastTargetDevice?.address == device.address) {
                clearLastTarget()
            }
            val removeBondMethod = device.javaClass.getMethod("removeBond")
            val success = removeBondMethod.invoke(device) as? Boolean ?: false
            Log.i(tag, "unpairDevice ${device.name ?: device.address}: $success")
            mainHandler.postDelayed({ refreshPairedDevices() }, 300)
            success
        } catch (e: Exception) {
            Log.e(tag, "Failed to unpair device ${device.address}: ${e.message}", e)
            false
        }
    }

    // -------------------------------------------------------------
    // BLE Advertising (0x1812 HID) & Discovery for Android TV Pairing
    // -------------------------------------------------------------
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var isBleAdvertising = false

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var isReceiverRegistered = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(tag, "BLE HID Advertising (0x1812) started successfully")
            isBleAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(tag, "BLE HID Advertising failed: error code $errorCode")
            isBleAdvertising = false
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val name = device?.name
                    if (device != null && !name.isNullOrBlank()) {
                        val current = _discoveredDevices.value
                        if (current.none { it.address == device.address }) {
                            _discoveredDevices.value = current + device
                            Log.i(tag, "Discovered device: $name (${device.address})")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    Log.i(tag, "Bluetooth discovery finished")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    Log.i(tag, "Bond state changed for ${device?.name ?: device?.address}: $bondState")
                    if (device != null && bondState == BluetoothDevice.BOND_BONDED) {
                        if (device.address == lastTargetDevice?.address) {
                            Log.i(tag, "Bond complete for target ${device.name}, initiating HID connect...")
                            mainHandler.postDelayed({ connect(device) }, 1000)
                        }
                    }
                }
            }
        }
    }

    fun startBleAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        if (isBleAdvertising) return

        try {
            bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (bleAdvertiser == null) {
                Log.w(tag, "BLE Advertiser not supported on this device")
                return
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build()

            val pUuid = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb")
            val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(pUuid)
                .build()

            val scanResponseData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            bleAdvertiser?.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)
            Log.i(tag, "Started BLE Advertising for HID 0x1812")
        } catch (e: Exception) {
            Log.e(tag, "Error starting BLE advertising: ${e.message}", e)
        }
    }

    fun stopBleAdvertising() {
        if (isBleAdvertising && bleAdvertiser != null) {
            try {
                bleAdvertiser?.stopAdvertising(advertiseCallback)
                Log.i(tag, "Stopped BLE Advertising")
            } catch (e: Exception) {
                Log.e(tag, "Error stopping BLE advertising: ${e.message}", e)
            } finally {
                isBleAdvertising = false
            }
        }
    }

    fun startDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        _discoveredDevices.value = emptyList()
        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                }
                ContextCompat.registerReceiver(
                    context,
                    discoveryReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
                isReceiverRegistered = true
            }
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            val started = bluetoothAdapter.startDiscovery()
            _isScanning.value = started
            Log.i(tag, "startDiscovery started: $started")
        } catch (e: Exception) {
            Log.e(tag, "Error starting discovery: ${e.message}", e)
            _isScanning.value = false
        }
    }

    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            if (isReceiverRegistered) {
                try {
                    context.unregisterReceiver(discoveryReceiver)
                } catch (e: IllegalArgumentException) {
                    // Ignored
                }
                isReceiverRegistered = false
            }
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery: ${e.message}", e)
        }
    }

    fun pairAndConnect(device: BluetoothDevice) {
        stopDiscovery()
        stopBleAdvertising()
        lastTargetDevice = device
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = device.name ?: device.address
        Log.i(tag, "pairAndConnect initiated for ${device.name} (${device.address})")
        val isBonded = bluetoothAdapter?.bondedDevices?.any { it.address == device.address } == true
        if (!isBonded) {
            val bondCreated = device.createBond()
            Log.i(tag, "createBond called: $bondCreated")
        } else {
            connect(device)
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

    /**
     * Sends dual play/pause codes: standard Media Play/Pause (Consumer 0x00CD)
     * AND Keyboard Spacebar (HID Key 0x2C) for universal compatibility across apps & browsers.
     */
    fun sendPlayPauseCombo() {
        sendConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE)
        sendKeyboardReport(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
    }
}

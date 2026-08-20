package com.example.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import com.example.model.CurrentNetworkStatus
import com.example.model.WifiNetworkItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class WifiScannerController(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _scannedNetworks = MutableStateFlow<List<WifiNetworkItem>>(emptyList())
    val scannedNetworks: StateFlow<List<WifiNetworkItem>> = _scannedNetworks.asStateFlow()

    private val _currentStatus = MutableStateFlow(CurrentNetworkStatus())
    val currentStatus: StateFlow<CurrentNetworkStatus> = _currentStatus.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                _isScanning.value = false
                processScanResults()
                updateCurrentStatus()
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(scanReceiver, filter)
        } catch (e: Exception) {
            // Register receiver failure
        }
        setupNetworkCallback()
        updateCurrentStatus()
    }

    private fun setupNetworkCallback() {
        try {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    CoroutineScope(Dispatchers.Main).launch {
                        updateCurrentStatus()
                    }
                }

                override fun onLost(network: Network) {
                    CoroutineScope(Dispatchers.Main).launch {
                        updateCurrentStatus()
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    CoroutineScope(Dispatchers.Main).launch {
                        updateCurrentStatus()
                    }
                }
            }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            // Callback registration fallback
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        _isScanning.value = true
        updateCurrentStatus()

        val wm = wifiManager ?: return false
        return try {
            @Suppress("DEPRECATION")
            val success = wm.startScan()
            if (!success) {
                // If throttled, still process existing scan results
                processScanResults()
                _isScanning.value = false
            }
            success
        } catch (e: Exception) {
            processScanResults()
            _isScanning.value = false
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun processScanResults() {
        val wm = wifiManager ?: return
        val results: List<ScanResult> = try {
            wm.scanResults ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val currentSsid = _currentStatus.value.ssid.removeSurrounding("\"")

        val mapped = results
            .filter { !it.SSID.isNullOrBlank() }
            .distinctBy { it.SSID }
            .map { scanResult ->
                val level = scanResult.level
                val bars = calculateSignalBars(level)
                val freq = scanResult.frequency
                val band = when {
                    freq >= 5925 -> "6 GHz"
                    freq >= 4900 -> "5 GHz"
                    else -> "2.4 GHz"
                }
                val security = parseSecurityType(scanResult.capabilities)
                val isSecured = !security.contains("Open", ignoreCase = true)
                val isConnected = currentSsid.isNotBlank() &&
                        currentSsid != "Tidak Terhubung" &&
                        currentSsid.equals(scanResult.SSID, ignoreCase = true)

                WifiNetworkItem(
                    ssid = scanResult.SSID,
                    bssid = scanResult.BSSID ?: "-",
                    levelDbm = level,
                    signalLevelBars = bars,
                    frequencyMhz = freq,
                    frequencyBand = band,
                    securityType = security,
                    isSecured = isSecured,
                    capabilities = scanResult.capabilities ?: "",
                    isCurrentlyConnected = isConnected
                )
            }
            .sortedWith(
                compareByDescending<WifiNetworkItem> { it.isCurrentlyConnected }
                    .thenByDescending { it.levelDbm }
            )

        _scannedNetworks.value = mapped
    }

    @SuppressLint("MissingPermission")
    fun updateCurrentStatus() {
        val cm = connectivityManager
        val wm = wifiManager

        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        var ssid = "Tidak Terhubung"
        var bssid = "-"
        var linkSpeed = 0
        var freq = 0
        var rssi = -100

        if (isWifi && wm != null) {
            @Suppress("DEPRECATION")
            val info: WifiInfo? = wm.connectionInfo
            if (info != null) {
                val rawSsid = info.ssid ?: ""
                if (rawSsid.isNotBlank() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid.removeSurrounding("\"")
                }
                bssid = info.bssid ?: "-"
                linkSpeed = info.linkSpeed
                freq = info.frequency
                rssi = info.rssi
            }
        }

        val ip = getDeviceIpv4Address()
        val gateway = getGatewayIp()
        val bars = calculateSignalBars(rssi)

        _currentStatus.value = CurrentNetworkStatus(
            isConnected = isWifi,
            ssid = if (isWifi && ssid != "Tidak Terhubung") ssid else if (isWifi) "WiFi Terhubung" else "Tidak Terhubung",
            bssid = bssid,
            ipAddress = ip,
            gateway = gateway,
            linkSpeedMbps = linkSpeed,
            frequencyMhz = freq,
            rssiDbm = rssi,
            signalLevelBars = bars,
            securityType = "WPA2/WPA3"
        )
    }

    fun connectToNetwork(ssid: String, password: String) {
        val cm = connectivityManager ?: return
        _connectionMessage.value = "Menghubungkan ke $ssid..."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val specifierBuilder = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)

                if (password.isNotBlank()) {
                    specifierBuilder.setWpa2Passphrase(password)
                }

                val specifier = specifierBuilder.build()
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build()

                cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _connectionMessage.value = "Tersambung ke $ssid"
                        updateCurrentStatus()
                    }

                    override fun onUnavailable() {
                        _connectionMessage.value = "Gagal tersambung ke $ssid (Waktu habis)"
                    }
                })
            } catch (e: Exception) {
                _connectionMessage.value = "Buka Pengaturan WiFi untuk menyambung"
                openWifiSettings()
            }
        } else {
            openWifiSettings()
        }
    }

    fun openWifiSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(panelIntent)
            } else {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun clearConnectionMessage() {
        _connectionMessage.value = null
    }

    fun unregister() {
        try {
            context.unregisterReceiver(scanReceiver)
        } catch (e: Exception) {
            // ignore
        }
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun getDeviceIpv4Address(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.name.contains("wlan", ignoreCase = true) || intf.name.contains("eth", ignoreCase = true) || intf.isUp) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: continue
                            if (host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return "192.168.1.102"
    }

    private fun getGatewayIp(): String {
        val ip = getDeviceIpv4Address()
        val parts = ip.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}.1"
        } else {
            "192.168.1.1"
        }
    }

    private fun calculateSignalBars(rssiDbm: Int): Int {
        return when {
            rssiDbm >= -55 -> 4
            rssiDbm >= -68 -> 3
            rssiDbm >= -80 -> 2
            rssiDbm >= -90 -> 1
            else -> 0
        }
    }

    private fun parseSecurityType(capabilities: String?): String {
        if (capabilities.isNullOrBlank()) return "Open (Terbuka)"
        val caps = capabilities.uppercase()
        return when {
            caps.contains("SAE") || caps.contains("WPA3") -> "WPA3-SAE"
            caps.contains("WPA2") && caps.contains("WPA-") -> "WPA/WPA2-PSK"
            caps.contains("WPA2") -> "WPA2-PSK"
            caps.contains("WPA") -> "WPA-PSK"
            caps.contains("WEP") -> "WEP"
            caps.contains("OWE") -> "Enhanced Open (OWE)"
            else -> "Open (Terbuka)"
        }
    }
}

package com.example.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.TrafficStressEngine
import com.example.model.CurrentNetworkStatus
import com.example.model.StressTestMetric
import com.example.model.WifiNetworkItem
import com.example.service.WifiScannerController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiScanner = WifiScannerController(application.applicationContext)
    private val stressEngine = TrafficStressEngine()

    val scannedNetworks: StateFlow<List<WifiNetworkItem>> = wifiScanner.scannedNetworks
    val currentStatus: StateFlow<CurrentNetworkStatus> = wifiScanner.currentStatus
    val isScanning: StateFlow<Boolean> = wifiScanner.isScanning
    val connectionMessage: StateFlow<String?> = wifiScanner.connectionMessage

    val stressMetric: StateFlow<StressTestMetric> = stressEngine.metric

    private val _selectedNetworkForConnect = MutableStateFlow<WifiNetworkItem?>(null)
    val selectedNetworkForConnect: StateFlow<WifiNetworkItem?> = _selectedNetworkForConnect.asStateFlow()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        refreshNetworkStatus()
    }

    fun onPermissionsGranted() {
        _hasPermissions.value = true
        wifiScanner.updateCurrentStatus()
        wifiScanner.startScan()
    }

    fun refreshNetworkStatus() {
        wifiScanner.updateCurrentStatus()
        wifiScanner.processScanResults()
    }

    fun triggerScan() {
        wifiScanner.startScan()
    }

    fun selectNetworkForConnect(network: WifiNetworkItem) {
        _selectedNetworkForConnect.value = network
    }

    fun dismissConnectDialog() {
        _selectedNetworkForConnect.value = null
    }

    fun connectToNetwork(ssid: String, password: String) {
        wifiScanner.connectToNetwork(ssid, password)
        _selectedNetworkForConnect.value = null
    }

    fun openWifiSettings() {
        wifiScanner.openWifiSettings()
    }

    fun clearConnectionMessage() {
        wifiScanner.clearConnectionMessage()
    }

    fun startStressTest() {
        val current = currentStatus.value
        val gateway = current.gateway

        stressEngine.startStressTest(gatewayIp = gateway) {
            viewModelScope.launch {
                val msg = "Stress test selesai! Jaringan kembali normal."
                _toastEvent.emit(msg)
                Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
                refreshNetworkStatus()
            }
        }
    }

    fun stopStressTest() {
        stressEngine.stopStressTest()
        viewModelScope.launch {
            _toastEvent.emit("Stress test dihentikan oleh pengguna.")
            Toast.makeText(getApplication(), "Stress test dihentikan.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stressEngine.stopStressTest()
        wifiScanner.unregister()
    }
}

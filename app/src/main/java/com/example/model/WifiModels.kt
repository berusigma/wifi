package com.example.model

data class WifiNetworkItem(
    val ssid: String,
    val bssid: String,
    val levelDbm: Int,
    val signalLevelBars: Int, // 0 to 4
    val frequencyMhz: Int,
    val frequencyBand: String, // "2.4 GHz", "5 GHz", "6 GHz"
    val securityType: String, // "WPA3", "WPA2", "WEP", "Open (Terbuka)"
    val isSecured: Boolean,
    val capabilities: String,
    val isCurrentlyConnected: Boolean = false
)

data class CurrentNetworkStatus(
    val isConnected: Boolean = false,
    val ssid: String = "Tidak Terhubung",
    val bssid: String = "-",
    val ipAddress: String = "0.0.0.0",
    val gateway: String = "0.0.0.0",
    val linkSpeedMbps: Int = 0,
    val frequencyMhz: Int = 0,
    val rssiDbm: Int = 0,
    val signalLevelBars: Int = 0,
    val securityType: String = "-"
)

data class StressTestMetric(
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 5,
    val elapsedMillis: Long = 0L,
    val progress: Float = 0f, // 0.0 to 1.0
    val totalRequestsSent: Long = 0L,
    val totalRequestsSuccess: Long = 0L,
    val totalRequestsFailed: Long = 0L,
    val totalBytesTransferred: Long = 0L,
    val currentSpeedMbps: Double = 0.0,
    val peakSpeedMbps: Double = 0.0,
    val udpPacketsSent: Long = 0L,
    val activeThreads: Int = 0,
    val statusMessage: String = "Siap untuk uji tekanan",
    val liveLogs: List<String> = emptyList()
) {
    val totalDataMb: Double
        get() = totalBytesTransferred / (1024.0 * 1024.0)

    val currentSpeedMbPerSec: Double
        get() = currentSpeedMbps / 8.0
}

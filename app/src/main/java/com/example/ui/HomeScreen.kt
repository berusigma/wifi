package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrentNetworkStatus
import com.example.model.StressTestMetric
import com.example.model.WifiNetworkItem
import com.example.ui.components.ConnectWifiDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.PulseStressButton
import com.example.ui.components.RadarScanner
import com.example.ui.components.TrafficVisualizer
import com.example.ui.components.WifiItemCard
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberDarkCard
import com.example.ui.theme.CyberDarkCardBorder
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    hasLocationPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    val currentStatus by viewModel.currentStatus.collectAsState()
    val scannedNetworks by viewModel.scannedNetworks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val stressMetric by viewModel.stressMetric.collectAsState()
    val selectedNetworkForConnect by viewModel.selectedNetworkForConnect.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(connectionMessage) {
        connectionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearConnectionMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header
            item {
                AppHeader(
                    isScanning = isScanning,
                    onRefresh = {
                        if (hasLocationPermission) {
                            viewModel.triggerScan()
                        } else {
                            onRequestPermissions()
                        }
                    }
                )
            }

            // Permission Request Banner (if needed)
            if (!hasLocationPermission) {
                item {
                    PermissionCard(onRequestPermissions = onRequestPermissions)
                }
            }

            // Section 1: Connected WiFi Status (SSID + IP Address + Gateway + Signal)
            item {
                ConnectedWifiStatusCard(
                    status = currentStatus,
                    onOpenSettings = { viewModel.openWifiSettings() }
                )
            }

            // Section 2: Stress Test Attack Controller (Traffic Flood Engine)
            item {
                StressTestSection(
                    metric = stressMetric,
                    isConnected = currentStatus.isConnected,
                    onStartClick = { viewModel.startStressTest() },
                    onStopClick = { viewModel.stopStressTest() }
                )
            }

            // Section 3: Nearby WiFi Networks Section Header & Radar
            item {
                ScannedNetworksHeader(
                    count = scannedNetworks.size,
                    isScanning = isScanning,
                    onScanClick = {
                        if (hasLocationPermission) {
                            viewModel.triggerScan()
                        } else {
                            onRequestPermissions()
                        }
                    }
                )
            }

            // Scanned Networks List
            if (scannedNetworks.isEmpty()) {
                item {
                    EmptyScanCard(
                        isScanning = isScanning,
                        hasPermission = hasLocationPermission,
                        onScanClick = {
                            if (hasLocationPermission) {
                                viewModel.triggerScan()
                            } else {
                                onRequestPermissions()
                            }
                        }
                    )
                }
            } else {
                items(scannedNetworks, key = { it.bssid + it.ssid }) { network ->
                    WifiItemCard(
                        network = network,
                        onConnectClick = { viewModel.selectNetworkForConnect(it) }
                    )
                }
            }
        }
    }

    // Connect Dialog
    selectedNetworkForConnect?.let { network ->
        ConnectWifiDialog(
            network = network,
            onDismiss = { viewModel.dismissConnectDialog() },
            onConnect = { password -> viewModel.connectToNetwork(network.ssid, password) },
            onOpenSettings = {
                viewModel.dismissConnectDialog()
                viewModel.openWifiSettings()
            }
        )
    }
}

@Composable
private fun AppHeader(
    isScanning: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonRed, NeonOrange)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "WiFi Stress Test",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "High-Traffic Flood & Scanner",
                    color = NeonCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Radar / Refresh icon
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CyberDarkSurface)
                .border(1.dp, CyberDarkCardBorder, CircleShape)
                .testTag("header_refresh_button")
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Pindai Ulang",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    onRequestPermissions: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CyberDarkCard,
        borderColor = NeonAmber.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Izin Lokasi & WiFi Diperlukan",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android mewajibkan izin lokasi untuk membaca nama WiFi & daftar sinyal.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Izinkan", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConnectedWifiStatusCard(
    status: CurrentNetworkStatus,
    onOpenSettings: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CyberDarkCard.copy(alpha = 0.9f),
        borderColor = if (status.isConnected) NeonCyan.copy(alpha = 0.5f) else CyberDarkCardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (status.isConnected) NeonGreen else NeonRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status.isConnected) "JARINGAN TERHUBUNG" else "TIDAK TERHUBUNG",
                        color = if (status.isConnected) NeonGreen else NeonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan WiFi",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SSID Name & Icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (status.isConnected) NeonCyan.copy(alpha = 0.15f) else CyberDarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (status.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (status.isConnected) NeonCyan else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = status.ssid,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (status.isConnected) "Status: Online & Siap Ditest" else "Sambungkan ke WiFi terlebih dahulu",
                        color = if (status.isConnected) TextSecondary else TextMuted,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Network telemetry grid (IP Address + Gateway + Link Speed + Sinyal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // IP Address Box
                NetworkInfoPill(
                    modifier = Modifier.weight(1f),
                    label = "IP DEVICE",
                    value = status.ipAddress,
                    icon = Icons.Default.Public,
                    accentColor = NeonCyan
                )

                // Gateway IP Box
                NetworkInfoPill(
                    modifier = Modifier.weight(1f),
                    label = "GATEWAY",
                    value = status.gateway,
                    icon = Icons.Default.Router,
                    accentColor = NeonOrange
                )

                // Link Speed Box
                NetworkInfoPill(
                    modifier = Modifier.weight(1f),
                    label = "KECEPATAN",
                    value = if (status.linkSpeedMbps > 0) "${status.linkSpeedMbps} Mbps" else "-",
                    icon = Icons.Default.Speed,
                    accentColor = NeonGreen
                )
            }
        }
    }
}

@Composable
private fun NetworkInfoPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberDarkSurface.copy(alpha = 0.7f))
            .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StressTestSection(
    metric: StressTestMetric,
    isConnected: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Fiery Action Button
        PulseStressButton(
            isRunning = metric.isRunning,
            remainingSeconds = metric.remainingSeconds,
            enabled = true,
            onClick = onStartClick
        )

        // Live Real-Time Traffic Flooding Monitor HUD
        AnimatedVisibility(
            visible = metric.isRunning || metric.totalRequestsSent > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TrafficVisualizer(
                metric = metric,
                onStopClick = onStopClick
            )
        }
    }
}

@Composable
private fun ScannedNetworksHeader(
    count: Int,
    isScanning: Boolean,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadarScanner(
                size = 28.dp,
                isScanning = isScanning,
                radarColor = NeonCyan
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Daftar WiFi Sekitar",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isScanning) "Memindai frekuensi 2.4 & 5 GHz..." else "$count Jaringan terdeteksi",
                    color = if (isScanning) NeonCyan else TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CyberDarkSurface)
                .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(8.dp))
                .clickable { onScanClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isScanning) "Scanning..." else "Scan Ulang",
                color = NeonCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyScanCard(
    isScanning: Boolean,
    hasPermission: Boolean,
    onScanClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CyberDarkCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RadarScanner(
                size = 64.dp,
                isScanning = isScanning,
                radarColor = NeonCyan
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isScanning) "Sedang Memindai Jaringan WiFi..." else "Belum Ada Jaringan Terdeteksi",
                color = TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (!hasPermission)
                    "Aktifkan izin lokasi untuk menampilkan daftar WiFi di sekitar Anda."
                else
                    "Tekan tombol di bawah untuk memindai sinyal WiFi yang tersedia.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScanClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text(
                    text = if (isScanning) "Memindai..." else "Pindai Sekarang",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

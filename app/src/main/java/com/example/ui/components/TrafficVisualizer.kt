package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StressTestMetric
import com.example.ui.theme.CyberDarkCard
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TrafficVisualizer(
    modifier: Modifier = Modifier,
    metric: StressTestMetric,
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "traffic_particles")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = CyberDarkCard.copy(alpha = 0.95f),
        borderColor = if (metric.isRunning) NeonRed else NeonCyan
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Attack Status & Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (metric.isRunning) NeonRed else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (metric.isRunning) "BANJIR TRAFFIC AKTIF" else "STATUS UJI TEKANAN",
                        color = if (metric.isRunning) NeonRed else NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Countdown Timer Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (metric.isRunning) NeonRed.copy(alpha = 0.2f)
                            else CyberDarkSurface
                        )
                        .border(
                            1.dp,
                            if (metric.isRunning) NeonRed else TextMuted,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (metric.isRunning) "${metric.remainingSeconds}s Tersisa" else "Selesai",
                        color = if (metric.isRunning) Color.White else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Traffic Wave Canvas (Data Flood Visualizer)
            if (metric.isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberDarkSurface.copy(alpha = 0.6f))
                        .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val width = size.width
                        val height = size.height
                        val dotCount = 24
                        val spacing = width / dotCount

                        for (i in 0 until dotCount) {
                            val x = ((i * spacing) + (waveOffset * spacing * 4)) % width
                            val yOffset = (kotlin.math.sin((i + waveOffset * 6) * 0.8f) * (height / 4f))
                            val y = (height / 2f) + yOffset
                            val alpha = ((kotlin.math.sin(i * 0.5f + waveOffset * 3) + 1f) / 2f).coerceIn(0.2f, 1f)

                            drawCircle(
                                color = NeonRed.copy(alpha = alpha),
                                radius = 3.5.dp.toPx(),
                                center = Offset(x, y)
                            )

                            // Trail
                            drawLine(
                                color = NeonOrange.copy(alpha = alpha * 0.5f),
                                start = Offset(x - 12.dp.toPx(), y),
                                end = Offset(x, y),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Foreground stream banner
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "64 HTTP Streams + UDP Flood Aktif",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // High Precision Linear Progress Bar (Flood timeline)
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { if (metric.isRunning) metric.progress else 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (metric.isRunning) NeonRed else NeonGreen,
                    trackColor = CyberDarkSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Metrics Grid (4 Main Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "REQUEST TERKIRIM",
                    value = String.format(Locale.US, "%,d", metric.totalRequestsSent),
                    subtitle = "${metric.totalRequestsSuccess} Sukses",
                    icon = Icons.Default.Send,
                    accentColor = NeonCyan
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "DATA DITRANSFER",
                    value = String.format(Locale.US, "%.2f MB", metric.totalDataMb),
                    subtitle = "${String.format(Locale.US, "%.1f", metric.totalBytesTransferred / 1024.0)} KB",
                    icon = Icons.Default.DataUsage,
                    accentColor = NeonOrange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "BANDWIDTH REALTIME",
                    value = String.format(Locale.US, "%.1f MB/s", metric.currentSpeedMbPerSec),
                    subtitle = "${String.format(Locale.US, "%.1f", metric.currentSpeedMbps)} Mbps",
                    icon = Icons.Default.Speed,
                    accentColor = NeonGreen
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "UDP PACKET FLOOD",
                    value = String.format(Locale.US, "%,d", metric.udpPacketsSent),
                    subtitle = "Gateway Congestion",
                    icon = Icons.Default.Warning,
                    accentColor = NeonRed
                )
            }

            // Stop / Abort Button when running
            if (metric.isRunning) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onStopClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("stop_stress_test_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hentikan Sekarang",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hentikan Sekarang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CyberDarkSurface.copy(alpha = 0.7f))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = accentColor.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

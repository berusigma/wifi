package com.example.engine

import com.example.model.StressTestMetric
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class TrafficStressEngine {

    private val _metric = MutableStateFlow(StressTestMetric())
    val metric: StateFlow<StressTestMetric> = _metric.asStateFlow()

    private var attackJob: Job? = null
    private var okHttpClient: OkHttpClient? = null

    private val targetUrls = listOf(
        "https://www.google.com/generate_204",
        "https://www.cloudflare.com/cdn-cgi/trace",
        "https://httpbin.org/bytes/1048576",
        "https://www.microsoft.com",
        "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css",
        "https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js",
        "https://speed.hetzner.de/100MB.bin",
        "https://proof.ovh.net/files/10Mb.dat",
        "https://www.wikipedia.org",
        "https://www.apple.com"
    )

    private fun buildOkHttpClient(): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        }
        val pool = ConnectionPool(10, 5, TimeUnit.MINUTES)

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(pool)
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun startStressTest(
        gatewayIp: String = "192.168.1.1",
        onCompleted: () -> Unit = {}
    ) {
        if (_metric.value.isRunning) return

        val client = buildOkHttpClient()
        okHttpClient = client

        val totalSent = AtomicLong(0)
        val totalSuccess = AtomicLong(0)
        val totalFailed = AtomicLong(0)
        val totalBytes = AtomicLong(0)
        val totalUdp = AtomicLong(0)

        val durationMillis = 5000L
        val startTime = System.currentTimeMillis()

        _metric.value = StressTestMetric(
            isRunning = true,
            remainingSeconds = 5,
            elapsedMillis = 0L,
            progress = 0f,
            totalRequestsSent = 0L,
            totalRequestsSuccess = 0L,
            totalRequestsFailed = 0L,
            totalBytesTransferred = 0L,
            currentSpeedMbps = 0.0,
            peakSpeedMbps = 0.0,
            udpPacketsSent = 0L,
            activeThreads = 64,
            statusMessage = "Membanjiri traffic jaringan..."
        )

        attackJob = CoroutineScope(Dispatchers.IO).launch {
            // Monitor job for timer & metrics calculation
            val monitorJob = launch {
                var lastBytes = 0L
                var lastTime = System.currentTimeMillis()
                var peakSpeed = 0.0

                while (isActive) {
                    val now = System.currentTimeMillis()
                    val elapsed = now - startTime
                    val remaining = max(0L, durationMillis - elapsed)
                    val remainingSecs = ((remaining + 999) / 1000).toInt()
                    val progress = (elapsed.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)

                    val timeDeltaSec = max(0.1, (now - lastTime) / 1000.0)
                    val bytesCurrent = totalBytes.get()
                    val deltaBytes = max(0L, bytesCurrent - lastBytes)
                    val currentSpeedMbps = (deltaBytes * 8.0) / (timeDeltaSec * 1_000_000.0)
                    if (currentSpeedMbps > peakSpeed) {
                        peakSpeed = currentSpeedMbps
                    }

                    lastBytes = bytesCurrent
                    lastTime = now

                    _metric.update {
                        it.copy(
                            remainingSeconds = remainingSecs,
                            elapsedMillis = elapsed,
                            progress = progress,
                            totalRequestsSent = totalSent.get(),
                            totalRequestsSuccess = totalSuccess.get(),
                            totalRequestsFailed = totalFailed.get(),
                            totalBytesTransferred = bytesCurrent,
                            currentSpeedMbps = currentSpeedMbps,
                            peakSpeedMbps = peakSpeed,
                            udpPacketsSent = totalUdp.get(),
                            statusMessage = "Flooding: ${it.totalDataMb.let { mb -> String.format("%.2f MB", mb) }} | $remainingSecs detik"
                        )
                    }

                    if (elapsed >= durationMillis) {
                        break
                    }
                    delay(100)
                }
            }

            // High concurrency HTTP/HTTPS flood workers (64 parallel concurrent workers)
            val workerCount = 64
            val workerJobs = (0 until workerCount).map { workerId ->
                launch {
                    val buffer = ByteArray(16384)
                    val noCacheHeader = CacheControl.Builder().noCache().noStore().build()

                    while (isActive && (System.currentTimeMillis() - startTime) < durationMillis) {
                        val targetUrl = targetUrls[(workerId + (0..targetUrls.size - 1).random()) % targetUrls.size]
                        val nonce = UUID.randomUUID().toString().take(8)
                        val urlWithParam = if (targetUrl.contains("?")) "$targetUrl&cb=$nonce" else "$targetUrl?cb=$nonce"

                        // Random range to force multi-megabyte streams
                        val rangeStart = (0..500_000).random()
                        val rangeEnd = rangeStart + 1_048_576

                        val request = Request.Builder()
                            .url(urlWithParam)
                            .cacheControl(noCacheHeader)
                            .addHeader("User-Agent", "Mozilla/5.0 (Android; WiFi-Stress-Tester/$nonce)")
                            .addHeader("Range", "bytes=$rangeStart-$rangeEnd")
                            .addHeader("Accept-Encoding", "identity")
                            .addHeader("X-Stress-Id", nonce)
                            .build()

                        totalSent.incrementAndGet()

                        try {
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful || response.code == 206) {
                                    totalSuccess.incrementAndGet()
                                    val body = response.body
                                    val inputStream = body?.byteStream()
                                    if (inputStream != null) {
                                        var readBytes = 0
                                        while (isActive && (System.currentTimeMillis() - startTime) < durationMillis) {
                                            val count = inputStream.read(buffer)
                                            if (count == -1) break
                                            totalBytes.addAndGet(count.toLong())
                                        }
                                    }
                                } else {
                                    totalFailed.incrementAndGet()
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            totalFailed.incrementAndGet()
                        }
                    }
                }
            }

            // UDP packet airtime flood co-worker (floods router MAC queues with rapid UDP datagrams)
            val udpJob = launch {
                try {
                    val socket = DatagramSocket()
                    val destAddress = try {
                        InetAddress.getByName(gatewayIp)
                    } catch (e: Exception) {
                        InetAddress.getByName("8.8.8.8")
                    }
                    val payload = ByteArray(1024) { (it % 256).toByte() }
                    val ports = listOf(53, 80, 8080, 443, 1900, 5353)

                    while (isActive && (System.currentTimeMillis() - startTime) < durationMillis) {
                        val port = ports.random()
                        val packet = DatagramPacket(payload, payload.size, destAddress, port)
                        try {
                            socket.send(packet)
                            totalUdp.incrementAndGet()
                            totalBytes.addAndGet(payload.size.toLong())
                        } catch (e: Exception) {
                            // Continue flood
                        }
                    }
                    socket.close()
                } catch (e: Exception) {
                    // Ignore UDP socket exception
                }
            }

            // Wait until 5 seconds elapses
            delay(durationMillis)

            // Force cancel all ongoing calls and jobs
            client.dispatcher.cancelAll()
            workerJobs.forEach { it.cancel() }
            udpJob.cancel()
            monitorJob.cancel()

            _metric.update {
                it.copy(
                    isRunning = false,
                    remainingSeconds = 0,
                    progress = 1f,
                    statusMessage = "Stress test selesai (5 detik)"
                )
            }

            onCompleted()
        }
    }

    fun stopStressTest() {
        attackJob?.cancel()
        attackJob = null
        okHttpClient?.dispatcher?.cancelAll()

        _metric.update {
            it.copy(
                isRunning = false,
                remainingSeconds = 0,
                statusMessage = "Stress test dihentikan"
            )
        }
    }
}

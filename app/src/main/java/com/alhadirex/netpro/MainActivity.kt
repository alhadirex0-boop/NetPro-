
package com.alhadirex.netpro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

private val Navy = Color(0xFF071426)
private val CardColor = Color(0xFF132943)
private val Cyan = Color(0xFF00D4FF)
private val Purple = Color(0xFF9B7BFF)
private val White = Color(0xFFF4F7FF)
private val Green = Color(0xFF35D69A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Navy,
                    surface = CardColor,
                    primary = Cyan
                )
            ) {
                NetProScreen()
            }
        }
    }
}

@Composable
fun NetProScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wifiValue by remember { mutableStateOf("--") }
    var wifiStatus by remember { mutableStateOf("لم يتم القياس") }

    var mobileValue by remember { mutableStateOf("--") }
    var mobileStatus by remember { mutableStateOf("لم يتم القياس") }
    var mobileType by remember { mutableStateOf("غير معروف") }

    var download by remember { mutableStateOf("--") }
    var upload by remember { mutableStateOf("--") }
    var ping by remember { mutableStateOf("--") }

    var testing by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf("جاهز لاختبار السرعة") }

    fun refreshSignals() {
        try {
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            val rssi = wifi.connectionInfo.rssi

            if (wifi.isWifiEnabled && rssi in -100..-1) {
                val percent = ((rssi + 100) * 2).coerceIn(0, 100)
                wifiValue = "$percent%"
                wifiStatus = when {
                    rssi >= -55 -> "ممتازة"
                    rssi >= -67 -> "جيدة جداً"
                    rssi >= -75 -> "جيدة"
                    rssi >= -85 -> "ضعيفة"
                    else -> "ضعيفة جداً"
                }
            } else {
                wifiValue = "--"
                wifiStatus = "غير متصل بالواي فاي"
            }
        } catch (e: Exception) {
            wifiStatus = "تعذر قراءة الإشارة"
        }

        try {
            val phone = context.getSystemService(
                Context.TELEPHONY_SERVICE
            ) as TelephonyManager

            mobileType = when (phone.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_GSM -> "2G"
                else -> "غير معروف"
            }

            val level = phone.signalStrength?.level

            if (level != null && level in 1..4) {
                mobileValue = "${level * 25}%"
                mobileStatus = when (level) {
                    4 -> "ممتازة"
                    3 -> "جيدة جداً"
                    2 -> "جيدة"
                    else -> "ضعيفة"
                }
            } else {
                mobileValue = "--"
                mobileStatus = "قراءة الإشارة غير متاحة"
            }
        } catch (e: SecurityException) {
            mobileValue = "--"
            mobileStatus = "إذن الهاتف غير متاح"
        } catch (e: Exception) {
            mobileValue = "--"
            mobileStatus = "تعذر قراءة الإشارة"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshSignals()
    }

    fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(
                context, it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            refreshSignals()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "NetPro",
            color = Cyan,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "محلل قوة الشبكات وسرعة الإنترنت",
            color = White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        SignalCard(
            emoji = "📶",
            title = "شبكة Wi-Fi",
            value = wifiValue,
            status = wifiStatus,
            description = "قوة إشارة الواي فاي",
            color = Cyan
        )

        Spacer(modifier = Modifier.height(15.dp))

        SignalCard(
            emoji = "📡",
            title = "شبكة الهاتف",
            value = mobileValue,
            status = mobileStatus,
            description = "نوع الاتصال: $mobileType",
            color = Purple
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "اختبار سرعة الإنترنت",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = testStatus,
            color = Cyan,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(
                            Navy,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (testing) "..." else download,
                            color = Cyan,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Mbps",
                            color = White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "DOWNLOAD",
                            color = White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpeedResult("UPLOAD", upload, Cyan)
                    SpeedResult("PING", ping, Purple)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (!testing) {
                            testing = true
                            download = "--"
                            upload = "--"
                            ping = "--"
                            testStatus = "جاري الاتصال بخادم الاختبار..."

                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        runSpeedTest()
                                    }

                                    ping = "${result.ping} ms"
                                    download = String.format(
                                        java.util.Locale.US,
                                        "%.2f",
                                        result.download
                                    )
                                    upload = String.format(
                                        java.util.Locale.US,
                                        "%.2f",
                                        result.upload
                                    )
                                    testStatus = "اكتمل اختبار السرعة بنجاح"
                                } catch (e: Exception) {
                                    testStatus =
                                        "فشل الاختبار: تحقق من اتصال الإنترنت"
                                } finally {
                                    testing = false
                                }
                            }
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan,
                        contentColor = Navy
                    )
                ) {
                    Text(
                        text = if (testing) "جاري الاختبار..." else "ابدأ اختبار السرعة",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = { checkPermissions() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("تحديث قياس الإشارة", color = Cyan)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Developed & Designed by",
            color = White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )

        Text(
            text = "Alhadi Rex",
            color = Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SignalCard(
    emoji: String,
    title: String,
    value: String,
    status: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(7.dp))

            Text(
                title,
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                value,
                color = color,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                status,
                color = color,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                description,
                color = White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SpeedResult(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            color = White.copy(alpha = 0.65f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            value,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

data class SpeedTestResult(
    val ping: Long,
    val download: Double,
    val upload: Double
)

fun runSpeedTest(): SpeedTestResult {
    val pingTimes = mutableListOf<Long>()

    repeat(5) {
        val start = System.nanoTime()

        val connection = URL(
            "https://speed.cloudflare.com/__down?bytes=1000"
        ).openConnection() as HttpURLConnection

        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"

        try {
            connection.inputStream.use { input ->
                val buffer = ByteArray(2048)
                while (input.read(buffer) != -1) {
                    // Read response
                }
            }
        } finally {
            connection.disconnect()
        }

        val elapsed = (System.nanoTime() - start) / 1_000_000
        pingTimes.add(elapsed)
    }

    val averagePing = pingTimes.average().toLong()

    val downloadBytes = 8_000_000L
    val downloadStart = System.nanoTime()

    val downloadConnection = URL(
        "https://speed.cloudflare.com/__down?bytes=$downloadBytes"
    ).openConnection() as HttpURLConnection

    downloadConnection.connectTimeout = 20000
    downloadConnection.readTimeout = 60000
    downloadConnection.requestMethod = "GET"

    var received = 0L

    try {
        downloadConnection.inputStream.use { input ->
            val buffer = ByteArray(32768)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                received += count
            }
        }
    } finally {
        downloadConnection.disconnect()
    }

    val downloadSeconds =
        (System.nanoTime() - downloadStart) / 1_000_000_000.0

    if (received == 0L || downloadSeconds <= 0) {
        throw Exception("Download failed")
    }

    val downloadMbps =
        received * 8.0 / downloadSeconds / 1_000_000.0

    val uploadBytes = 2_000_000
    val data = ByteArray(uploadBytes)

    val uploadStart = System.nanoTime()

    val uploadConnection = URL(
        "https://speed.cloudflare.com/__up"
    ).openConnection() as HttpURLConnection

    uploadConnection.connectTimeout = 20000
    uploadConnection.readTimeout = 60000
    uploadConnection.requestMethod = "POST"
    uploadConnection.doOutput = true
    uploadConnection.setRequestProperty(
        "Content-Type",
        "application/octet-stream"
    )
    uploadConnection.setFixedLengthStreamingMode(uploadBytes)

    try {
        uploadConnection.outputStream.use { output ->
            output.write(data)
            output.flush()
        }

        val responseCode = uploadConnection.responseCode

        if (responseCode !in 200..299) {
            throw Exception("Upload failed: $responseCode")
        }

        uploadConnection.inputStream.use { input ->
            val buffer = ByteArray(1024)
            while (input.read(buffer) != -1) {
                // Read response
            }
        }
    } finally {
        uploadConnection.disconnect()
    }

    val uploadSeconds =
        (System.nanoTime() - uploadStart) / 1_000_000_000.0

    if (uploadSeconds <= 0) {
        throw Exception("Upload failed")
    }

    val uploadMbps =
        uploadBytes * 8.0 / uploadSeconds / 1_000_000.0

    return SpeedTestResult(
        ping = averagePing,
        download = downloadMbps,
        upload = uploadMbps
    )
}


package com.alhadirex.netpro

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.os.Build
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
import java.util.Locale

private val Navy = Color(0xFF071426)
private val CardColor = Color(0xFF132943)
private val Cyan = Color(0xFF00D4FF)
private val Purple = Color(0xFF9B7BFF)
private val White = Color(0xFFF4F7FF)
private val Green = Color(0xFF35D69A)
private val Orange = Color(0xFFFFB547)

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
    var wifiStatus by remember { mutableStateOf("بانتظار القراءة") }

    var mobileValue by remember { mutableStateOf("--") }
    var mobileDbm by remember { mutableStateOf("--") }
    var mobileStatus by remember { mutableStateOf("بانتظار قراءة الإشارة") }
    var mobileType by remember { mutableStateOf("غير معروف") }

    var phonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var locationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var download by remember { mutableStateOf("--") }
    var upload by remember { mutableStateOf("--") }
    var ping by remember { mutableStateOf("--") }

    var testing by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf("جاهز لاختبار السرعة") }

    fun refreshWifi() {
        try {
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            val info = wifi.connectionInfo

            if (wifi.isWifiEnabled &&
                info.networkId != -1 &&
                info.rssi in -100..-1
            ) {
                val rssi = info.rssi
                wifiValue = "$rssi dBm"

                wifiStatus = when {
                    rssi >= -50 -> "ممتازة جداً"
                    rssi >= -60 -> "ممتازة"
                    rssi >= -67 -> "جيدة جداً"
                    rssi >= -75 -> "جيدة"
                    rssi >= -85 -> "ضعيفة"
                    else -> "ضعيفة جداً"
                }
            } else {
                wifiValue = "--"
                wifiStatus = "غير متصل بشبكة Wi-Fi"
            }
        } catch (e: Exception) {
            wifiValue = "--"
            wifiStatus = "تعذر قراءة الإشارة"
        }
    }

    fun updateMobileSignal(signal: SignalStrength?) {
        if (signal == null) {
            mobileValue = "--"
            mobileDbm = "--"
            mobileStatus = "بانتظار بيانات الإشارة"
            return
        }

        val level = signal.level.coerceIn(0, 4)

        mobileValue = "$level / 4"

        mobileStatus = when (level) {
            4 -> "ممتازة جداً"
            3 -> "جيدة جداً"
            2 -> "جيدة"
            1 -> "ضعيفة"
            else -> "ضعيفة جداً"
        }

        mobileDbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val dbm = signal.cellSignalStrengths
                    .map { it.dbm }
                    .firstOrNull { it in -150..-1 }

                dbm?.let { "$it dBm" } ?: "غير متاح"
            } catch (e: Exception) {
                "غير متاح"
            }
        } else {
            "غير متاح"
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshMobileType() {
        if (!phonePermission) {
            mobileType = "إذن الهاتف غير متاح"
            return
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
                TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi Calling"
                else -> "غير معروف أو غير متصل"
            }
        } catch (e: Exception) {
            mobileType = "غير متاح"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        phonePermission = result[Manifest.permission.READ_PHONE_STATE]
            ?: phonePermission

        locationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION]
            ?: locationPermission

        refreshWifi()
        refreshMobileType()
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
            phonePermission = true
            locationPermission = true
            refreshWifi()
            refreshMobileType()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    DisposableEffect(phonePermission) {
        if (!phonePermission) {
            mobileValue = "--"
            mobileDbm = "--"
            mobileStatus = "يرجى السماح بإذن الهاتف"
            onDispose { }
        } else {
            val phone = context.getSystemService(
                Context.TELEPHONY_SERVICE
            ) as TelephonyManager

            val listener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(
                    signalStrength: SignalStrength
                ) {
                    super.onSignalStrengthsChanged(signalStrength)
                    updateMobileSignal(signalStrength)
                }
            }

            try {
                phone.listen(
                    listener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                )

                try {
                    updateMobileSignal(phone.signalStrength)
                } catch (e: Exception) {
                    mobileStatus = "بانتظار تحديث الإشارة"
                }
            } catch (e: SecurityException) {
                mobileStatus = "تعذر الوصول إلى إشارة الهاتف"
            }

            onDispose {
                phone.listen(
                    listener,
                    PhoneStateListener.LISTEN_NONE
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()

        while (true) {
            refreshWifi()
            refreshMobileType()
            kotlinx.coroutines.delay(3000)
        }
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
            "NetPro",
            color = Cyan,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "محلل قوة الشبكات وسرعة الإنترنت",
            color = White,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(25.dp))

        SignalCard(
            emoji = "📶",
            title = "شبكة Wi-Fi",
            value = wifiValue,
            status = wifiStatus,
            description = "قوة الإشارة الفعلية بوحدة dBm",
            color = Cyan
        )

        Spacer(Modifier.height(15.dp))

        SignalCard(
            emoji = "📡",
            title = "شبكة الهاتف",
            value = mobileValue,
            status = mobileStatus,
            description = "قوة الإشارة: $mobileDbm | $mobileType",
            color = Purple
        )

        Spacer(Modifier.height(25.dp))

        Text(
            "اختبار سرعة الإنترنت",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            testStatus,
            color = Cyan,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(18.dp))

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
                        .size(155.dp)
                        .background(Navy, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            download,
                            color = Cyan,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("Mbps", color = White)
                        Text(
                            "DOWNLOAD",
                            color = White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpeedResult("UPLOAD", upload, Cyan)
                    SpeedResult("LATENCY", ping, Purple)
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (!testing) {
                            testing = true
                            download = "--"
                            upload = "--"
                            ping = "--"
                            testStatus = "جاري اختبار الاتصال..."

                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        runSpeedTest()
                                    }

                                    ping = "${result.ping} ms"
                                    download = String.format(
                                        Locale.US,
                                        "%.2f",
                                        result.download
                                    )
                                    upload = String.format(
                                        Locale.US,
                                        "%.2f",
                                        result.upload
                                    )
                                    testStatus = "اكتمل الاختبار"
                                } catch (e: Exception) {
                                    testStatus =
                                        "فشل الاختبار: ${e.message ?: "تحقق من الإنترنت"}"
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
                        if (testing) "جاري الاختبار..." else "ابدأ اختبار السرعة",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = {
                checkPermissions()
                refreshWifi()
                refreshMobileType()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("تحديث قياس الإشارة", color = Cyan)
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "Developed & Designed by",
            color = White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )

        Text(
            "Alhadi Rex",
            color = Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
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
            Spacer(Modifier.height(7.dp))

            Text(
                title,
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                value,
                color = color,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                status,
                color = color,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(5.dp))

            Text(
                description,
                color = White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            color = White.copy(alpha = 0.65f),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(5.dp))

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

    repeat(3) {
        val start = System.nanoTime()

        val connection = URL(
            "https://speed.cloudflare.com/__down?bytes=1000"
        ).openConnection() as HttpURLConnection

        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        try {
            connection.inputStream.use { input ->
                val buffer = ByteArray(1024)
                while (input.read(buffer) != -1) { }
            }
        } finally {
            connection.disconnect()
        }

        pingTimes.add(
            (System.nanoTime() - start) / 1_000_000
        )
    }

    val averagePing = pingTimes.average().toLong()

    val downloadBytes = 8_000_000L
    val downloadStart = System.nanoTime()

    val downloadConnection = URL(
        "https://speed.cloudflare.com/__down?bytes=$downloadBytes"
    ).openConnection() as HttpURLConnection

    downloadConnection.connectTimeout = 20000
    downloadConnection.readTimeout = 60000

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
            while (input.read(buffer) != -1) { }
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

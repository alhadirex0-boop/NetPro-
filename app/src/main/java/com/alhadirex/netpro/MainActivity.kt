package com.alhadirex.netpro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

private val Navy = Color(0xFF081426)
private val CardColor = Color(0xFF14243B)
private val Cyan = Color(0xFF00D4FF)
private val Purple = Color(0xFF9B7BFF)
private val White = Color(0xFFF4F7FF)

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

    var wifiStrength by remember { mutableStateOf("--") }
    var wifiStatus by remember { mutableStateOf("لم يتم القياس") }
    var mobileStrength by remember { mutableStateOf("--") }
    var mobileType by remember { mutableStateOf("غير معروف") }
    var mobileStatus by remember { mutableStateOf("لم يتم القياس") }

    fun refreshSignals() {
        try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val rssi = wifiManager.connectionInfo.rssi

            if (wifiManager.isWifiEnabled && rssi in -100..-1) {
                val percent = ((rssi + 100) * 2).coerceIn(0, 100)
                wifiStrength = "$percent%"
                wifiStatus = when {
                    rssi >= -55 -> "ممتازة"
                    rssi >= -67 -> "جيدة جداً"
                    rssi >= -75 -> "جيدة"
                    rssi >= -85 -> "ضعيفة"
                    else -> "ضعيفة جداً"
                }
            } else {
                wifiStrength = "--"
                wifiStatus = "الواي فاي غير متصل"
            }
        } catch (e: Exception) {
            wifiStatus = "تعذر قراءة الإشارة"
        }

        try {
            val manager = context.getSystemService(
                Context.TELEPHONY_SERVICE
            ) as TelephonyManager

            mobileType = when (manager.dataNetworkType) {
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

            if (Build.VERSION.SDK_INT >= 28) {
                val level = manager.signalStrength?.level ?: 0
                mobileStrength = "${level * 20}%"
                mobileStatus = when (level) {
                    4 -> "ممتازة"
                    3 -> "جيدة جداً"
                    2 -> "جيدة"
                    1 -> "ضعيفة"
                    else -> "ضعيفة جداً أو غير متاحة"
                }
            } else {
                mobileStrength = "--"
                mobileStatus = "غير مدعوم"
            }
        } catch (e: SecurityException) {
            mobileStrength = "--"
            mobileStatus = "يحتاج إلى إذن الهاتف"
        } catch (e: Exception) {
            mobileStrength = "--"
            mobileStatus = "تعذر قراءة الإشارة"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshSignals()
    }

    fun checkPermissionsAndRefresh() {
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
        checkPermissionsAndRefresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "NetPro",
            color = Cyan,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "محلل قوة الشبكات والاتصال",
            color = White,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        SignalCard(
            emoji = "📶",
            title = "شبكة Wi-Fi",
            value = wifiStrength,
            status = wifiStatus,
            color = Cyan,
            description = "قوة إشارة الواي فاي"
        )

        Spacer(modifier = Modifier.height(18.dp))

        SignalCard(
            emoji = "📡",
            title = "شبكة الهاتف",
            value = mobileStrength,
            status = mobileStatus,
            color = Purple,
            description = "نوع الشبكة: $mobileType"
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { checkPermissionsAndRefresh() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Cyan,
                contentColor = Navy
            )
        ) {
            Text(
                text = "تحديث قياس الإشارة",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "قياس ومتابعة جودة الاتصال",
            color = White.copy(alpha = 0.65f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Developed & Designed by",
            color = White.copy(alpha = 0.55f),
            fontSize = 11.sp
        )

        Text(
            text = "Alhadi Rex",
            color = Cyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(15.dp))
    }
}

@Composable
fun SignalCard(
    emoji: String,
    title: String,
    value: String,
    status: String,
    color: Color,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 35.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                color = color,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = status,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

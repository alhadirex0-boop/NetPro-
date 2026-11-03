package com.alhadirex.netpro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetProScreen(this)
        }
    }
}

@Composable
fun NetProScreen(context: Context) {
    var signal by remember { mutableStateOf("--") }
    var network by remember { mutableStateOf("غير معروف") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifi.connectionInfo
            signal = "${info.rssi} dBm"
            network = info.ssid ?: "غير معروف"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("NetPro", style = MaterialTheme.typography.headlineLarge)
                Text("NETWORK ANALYZER")
                Spacer(modifier = Modifier.height(30.dp))

                Text("قوة إشارة Wi-Fi")
                Text(signal, style = MaterialTheme.typography.headlineMedium)

                Spacer(modifier = Modifier.height(12.dp))
                Text("اسم الشبكة: $network")

                Spacer(modifier = Modifier.height(25.dp))

                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val wifi = context.applicationContext
                            .getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val info = wifi.connectionInfo
                        signal = "${info.rssi} dBm"
                        network = info.ssid ?: "غير معروف"
                    } else {
                        permissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                }) {
                    Text("قياس قوة الإشارة")
                }

                Spacer(modifier = Modifier.height(40.dp))
                Text("Developed & Designed by Alhadi Rex")
                Text("© 2026 All Rights Reserved")
            }
        }
    }
}

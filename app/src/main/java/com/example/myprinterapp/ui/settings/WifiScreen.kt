package com.example.myprinterapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки Wi-Fi", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад", modifier = Modifier.size(36.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Статус Wi-Fi
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Wi-Fi включен", style = MaterialTheme.typography.titleMedium)
                        Text("Подключено к: MyNetwork", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }
            }

            // Список сетей
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Доступные сети",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Примеры сетей
                    WifiNetworkItem("MyNetwork", -45, true, true)
                    WifiNetworkItem("Office_WiFi", -60, true, false)
                    WifiNetworkItem("Guest", -70, false, false)
                }
            }
        }
    }
}

@Composable
fun WifiNetworkItem(
    name: String,
    signal: Int,
    isSecured: Boolean,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Wifi,
                contentDescription = null,
                tint = when {
                    signal >= -50 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    signal >= -70 -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                    else -> androidx.compose.ui.graphics.Color(0xFFF44336)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium)
                Text("$signal dBm", style = MaterialTheme.typography.bodySmall)
            }
            if (isSecured) {
                Icon(Icons.Filled.Lock, contentDescription = "Защищенная сеть")
            }
            if (isConnected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.CheckCircle, contentDescription = "Подключено",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

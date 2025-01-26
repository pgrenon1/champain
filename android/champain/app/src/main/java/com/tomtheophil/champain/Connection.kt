package com.tomtheophil.champain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPortOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.UUID

/**
 * Manages OSC connection and message sending
 */
class ConnectionManager(context: Context) {
    companion object {
        private const val TAG = "ConnectionManager"
        private const val PREFS_NAME = "ConnectionPrefs"
    }

    private object PrefsKeys {
        const val LAST_IP = "lastIpAddress"
        const val LAST_PORT = "lastPort"
        const val DEVICE_ID = "deviceId"
    }

    private var oscPort: OSCPortOut? = null
    private var connected = false
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var deviceId: String = getOrCreateDeviceId()

    init {
        attemptReconnection()
    }

    private fun getOrCreateDeviceId(): String {
        return prefs.getString(PrefsKeys.DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString().take(8)
            prefs.edit().putString(PrefsKeys.DEVICE_ID, newId).apply()
            newId
        }
    }

    fun getDeviceId(): String = deviceId
    fun getLastIpAddress(): String? = prefs.getString(PrefsKeys.LAST_IP, null)
    fun getLastPort(): String? = prefs.getString(PrefsKeys.LAST_PORT, null)

    private fun attemptReconnection() {
        val lastIp = getLastIpAddress()
        val lastPort = getLastPort()
        if (lastIp != null && lastPort != null) {
            try {
                connect(lastIp, lastPort.toInt())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconnect to last known server", e)
                disconnect()
            }
        }
    }

    fun connect(ipAddress: String, port: Int): Boolean {
        try {
            disconnect()
            val address = InetAddress.getByName(ipAddress)
            oscPort = OSCPortOut(address, port)
            connected = true
            
            saveConnectionDetails(ipAddress, port)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server", e)
            disconnect()
            return false
        }
    }

    private fun saveConnectionDetails(ipAddress: String, port: Int) {
        prefs.edit()
            .putString(PrefsKeys.LAST_IP, ipAddress)
            .putString(PrefsKeys.LAST_PORT, port.toString())
            .apply()
    }

    fun disconnect() {
        try {
            oscPort?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing OSC port", e)
        }
        oscPort = null
        connected = false
        clearConnectionDetails()
    }

    private fun clearConnectionDetails() {
        prefs.edit()
            .remove(PrefsKeys.LAST_IP)
            .remove(PrefsKeys.LAST_PORT)
            .apply()
    }

    fun isConnected(): Boolean = connected && oscPort != null

    suspend fun sendMessage(address: String, args: List<Any>) {
        oscPort?.let { port ->
            if (isConnected()) {
                try {
                    withContext(Dispatchers.IO) {
                        val message = OSCMessage(address, args)
                        port.send(message)
                        Log.d(TAG, "Sent message: $address, args=$args")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending message: $address", e)
                }
            }
        }
    }

    suspend fun sendQuaternion(x: Float, y: Float, z: Float, w: Float) {
        sendMessage("/quaternion", listOf(deviceId, x, y, z, w))
    }

    suspend fun sendShake(timestamp: Long) {
        sendMessage("/shake", listOf(deviceId, timestamp.toString()))
    }

    suspend fun sendPop() {
        sendMessage("/pop", listOf(deviceId))
    }
}

/**
 * Connection settings UI
 */
@Composable
fun ConnectionPage(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var ipAddress by remember { 
        mutableStateOf(connectionManager.getLastIpAddress() ?: "192.168.0.22") 
    }
    var port by remember { 
        mutableStateOf(connectionManager.getLastPort() ?: "4646") 
    }
    var connectionStatus by remember { 
        mutableStateOf(
            if (connectionManager.isConnected()) "Connected" else "Disconnected"
        ) 
    }
    var deviceId by remember { mutableStateOf(connectionManager.getDeviceId()) }

    ConnectionPageContent(
        connectionStatus = connectionStatus,
        deviceId = deviceId,
        isConnected = connectionManager.isConnected(),
        onConnect = { showDialog = true },
        onDisconnect = {
            connectionManager.disconnect()
            connectionStatus = "Disconnected"
            deviceId = connectionManager.getDeviceId()
        },
        modifier = modifier
    )

    if (showDialog) {
        ConnectionDialog(
            ipAddress = ipAddress,
            port = port,
            onIpChange = { ipAddress = it },
            onPortChange = { port = it },
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                connectionStatus = "Connecting..."
                if (connectionManager.connect(ipAddress, port.toInt())) {
                    connectionStatus = "Connected"
                    deviceId = connectionManager.getDeviceId()
                } else {
                    connectionStatus = "Connection failed"
                }
            }
        )
    }
}

@Composable
private fun ConnectionPageContent(
    connectionStatus: String,
    deviceId: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Connection Status",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace
        )

        ConnectionButtons(
            isConnected = isConnected,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )

        ConnectionInfo(
            status = connectionStatus,
            deviceId = deviceId
        )
    }
}

@Composable
private fun ConnectionButtons(
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onConnect,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            enabled = !isConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = UIConstants.UI_DARK,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = UIConstants.UI_LIGHT.copy(alpha = 0.5f)
            )
        ) {
            Text("Connect", fontFamily = FontFamily.Monospace)
        }

        Button(
            onClick = onDisconnect,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            enabled = isConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = UIConstants.UI_LIGHT,
                contentColor = UIConstants.UI_DARK,
                disabledContainerColor = UIConstants.UI_MEDIUM.copy(alpha = 0.5f)
            )
        ) {
            Text("Disconnect", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ConnectionInfo(
    status: String,
    deviceId: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Status: $status",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "Device ID: $deviceId",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ConnectionDialog(
    ipAddress: String,
    port: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Enter Connection Details",
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = onIpChange,
                    label = { Text("IP Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 
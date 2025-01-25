package com.tomtheophil.champain

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URISyntaxException

@Composable
fun ConnectionPage(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf("192.168.0.22") }
    var port by remember { mutableStateOf("3000") }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var deviceId by remember { mutableStateOf("None") }
    var signalStrength by remember { mutableStateOf("--") }

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

        // Connection buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                enabled = !connectionManager.isConnected()
            ) {
                Text(
                    "Connect",
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = {
                    connectionManager.disconnect()
                    connectionStatus = "Disconnected"
                    deviceId = "None"
                    signalStrength = "--"
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                enabled = connectionManager.isConnected()
            ) {
                Text(
                    "Disconnect",
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Status: $connectionStatus",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Device ID: $deviceId",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Signal Strength: $signalStrength",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                        onValueChange = { ipAddress = it },
                        label = { Text("IP Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        // Use the provided connectionManager
                        connectionStatus = "Connecting..."
                        CoroutineScope(Dispatchers.IO).launch {
                            connectionManager.connect(ipAddress, port) { status, id, strength ->
                                connectionStatus = status
                                deviceId = id
                                signalStrength = strength
                            }
                        }
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

class ConnectionManager {
    private var socket: Socket? = null
    private val TAG = "SocketConnection"  // Tag for logging

    fun connect(
        ipAddress: String,
        port: String,
        onConnectionUpdate: (status: String, deviceId: String, signalStrength: String) -> Unit
    ) {
        try {
            val actualIp = if (ipAddress == "127.0.0.1" || ipAddress == "localhost") {
                "10.0.2.2"
            } else {
                ipAddress
            }
            
            val serverUrl = "http://$actualIp:$port"
            Log.d(TAG, "Attempting connection to: $serverUrl")
            onConnectionUpdate("Attempting connection to $serverUrl", "None", "--")
            
            val options = IO.Options().apply {
                reconnection = true
                reconnectionDelay = 1000
                timeout = 10000
                transports = arrayOf("websocket")
                forceNew = true
                multiplex = false
                path = "/socket.io/"
            }
            Log.d(TAG, "Connection options: $options")
            
            socket = IO.socket(serverUrl, options)
            Log.d(TAG, "Socket created")

            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket connected successfully")
                    Log.d(TAG, "Socket ID: ${this.id()}")
                    onConnectionUpdate(
                        "Connected to $actualIp:$port",
                        "CONNECTED",
                        "Connected"
                    )
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Socket disconnected")
                    onConnectionUpdate(
                        "Disconnected",
                        "None",
                        "--"
                    )
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val exception = args.firstOrNull() as? Exception
                    Log.e(TAG, "Socket connection error details:", exception)
                    Log.e(TAG, "Socket connection error args: ${args.joinToString { it?.toString() ?: "null" }}")
                    
                    // Get the underlying cause if it exists
                    exception?.cause?.let { cause ->
                        Log.e(TAG, "Underlying cause:", cause)
                    }

                    onConnectionUpdate(
                        "Connection failed: ${exception?.message ?: "Unknown error"}",
                        "None",
                        "--"
                    )
                }

                on(io.socket.engineio.client.Socket.EVENT_ERROR) { args ->
                    Log.e(TAG, "Engine.IO error args: ${args.joinToString { it?.toString() ?: "null" }}")
                    val error = args.firstOrNull()
                    if (error is Exception) {
                        Log.e(TAG, "Engine.IO error details:", error)
                        error.cause?.let { cause ->
                            Log.e(TAG, "Engine.IO error cause:", cause)
                        }
                    }
                }

                // Add transport state logging
                io().on(io.socket.engineio.client.Socket.EVENT_TRANSPORT) { args ->
                    val transport = args.firstOrNull()
                    Log.d(TAG, "Transport event: transport=${transport}")
                }

                // Add handshake logging
                io().on(io.socket.engineio.client.Socket.EVENT_HANDSHAKE) { args ->
                    Log.d(TAG, "Handshake event: ${args.joinToString()}")
                }

                // Add upgrade logging
                io().on(io.socket.engineio.client.Socket.EVENT_UPGRADE) { args ->
                    Log.d(TAG, "Upgrade event: ${args.joinToString()}")
                }

                Log.d(TAG, "Starting connection...")
                connect()
            }

        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid URI: ${e.message}", e)
            onConnectionUpdate(
                "Invalid server address: ${e.message}",
                "None",
                "--"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
            onConnectionUpdate(
                "Connection error: ${e.message}",
                "None",
                "--"
            )
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting socket")
        socket?.disconnect()
        socket = null
    }

    fun isConnected(): Boolean {
        val connected = socket?.connected() == true
        return connected
    }

    fun sendQuaternion(x: Float, y: Float, z: Float, w: Float) {
        socket?.let { socket ->
            if (socket.connected()) {
                val quaternion = JSONArray().apply {
                    put(x)
                    put(y)
                    put(z)
                    put(w)
                }
                socket.emit("quaternion", quaternion)
                Log.d(TAG, "Sent quaternion: $quaternion")
            }
        }
    }

    fun sendShake(timestamp: Long) {
        socket?.let { socket ->
            if (socket.connected()) {
                socket.emit("shake", timestamp)
                Log.d(TAG, "Sent shake event: timestamp=$timestamp")
            }
        }
    }
} 
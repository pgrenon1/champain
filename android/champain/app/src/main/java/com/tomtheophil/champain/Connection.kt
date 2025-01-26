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
package com.tomtheophil.champain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Manages sensor-related components
 */
class SensorComponents(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val connectionManager = ConnectionManager(context)
    val shakeDetector = ShakeDetector(sensorManager, accelerometer!!)
    val orientationDetector = OrientationDetector(sensorManager, rotationSensor!!)

    init {
        requireNotNull(accelerometer) { "Accelerometer not found on device" }
        requireNotNull(rotationSensor) { "Rotation sensor not found on device" }
    }

    fun startListening() {
        shakeDetector.startListening()
        orientationDetector.startListening()
    }

    fun stopListening() {
        shakeDetector.stopListening()
        orientationDetector.stopListening()
    }
} 
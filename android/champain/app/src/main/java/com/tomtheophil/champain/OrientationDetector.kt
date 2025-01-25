package com.tomtheophil.champain

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

class OrientationDetector(
    private val sensorManager: SensorManager,
    private val rotationSensor: Sensor,
) : SensorEventListener {

    enum class SensorSpeed(val delay: Int, val label: String) {
        FASTEST(SensorManager.SENSOR_DELAY_FASTEST, "Fastest"),
        GAME(SensorManager.SENSOR_DELAY_GAME, "Game"),
        UI(SensorManager.SENSOR_DELAY_UI, "UI"),
        NORMAL(SensorManager.SENSOR_DELAY_NORMAL, "Normal")
    }

    // Raw sensor values
    val x = mutableFloatStateOf(0f)
    val y = mutableFloatStateOf(0f)
    val z = mutableFloatStateOf(0f)
    val w = mutableFloatStateOf(0f)  // Some devices provide this 4th component

    // Calibrated values
    val calibratedX = mutableFloatStateOf(0f)
    val calibratedY = mutableFloatStateOf(0f)
    val calibratedZ = mutableFloatStateOf(0f)
    val calibratedW = mutableFloatStateOf(0f)

    // Calibration state
    private var referenceQuaternion: FloatArray? = null
    val isCalibrated = mutableStateOf(false)

    val currentSpeed = mutableStateOf(SensorSpeed.GAME)

    fun calibrate() {
        // Store current rotation as reference (inverse will be applied)
        referenceQuaternion = floatArrayOf(
            -x.floatValue,  // Negate for inverse quaternion
            -y.floatValue,
            -z.floatValue,
            w.floatValue    // w component stays the same for inverse
        )
        isCalibrated.value = true
        updateCalibratedValues()
    }

    private fun updateCalibratedValues() {
        if (!isCalibrated.value || referenceQuaternion == null) {
            // If not calibrated, use raw values
            calibratedX.floatValue = x.floatValue
            calibratedY.floatValue = y.floatValue
            calibratedZ.floatValue = z.floatValue
            calibratedW.floatValue = w.floatValue
            return
        }

        // Quaternion multiplication: reference * raw
        // This gives us rotation relative to calibration point
        val ref = referenceQuaternion!!
        val raw = floatArrayOf(x.floatValue, y.floatValue, z.floatValue, w.floatValue)
        
        calibratedW.floatValue = ref[3] * raw[3] - ref[0] * raw[0] - ref[1] * raw[1] - ref[2] * raw[2]
        calibratedX.floatValue = ref[3] * raw[0] + ref[0] * raw[3] + ref[1] * raw[2] - ref[2] * raw[1]
        calibratedY.floatValue = ref[3] * raw[1] - ref[0] * raw[2] + ref[1] * raw[3] + ref[2] * raw[0]
        calibratedZ.floatValue = ref[3] * raw[2] + ref[0] * raw[1] - ref[1] * raw[0] + ref[2] * raw[3]
    }

    fun setSpeed(speed: SensorSpeed) {
        currentSpeed.value = speed
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
            stopListening()
            startListening()
        }
    }

    fun startListening() {
        sensorManager.registerListener(
            this,
            rotationSensor,
            currentSpeed.value.delay
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            x.floatValue = event.values[0]
            y.floatValue = event.values[1]
            z.floatValue = event.values[2]
            if (event.values.size > 3) {
                w.floatValue = event.values[3]
            }
            updateCalibratedValues()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
} 
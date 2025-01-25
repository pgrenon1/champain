package com.tomtheophil.champain

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.math.abs

class ShakeDetector(
    private val sensorManager: SensorManager,
    private val accelerometer: Sensor
) : SensorEventListener {
    companion object {
        const val DEFAULT_THRESHOLD = 35f
        const val MIN_THRESHOLD = 0f
        const val MAX_THRESHOLD = 100f
        const val ALPHA = 0.8f
        const val FREQUENCY_WINDOW = 2000L
        const val SAMPLING_INTERVAL = 5L
        const val MIN_SHAKES_FOR_FREQUENCY = 2
    }

    // Public state that can be observed
    val currentAccelY = mutableFloatStateOf(0f)
    val isShakeDetected = mutableStateOf(false)
    val shakeThreshold = mutableFloatStateOf(DEFAULT_THRESHOLD)
    val shakeCount = mutableIntStateOf(0)
    val currentFrequency = mutableFloatStateOf(0f)
    val recentShakeTimes = mutableListOf<Long>()

    // Private state
    private var lastUpdate: Long = 0
    private var lastDirection: Int = 0
    private var directionChanges: Int = 0
    private var lastSignificantY: Float = 0f
    private var gravityY: Float = 0f
    private var filteredY: Float = 0f

    fun startListening() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > SAMPLING_INTERVAL) {
                lastUpdate = currentTime

                val y = event.values[1]

                // Apply high-pass filter to remove gravity
                gravityY = ALPHA * gravityY + (1 - ALPHA) * y
                filteredY = y - gravityY

                // Only consider significant movements
                if (abs(filteredY) > shakeThreshold.floatValue) {
                    val currentDirection = if (filteredY > 0) 1 else -1

                    // Detect direction changes
                    if (currentDirection != lastDirection) {
                        directionChanges++
                        
                        // Every two direction changes completes one back-and-forth
                        if (directionChanges >= 2) {
                            shakeCount.value += 1
                            recentShakeTimes.add(currentTime)
                            
                            // Calculate frequency based on time between shakes
                            if (recentShakeTimes.size >= MIN_SHAKES_FOR_FREQUENCY) {
                                val recentTime = recentShakeTimes.takeLast(MIN_SHAKES_FOR_FREQUENCY)
                                val timeDiff = recentTime.last() - recentTime.first()
                                if (timeDiff > 0) {
                                    currentFrequency.floatValue = ((MIN_SHAKES_FOR_FREQUENCY - 1) * 1000f) / timeDiff
                                }
                            } else {
                                currentFrequency.floatValue = 0f
                            }
                            directionChanges = 0
                            isShakeDetected.value = true
                        }
                    }
                    
                    lastDirection = currentDirection
                    lastSignificantY = filteredY
                } else {
                    isShakeDetected.value = false
                    if (abs(filteredY - lastSignificantY) < 0.5f) {
                        lastDirection = 0
                        directionChanges = 0
                    }
                    
                    // Update frequency to 0 if no recent shakes
                    if (recentShakeTimes.isNotEmpty() && 
                        currentTime - recentShakeTimes.last() > FREQUENCY_WINDOW) {
                        currentFrequency.floatValue = 0f
                    }
                }

                currentAccelY.floatValue = filteredY
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
} 
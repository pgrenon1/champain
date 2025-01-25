package com.tomtheophil.champain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tomtheophil.champain.ui.theme.ChampainTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

object UIConstants {
    const val MAX_FREQUENCY = 10f // Maximum expected movements per second
}

class MainActivity : ComponentActivity() {
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var orientationDetector: OrientationDetector
    private lateinit var connectionManager: ConnectionManager

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        if (accelerometer == null || rotationSensor == null) {
            Log.e("MainActivity", "Required sensors not found on device")
            return
        }

        connectionManager = ConnectionManager()
        shakeDetector = ShakeDetector(sensorManager, accelerometer)
        orientationDetector = OrientationDetector(sensorManager, rotationSensor)

        // Watch for shakes and send to server
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow { shakeDetector.shakeCount.intValue }
                    .collect { count ->
                        if (connectionManager.isConnected() && count > 0) {
                            // Send the current timestamp when a shake is detected
                            connectionManager.sendShake(System.currentTimeMillis())
                        }
                    }
            }
        }

        // Existing orientation flow collector
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow {
                    arrayOf(
                        orientationDetector.calibratedX.floatValue,
                        orientationDetector.calibratedY.floatValue,
                        orientationDetector.calibratedZ.floatValue,
                        orientationDetector.calibratedW.floatValue
                    )
                }.collect { quaternion ->
                    if (connectionManager.isConnected()) {
                        connectionManager.sendQuaternion(
                            quaternion[0],
                            quaternion[1],
                            quaternion[2],
                            quaternion[3]
                        )
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            ChampainTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val pagerState = rememberPagerState { 4 }
                        val scope = rememberCoroutineScope()
                        
                        Column(modifier = Modifier.padding(innerPadding)) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == 0,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                    text = { 
                                        Text(
                                            "Game",
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 1,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                    text = { 
                                        Text(
                                            "Shake",
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 2,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                                    text = { 
                                        Text(
                                            "Orientation",
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 3,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(3) } },
                                    text = { 
                                        Text(
                                            "Connection",
                                            fontFamily = FontFamily.Monospace
                                        ) 
                                    }
                                )
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> GamePage()
                                    1 -> ShakePage(
                                        threshold = shakeDetector.shakeThreshold.floatValue,
                                        onThresholdChange = { shakeDetector.shakeThreshold.floatValue = it },
                                        accelY = shakeDetector.currentAccelY.floatValue,
                                        shakeCount = shakeDetector.shakeCount.intValue,
                                        frequency = shakeDetector.currentFrequency.floatValue,
                                        isShaking = shakeDetector.isShakeDetected.value,
                                        timestamps = shakeDetector.recentShakeTimes.toList()
                                    )
                                    2 -> OrientationPage(
                                        x = orientationDetector.x.floatValue,
                                        y = orientationDetector.y.floatValue,
                                        z = orientationDetector.z.floatValue,
                                        w = orientationDetector.w.floatValue,
                                        calibratedX = orientationDetector.calibratedX.floatValue,
                                        calibratedY = orientationDetector.calibratedY.floatValue,
                                        calibratedZ = orientationDetector.calibratedZ.floatValue,
                                        calibratedW = orientationDetector.calibratedW.floatValue,
                                        isCalibrated = orientationDetector.isCalibrated.value,
                                        onCalibrate = { orientationDetector.calibrate() },
                                        onSpeedChange = { speed -> orientationDetector.setSpeed(speed) },
                                        currentSpeed = orientationDetector.currentSpeed.value
                                    )
                                    3 -> ConnectionPage(
                                        connectionManager = connectionManager
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.startListening()
        orientationDetector.startListening()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.stopListening()
        orientationDetector.stopListening()
    }
}

@Composable
fun ThresholdSlider(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Shake Threshold: ±${String.format("%6.2f", threshold)}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
        Slider(
            value = threshold,
            onValueChange = onThresholdChange,
            valueRange = ShakeDetector.MIN_THRESHOLD..ShakeDetector.MAX_THRESHOLD,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AccelerationDisplay(
    accelY: Float,
    shakeCount: Int,
    frequency: Float,
    isShaking: Boolean,
    timestamps: List<Long>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Debug Information:",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Up-and-Down Count: ${String.format("%d", shakeCount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Shaking: $isShaking",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace
            )
        }

        // Add Y-acceleration gauge bar
        GaugeBar(
            value = abs(accelY),
            maxValue = ShakeDetector.DEFAULT_THRESHOLD,
            label = "Acceleration: ${String.format("%6.2f", abs(accelY))} m/s²",
            color = MaterialTheme.colorScheme.primary
        )

        GaugeBar(
            value = frequency,
            maxValue = UIConstants.MAX_FREQUENCY,
            label = "Shake Frequency: ${String.format("%6.2f", frequency)} per second",
            color = MaterialTheme.colorScheme.secondary
        )

        TimelineDisplay(
            timestamps = timestamps,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun GaugeBar(
    value: Float,
    maxValue: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(minOf(value / maxValue, 1f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun TimelineDisplay(
    timestamps: List<Long>,
    modifier: Modifier = Modifier,
    timeWindow: Long = ShakeDetector.FREQUENCY_WINDOW  // Add default parameter
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        // Draw timeline base line
        drawLine(
            color = baseColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw shake event dots
        timestamps.forEach { timestamp ->
            val currentTime = System.currentTimeMillis()
            val timeSinceEvent = (currentTime - timestamp).toFloat()
            val dotPosition = 1f - (timeSinceEvent / timeWindow.toFloat())  // Use timeWindow parameter
            if (dotPosition > 0) {
                drawCircle(
                    color = activeColor,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width * dotPosition, size.height / 2)
                )
            }
        }
    }
}

@Composable
fun ShakePage(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    accelY: Float,
    shakeCount: Int,
    frequency: Float,
    isShaking: Boolean,
    timestamps: List<Long>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ThresholdSlider(
            threshold = threshold,
            onThresholdChange = onThresholdChange
        )
        AccelerationDisplay(
            accelY = accelY,
            shakeCount = shakeCount,
            frequency = frequency,
            isShaking = isShaking,
            timestamps = timestamps,
            modifier = Modifier
        )
    }
}

@Composable
fun OrientationPage(
    x: Float,
    y: Float,
    z: Float,
    w: Float,
    calibratedX: Float,
    calibratedY: Float,
    calibratedZ: Float,
    calibratedW: Float,
    isCalibrated: Boolean,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeedChange: (OrientationDetector.SensorSpeed) -> Unit = {},
    currentSpeed: OrientationDetector.SensorSpeed
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speed selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Sensor Speed",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = spacedBy(4.dp)
            ) {
                OrientationDetector.SensorSpeed.values().forEach { speed ->
                    FilledTonalButton(
                        onClick = { onSpeedChange(speed) },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (speed == currentSpeed) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            speed.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            color = if (speed == currentSpeed)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Calibration button
        FilledTonalButton(
            onClick = onCalibrate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isCalibrated) "Recalibrate" else "Calibrate",
                fontFamily = FontFamily.Monospace
            )
        }

        // Raw sensor values
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Raw Rotation Vector",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace
            )
            Text("X: ${String.format("%6.4f", x)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("Y: ${String.format("%6.4f", y)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("Z: ${String.format("%6.4f", z)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("W: ${String.format("%6.4f", w)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        }

        // Calibrated values
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Calibrated Rotation",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace
            )
            Text("X: ${String.format("%6.4f", calibratedX)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("Y: ${String.format("%6.4f", calibratedY)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("Z: ${String.format("%6.4f", calibratedZ)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("W: ${String.format("%6.4f", calibratedW)}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        }
    }
}
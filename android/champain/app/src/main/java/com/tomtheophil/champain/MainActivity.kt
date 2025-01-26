package com.tomtheophil.champain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tomtheophil.champain.ui.theme.ChampainTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private lateinit var sensors: SensorComponents
    private lateinit var windowComponents: WindowComponents
    private lateinit var haptics: HapticsManager
    private var pressure = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupComponents()
        setupLifecycleObservers()
        setupUI()
    }

    private fun setupComponents() {
        windowComponents = WindowComponents(window)
        sensors = SensorComponents(this)
        haptics = HapticsManager(this)
    }

    private fun setupLifecycleObservers() {
        observeShakes()
        observeOrientation()
    }

    private fun observeShakes() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow { sensors.shakeDetector.shakeCount.intValue }
                    .collect { count ->
                        handleShake(count)
                    }
            }
        }
    }

    private fun handleShake(count: Int) {
        if (sensors.connectionManager.isConnected() && count > 0) {
            lifecycleScope.launch {
                sensors.connectionManager.sendShake(System.currentTimeMillis())
            }
            pressure = (pressure + 1).coerceAtMost(Config.MAX_PRESSURE)
            haptics.vibrate(pressure)
        }
    }

    private fun observeOrientation() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow {
                    with(sensors.orientationDetector) {
                        arrayOf(
                            calibratedX.floatValue,
                            calibratedY.floatValue,
                            calibratedZ.floatValue,
                            calibratedW.floatValue
                        )
                    }
                }.collect { quaternion ->
                    if (sensors.connectionManager.isConnected()) {
                        sensors.connectionManager.sendQuaternion(
                            quaternion[0],
                            quaternion[1],
                            quaternion[2],
                            quaternion[3]
                        )
                    }
                }
            }
        }
    }

    private fun setupUI() {
        enableEdgeToEdge()
        setContent {
            ChampainTheme {
                MainScreen(
                    sensors = sensors,
                    onPop = ::handlePop
                )
            }
        }
    }

    private fun handlePop() {
        pressure = 0
        haptics.cancel()
    }

    override fun onResume() {
        super.onResume()
        sensors.startListening()
    }

    override fun onPause() {
        super.onPause()
        sensors.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        windowComponents.cleanup()
    }
}

/**
 * Manages window-related components and settings
 */
private class WindowComponents(private val window: android.view.Window) {
    private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

    init {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun cleanup() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Manages haptic feedback
 */
private class HapticsManager(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrate(pressure: Int) {
        if (pressure > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    Config.VIBRATION_DURATION,
                    (pressure * (Config.MAX_VIBRATION_AMPLITUDE / Config.MAX_PRESSURE)).toInt()
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(Config.VIBRATION_DURATION)
            }
        }
    }

    fun cancel() {
        vibrator.cancel()
    }
}

@Composable
private fun MainScreen(
    sensors: SensorComponents,
    onPop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPage by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    NavigationDrawer(
        drawerState = drawerState,
        selectedPage = selectedPage,
        onPageSelected = { selectedPage = it },
        onCalibrate = { sensors.orientationDetector.calibrate() },
        scope = scope
    ) {
        GameContent(
            selectedPage = selectedPage,
            sensors = sensors,
            onPop = onPop,
            modifier = modifier
        )
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
            fontFamily = FontFamily.Monospace,
            color = UIConstants.UI_DARK  // Dark brown for better contrast
        )
        Slider(
            value = threshold,
            onValueChange = onThresholdChange,
            valueRange = ShakeDetector.MIN_THRESHOLD..ShakeDetector.MAX_THRESHOLD,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = UIConstants.UI_DARK,
                activeTrackColor = UIConstants.UI_DARK,
                inactiveTrackColor = Color(0x66442B00)
            )
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
            color = UIConstants.UI_DARK,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UIConstants.UI_LIGHT.copy(alpha = 0.7f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(minOf(value / maxValue, 1f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(UIConstants.UI_DARK)
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
    onDebugShake: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onDebugShake() }  // Add click detection to the whole page
    ) {
        Column {
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Calibration button
        FilledTonalButton(
            onClick = onCalibrate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = UIConstants.UI_DARK,
                contentColor = Color.White,
                disabledContainerColor = UIConstants.UI_LIGHT
            )
        ) {
            Text(
                if (isCalibrated) "Recalibrate" else "Calibrate",
                fontFamily = FontFamily.Monospace
            )
        }

        // Sensor speed buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrientationDetector.SensorSpeed.values().forEach { speed ->
                FilledTonalButton(
                    onClick = { onSpeedChange(speed) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (speed == currentSpeed) UIConstants.UI_DARK else UIConstants.UI_MEDIUM,
                        contentColor = Color.White,
                        disabledContainerColor = UIConstants.UI_LIGHT
                    )
                ) {
                    Text(
                        speed.label,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsPage(
    shakeDetector: ShakeDetector,
    orientationDetector: OrientationDetector,
    connectionManager: ConnectionManager,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) { page ->
        when (page) {
            0 -> ShakePage(
                threshold = shakeDetector.shakeThreshold.floatValue,
                onThresholdChange = { shakeDetector.shakeThreshold.floatValue = it },
                accelY = shakeDetector.currentAccelY.floatValue,
                shakeCount = shakeDetector.shakeCount.intValue,
                frequency = shakeDetector.currentFrequency.floatValue,
                isShaking = shakeDetector.isShakeDetected.value,
                timestamps = shakeDetector.recentShakeTimes.toList(),
                onDebugShake = { shakeDetector.debugTriggerShake() },
                modifier = Modifier.fillMaxSize()
            )
            1 -> OrientationPage(
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
                onSpeedChange = { orientationDetector.setSpeed(it) },
                currentSpeed = orientationDetector.currentSpeed.value,
                modifier = Modifier.fillMaxSize()
            )
            2 -> ConnectionPage(
                connectionManager = connectionManager,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
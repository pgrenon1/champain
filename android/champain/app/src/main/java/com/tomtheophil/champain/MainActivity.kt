package com.tomtheophil.champain

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
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
    internal lateinit var windowComponents: WindowComponents
    private lateinit var sensors: SensorComponents
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
internal class WindowComponents(private val window: android.view.Window) {
    private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

    init {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Always hide system bars
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun cleanup() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Don't show system bars on cleanup
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
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val isConnected = sensors.connectionManager.isConnected()

    Box(modifier = Modifier.fillMaxSize()) {
        NavigationDrawer(
            drawerState = drawerState,
            selectedPage = selectedPage,
            onPageSelected = { selectedPage = it },
            onCalibrate = { sensors.orientationDetector.calibrate() },
            scope = scope,
            isConnected = isConnected
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedPage) {
                    0 -> GamePage(sensors, onPop, systemBarsPadding)
                    1 -> ShakeSettingsPage(sensors, systemBarsPadding)
                    2 -> OrientationSettingsPage(sensors, systemBarsPadding)
                    3 -> ConnectionSettingsPage(sensors, systemBarsPadding)
                }
            }
        }

        // Menu button always on top
        IconButton(
            onClick = { 
                scope.launch { 
                    if (drawerState.isClosed) {
                        drawerState.open()
                    } else {
                        drawerState.close()
                    }
                }
            },
            modifier = Modifier
                .padding(start = 8.dp, top = systemBarsPadding.calculateTopPadding() + 8.dp)
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun ShakeSettingsPage(
    sensors: SensorComponents,
    systemBarsPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UIConstants.CHAMPAGNE_LIGHT,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_DARK
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(96.dp))

            Text(
                "Shake Settings",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
            
            ShakePage(
                threshold = sensors.shakeDetector.shakeThreshold.floatValue,
                onThresholdChange = { sensors.shakeDetector.shakeThreshold.floatValue = it },
                accelY = sensors.shakeDetector.currentAccelY.floatValue,
                shakeCount = sensors.shakeDetector.shakeCount.intValue,
                frequency = sensors.shakeDetector.currentFrequency.floatValue,
                isShaking = sensors.shakeDetector.isShakeDetected.value,
                timestamps = sensors.shakeDetector.recentShakeTimes.toList(),
                onDebugShake = { sensors.shakeDetector.debugTriggerShake() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun OrientationSettingsPage(
    sensors: SensorComponents,
    systemBarsPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UIConstants.CHAMPAGNE_LIGHT,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_DARK
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(96.dp))

            Text(
                "Orientation Settings",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
            
            OrientationPage(
                x = sensors.orientationDetector.x.floatValue,
                y = sensors.orientationDetector.y.floatValue,
                z = sensors.orientationDetector.z.floatValue,
                w = sensors.orientationDetector.w.floatValue,
                calibratedX = sensors.orientationDetector.calibratedX.floatValue,
                calibratedY = sensors.orientationDetector.calibratedY.floatValue,
                calibratedZ = sensors.orientationDetector.calibratedZ.floatValue,
                calibratedW = sensors.orientationDetector.calibratedW.floatValue,
                isCalibrated = sensors.orientationDetector.isCalibrated.value,
                onCalibrate = { sensors.orientationDetector.calibrate() },
                onSpeedChange = { sensors.orientationDetector.setSpeed(it) },
                currentSpeed = sensors.orientationDetector.currentSpeed.value,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ConnectionSettingsPage(
    sensors: SensorComponents,
    systemBarsPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UIConstants.CHAMPAGNE_LIGHT,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_DARK
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(96.dp))

            Text(
                "Connection Settings",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
            
            ConnectionPage(
                connectionManager = sensors.connectionManager,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            )
        }
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

@Composable
private fun SettingsPage(
    sensors: SensorComponents,
    systemBarsPadding: PaddingValues
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UIConstants.CHAMPAGNE_LIGHT,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_DARK
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = UIConstants.UI_DARK,
                contentColor = UIConstants.CONTENT_PRIMARY,
                modifier = Modifier.padding(top = systemBarsPadding.calculateTopPadding()+ 48.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Shake", color = UIConstants.CONTENT_PRIMARY) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Orientation", color = UIConstants.CONTENT_PRIMARY) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Connection", color = UIConstants.CONTENT_PRIMARY) }
                )
            }
            
            when (selectedTab) {
                0 -> ShakePage(
                    threshold = sensors.shakeDetector.shakeThreshold.floatValue,
                    onThresholdChange = { sensors.shakeDetector.shakeThreshold.floatValue = it },
                    accelY = sensors.shakeDetector.currentAccelY.floatValue,
                    shakeCount = sensors.shakeDetector.shakeCount.intValue,
                    frequency = sensors.shakeDetector.currentFrequency.floatValue,
                    isShaking = sensors.shakeDetector.isShakeDetected.value,
                    timestamps = sensors.shakeDetector.recentShakeTimes.toList(),
                    onDebugShake = { sensors.shakeDetector.debugTriggerShake() },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> OrientationPage(
                    x = sensors.orientationDetector.x.floatValue,
                    y = sensors.orientationDetector.y.floatValue,
                    z = sensors.orientationDetector.z.floatValue,
                    w = sensors.orientationDetector.w.floatValue,
                    calibratedX = sensors.orientationDetector.calibratedX.floatValue,
                    calibratedY = sensors.orientationDetector.calibratedY.floatValue,
                    calibratedZ = sensors.orientationDetector.calibratedZ.floatValue,
                    calibratedW = sensors.orientationDetector.calibratedW.floatValue,
                    isCalibrated = sensors.orientationDetector.isCalibrated.value,
                    onCalibrate = { sensors.orientationDetector.calibrate() },
                    onSpeedChange = { sensors.orientationDetector.setSpeed(it) },
                    currentSpeed = sensors.orientationDetector.currentSpeed.value,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> ConnectionPage(
                    connectionManager = sensors.connectionManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = UIConstants.CONTENT_PRIMARY,
            activeTrackColor = UIConstants.CONTENT_PRIMARY,
            inactiveTrackColor = UIConstants.CONTENT_SECONDARY
        )
    )
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = UIConstants.SHAPE_MEDIUM,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = UIConstants.UI_DARK,
            contentColor = UIConstants.CONTENT_PRIMARY,
            disabledContainerColor = UIConstants.UI_MEDIUM,
            disabledContentColor = UIConstants.CONTENT_DISABLED
        )
    ) {
        Text(text, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameContent(
    selectedPage: Int,
    sensors: SensorComponents,
    onPop: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedPage) {
        0 -> Game(
            frequency = sensors.shakeDetector.currentFrequency.floatValue,
            connectionManager = sensors.connectionManager,
            onPop = onPop,
            modifier = modifier
        )
        1 -> {
            Column(modifier = modifier.fillMaxSize()) {
                val pagerState = rememberPagerState { 3 }
                val scope = rememberCoroutineScope()
                
                // Tabs
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Shake", color = Color.White) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Orientation", color = Color.White) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        text = { Text("Connection", color = Color.White) }
                    )
                }

                // Pages
                SettingsPage(
                    sensors = sensors,
                    systemBarsPadding = PaddingValues(0.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionPage(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf(
        connectionManager.getLastEnteredIpAddress() ?: ""
    )}
    var port by remember { mutableStateOf(
        connectionManager.getLastEnteredPort() ?: ""
    )}
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    val deviceId = connectionManager.getDeviceId()
    val isConnected = connectionManager.isConnected()

    if (showDialog) {
        ConnectionDialog(
            ipAddress = ipAddress,
            port = port,
            onIpChange = { ipAddress = it },
            onPortChange = { port = it },
            onDismiss = { showDialog = false },
            onConfirm = {
                try {
                    val portInt = port.toInt()
                    if (connectionManager.connect(ipAddress, portInt)) {
                        connectionStatus = "Connected to $ipAddress:$port"
                    } else {
                        connectionStatus = "Failed to connect"
                    }
                } catch (e: Exception) {
                    connectionStatus = "Invalid port number"
                }
                showDialog = false
            }
        )
    }

    ConnectionPageContent(
        connectionStatus = connectionStatus,
        deviceId = deviceId,
        isConnected = isConnected,
        onConnect = { showDialog = true },
        onDisconnect = {
            connectionManager.disconnect()
            connectionStatus = "Disconnected"
        },
        modifier = modifier
    )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsButton(
                onClick = onConnect,
                text = "Connect",
                modifier = Modifier.weight(1f)
            )

            SettingsButton(
                onClick = onDisconnect,
                text = "Disconnect",
                modifier = Modifier.weight(1f)
            )
        }

        ConnectionInfo(
            status = connectionStatus,
            deviceId = deviceId,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConnectionInfo(
    status: String,
    deviceId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Device ID: $deviceId",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = UIConstants.SHAPE_MEDIUM,
            color = UIConstants.UI_DARK
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Enter Connection Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    color = UIConstants.CONTENT_PRIMARY
                )

                BasicTextField(
                    value = ipAddress,
                    onValueChange = onIpChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = UIConstants.CONTENT_PRIMARY,
                        fontFamily = FontFamily.Monospace
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    UIConstants.CONTENT_SECONDARY,
                                    UIConstants.SHAPE_SMALL
                                )
                                .padding(16.dp)
                        ) {
                            if (ipAddress.isEmpty()) {
                                Text(
                                    "IP Address",
                                    color = UIConstants.CONTENT_SECONDARY,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    },
                    singleLine = true
                )

                BasicTextField(
                    value = port,
                    onValueChange = onPortChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = UIConstants.CONTENT_PRIMARY,
                        fontFamily = FontFamily.Monospace
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    UIConstants.CONTENT_SECONDARY,
                                    UIConstants.SHAPE_SMALL
                                )
                                .padding(16.dp)
                        ) {
                            if (port.isEmpty()) {
                                Text(
                                    "Port",
                                    color = UIConstants.CONTENT_SECONDARY,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UIConstants.UI_MEDIUM,
                            contentColor = UIConstants.CONTENT_PRIMARY
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UIConstants.UI_DARK,
                            contentColor = UIConstants.CONTENT_PRIMARY
                        )
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePage(
    sensors: SensorComponents,
    onPop: () -> Unit,
    systemBarsPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UIConstants.CHAMPAGNE_LIGHT,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_MEDIUM,
                        UIConstants.CHAMPAGNE_DARK
                    )
                )
            )
    ) {
        Game(
            frequency = sensors.shakeDetector.currentFrequency.floatValue,
            connectionManager = sensors.connectionManager,
            onPop = onPop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
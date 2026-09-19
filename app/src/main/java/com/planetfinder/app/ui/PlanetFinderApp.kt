package com.planetfinder.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.planetfinder.app.data.CelestialBody
import com.planetfinder.app.data.LocationSource
import com.planetfinder.app.data.ObservationForecast
import com.planetfinder.app.data.ObservabilityLevel
import com.planetfinder.app.data.ObservabilityRating
import com.planetfinder.app.data.ObservationSample
import com.planetfinder.app.data.ObserverLocation
import com.planetfinder.app.data.SkyPosition
import com.planetfinder.app.sensor.HeadingSensor
import com.planetfinder.app.sensor.SensorAccuracy
import com.planetfinder.app.util.alignmentText
import com.planetfinder.app.util.altitudeAlignmentText
import com.planetfinder.app.util.cardinalDirection
import com.planetfinder.app.util.relativeBearingDegrees
import com.planetfinder.app.util.signedAngleDelta
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class Tab(val route: String, val title: String, val symbol: String) {
    Home("home", "今晚", "✦"), Favorites("favorites", "收藏", "♥"), Finder("finder", "尋星", "⌖")
}

@Composable
fun PlanetFinderApp(viewModel: PlanetFinderViewModel, requestDeviceLocation: () -> Unit) {
    PlanetFinderTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route.orEmpty()
        val selectedTab = Tab.entries.firstOrNull { route == it.route || route.startsWith("${it.route}/") } ?: Tab.Home
        var showLocationDialog by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            StarField()
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    if (!route.startsWith("detail/")) {
                        BottomNavigation(selectedTab) { selected ->
                            navigateToTab(navController, selected)
                        }
                    }
                },
            ) { innerPadding ->
                if (state.positions.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(16.dp))
                            Text("正在計算星空與今晚最佳時段…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    NavHost(navController = navController, startDestination = Tab.Home.route) {
                        composable(Tab.Home.route) {
                            HomeScreen(
                                positions = state.positions,
                                location = state.location,
                                observationTimeMillis = state.observationTimeMillis,
                                isLive = state.isLive,
                                requestLocation = { showLocationDialog = true },
                                onUseLiveTime = viewModel::useLiveTime,
                                onShiftTime = viewModel::shiftObservationTime,
                                onUseTonight = viewModel::useTonight,
                                onOpen = { navController.navigate("detail/${it.body.id}") },
                                onFind = { navController.navigate("finder/${it.body.id}") },
                            )
                        }
                        composable(Tab.Favorites.route) {
                            FavoritesScreen(
                                positions = state.positions.filter { it.body.id in state.favorites },
                                onOpen = { navController.navigate("detail/${it.body.id}") },
                            )
                        }
                        composable(Tab.Finder.route) {
                            FinderScreen(
                                positions = state.positions,
                                selected = state.positions.firstOrNull { it.isAboveHorizon } ?: state.positions.first(),
                                location = state.location,
                                observationTimeMillis = state.observationTimeMillis,
                                isLive = state.isLive,
                                onUseLiveTime = viewModel::useLiveTime,
                                onShiftTime = viewModel::shiftObservationTime,
                                onUseTonight = viewModel::useTonight,
                                onSelected = { replaceFinderTarget(navController, it.body.id) },
                            )
                        }
                        composable("finder/{bodyId}") { entry ->
                            val selected = state.positions.firstOrNull { it.body.id == entry.arguments?.getString("bodyId") }
                                ?: state.positions.first()
                            FinderScreen(
                                positions = state.positions,
                                selected = selected,
                                location = state.location,
                                observationTimeMillis = state.observationTimeMillis,
                                isLive = state.isLive,
                                onUseLiveTime = viewModel::useLiveTime,
                                onShiftTime = viewModel::shiftObservationTime,
                                onUseTonight = viewModel::useTonight,
                                onSelected = { replaceFinderTarget(navController, it.body.id) },
                            )
                        }
                        composable("detail/{bodyId}") { entry ->
                            val position = state.positions.firstOrNull { it.body.id == entry.arguments?.getString("bodyId") }
                                ?: state.positions.first()
                            DetailScreen(
                                position = position,
                                isFavorite = position.body.id in state.favorites,
                                observationTimeMillis = state.observationTimeMillis,
                                isLive = state.isLive,
                                onBack = { navController.popBackStack() },
                                onFavorite = { viewModel.toggleFavorite(position.body.id) },
                                onFind = { navController.navigate("finder/${position.body.id}") },
                                onSelectTime = viewModel::setObservationTime,
                                onUseLiveTime = viewModel::useLiveTime,
                            )
                        }
                    }
                }
            }
            if (showLocationDialog) {
                LocationDialog(
                    current = state.location,
                    onDismiss = { showLocationDialog = false },
                    onDeviceLocation = {
                        showLocationDialog = false
                        requestDeviceLocation()
                    },
                    onManualLocation = {
                        viewModel.setLocation(it)
                        showLocationDialog = false
                    },
                )
            }
        }
    }
}

private fun replaceFinderTarget(navController: androidx.navigation.NavHostController, bodyId: String) {
    val currentId = navController.currentDestination?.id ?: return
    navController.navigate("finder/$bodyId") {
        popUpTo(currentId) { inclusive = true }
        launchSingleTop = true
    }
}

private fun navigateToTab(navController: androidx.navigation.NavHostController, tab: Tab) {
    if (tab == Tab.Home) {
        val returnedHome = navController.popBackStack(Tab.Home.route, inclusive = false)
        if (!returnedHome && navController.currentDestination?.route != Tab.Home.route) {
            navController.navigate(Tab.Home.route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
        return
    }
    navController.navigate(tab.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun StarField() {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxSize()) {
        val random = Random(42)
        repeat(90) {
            val point = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * 0.85f)
            drawCircle(colors.onBackground.copy(alpha = random.nextFloat() * 0.22f + 0.08f), random.nextFloat() * 1.5f + 0.4f, point)
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.primary.copy(alpha = .16f), Color.Transparent), center = Offset(size.width * .82f, size.height * .18f)),
            radius = size.minDimension * .55f,
            center = Offset(size.width * .82f, size.height * .18f),
        )
    }
}

@Composable
private fun ScreenHeader(location: ObserverLocation, requestLocation: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("找到星球", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("PLANET FINDER", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, letterSpacing = 3.sp)
        }
        Surface(
            modifier = Modifier.clickable(enabled = requestLocation != null) { requestLocation?.invoke() },
            color = MaterialTheme.colorScheme.surface.copy(alpha = .9f), shape = RoundedCornerShape(50),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), horizontalAlignment = Alignment.End) {
                Text("⌖ ${location.label}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(location.source.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun TimeControls(
    observationTimeMillis: Long,
    isLive: Boolean,
    onUseLiveTime: () -> Unit,
    onShiftTime: (Int) -> Unit,
    onUseTonight: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isLive) "即時天空" else "模擬時間", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(formatDateTime(observationTimeMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { onShiftTime(-1) }) { Text("−1 小時") }
                Button(onClick = onUseLiveTime, enabled = !isLive) { Text("現在") }
                OutlinedButton(onClick = { onShiftTime(1) }) { Text("＋1 小時") }
                OutlinedButton(onClick = onUseTonight) { Text("今晚 20:00") }
            }
        }
    }
}

@Composable
private fun LocationDialog(
    current: ObserverLocation,
    onDismiss: () -> Unit,
    onDeviceLocation: () -> Unit,
    onManualLocation: (ObserverLocation) -> Unit,
) {
    var label by remember(current) { mutableStateOf(current.label) }
    var latitude by remember(current) { mutableStateOf(current.latitude.toString()) }
    var longitude by remember(current) { mutableStateOf(current.longitude.toString()) }
    val latitudeValue = latitude.toDoubleOrNull()
    val longitudeValue = longitude.toDoubleOrNull()
    val valid = label.isNotBlank() && latitudeValue != null && latitudeValue in -90.0..90.0 &&
        longitudeValue != null && longitudeValue in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("觀測位置", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("目前：${current.label} · ${current.source.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onDeviceLocation) { Text("使用裝置目前位置") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onManualLocation(ObserverLocation(25.0330, 121.5654, label = "台北", source = LocationSource.MANUAL)) }) { Text("台北") }
                    TextButton(onClick = { onManualLocation(ObserverLocation(35.6762, 139.6503, label = "東京", source = LocationSource.MANUAL)) }) { Text("東京") }
                    TextButton(onClick = { onManualLocation(ObserverLocation(1.3521, 103.8198, label = "新加坡", source = LocationSource.MANUAL)) }) { Text("新加坡") }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("位置名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("緯度（-90 至 90）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("經度（-180 至 180）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onManualLocation(
                        ObserverLocation(
                            latitude = latitudeValue!!,
                            longitude = longitudeValue!!,
                            label = label.trim(),
                            source = LocationSource.MANUAL,
                        )
                    )
                },
            ) { Text("套用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun HomeScreen(
    positions: List<SkyPosition>,
    location: ObserverLocation,
    observationTimeMillis: Long,
    isLive: Boolean,
    requestLocation: () -> Unit,
    onUseLiveTime: () -> Unit,
    onShiftTime: (Int) -> Unit,
    onUseTonight: () -> Unit,
    onOpen: (SkyPosition) -> Unit,
    onFind: (SkyPosition) -> Unit,
) {
    val best = positions.filter { it.isAboveHorizon }.maxByOrNull { it.altitude } ?: positions.first()
    LazyColumn(contentPadding = PaddingValues(bottom = 132.dp)) {
        item { ScreenHeader(location, requestLocation) }
        item {
            TimeControls(observationTimeMillis, isLive, onUseLiveTime, onShiftTime, onUseTonight)
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("此刻，在你頭頂", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, letterSpacing = 2.sp)
                Text(best.body.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Text("${best.body.englishName}  ·  仰角 ${best.altitude.degree()}", color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onFind(best) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("⌖  用羅盤尋找", Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("今日天體", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("即時高度與方位", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        items(positions, key = { it.body.id }) { position -> BodyRow(position, onOpen) }
    }
}

@Composable
private fun BodyRow(position: SkyPosition, onOpen: (SkyPosition) -> Unit) {
    val forecast = position.observationForecast
    val isSun = position.body.id == "sun"
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onOpen(position) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(bodyColor(position.body).copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                BoldBodySymbol(position.body, fontSize = 28.sp, strokeWidth = 2.2f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(position.body.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${cardinalDirection(position.azimuth)} ${position.azimuth.degree()}  ·  仰角 ${position.altitude.degree()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ObservationRatingBadge(forecast.overallRating, if (isSun) "需濾鏡" else null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isSun) {
                            if (forecast.samples.isEmpty()) "今日無日出" else "白天 ${formatClock(forecast.samples.first().timeMillis)}–${formatClock(forecast.samples.last().timeMillis)}"
                        } else {
                            forecast.bestWindow?.let { "${formatClock(it.startMillis)}–${formatClock(it.endMillis)}" }
                                ?: "今晚無適合時段"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    if (isSun) forecast.overallRating.equipment else forecast.overallRating.reasons.firstOrNull()
                        ?: position.bestObservation?.let { "最高 ${it.altitude.degree()}" }
                        ?: "今晚不在地平線上",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                )
            }
            Text(
                if (position.isAboveHorizon) "可見\n→" else "地平線下",
                color = if (position.isAboveHorizon) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (position.isAboveHorizon) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun FavoritesScreen(positions: List<SkyPosition>, onOpen: (SkyPosition) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 132.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(20.dp)) {
                Text("收藏星體", color = MaterialTheme.colorScheme.onBackground, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("你的私人觀測清單", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (positions.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("♡", color = MaterialTheme.colorScheme.primary, fontSize = 48.sp)
                Text("還沒有收藏", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("開啟天體詳情，點一下愛心加入", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        items(positions, key = { it.body.id }) { BodyRow(it, onOpen) }
    }
}

@Composable
private fun FinderScreen(
    positions: List<SkyPosition>,
    selected: SkyPosition,
    location: ObserverLocation,
    observationTimeMillis: Long,
    isLive: Boolean,
    onUseLiveTime: () -> Unit,
    onShiftTime: (Int) -> Unit,
    onUseTonight: () -> Unit,
    onSelected: (SkyPosition) -> Unit,
) {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var phoneAltitude by remember { mutableStateOf<Float?>(null) }
    var sensorAccuracy by remember { mutableStateOf(SensorAccuracy.INITIALIZING) }
    val sensor = remember {
        HeadingSensor(
            context = context,
            onOrientation = { newHeading, newAltitude ->
                heading = newHeading
                phoneAltitude = newAltitude
            },
            onAccuracy = { sensorAccuracy = it },
        )
    }
    LaunchedEffect(location) { sensor.updateLocation(location) }
    DisposableEffect(sensor) {
        sensor.start()
        onDispose { sensor.stop() }
    }
    val delta = heading?.let { signedAngleDelta(selected.azimuth, it.toDouble()) }
    val currentPhoneAltitude = phoneAltitude
    val altitudeDelta = currentPhoneAltitude?.let { selected.altitude - it }
    val isAligned = selected.isAboveHorizon && delta != null && altitudeDelta != null &&
        abs(delta) < 3.0 && abs(altitudeDelta) < 3.0

    LaunchedEffect(isAligned) {
        if (isAligned) vibrateAlignment(context)
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { ScreenHeader(location) }
        item {
            TimeControls(observationTimeMillis, isLive, onUseLiveTime, onShiftTime, onUseTonight)
        }
        item {
            Text("尋找 ${selected.body.name}", color = MaterialTheme.colorScheme.onBackground, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("方位 ${selected.azimuth.degree()} · 仰角 ${selected.altitude.degree()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SensorStatus(sensorAccuracy)
            Spacer(Modifier.height(14.dp))
            Compass(selected, heading, delta)
            Text(
                when {
                    !selected.isAboveHorizon -> "目前在地平線下 ${abs(selected.altitude).degree()}，請等待升起"
                    sensorAccuracy == SensorAccuracy.UNAVAILABLE -> "無法使用羅盤導引"
                    heading == null || currentPhoneAltitude == null -> "正在讀取方向感測器…"
                    isAligned -> "已對準 ${selected.body.name}"
                    else -> alignmentText(delta!!)
                },
                color = if (selected.isAboveHorizon) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                when {
                    !selected.isAboveHorizon -> "下一次升起：${formatTime(selected.riseMillis)}"
                    altitudeDelta == null -> "將手機頂端朝向目標，並依提示抬高手機"
                    else -> "${altitudeAlignmentText(altitudeDelta)} · 手機 ${currentPhoneAltitude.toDouble().degree()} · 目標 ${selected.altitude.degree()}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp),
            )
        }
        if (selected.body.id == "sun") item { SunWarning() }
        item {
            Text("切換目標", Modifier.fillMaxWidth().padding(start = 20.dp, top = 22.dp, bottom = 8.dp), fontWeight = FontWeight.Bold)
        }
        items(positions.chunked(3)) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { item ->
                    Surface(
                        modifier = Modifier.padding(4.dp).weight(1f).clickable { onSelected(item) },
                        color = if (item.body.id == selected.body.id) MaterialTheme.colorScheme.primary.copy(alpha = .24f) else MaterialTheme.colorScheme.surface.copy(alpha = .88f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            BoldBodySymbol(item.body, fontSize = 24.sp)
                            Text(item.body.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.padding(4.dp).weight(1f)) }
            }
        }
    }
}

@Composable
private fun SensorStatus(accuracy: SensorAccuracy) {
    val warning = accuracy == SensorAccuracy.UNAVAILABLE || accuracy == SensorAccuracy.UNRELIABLE || accuracy == SensorAccuracy.LOW
    Surface(
        modifier = Modifier.padding(top = 8.dp),
        color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            accuracy.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Suppress("DEPRECATION")
private fun vibrateAlignment(context: Context) {
    val effect = VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
    } else {
        context.getSystemService(Vibrator::class.java).vibrate(effect)
    }
}

@Composable
private fun BoldBodySymbol(body: CelestialBody, fontSize: TextUnit, strokeWidth: Float = 2f) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            body.symbol,
            color = bodyColor(body),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            style = TextStyle(drawStyle = Stroke(width = strokeWidth)),
        )
        Text(
            body.symbol,
            color = bodyColor(body),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun Compass(selected: SkyPosition, heading: Float?, delta: Double?) {
    val colors = MaterialTheme.colorScheme
    val compassHeading = heading?.toDouble() ?: 0.0
    Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val center = this.center
            val radius = size.minDimension / 2
            drawCircle(colors.surfaceVariant.copy(alpha = .82f), radius)
            drawCircle(colors.primary.copy(alpha = .55f), radius, style = Stroke(2.dp.toPx()))
            rotate(degrees = -compassHeading.toFloat(), pivot = center) {
                repeat(36) { index ->
                    val angle = index * 10.0 * PI / 180.0
                    val outer = Offset(center.x + sin(angle).toFloat() * radius, center.y - cos(angle).toFloat() * radius)
                    val innerRadius = radius - if (index % 9 == 0) 15.dp.toPx() else 7.dp.toPx()
                    val inner = Offset(center.x + sin(angle).toFloat() * innerRadius, center.y - cos(angle).toFloat() * innerRadius)
                    drawLine(colors.onSurface.copy(alpha = if (index % 9 == 0) .75f else .22f), inner, outer, strokeWidth = if (index % 9 == 0) 2.dp.toPx() else 1.dp.toPx())
                }
            }
            if (delta != null && selected.isAboveHorizon) {
                val a = delta * PI / 180.0
                val tip = Offset(center.x + sin(a).toFloat() * radius * .56f, center.y - cos(a).toFloat() * radius * .56f)
                drawLine(colors.secondary, center, tip, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(colors.secondary, 8.dp.toPx(), tip)
            }
            drawCircle(colors.background, 47.dp.toPx(), center)
            drawCircle(colors.primary, 5.dp.toPx(), center)
        }
        CompassLabelAt("北", 0.0, compassHeading, isNorth = true)
        CompassLabelAt("東", 90.0, compassHeading)
        CompassLabelAt("南", 180.0, compassHeading)
        CompassLabelAt("西", 270.0, compassHeading)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(selected.body.symbol, color = bodyColor(selected.body), fontSize = 34.sp)
            Text(heading?.let { "${it.toInt()}°" } ?: "--°", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BoxScope.CompassLabelAt(text: String, bearing: Double, heading: Double, isNorth: Boolean = false) {
    val angle = Math.toRadians(relativeBearingDegrees(bearing, heading))
    CompassLabel(
        text,
        Modifier.align(Alignment.Center).offset(
            x = (sin(angle) * 96.0).toFloat().dp,
            y = (-cos(angle) * 96.0).toFloat().dp,
        ),
        isNorth,
    )
}

@Composable
private fun CompassLabel(text: String, modifier: Modifier, isNorth: Boolean = false) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            color = if (isNorth) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isNorth) FontWeight.Black else FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SunWarning() {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), color = Color(0xFF593B17), shape = RoundedCornerShape(16.dp)) {
        Text("太陽安全警告\n切勿用肉眼、相機或未裝合格太陽濾鏡的望遠鏡直視太陽。只依羅盤判斷方向。", Modifier.padding(16.dp), color = Color(0xFFFFD998), lineHeight = 21.sp)
    }
}

@Composable
private fun ObservationRatingBadge(rating: ObservabilityRating, labelOverride: String? = null) {
    val color = if (labelOverride != null) MaterialTheme.colorScheme.tertiary else when (rating.level) {
        ObservabilityLevel.RECOMMENDED -> MaterialTheme.colorScheme.secondary
        ObservabilityLevel.FAIR -> MaterialTheme.colorScheme.primary
        ObservabilityLevel.DIFFICULT -> MaterialTheme.colorScheme.tertiary
        ObservabilityLevel.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = .16f), shape = RoundedCornerShape(50)) {
        Text(
            labelOverride ?: rating.level.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun ObservationTimelineCard(
    body: CelestialBody,
    observation: ObservationForecast,
    observationTimeMillis: Long,
    isLive: Boolean,
    onSelectTime: (Long) -> Unit,
    onUseLiveTime: () -> Unit,
) {
    val samples = observation.samples
    val isSun = body.id == "sun"
    var selectedIndex by remember(observation, observationTimeMillis) {
        mutableStateOf(samples.indices.minByOrNull { abs(samples[it].timeMillis - observationTimeMillis) } ?: 0)
    }
    val selected = samples.getOrNull(selectedIndex)
    val lineColor = bodyColor(body)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)
    val horizonColor = MaterialTheme.colorScheme.tertiary.copy(alpha = .65f)
    val cursorColor = MaterialTheme.colorScheme.onSurface
    val bestWindowColor = MaterialTheme.colorScheme.secondary.copy(alpha = .12f)
    val twilightColors = mapOf(
        com.planetfinder.app.data.TwilightPhase.DAYLIGHT to Color(0x33FFD36A),
        com.planetfinder.app.data.TwilightPhase.CIVIL to Color(0x334C8DFF),
        com.planetfinder.app.data.TwilightPhase.NAUTICAL to Color(0x333E5BA9),
        com.planetfinder.app.data.TwilightPhase.ASTRONOMICAL to Color(0x33293A78),
        com.planetfinder.app.data.TwilightPhase.DARK to Color(0x33070B1C),
    )

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ObservationRatingBadge(observation.overallRating, if (isSun) "需濾鏡" else null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSun) {
                        if (samples.isEmpty()) "今日沒有日照時段" else "日出 ${formatClock(samples.first().timeMillis)} · 日落 ${formatClock(samples.last().timeMillis)}"
                    } else {
                        observation.bestWindow?.let { "最佳 ${formatClock(it.startMillis)}–${formatClock(it.endMillis)}" }
                            ?: "今晚沒有適合觀測的區間"
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                if (!isLive) TextButton(onClick = onUseLiveTime) { Text("回到現在") }
            }
            Spacer(Modifier.height(8.dp))
            Canvas(
                Modifier.fillMaxWidth().height(190.dp).pointerInput(samples) {
                    detectTapGestures { offset ->
                        if (samples.isNotEmpty()) {
                            val selectedMillis = samples.first().timeMillis +
                                ((offset.x / size.width) * (samples.last().timeMillis - samples.first().timeMillis)).toLong()
                            selectedIndex = samples.indices.minByOrNull { abs(samples[it].timeMillis - selectedMillis) } ?: 0
                            onSelectTime(samples[selectedIndex].timeMillis)
                        }
                    }
                }
            ) {
                if (samples.size < 2) return@Canvas
                val startMillis = samples.first().timeMillis
                val durationMillis = (samples.last().timeMillis - startMillis).coerceAtLeast(1L)
                fun x(timeMillis: Long) = size.width * (timeMillis - startMillis).toFloat() / durationMillis.toFloat()
                fun y(altitude: Double): Float {
                    val normalized = ((altitude.coerceIn(-20.0, 90.0) + 20.0) / 110.0).toFloat()
                    return size.height * (1f - normalized)
                }

                samples.dropLast(1).forEachIndexed { index, sample ->
                    drawRect(
                        color = twilightColors.getValue(com.planetfinder.app.data.twilightPhase(sample.sunAltitude)),
                        topLeft = Offset(x(sample.timeMillis), 0f),
                        size = androidx.compose.ui.geometry.Size(x(samples[index + 1].timeMillis) - x(sample.timeMillis) + 1f, size.height),
                    )
                }
                listOf(0.0, 30.0, 60.0, 90.0).forEach { altitude ->
                    drawLine(if (altitude == 0.0) horizonColor else gridColor, Offset(0f, y(altitude)), Offset(size.width, y(altitude)), strokeWidth = if (altitude == 0.0) 2f else 1f)
                }
                observation.bestWindow?.let { window ->
                    drawRect(
                        color = bestWindowColor,
                        topLeft = Offset(x(window.startMillis), 0f),
                        size = androidx.compose.ui.geometry.Size((x(window.endMillis) - x(window.startMillis)).coerceAtLeast(4f), size.height),
                    )
                }
                val path = Path().apply {
                    samples.forEachIndexed { index, sample ->
                        if (index == 0) moveTo(x(sample.timeMillis), y(sample.altitude)) else lineTo(x(sample.timeMillis), y(sample.altitude))
                    }
                }
                drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                val cursorX = x(samples[selectedIndex.coerceIn(samples.indices)].timeMillis)
                drawLine(cursorColor.copy(alpha = .65f), Offset(cursorX, 0f), Offset(cursorX, size.height), strokeWidth = 2f)
                drawCircle(lineColor, 7f, Offset(cursorX, y(samples[selectedIndex.coerceIn(samples.indices)].altitude)))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val firstMillis = samples.firstOrNull()?.timeMillis
                val lastMillis = samples.lastOrNull()?.timeMillis
                if (firstMillis != null && lastMillis != null) repeat(5) { index ->
                    Text(formatClock(firstMillis + (lastMillis - firstMillis) * index / 4), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
            selected?.let { sample ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${formatClock(sample.timeMillis)} · 仰角 ${sample.altitude.degree()} · ${cardinalDirection(sample.azimuth)}", fontWeight = FontWeight.Bold)
                        Text(sample.rating.reasons.joinToString("、"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    ObservationRatingBadge(sample.rating, if (isSun) "需濾鏡" else null)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (isSun) {
                Text("僅可透過正確安裝的合格太陽濾鏡觀測", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
            } else {
                TwilightLegend()
            }
            Text("點選曲線可將整個 APP 切換至該模擬時間", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TwilightLegend() {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(
            "日光" to Color(0xFFFFD36A),
            "民用" to Color(0xFF4C8DFF),
            "航海" to Color(0xFF3E5BA9),
            "天文" to Color(0xFF293A78),
            "暗夜" to Color(0xFF10172F),
        ).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailScreen(
    position: SkyPosition,
    isFavorite: Boolean,
    observationTimeMillis: Long,
    isLive: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onFind: () -> Unit,
    onSelectTime: (Long) -> Unit,
    onUseLiveTime: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹  返回", Modifier.clip(RoundedCornerShape(12.dp)).clickable { onBack() }.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(if (isFavorite) "♥" else "♡", Modifier.clip(CircleShape).clickable { onFavorite() }.padding(10.dp), color = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground, fontSize = 26.sp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(112.dp).background(Brush.radialGradient(listOf(bodyColor(position.body).copy(alpha = .45f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
                    Text(position.body.symbol, color = bodyColor(position.body), fontSize = 78.sp)
                }
                Text(position.body.englishName, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, letterSpacing = 4.sp)
                Text(position.body.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(position.body.description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 23.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onFind, shape = RoundedCornerShape(14.dp)) { Text("⌖  用羅盤尋找", Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.Bold) }
            }
        }
        if (position.body.id == "sun") item { SunWarning() }
        item {
            Text(if (position.body.id == "sun") "白天觀測時間軸" else "今晚觀測時間軸", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            ObservationTimelineCard(
                body = position.body,
                observation = position.observationForecast,
                observationTimeMillis = observationTimeMillis,
                isLive = isLive,
                onSelectTime = onSelectTime,
                onUseLiveTime = onUseLiveTime,
            )
        }
        item {
            Text("今日觀測", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .9f), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    DataLine("目前狀態", if (position.isAboveHorizon) "地平線上 · 可觀測" else "地平線下")
                    DataLine("方位", "${cardinalDirection(position.azimuth)}  ${position.azimuth.degree()}")
                    DataLine("仰角", position.altitude.degree())
                    DataLine("升起", formatTime(position.riseMillis))
                    DataLine("落下", formatTime(position.setMillis))
                    DataLine(
                        if (position.body.id == "sun") "白天最高" else "今晚最佳",
                        position.bestObservation?.let { "${formatClock(it.timeMillis)} · 仰角 ${it.altitude.degree()}" }
                            ?: if (position.body.id == "sun") "今日沒有日照" else "今晚不在地平線上",
                    )
                    DataLine("可觀測性", position.currentRating.level.displayName)
                    DataLine("主要原因", position.currentRating.reasons.joinToString("、").ifEmpty { "—" })
                    DataLine("器材建議", position.currentRating.equipment)
                    DataLine("照明比例", "${(position.illumination * 100).toInt()}%")
                    DataLine("視星等", position.magnitude?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—", divider = false)
                }
            }
        }
    }
}

@Composable
private fun DataLine(label: String, value: String, divider: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.35f))
        Text(
            value,
            modifier = Modifier.weight(.65f),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
    if (divider) HorizontalDivider(color = Color.White.copy(alpha = .08f))
}

@Composable
private fun BottomNavigation(selected: Tab, onSelected: (Tab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .96f), shadowElevation = 20.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(76.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tab.entries.forEach { tab ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(tab.symbol, color = if (tab == selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp)
                    Text(tab.title, color = if (tab == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

private fun Double.degree(): String = String.format(Locale.getDefault(), "%.1f°", this)

private fun formatTime(millis: Long?): String = millis?.let {
    DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
} ?: "兩日內無事件"

private fun formatClock(millis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis))

private fun formatDateTime(millis: Long): String =
    DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis))

private fun bodyColor(body: CelestialBody): Color = when (body.id) {
    "sun" -> Solar
    "moon" -> Color(0xFFE4E8F7)
    "mercury" -> Color(0xFFB8B6AE)
    "venus" -> Color(0xFFFFDB91)
    "mars" -> Color(0xFFFF7B62)
    "jupiter" -> Color(0xFFE7B58A)
    "saturn" -> Color(0xFFF0D28D)
    "uranus" -> Color(0xFF82DFE6)
    else -> Color(0xFF7395FF)
}

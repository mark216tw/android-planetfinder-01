package com.planetfinder.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.planetfinder.app.data.AstronomyRepository
import com.planetfinder.app.data.CelestialBody
import com.planetfinder.app.data.FavoritesStore
import com.planetfinder.app.data.ObserverLocation
import com.planetfinder.app.data.SkyPosition
import com.planetfinder.app.sensor.HeadingSensor
import com.planetfinder.app.util.alignmentText
import com.planetfinder.app.util.altitudeAlignmentText
import com.planetfinder.app.util.cardinalDirection
import com.planetfinder.app.util.signedAngleDelta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class Tab(val title: String, val symbol: String) {
    Home("今晚", "✦"), Favorites("收藏", "♥"), Finder("尋星", "⌖")
}

@Composable
fun PlanetFinderApp(location: ObserverLocation, requestLocation: () -> Unit) {
    PlanetFinderTheme {
        val context = LocalContext.current
        val favoritesStore = remember { FavoritesStore(context) }
        var favorites by remember { mutableStateOf(favoritesStore.load()) }
        var positions by remember { mutableStateOf<List<SkyPosition>>(emptyList()) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var previousTab by remember { mutableStateOf(Tab.Home) }
        var detail by remember { mutableStateOf<SkyPosition?>(null) }
        var finderTarget by remember { mutableStateOf<SkyPosition?>(null) }
        var finderReturnDetail by remember { mutableStateOf<SkyPosition?>(null) }

        BackHandler(enabled = detail != null || tab != Tab.Home) {
            when {
                detail != null -> detail = null
                tab == Tab.Finder && finderReturnDetail != null -> {
                    detail = finderReturnDetail
                    finderReturnDetail = null
                    tab = previousTab
                }
                else -> {
                    tab = if (previousTab == tab) Tab.Home else previousTab
                    previousTab = Tab.Home
                    finderReturnDetail = null
                }
            }
        }

        LaunchedEffect(location) {
            while (true) {
                positions = withContext(Dispatchers.Default) { AstronomyRepository.positions(location) }
                delay(60_000)
            }
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            StarField()
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    if (detail == null) BottomNavigation(tab) { selected ->
                        if (selected != tab) {
                            previousTab = tab
                            tab = selected
                            finderReturnDetail = null
                        }
                    }
                },
            ) { innerPadding ->
                if (positions.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(16.dp))
                            Text("正在讀取此刻星空…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (detail != null) {
                    DetailScreen(
                        position = detail!!,
                        isFavorite = detail!!.body.id in favorites,
                        onBack = { detail = null },
                        onFavorite = {
                            favorites = favorites.toggle(detail!!.body.id)
                            favoritesStore.save(favorites)
                        },
                        onFind = {
                            finderTarget = detail
                            finderReturnDetail = detail
                            previousTab = tab
                            detail = null
                            tab = Tab.Finder
                        },
                    )
                } else {
                    when (tab) {
                        Tab.Home -> HomeScreen(
                            positions, location, requestLocation,
                            onOpen = { detail = it },
                            onFind = {
                                finderTarget = it
                                finderReturnDetail = null
                                previousTab = tab
                                tab = Tab.Finder
                            },
                        )
                        Tab.Favorites -> FavoritesScreen(
                            positions.filter { it.body.id in favorites },
                            onOpen = { detail = it },
                        )
                        Tab.Finder -> FinderScreen(
                            positions = positions,
                            selected = finderTarget ?: positions.firstOrNull { it.isAboveHorizon } ?: positions.first(),
                            location = location,
                            onSelected = { finderTarget = it },
                        )
                    }
                }
            }
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

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
            Text("⌖ ${location.label}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HomeScreen(
    positions: List<SkyPosition>,
    location: ObserverLocation,
    requestLocation: () -> Unit,
    onOpen: (SkyPosition) -> Unit,
    onFind: (SkyPosition) -> Unit,
) {
    val best = positions.filter { it.isAboveHorizon }.maxByOrNull { it.altitude } ?: positions.first()
    LazyColumn(contentPadding = PaddingValues(bottom = 132.dp)) {
        item { ScreenHeader(location, requestLocation) }
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
                ) { Text("開啟尋星羅盤  →", Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.Bold) }
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
    onSelected: (SkyPosition) -> Unit,
) {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var phoneAltitude by remember { mutableStateOf<Float?>(null) }
    val sensor = remember {
        HeadingSensor(context) { newHeading, newAltitude ->
            heading = newHeading
            phoneAltitude = newAltitude
        }
    }
    LaunchedEffect(location) { sensor.updateLocation(location) }
    DisposableEffect(sensor) {
        sensor.start()
        onDispose { sensor.stop() }
    }
    val delta = heading?.let { signedAngleDelta(selected.azimuth, it.toDouble()) }
    val currentPhoneAltitude = phoneAltitude
    val altitudeDelta = currentPhoneAltitude?.let { selected.altitude - it }

    LazyColumn(contentPadding = PaddingValues(bottom = 132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { ScreenHeader(location) }
        item {
            Text("尋找 ${selected.body.name}", color = MaterialTheme.colorScheme.onBackground, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("方位 ${selected.azimuth.degree()} · 仰角 ${selected.altitude.degree()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Compass(selected, heading, delta)
            Text(
                when {
                    !selected.isAboveHorizon -> "目前在地平線下 ${abs(selected.altitude).degree()}，請等待升起"
                    heading == null || currentPhoneAltitude == null -> "正在讀取方向感測器…"
                    abs(delta!!) < 3.0 && abs(altitudeDelta!!) < 3.0 -> "已對準 ${selected.body.name}"
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
    Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val center = this.center
            val radius = size.minDimension / 2
            drawCircle(colors.surfaceVariant.copy(alpha = .82f), radius)
            drawCircle(colors.primary.copy(alpha = .55f), radius, style = Stroke(2.dp.toPx()))
            repeat(36) { index ->
                val angle = index * 10.0 * PI / 180.0
                val outer = Offset(center.x + sin(angle).toFloat() * radius, center.y - cos(angle).toFloat() * radius)
                val innerRadius = radius - if (index % 9 == 0) 15.dp.toPx() else 7.dp.toPx()
                val inner = Offset(center.x + sin(angle).toFloat() * innerRadius, center.y - cos(angle).toFloat() * innerRadius)
                drawLine(colors.onSurface.copy(alpha = if (index % 9 == 0) .75f else .22f), inner, outer, strokeWidth = if (index % 9 == 0) 2.dp.toPx() else 1.dp.toPx())
            }
            if (delta != null && selected.isAboveHorizon) {
                val a = delta * PI / 180.0
                val tip = Offset(center.x + sin(a).toFloat() * radius * .7f, center.y - cos(a).toFloat() * radius * .7f)
                drawLine(colors.secondary, center, tip, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(colors.secondary, 9.dp.toPx(), tip)
            }
            drawCircle(colors.background, 47.dp.toPx(), center)
            drawCircle(colors.primary, 5.dp.toPx(), center)
        }
        CompassLabel("北", Modifier.align(Alignment.TopCenter).padding(top = 22.dp), isNorth = true)
        CompassLabel("東", Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
        CompassLabel("南", Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
        CompassLabel("西", Modifier.align(Alignment.CenterStart).padding(start = 22.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(selected.body.symbol, color = bodyColor(selected.body), fontSize = 34.sp)
            Text(heading?.let { "${it.toInt()}°" } ?: "--°", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
private fun DetailScreen(
    position: SkyPosition,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onFind: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(Modifier.statusBarsPadding().fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹  返回", Modifier.clip(RoundedCornerShape(12.dp)).clickable { onBack() }.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(if (isFavorite) "♥" else "♡", Modifier.clip(CircleShape).clickable { onFavorite() }.padding(10.dp), color = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground, fontSize = 26.sp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 4.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(136.dp).background(Brush.radialGradient(listOf(bodyColor(position.body).copy(alpha = .45f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
                    Text(position.body.symbol, color = bodyColor(position.body), fontSize = 78.sp)
                }
                Text(position.body.englishName, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, letterSpacing = 4.sp)
                Text(position.body.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(position.body.description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 23.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onFind, shape = RoundedCornerShape(14.dp)) { Text("⌖  用羅盤尋找", Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.Bold) }
            }
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
                    DataLine("照明比例", "${(position.illumination * 100).toInt()}%")
                    DataLine("視星等", position.magnitude?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—", divider = false)
                }
            }
        }
        if (position.body.id == "sun") item { SunWarning() }
    }
}

@Composable
private fun DataLine(label: String, value: String, divider: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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

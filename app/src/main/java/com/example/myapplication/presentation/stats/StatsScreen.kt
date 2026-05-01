package com.example.myapplication.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.DailyStats
import com.example.myapplication.ui.theme.QualityGood
import com.example.myapplication.ui.theme.StreakFlame
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    // Refresh stats every time this screen enters composition (tab switch, navigateUp, etc.)
    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = cs.primary)
        }
        return
    }

    val stats = uiState.stats

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Thống kê", color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Tổng quan tiến độ học tập", color = cs.onSurfaceVariant, fontSize = 14.sp)
        }

        // Overview stats grid (2x2)
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewCard("Tổng thẻ", "${stats?.totalCards ?: 0}", Icons.Default.School, cs.primary, Modifier.weight(1f))
                OverviewCard("Đã ôn tập", "${stats?.totalReviews ?: 0}", Icons.Default.FlashOn, cs.secondary, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewCard("Chuỗi ngày", "${stats?.currentStreak ?: 0}", Icons.Default.LocalFireDepartment, StreakFlame, Modifier.weight(1f))
                OverviewCard("Chính xác", "${((stats?.averageAccuracy ?: 0f) * 100).toInt()}%", Icons.AutoMirrored.Filled.TrendingUp, QualityGood, Modifier.weight(1f))
            }
        }

        // STATS-05: Forecast card
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.primary.copy(alpha = 0.08f))
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = cs.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Dự báo ngày mai", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            text = "Cần ôn ${uiState.tomorrowForecast} thẻ",
                            color = cs.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        // STATS-04: Pie chart
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Phân bổ thẻ", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    val mastered = stats?.masteredCards ?: 0
                    val learning = stats?.learningCards ?: 0
                    val newCards = stats?.newCards ?: 0

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PieChart(
                            data = listOf(
                                PieSlice(mastered.toFloat(), QualityGood),
                                PieSlice(learning.toFloat(), cs.primary),
                                PieSlice(newCards.toFloat(), cs.secondary)
                            ),
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(Modifier.width(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PieLegendItem("Thành thạo", mastered, QualityGood)
                            PieLegendItem("Đang học", learning, cs.primary)
                            PieLegendItem("Mới", newCards, cs.secondary)
                        }
                    }
                }
            }
        }

        // STATS-02: Calendar heatmap
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Lịch hoạt động", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    CalendarHeatmap(dailyStats = stats?.dailyStats ?: emptyList())
                }
            }
        }

        // STATS-03: Bar chart
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Thẻ ôn theo ngày (7 ngày)", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    WeeklyBarChart(dailyStats = stats?.dailyStats ?: emptyList())
                }
            }
        }

        // Mastery breakdown bars
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Tiến độ chi tiết", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    val total = (stats?.totalCards ?: 1).coerceAtLeast(1)
                    MasteryRow("Thành thạo", stats?.masteredCards ?: 0, total, QualityGood)
                    Spacer(Modifier.height(10.dp))
                    MasteryRow("Đang học", stats?.learningCards ?: 0, total, cs.primary)
                    Spacer(Modifier.height(10.dp))
                    MasteryRow("Mới", stats?.newCards ?: 0, total, cs.secondary)
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun OverviewCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = cs.onSurfaceVariant, fontSize = 12.sp)
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = cs.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MasteryRow(label: String, count: Int, total: Int, color: Color) {
    val cs = MaterialTheme.colorScheme
    val fraction = count.toFloat() / total
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp)
            Text("$count", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(cs.outlineVariant.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

// ── STATS-04: Pie Chart ──

data class PieSlice(val value: Float, val color: Color)

@Composable
private fun PieChart(data: List<PieSlice>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        var startAngle = -90f
        val strokeWidth = 24.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        data.forEach { slice ->
            val sweep = (slice.value / total) * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep.coerceAtLeast(0.5f),
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun PieLegendItem(label: String, count: Int, color: Color) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", color = cs.onSurfaceVariant, fontSize = 13.sp)
        Text("$count", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// ── STATS-02: Calendar Heatmap ──

@Composable
private fun CalendarHeatmap(dailyStats: List<DailyStats>) {
    val cs = MaterialTheme.colorScheme

    // Track which month to display (default = current month)
    var displayMonth by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    }
    var displayYear by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
    }

    // Build a calendar for the displayed month
    val calStart = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val daysInMonth = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Monday=0 ... Sunday=6  (ISO week start)
    val firstDayOfWeek = ((calStart.get(Calendar.DAY_OF_WEEK) + 5) % 7)

    // Build list of day timestamps for the month
    val monthDays = (1..daysInMonth).map { day ->
        val cal = calStart.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.timeInMillis
    }

    // Map dailyStats by day-start timestamp
    val statsMap = dailyStats.associateBy { stat ->
        val cal = Calendar.getInstance().apply { timeInMillis = stat.date }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val maxReviewed = dailyStats.maxOfOrNull { it.cardsReviewed }?.coerceAtLeast(1) ?: 1

    // Today for highlighting
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val todayMs = todayCal.timeInMillis

    // Current month to limit forward navigation
    val nowMonth = Calendar.getInstance().get(Calendar.MONTH)
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)

    // Month name
    val monthName = SimpleDateFormat("MMMM yyyy", Locale("vi")).format(calStart.time)
        .replaceFirstChar { it.uppercaseChar() }

    Column {
        // ── Month header with prev/next ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(onClick = {
                if (displayMonth == 0) {
                    displayMonth = 11; displayYear--
                } else displayMonth--
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Tháng trước",
                    tint = cs.onSurfaceVariant
                )
            }
            Text(
                text = monthName,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = cs.onSurface
            )
            androidx.compose.material3.IconButton(
                onClick = {
                    if (displayMonth == 11) {
                        displayMonth = 0; displayYear++
                    } else displayMonth++
                },
                enabled = !(displayYear == nowYear && displayMonth >= nowMonth)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tháng sau",
                    tint = if (displayYear == nowYear && displayMonth >= nowMonth)
                        cs.outlineVariant else cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Day-of-week headers (Mon..Sun) ──
        val dayHeaders = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dayHeaders.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 10.sp, color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Calendar grid ──
        val totalCells = firstDayOfWeek + daysInMonth
        val totalWeeks = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until totalWeeks) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (dow in 0 until 7) {
                        val cellIndex = week * 7 + dow
                        val dayIndex = cellIndex - firstDayOfWeek // 0-based day of month

                        if (dayIndex in 0 until daysInMonth) {
                            val dayMs = monthDays[dayIndex]
                            val stat = statsMap[dayMs]
                            val intensity = if (stat != null)
                                (stat.cardsReviewed.toFloat() / maxReviewed).coerceIn(0f, 1f) else 0f
                            val isToday = dayMs == todayMs

                            val bgColor = when {
                                intensity == 0f -> cs.outlineVariant.copy(alpha = 0.15f)
                                intensity < 0.25f -> QualityGood.copy(alpha = 0.2f)
                                intensity < 0.5f -> QualityGood.copy(alpha = 0.4f)
                                intensity < 0.75f -> QualityGood.copy(alpha = 0.65f)
                                else -> QualityGood
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dayIndex + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        intensity > 0.5f -> Color.White
                                        isToday -> cs.primary
                                        else -> cs.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Empty cell (before day 1 or after last day)
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Text("Ít", color = cs.onSurfaceVariant, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        listOf(0.15f, 0.2f, 0.4f, 0.65f, 1.0f).forEach { alpha ->
            Box(
                Modifier.size(12.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (alpha < 0.18f) cs.outlineVariant.copy(alpha = alpha) else QualityGood.copy(alpha = alpha))
            )
            Spacer(Modifier.width(2.dp))
        }
        Text("Nhiều", color = cs.onSurfaceVariant, fontSize = 10.sp)
    }
}

// ── STATS-03: Bar Chart ──

@Composable
private fun WeeklyBarChart(dailyStats: List<DailyStats>) {
    val cs = MaterialTheme.colorScheme
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    val last7 = dailyStats.sortedBy { it.date }.takeLast(7)
    val maxCount = last7.maxOfOrNull { it.cardsReviewed }?.coerceAtLeast(1) ?: 1

    if (last7.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("Chưa có dữ liệu", color = cs.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }

    Row(
        Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        last7.forEach { day ->
            val fraction = (day.cardsReviewed.toFloat() / maxCount).coerceIn(0.05f, 1f)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${day.cardsReviewed}", color = cs.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth(0.7f).weight(fraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(cs.primary)
                )
                if (fraction < 1f) {
                    Spacer(Modifier.weight(1f - fraction))
                }
                Spacer(Modifier.height(4.dp))
                Text(dateFormat.format(Date(day.date)), color = cs.onSurfaceVariant, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

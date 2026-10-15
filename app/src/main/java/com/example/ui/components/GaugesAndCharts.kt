package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Dose
import com.example.model.Strain
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface1
import com.example.ui.theme.CardSurface2
import com.example.ui.theme.ColorDanger
import com.example.ui.theme.ColorNight
import com.example.ui.theme.ColorSuccess
import com.example.ui.theme.ColorWarning
import com.example.ui.theme.ColorWater
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.parseHexColor
import com.example.util.calcActiveMitAtTime
import com.example.util.dayKey
import com.example.util.r1
import com.example.util.r2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// --- 1. PRECISION DIAL (Tachometr s 28 ryskami) ---
@Composable
fun PrecisionDial(
    currentG: Double,
    limitG: Double,
    hardLimitG: Double,
    currentCostKc: Double,
    budgetKc: Double,
    privacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = if (limitG > 0) (currentG / limitG).toFloat().coerceIn(0f, 2f) else 0f
    val budgetProgress = if (budgetKc > 0) (currentCostKc / budgetKc).toFloat().coerceIn(0f, 2f) else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(700), label = "dial_prog")
    val animatedBudget by animateFloatAsState(targetValue = budgetProgress, animationSpec = tween(700), label = "dial_budget")

    val arcColor = when {
        currentG >= hardLimitG -> ColorDanger
        currentG >= limitG -> ColorWarning
        currentG >= limitG * 0.8 -> Color(0xFFFFCC00)
        else -> ColorSuccess
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("precision_dial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val startAngle = 135f
            val sweepTotal = 270f

            // 1. Draw 28 Tick Marks
            val tickCount = 28
            for (i in 0..tickCount) {
                val tickFraction = i.toFloat() / tickCount
                val angleDeg = startAngle + (tickFraction * sweepTotal)
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val is60 = i == (tickCount * 0.6f).toInt()
                val is80 = i == (tickCount * 0.8f).toInt()

                val tickLen = if (is60 || is80) radius * 0.16f else radius * 0.10f
                val tickWidth = if (is60 || is80) 2.5f else 1.2f
                val tickColor = when {
                    is80 -> ColorWarning
                    is60 -> Color(0xFFFFCC00)
                    tickFraction <= animatedProgress -> arcColor.copy(alpha = 0.8f)
                    else -> Color(0x33FFFFFF)
                }

                val pStart = Offset(
                    (center.x + (radius - tickLen) * cos(angleRad)).toFloat(),
                    (center.y + (radius - tickLen) * sin(angleRad)).toFloat()
                )
                val pEnd = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )
                drawLine(tickColor, pStart, pEnd, strokeWidth = tickWidth, cap = StrokeCap.Round)
            }

            // 2. Background Arc
            val arcRadius = radius * 0.76f
            val arcSize = Size(arcRadius * 2, arcRadius * 2)
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

            drawArc(
                color = Color(0x1AFFFFFF),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )

            // 3. Progress Arc
            val sweepG = (animatedProgress.coerceIn(0f, 1f) * sweepTotal)
            if (sweepG > 0) {
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = sweepG,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
            }

            // Over-limit indicator (second lap glow)
            if (animatedProgress > 1f) {
                val overSweep = ((animatedProgress - 1f).coerceIn(0f, 1f) * sweepTotal)
                drawArc(
                    color = ColorDanger,
                    startAngle = startAngle,
                    sweepAngle = overSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
            }

            // 4. Inner Cyan Budget Arc
            val budgetRadius = radius * 0.58f
            val budgetSize = Size(budgetRadius * 2, budgetRadius * 2)
            val budgetTopLeft = Offset(center.x - budgetRadius, center.y - budgetRadius)

            drawArc(
                color = Color(0x1464D2FF),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = budgetTopLeft,
                size = budgetSize,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            val sweepB = (animatedBudget.coerceIn(0f, 1f) * sweepTotal)
            if (sweepB > 0) {
                drawArc(
                    color = ColorWater,
                    startAngle = startAngle,
                    sweepAngle = sweepB,
                    useCenter = false,
                    topLeft = budgetTopLeft,
                    size = budgetSize,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            // 5. Needle pointer glowing dot
            val needleFraction = animatedProgress.coerceIn(0f, 1f)
            val needleAngleRad = Math.toRadians((startAngle + needleFraction * sweepTotal).toDouble())
            val dotCenter = Offset(
                (center.x + arcRadius * cos(needleAngleRad)).toFloat(),
                (center.y + arcRadius * sin(needleAngleRad)).toFloat()
            )
            drawCircle(arcColor.copy(alpha = 0.35f), radius = 10f, center = dotCenter)
            drawCircle(Color.White, radius = 4f, center = dotCenter)
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textG = if (privacyMode) "••• g" else "${r1(currentG)} g"
            Text(
                text = textG,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            val subText = if (privacyMode) "limit •• g" else "z ${r1(limitG)} g"
            Text(
                text = subText,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            val (badgeText, badgeBg, badgeFg) = when {
                currentG >= hardLimitG -> Triple("STOP +${r1(currentG - hardLimitG)}g", ColorDanger.copy(alpha = 0.25f), ColorDanger)
                currentG >= limitG -> Triple("+${r1(currentG - limitG)}g PŘES", ColorWarning.copy(alpha = 0.25f), ColorWarning)
                else -> Triple("zbývá ${r1(max(0.0, limitG - currentG))}g", ColorSuccess.copy(alpha = 0.15f), ColorSuccess)
            }
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(12.dp))
                    .border(1.dp, badgeFg.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeFg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- 2. CONCENTRIC RINGS (Soustředné kruhy) ---
@Composable
fun ConcentricRings(
    currentG: Double,
    limitG: Double,
    currentCostKc: Double,
    budgetKc: Double,
    privacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progG = if (limitG > 0) (currentG / limitG).toFloat() else 0f
    val progB = if (budgetKc > 0) (currentCostKc / budgetKc).toFloat() else 0f

    val animG by animateFloatAsState(targetValue = progG, animationSpec = tween(700), label = "conc_g")
    val animB by animateFloatAsState(targetValue = progB, animationSpec = tween(700), label = "conc_b")

    val outerColor = if (currentG > limitG) ColorDanger else ColorSuccess

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("concentric_rings"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f * 0.9f
            val innerRadius = outerRadius * 0.72f

            // Outer Gram Ring
            drawCircle(Color(0x1AFFFFFF), radius = outerRadius, center = center, style = Stroke(width = 12f))
            drawArc(
                color = outerColor,
                startAngle = -90f,
                sweepAngle = animG.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            if (animG > 1f) {
                drawArc(
                    color = ColorDanger,
                    startAngle = -90f,
                    sweepAngle = (animG - 1f).coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 15f, cap = StrokeCap.Round)
                )
            }

            // Inner Budget Ring
            drawCircle(Color(0x1464D2FF), radius = innerRadius, center = center, style = Stroke(width = 8f))
            drawArc(
                color = ColorWater,
                startAngle = -90f,
                sweepAngle = animB.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (privacyMode) "••• g" else "${r1(currentG)} g",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (privacyMode) "••• Kč" else "${currentCostKc.toInt()} Kč",
                color = ColorWater,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "z ${r1(limitG)}g / ${budgetKc.toInt()}Kč",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

// --- 3. SPARKLINE CHART (Mikrograf hladiny MIT na ±6h) ---
@Composable
fun SparklineChart(
    doses: List<Dose>,
    modifier: Modifier = Modifier,
    tMaxH: Double = 1.2,
    alphaH: Double = 3.8,
    betaH: Double = 24.0
) {
    val now = System.currentTimeMillis()
    val windowStart = now - (6 * 3600000L)
    val windowEnd = now + (6 * 3600000L)
    val totalMs = (windowEnd - windowStart).toDouble()

    // Sample 25 points
    val samples = 25
    val values = ArrayList<Pair<Float, Float>>() // xNorm, val
    var maxVal = 1.0

    for (i in 0 until samples) {
        val t = windowStart + (i.toDouble() / (samples - 1) * totalMs).toLong()
        val mit = calcActiveMitAtTime(doses, t, tMaxH, alphaH, betaH)
        if (mit > maxVal) maxVal = mit
        values.add((i.toFloat() / (samples - 1)) to mit.toFloat())
    }

    val nowFraction = ((now - windowStart) / totalMs).toFloat().coerceIn(0f, 1f)
    val nowMit = calcActiveMitAtTime(doses, now, tMaxH, alphaH, betaH).toFloat()

    Canvas(modifier = modifier.height(42.dp).fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
        if (values.size < 2) return@Canvas
        val w = size.width
        val h = size.height

        val path = Path()
        val fillPath = Path()

        val firstPt = Offset(values[0].first * w, h - (values[0].second / maxVal.toFloat()) * (h - 6f) - 3f)
        path.moveTo(firstPt.x, firstPt.y)
        fillPath.moveTo(firstPt.x, h)
        fillPath.lineTo(firstPt.x, firstPt.y)

        for (i in 1 until values.size) {
            val pt = Offset(values[i].first * w, h - (values[i].second / maxVal.toFloat()) * (h - 6f) - 3f)
            path.lineTo(pt.x, pt.y)
            fillPath.lineTo(pt.x, pt.y)
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(ColorWater.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = path,
            color = ColorWater,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
        )

        // Dotted "Now" line
        val nowX = nowFraction * w
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(nowX, 0f),
            end = Offset(nowX, h),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // Now Dot
        val nowY = h - (nowMit / maxVal.toFloat()) * (h - 6f) - 3f
        drawCircle(ColorWater.copy(alpha = 0.4f), radius = 6f, center = Offset(nowX, nowY))
        drawCircle(Color.White, radius = 2.5f, center = Offset(nowX, nowY))
    }
}

// --- 4. BEZIER CONSUMPTION CHART (Vyhlazená křivka spotřeby) ---
@Composable
fun BezierConsumptionChart(
    dailyData: List<Pair<String, Double>>, // DayKey to Grams
    limitG: Double,
    hardLimitG: Double,
    privacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty()) {
        Box(
            modifier = modifier.height(180.dp).fillMaxWidth().background(CardSurface2, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Zatím žádná data za zvolené období", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    val maxVal = max(hardLimitG * 1.15, (dailyData.maxOfOrNull { it.second } ?: 10.0) * 1.15)
    val avgG = dailyData.map { it.second }.average()

    // Calculate 7-day Moving Average (MA)
    val maValues = ArrayList<Double>()
    for (i in dailyData.indices) {
        val window = dailyData.subList(max(0, i - 6), i + 1)
        maValues.add(window.map { it.second }.average())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Vývoj denní spotřeby", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = if (privacyMode) "Průměr: •• g/den" else "Průměr: ${r1(avgG)} g/den · 7D MA",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("Křivka", MaterialTheme.colorScheme.primary)
                LegendPill("7D MA", Color(0xFFFFCC00))
                LegendPill("STOP", ColorDanger)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            val n = dailyData.size
            if (n < 2) return@Canvas

            fun yForVal(v: Double): Float = (h - (v / maxVal) * (h - 20f) - 10f).toFloat().coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // Reference Lines
            // STOP Line
            val stopY = yForVal(hardLimitG)
            drawLine(ColorDanger.copy(alpha = 0.5f), Offset(0f, stopY), Offset(w, stopY), 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))

            // LIMIT Line
            val limitY = yForVal(limitG)
            drawLine(ColorWarning.copy(alpha = 0.5f), Offset(0f, limitY), Offset(w, limitY), 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))

            // AVG Line
            val avgY = yForVal(avgG)
            drawLine(ColorSuccess.copy(alpha = 0.4f), Offset(0f, avgY), Offset(w, avgY), 1f)

            // Bezier Path for Consumption
            val path = Path()
            val fillPath = Path()

            val p0 = Offset(xForIdx(0), yForVal(dailyData[0].second))
            path.moveTo(p0.x, p0.y)
            fillPath.moveTo(p0.x, h)
            fillPath.lineTo(p0.x, p0.y)

            for (i in 1 until n) {
                val prev = Offset(xForIdx(i - 1), yForVal(dailyData[i - 1].second))
                val curr = Offset(xForIdx(i), yForVal(dailyData[i].second))
                val midX = (prev.x + curr.x) / 2f
                path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            // Fill gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ColorSuccess.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            // Draw Bezier Line
            drawPath(
                path = path,
                color = ColorSuccess,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // Draw 7-Day MA Dotted Line
            val maPath = Path()
            val ma0 = Offset(xForIdx(0), yForVal(maValues[0]))
            maPath.moveTo(ma0.x, ma0.y)
            for (i in 1 until n) {
                val p = Offset(xForIdx(i), yForVal(maValues[i]))
                maPath.lineTo(p.x, p.y)
            }
            drawPath(
                path = maPath,
                color = Color(0xFFFFCC00),
                style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            )

            // Draw Data Dots & Peak
            val maxPt = dailyData.maxByOrNull { it.second }
            for (i in 0 until n) {
                val pt = Offset(xForIdx(i), yForVal(dailyData[i].second))
                if (dailyData[i].second >= hardLimitG) {
                    drawCircle(ColorDanger, radius = 4f, center = pt)
                } else if (dailyData[i] == maxPt && dailyData[i].second > 0) {
                    drawCircle(Color.White, radius = 5f, center = pt)
                    drawCircle(ColorSuccess, radius = 3f, center = pt)
                }
            }
        }
    }
}

// --- 5. 28-DENNÍ KALENDÁŘNÍ HEATMAPA ---
@Composable
fun Heatmap28Days(
    doses: List<Dose>,
    dailyLimitG: Double,
    selectedDayKey: String?,
    onSelectDay: (String?) -> Unit,
    dayStartHour: Int = 0,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val dailyMap = mutableMapOf<String, Double>()
    for (d in doses) {
        val dk = dayKey(d.timestamp, dayStartHour)
        dailyMap[dk] = (dailyMap[dk] ?: 0.0) + d.amountG
    }

    // 28 days back to today
    val daysList = (0..27).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, dayStartHour)
        val g = dailyMap[dk] ?: 0.0
        Triple(dk, g, ts)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("28denní aktivita", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (selectedDayKey != null) {
                Text(
                    text = "Zrušit filtr dne",
                    color = ColorWater,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSelectDay(null) }
                )
            } else {
                Text("Klepnutím filtruj", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid 14 cols x 2 rows
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0..13) {
                        val index = row * 14 + col
                        if (index < daysList.size) {
                            val (dk, g, _) = daysList[index]
                            val isSelected = dk == selectedDayKey
                            val ratio = if (dailyLimitG > 0) g / dailyLimitG else 0.0

                            val cellColor = when {
                                g == 0.0 -> Color(0xFF161619)
                                ratio > 1.0 -> ColorWarning
                                ratio >= 0.75 -> Color(0xFF30D158)
                                ratio >= 0.50 -> Color(0xFF28A745)
                                ratio >= 0.25 -> Color(0xFF1E7E34)
                                else -> Color(0xFF155724)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .padding(horizontal = 1.dp)
                                    .background(cellColor, RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) Color.White else Color(0x1AFFFFFF),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        if (isSelected) onSelectDay(null) else onSelectDay(dk)
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Před 28 dny", color = TextMuted, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Méně", color = TextMuted, fontSize = 9.sp)
                Box(Modifier.size(8.dp).background(Color(0xFF161619), RoundedCornerShape(2.dp)))
                Box(Modifier.size(8.dp).background(Color(0xFF1E7E34), RoundedCornerShape(2.dp)))
                Box(Modifier.size(8.dp).background(Color(0xFF30D158), RoundedCornerShape(2.dp)))
                Box(Modifier.size(8.dp).background(ColorWarning, RoundedCornerShape(2.dp)))
                Text("Více / Přes", color = TextMuted, fontSize = 9.sp)
            }
            Text("Dnes", color = TextMuted, fontSize = 10.sp)
        }
    }
}

// --- 6. DONUT CHART (Prstencový graf druhů) ---
@Composable
fun DonutChart(
    strainShares: List<Triple<String, Double, String>>, // Name, Grams, ColorHex
    modifier: Modifier = Modifier
) {
    val totalG = strainShares.sumOf { it.second }
    if (totalG <= 0.0) return

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            var startA = -90f

            for ((_, g, colorHex) in strainShares) {
                val sweep = ((g / totalG) * 360.0).toFloat()
                val color = parseHexColor(colorHex, ColorSuccess)
                drawArc(
                    color = color,
                    startAngle = startA,
                    sweepAngle = sweep - 1.5f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                )
                startA += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${r1(totalG)}g", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${strainShares.size} druhů", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

// --- 7. STACKED FINANCIAL BAR CHART ---
@Composable
fun StackedFinanceBarChart(
    days: List<Pair<String, Pair<Double, Double>>>, // DayKey -> (KratomCost, BeverageCost)
    dailyBudgetKc: Double,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val maxSpend = max(dailyBudgetKc * 1.2, days.maxOfOrNull { it.second.first + it.second.second } ?: 150.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Finanční útrata v čase", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("Kratom", ColorWarning)
                LegendPill("Nápoje", ColorWater)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = days.size
            val barW = (w / n) * 0.7f
            val spacing = (w / n) * 0.3f

            // Budget Line
            val budgetY = (h - (dailyBudgetKc / maxSpend) * (h - 15f) - 5f).toFloat()
            drawLine(Color(0x55FFFFFF), Offset(0f, budgetY), Offset(w, budgetY), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

            for (i in 0 until n) {
                val (kCost, bCost) = days[i].second
                val total = kCost + bCost
                val x = i * (barW + spacing) + spacing / 2f

                val kH = ((kCost / maxSpend) * (h - 15f)).toFloat()
                val bH = ((bCost / maxSpend) * (h - 15f)).toFloat()

                val kratomColor = if (total > dailyBudgetKc) ColorDanger else ColorWarning

                // Kratom bottom bar
                if (kH > 0) {
                    drawRoundRect(
                        color = kratomColor,
                        topLeft = Offset(x, h - kH),
                        size = Size(barW, kH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
                // Beverage top bar
                if (bH > 0) {
                    drawRoundRect(
                        color = ColorWater,
                        topLeft = Offset(x, h - kH - bH),
                        size = Size(barW, bH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}

// --- 8. CIRKADIÁNNÍ 24H GRAF ---
@Composable
fun CircadianHourlyChart(
    hourlyDosesCount: IntArray, // 24 values
    modifier: Modifier = Modifier
) {
    val maxCount = max(1, hourlyDosesCount.maxOrNull() ?: 1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("24h cirkadiánní rozložení", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("🌙 Fialově = noční zóna", color = ColorNight, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
            val w = size.width
            val h = size.height
            val colW = w / 24f

            // Night background zones: 0..5 and 22..23
            drawRect(ColorNight.copy(alpha = 0.12f), Offset(0f, 0f), Size(colW * 6, h))
            drawRect(ColorNight.copy(alpha = 0.12f), Offset(colW * 22, 0f), Size(colW * 2, h))

            for (hour in 0..23) {
                val count = hourlyDosesCount[hour]
                if (count > 0) {
                    val barH = (count.toFloat() / maxCount) * (h - 16f)
                    val isNight = hour in 22..23 || hour in 0..5
                    val color = if (isNight) ColorNight else ColorSuccess

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(hour * colW + colW * 0.15f, h - barH),
                        size = Size(colW * 0.7f, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00", color = TextMuted, fontSize = 9.sp)
            Text("06:00", color = TextMuted, fontSize = 9.sp)
            Text("12:00", color = TextMuted, fontSize = 9.sp)
            Text("18:00", color = TextMuted, fontSize = 9.sp)
            Text("23:59", color = TextMuted, fontSize = 9.sp)
        }
    }
}

// --- 9. TÝDENNÍ ČASOVÁ MATICE DÁVEK (7x24h) ---
@Composable
fun CircadianMatrix7x24(
    matrix: Array<IntArray>, // 7 days (0..6) x 24 hours
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    var maxVal = 1
    for (d in 0..6) {
        for (h in 0..23) {
            if (matrix[d][h] > maxVal) maxVal = matrix[d][h]
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Týdenní matice dávek (7 × 24h)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Kdy nejčastěji užíváte podle dnů v týdnu", color = TextSecondary, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (d in 0..6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayLabels[d],
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.width(22.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (h in 0..23) {
                            val c = matrix[d][h]
                            val alpha = if (c > 0) (0.2f + 0.8f * (c.toFloat() / maxVal)).coerceIn(0.2f, 1f) else 0.05f
                            val color = if (c > 0) ColorSuccess.copy(alpha = alpha) else Color(0x0FFFFFFF)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper legend pill
@Composable
fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

// =========================================================================
// NOVÉ POKROČILÉ ANALYTICKÉ GRAFY PRO VŠECH 5 PODZÁLOŽEK
// =========================================================================

// --- 10. EXPOZICE MITRAGYNINU V ČASE (BÉZIER VS PRAHOVÁ HODNOTY 1.5 mg/kg) ---
@Composable
fun MitExposureChart(
    points: List<com.example.util.DailyMitPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val maxMit = max(150.0, max((points.maxOfOrNull { it.mitMg } ?: 50.0) * 1.15, (points.firstOrNull()?.thresholdMg ?: 100.0) * 1.15))
    val total7Oh = r2(points.sumOf { it.sevenOhMg })
    val threshold = points.firstOrNull()?.thresholdMg ?: 112.5

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Expozice Mitragyninu v těle", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val peakMit = points.maxOfOrNull { it.mitMg } ?: 0.0
                Text("Peak: ${r1(peakMit)} mg · 7-OH: $total7Oh mg · práh 1.5 mg/kg", color = TextSecondary, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("Denní MIT", ColorWater)
                LegendPill("1.5 mg/kg", Color(0xFF64D2FF).copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
            val w = size.width
            val h = size.height
            val n = points.size
            if (n < 2) return@Canvas

            fun yForVal(v: Double): Float = (h - (v / maxMit) * (h - 18f) - 8f).toFloat().coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // Background subtle grid lines
            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val gy = h * fraction
                drawLine(Color(0x12FFFFFF), Offset(0f, gy), Offset(w, gy), 0.8f)
            }

            // Threshold 1.5 mg/kg line
            val threshY = yForVal(threshold)
            drawLine(
                color = ColorWater.copy(alpha = 0.5f),
                start = Offset(0f, threshY),
                end = Offset(w, threshY),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            val path = Path()
            val fillPath = Path()

            val p0 = Offset(xForIdx(0), yForVal(points[0].mitMg))
            path.moveTo(p0.x, p0.y)
            fillPath.moveTo(p0.x, h)
            fillPath.lineTo(p0.x, p0.y)

            for (i in 1 until n) {
                val prev = Offset(xForIdx(i - 1), yForVal(points[i - 1].mitMg))
                val curr = Offset(xForIdx(i), yForVal(points[i].mitMg))
                val midX = (prev.x + curr.x) / 2f
                path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ColorWater.copy(alpha = 0.3f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = ColorWater,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )

            // Over-threshold warning dots
            for (i in 0 until n) {
                if (points[i].mitMg >= threshold) {
                    val pt = Offset(xForIdx(i), yForVal(points[i].mitMg))
                    drawCircle(ColorDanger, radius = 3.5f, center = pt)
                }
            }
        }
    }
}

// --- 11. KŘIVKA VÝVOJE CITLIVOSTI RECEPTORŮ (SE 3 PÁSMY) ---
@Composable
fun ReceptorTrendChart(
    points: List<com.example.util.ReceptorTrendPoint>,
    cleanDaysNeeded: Int,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val currentScore = points.lastOrNull()?.score ?: 100

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Vývoj citlivosti receptorů", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = if (cleanDaysNeeded > 0) "Potřeba $cleanDaysNeeded čistých dní k návratu na 90 %" else "Receptory jsou na optimální citlivosti ($currentScore %)",
                    color = if (cleanDaysNeeded > 0) ColorWarning else ColorSuccess,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(if (currentScore >= 75) ColorSuccess.copy(alpha = 0.2f) else ColorWarning.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("$currentScore %", color = if (currentScore >= 75) ColorSuccess else ColorWarning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = points.size
            if (n < 2) return@Canvas

            // 3 background zones:
            // 80-100% (Green zone top 20%)
            drawRect(ColorSuccess.copy(alpha = 0.08f), Offset(0f, 0f), Size(w, h * 0.22f))
            // 50-80% (Orange zone mid 30%)
            drawRect(ColorWarning.copy(alpha = 0.08f), Offset(0f, h * 0.22f), Size(w, h * 0.33f))
            // 0-50% (Red zone bottom 50%)
            drawRect(ColorDanger.copy(alpha = 0.08f), Offset(0f, h * 0.55f), Size(w, h * 0.45f))

            fun yForScore(s: Int): Float = (h - (s / 100f) * (h - 16f) - 8f).coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            val path = Path()
            path.moveTo(xForIdx(0), yForScore(points[0].score))

            for (i in 1 until n) {
                val prev = Offset(xForIdx(i - 1), yForScore(points[i - 1].score))
                val curr = Offset(xForIdx(i), yForScore(points[i].score))
                val midX = (prev.x + curr.x) / 2f
                path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }

            drawPath(
                path = path,
                color = if (currentScore >= 75) ColorSuccess else ColorWarning,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )

            // Current dot
            val lastPt = Offset(xForIdx(n - 1), yForScore(currentScore))
            drawCircle(Color.White, radius = 4f, center = lastPt)
            drawCircle(if (currentScore >= 75) ColorSuccess else ColorWarning, radius = 2.5f, center = lastPt)
        }
    }
}

// --- 12. KONTINUÁLNÍ FARMAKOKINETICKÁ KŘIVKA HLADINY MIT ---
@Composable
fun ContinuousMitCurveChart(
    timeline: List<com.example.util.ContinuousMitPoint>,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty()) return
    val maxMit = max(40.0, (timeline.maxOfOrNull { it.mitMg } ?: 20.0) * 1.2)
    val curMit = timeline.lastOrNull()?.mitMg ?: 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Kontinuální hladina Mitragyninu v krvi", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val peakVal = timeline.maxOfOrNull { it.mitMg } ?: 0.0
                Text("Peak: ${r1(peakVal)} mg · simulace absorpce a 2 poločasů", color = TextSecondary, fontSize = 11.sp)
            }
            Text("${r1(curMit)} mg teď", color = ColorWater, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = timeline.size
            if (n < 2) return@Canvas

            fun yForVal(v: Double): Float = (h - (v / maxMit) * (h - 16f) - 8f).toFloat().coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // Background subtle grid lines
            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val gy = h * fraction
                drawLine(Color(0x12FFFFFF), Offset(0f, gy), Offset(w, gy), 0.8f)
            }

            // High saturation line (60 mg)
            if (maxMit >= 60.0) {
                val satY = yForVal(60.0)
                drawLine(ColorDanger.copy(alpha = 0.4f), Offset(0f, satY), Offset(w, satY), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            }

            val path = Path()
            val fillPath = Path()

            path.moveTo(xForIdx(0), yForVal(timeline[0].mitMg))
            fillPath.moveTo(xForIdx(0), h)
            fillPath.lineTo(xForIdx(0), yForVal(timeline[0].mitMg))

            for (i in 1 until n) {
                val pt = Offset(xForIdx(i), yForVal(timeline[i].mitMg))
                path.lineTo(pt.x, pt.y)
                fillPath.lineTo(pt.x, pt.y)
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ColorWater.copy(alpha = 0.3f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = ColorWater,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
    }
}

// --- 13. DENNÍ DISCIPLÍNA ROZESTUPŮ (COMPLIANCE RATE SLOUPOVÝ GRAF) ---
@Composable
fun IntervalComplianceBarChart(
    complianceList: List<com.example.util.DailyIntervalCompliance>,
    modifier: Modifier = Modifier
) {
    if (complianceList.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Denní disciplína rozestupů", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("≥80%", ColorSuccess)
                LegendPill("50-79%", ColorWarning)
                LegendPill("<50%", ColorDanger)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val w = size.width
            val h = size.height
            val n = complianceList.size
            val barW = (w / n) * 0.7f
            val spacing = (w / n) * 0.3f

            for (i in 0 until n) {
                val item = complianceList[i]
                val x = i * (barW + spacing) + spacing / 2f
                val pct = item.compliancePct
                val barH = ((pct / 100f) * (h - 20f)).coerceAtLeast(4f)

                val color = when {
                    pct >= 80 -> ColorSuccess
                    pct >= 50 -> ColorWarning
                    else -> ColorDanger
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, h - barH),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // If daily min interval is dangerously low (< 2h), draw red dot marker above bar
                if (item.totalIntervals > 0 && item.minIntervalH in 0.1..2.0) {
                    drawCircle(ColorDanger, radius = 3f, center = Offset(x + barW / 2f, h - barH - 6f))
                }
            }
        }
    }
}

// --- 14. EVOLUCE PODÍLU DRUHŮ V ČASE (STACKED DAILY BARS) ---
@Composable
fun StackedStrainEvolutionChart(
    stacks: List<com.example.util.DailyStrainStack>,
    strains: List<Strain>,
    modifier: Modifier = Modifier
) {
    if (stacks.isEmpty()) return
    val maxTotalG = max(10.0, (stacks.maxOfOrNull { it.totalG } ?: 10.0) * 1.15)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Evoluce podílu druhů v čase", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Denní skladba spotřebovaných odrůd", color = TextSecondary, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = stacks.size
            val barW = (w / n) * 0.72f
            val spacing = (w / n) * 0.28f

            for (i in 0 until n) {
                val stack = stacks[i]
                val x = i * (barW + spacing) + spacing / 2f
                var curY = h

                stack.strainShares.forEach { (strainId, amount) ->
                    if (amount > 0) {
                        val str = strains.find { it.id == strainId }
                        val col = parseHexColor(str?.color ?: "#30D158", ColorSuccess)
                        val segmentH = ((amount / maxTotalG) * (h - 16f)).toFloat()

                        drawRoundRect(
                            color = col,
                            topLeft = Offset(x, curY - segmentH),
                            size = Size(barW, segmentH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                        curY -= segmentH
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (str in strains) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(parseHexColor(str.color, ColorSuccess), CircleShape))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(str.name, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// --- 15. KARTA ÚČINKŮ A SPOKOJENOSTI DLE ODRŮD ---
@Composable
fun StrainEffectsCard(
    stats: List<com.example.util.StrainEffectStat>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Účinky a spokojenost dle odrůd", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Subjektivní profily efektů a průměrné hodnocení", color = TextSecondary, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (s in stats) {
                if (s.dosesCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface2),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(9.dp).background(parseHexColor(s.colorHex, ColorSuccess), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.strainName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(${s.dosesCount}x · ${s.totalGrams}g)", color = TextMuted, fontSize = 10.sp)
                                }
                                if (s.avgRating > 0) {
                                    Text("★ ${s.avgRating}", color = Color(0xFFFFD60A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Effect badges
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (s.energyCount > 0) EffectMiniPill("⚡ Energie", s.energyCount)
                                if (s.focusCount > 0) EffectMiniPill("🎯 Soustředění", s.focusCount)
                                if (s.relaxCount > 0) EffectMiniPill("🍃 Relax", s.relaxCount)
                                if (s.painCount > 0) EffectMiniPill("🛡️ Úleva", s.painCount)
                                if (s.sleepCount > 0) EffectMiniPill("🌙 Spánek", s.sleepCount)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectMiniPill(label: String, count: Int) {
    Box(
        modifier = Modifier
            .background(CardSurface1, RoundedCornerShape(6.dp))
            .border(0.5.dp, CardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("$label: $count", color = TextSecondary, fontSize = 10.sp)
    }
}

// --- 16. HYDRATAČNÍ BILANCE A KOMPENZACE VODY ---
@Composable
fun HydrationBalanceChart(
    balances: List<com.example.util.DailyHydrationBalance>,
    modifier: Modifier = Modifier
) {
    if (balances.isEmpty()) return
    val maxMl = max(3500.0, (balances.maxOfOrNull { max(it.actualWaterMl, it.targetWaterMl) } ?: 2500) * 1.15)
    val avgActual = balances.map { it.actualWaterMl }.average().toInt()
    val avgTarget = balances.map { it.targetWaterMl }.average().toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hydratační bilance", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Vypito Ø $avgActual ml · Cíl s kratomem Ø $avgTarget ml", color = TextSecondary, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("Vypito", ColorWater)
                LegendPill("Cíl", Color(0x88FFFFFF))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = balances.size
            if (n < 2) return@Canvas

            fun yForVal(v: Int): Float = (h - (v / maxMl) * (h - 16f) - 8f).toFloat().coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // Target dashed line
            val targetPath = Path()
            targetPath.moveTo(xForIdx(0), yForVal(balances[0].targetWaterMl))
            for (i in 1 until n) {
                targetPath.lineTo(xForIdx(i), yForVal(balances[i].targetWaterMl))
            }
            drawPath(
                path = targetPath,
                color = Color(0x66FFFFFF),
                style = Stroke(width = 1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
            )

            // Actual water curve
            val actualPath = Path()
            actualPath.moveTo(xForIdx(0), yForVal(balances[0].actualWaterMl))
            for (i in 1 until n) {
                val prev = Offset(xForIdx(i - 1), yForVal(balances[i - 1].actualWaterMl))
                val curr = Offset(xForIdx(i), yForVal(balances[i].actualWaterMl))
                val midX = (prev.x + curr.x) / 2f
                actualPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
            drawPath(
                path = actualPath,
                color = ColorWater,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )

            // Highlight days with met goal
            for (i in 0 until n) {
                val b = balances[i]
                val pt = Offset(xForIdx(i), yForVal(b.actualWaterMl))
                if (b.actualWaterMl >= b.targetWaterMl && b.actualWaterMl > 0) {
                    drawCircle(ColorSuccess, radius = 3.5f, center = pt)
                }
            }
        }
    }
}

// --- 17. SIMULÁTOR TOLERANCE PAUZY (T-BREAK PROJECTION CURVE) ---
@Composable
fun TBreakRecoveryProjectionChart(
    points: List<com.example.util.TBreakProjectionPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Simulátor regenerace receptorů při pauze", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Jak poroste citlivost při čisté T-Break pauze od dneška", color = TextSecondary, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val w = size.width
            val h = size.height
            val n = points.size
            if (n < 2) return@Canvas

            fun yForScore(s: Int): Float = (h - (s / 100f) * (h - 20f) - 10f).coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // 90% optimal threshold line
            val optY = yForScore(90)
            drawLine(ColorSuccess.copy(alpha = 0.4f), Offset(0f, optY), Offset(w, optY), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

            val path = Path()
            val fillPath = Path()

            path.moveTo(xForIdx(0), yForScore(points[0].projectedScore))
            fillPath.moveTo(xForIdx(0), h)
            fillPath.lineTo(xForIdx(0), yForScore(points[0].projectedScore))

            for (i in 1 until n) {
                val pt = Offset(xForIdx(i), yForScore(points[i].projectedScore))
                path.lineTo(pt.x, pt.y)
                fillPath.lineTo(pt.x, pt.y)
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ColorNight.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = ColorNight,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )

            // Milestone dots
            for (i in 0 until n) {
                val pt = Offset(xForIdx(i), yForScore(points[i].projectedScore))
                drawCircle(Color.White, radius = 3.5f, center = pt)
                drawCircle(ColorNight, radius = 2f, center = pt)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (p in points) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.label, color = TextMuted, fontSize = 9.sp)
                    Text("${p.projectedScore}%", color = ColorNight, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

// --- 18. TAPERING TRAJEKTORIE (PLÁN VS SKUTEČNOST) ---
@Composable
fun TaperingTrajectoryChart(
    trajectoryResult: com.example.util.TaperingResult,
    modifier: Modifier = Modifier
) {
    val points = trajectoryResult.points
    if (points.isEmpty()) return

    val maxG = max(10.0, max(points.maxOfOrNull { it.plannedG } ?: 10.0, points.maxOfOrNull { it.actualG } ?: 10.0) * 1.15)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Trajektorie Taperingu (Plán vs Realita)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Ušetřeno: ${trajectoryResult.savedGrams}g (${trajectoryResult.savedMoneyKc.toInt()} Kč)", color = ColorSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("Plán", ColorWater)
                LegendPill("Skutečnost", ColorWarning)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = points.size
            if (n < 2) return@Canvas

            fun yForVal(v: Double): Float = (h - (v / maxG) * (h - 16f) - 8f).toFloat().coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // Planned dashed curve
            val plannedPath = Path()
            plannedPath.moveTo(xForIdx(0), yForVal(points[0].plannedG))
            for (i in 1 until n) {
                plannedPath.lineTo(xForIdx(i), yForVal(points[i].plannedG))
            }
            drawPath(
                path = plannedPath,
                color = ColorWater,
                style = Stroke(width = 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
            )

            // Actual consumption curve
            val actualPath = Path()
            actualPath.moveTo(xForIdx(0), yForVal(points[0].actualG))
            for (i in 1 until n) {
                val prev = Offset(xForIdx(i - 1), yForVal(points[i - 1].actualG))
                val curr = Offset(xForIdx(i), yForVal(points[i].actualG))
                val midX = (prev.x + curr.x) / 2f
                actualPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
            drawPath(
                path = actualPath,
                color = ColorWarning,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )
        }
    }
}

// --- 19. SOUSTŘEDNÉ PRSTENCE DISCIPLÍNY (MULTI-RING GAUGE) ---
@Composable
fun DisciplineMultiRingChart(
    data: com.example.util.DisciplineRingData,
    modifier: Modifier = Modifier
) {
    val progLimit = (data.limitStreak.toFloat() / data.limitGoal).coerceIn(0f, 1f)
    val progTBreak = (data.completedTBreaks.toFloat() / data.tbreakGoal).coerceIn(0f, 1f)
    val progWater = (data.waterStreak.toFloat() / data.waterGoal).coerceIn(0f, 1f)
    val progInterval = (data.intervalCompliancePct / 100f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Radarové kruhy disciplíny", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Postup ve 4 hlavních pilířích", color = TextSecondary, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r4 = size.minDimension / 2f * 0.95f
                    val r3 = r4 * 0.78f
                    val r2 = r4 * 0.58f
                    val r1 = r4 * 0.38f

                    fun drawRing(r: Float, prog: Float, col: Color, stroke: Float) {
                        drawCircle(col.copy(alpha = 0.15f), radius = r, center = center, style = Stroke(stroke))
                        drawArc(
                            color = col,
                            startAngle = -90f,
                            sweepAngle = prog * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }

                    drawRing(r4, progLimit, ColorWarning, 7f)
                    drawRing(r3, progTBreak, ColorNight, 7f)
                    drawRing(r2, progWater, ColorWater, 7f)
                    drawRing(r1, progInterval, ColorSuccess, 7f)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RingLegendItem("V limitu", "${data.limitStreak}/${data.limitGoal}d", ColorWarning)
                RingLegendItem("T-Break pauzy", "${data.completedTBreaks}/${data.tbreakGoal}", ColorNight)
                RingLegendItem("Hydratace", "${data.waterStreak}/${data.waterGoal}d", ColorWater)
                RingLegendItem("Rozestupy", "${data.intervalCompliancePct}%", ColorSuccess)
            }
        }
    }
}

@Composable
private fun RingLegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

// --- 20. SPOJNICIVÝ GRAF VÝVOJE PRŮMĚRNÉHO A MINIMÁLNÍHO ROZESTUPU ---
@Composable
fun IntervalTrendChart(
    complianceList: List<com.example.util.DailyIntervalCompliance>,
    targetThresholdH: Double = 4.0,
    modifier: Modifier = Modifier
) {
    if (complianceList.isEmpty()) return
    val maxH = max(targetThresholdH * 1.5, (complianceList.maxOfOrNull { max(it.avgIntervalH, it.minIntervalH) } ?: targetThresholdH) * 1.2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Vývoj denních rozestupů", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val allAvg = complianceList.filter { it.totalIntervals > 0 }.map { it.avgIntervalH }
                val overallAvg = if (allAvg.isNotEmpty()) r1(allAvg.average()) else 0.0
                val allMin = complianceList.filter { it.totalIntervals > 0 && it.minIntervalH > 0 }.map { it.minIntervalH }
                val overallMin = if (allMin.isNotEmpty()) r1(allMin.minOrNull() ?: 0.0) else 0.0
                Text("Ø $overallAvg h · Nejkratší: $overallMin h · cíl: ≥ ${targetThresholdH.toInt()} h", color = TextSecondary, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendPill("Průměr", ColorWater)
                LegendPill("Minima", ColorDanger)
                LegendPill("Cíl", Color(0x66FFFFFF))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val n = complianceList.size
            if (n < 2) return@Canvas

            fun yForH(valH: Double): Float = (h - (valH / maxH) * (h - 20f) - 10f).toFloat().coerceIn(0f, h)
            fun xForIdx(i: Int): Float = (i.toFloat() / (n - 1)) * w

            // Background subtle grid lines
            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val gy = h * fraction
                drawLine(Color(0x12FFFFFF), Offset(0f, gy), Offset(w, gy), 0.8f)
            }

            // Target threshold dashed line
            val targetY = yForH(targetThresholdH)
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(0f, targetY),
                end = Offset(w, targetY),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Average interval curve
            val avgPath = Path()
            avgPath.moveTo(xForIdx(0), yForH(complianceList[0].avgIntervalH))
            for (i in 1 until n) {
                val prev = Offset(xForIdx(i - 1), yForH(complianceList[i - 1].avgIntervalH))
                val curr = Offset(xForIdx(i), yForH(complianceList[i].avgIntervalH))
                val midX = (prev.x + curr.x) / 2f
                avgPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }

            drawPath(
                path = avgPath,
                color = ColorWater,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )

            // Min interval points & warning dots
            for (i in 0 until n) {
                val item = complianceList[i]
                if (item.totalIntervals > 0) {
                    val ptMin = Offset(xForIdx(i), yForH(item.minIntervalH))
                    val isDanger = item.minIntervalH < 2.5
                    val dotColor = if (isDanger) ColorDanger else ColorWarning
                    drawCircle(dotColor, radius = if (isDanger) 4.5f else 3f, center = ptMin)
                }
            }
        }
    }
}

// --- 21. KVARTÁLNÍ CIRKADIÁNNÍ ROZPAD (4 DENNÍ KVADRANTY) ---
data class CircadianQuadrantStat(
    val icon: String,
    val label: String,
    val timeRange: String,
    val dosesCount: Int,
    val totalGrams: Double,
    val percentage: Int,
    val color: Color
)

@Composable
fun CircadianQuadrantsCard(
    doses: List<Dose>,
    modifier: Modifier = Modifier
) {
    val totalDoses = doses.size
    val totalG = doses.sumOf { it.amountG }

    val cal = java.util.Calendar.getInstance()
    var morningDoses = 0
    var morningG = 0.0
    var afternoonDoses = 0
    var afternoonG = 0.0
    var eveningDoses = 0
    var eveningG = 0.0
    var nightDoses = 0
    var nightG = 0.0

    for (d in doses) {
        cal.timeInMillis = d.timestamp
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        when (h) {
            in 6..11 -> {
                morningDoses++
                morningG += d.amountG
            }
            in 12..17 -> {
                afternoonDoses++
                afternoonG += d.amountG
            }
            in 18..21 -> {
                eveningDoses++
                eveningG += d.amountG
            }
            else -> {
                nightDoses++
                nightG += d.amountG
            }
        }
    }

    fun pct(count: Int) = if (totalDoses > 0) ((count.toDouble() / totalDoses) * 100).toInt() else 0

    val quadrants = listOf(
        CircadianQuadrantStat("🌅", "Ráno", "06:00–12:00", morningDoses, r1(morningG), pct(morningDoses), Color(0xFFFFCC00)),
        CircadianQuadrantStat("☀️", "Odpoledne", "12:00–18:00", afternoonDoses, r1(afternoonG), pct(afternoonDoses), ColorSuccess),
        CircadianQuadrantStat("🌇", "Večer", "18:00–22:00", eveningDoses, r1(eveningG), pct(eveningDoses), ColorWater),
        CircadianQuadrantStat("🌙", "Noc", "22:00–06:00", nightDoses, r1(nightG), pct(nightDoses), ColorNight)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Cirkadiánní kvadranty dne", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Časové rozvrstvení užívání ve 4 denních fázích", color = TextSecondary, fontSize = 11.sp)
            }
            if (nightDoses > 0) {
                Box(
                    modifier = Modifier
                        .background(ColorNight.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("$nightDoses nočních", color = ColorNight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal stacked proportion bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(CardSurface2, RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            for (q in quadrants) {
                if (q.percentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(q.percentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(q.color, RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Quadrants Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (q in quadrants) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardSurface2),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, q.color.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(q.icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(q.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(q.timeRange, color = TextMuted, fontSize = 8.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${q.percentage} %", color = q.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${q.dosesCount}x · ${q.totalGrams}g", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// --- 22. SHANNONŮV DIVERZITNÍ CIFERNÍK (ROTATION DIAL) ---
@Composable
fun ShannonRotationDialCard(
    diversity: com.example.util.StrainDiversityResult,
    threshold: Int = 3,
    modifier: Modifier = Modifier
) {
    val score = diversity.rotationScorePct
    val isGood = diversity.statusTier == "good"
    val arcColor = if (isGood) ColorSuccess else ColorWarning

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface1, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Shannonův diverzitní ciferník", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Střídání odrůd chrání receptory před tolerancí", color = TextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .background(arcColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(diversity.statusLabel, color = arcColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f

                    // Background track
                    drawCircle(Color(0x1AFFFFFF), radius = r, center = center, style = Stroke(9f))

                    // Arc
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = (score / 100f) * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(9f, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score %", color = arcColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("rotace", color = TextMuted, fontSize = 10.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (diversity.streak >= threshold) ColorWarning else ColorSuccess, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Série kmene: ${diversity.streak}x v řadě", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ColorWater, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shannon H': ${diversity.shannonH}", color = TextSecondary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ColorNight, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limit varování: ${threshold}x v řadě", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

// --- 23. DELTA BADGE (SROVNÁVACÍ ODCHYLKY SE ŠIPKAMI NAHORU/DOLŮ) ---
@Composable
fun DeltaBadge(
    delta: com.example.util.MetricDelta,
    label: String,
    inverseColor: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!delta.hasComparison) return

    val isZero = Math.abs(delta.diff) < 0.05
    val isUp = delta.isIncrease && !isZero
    val isDown = !delta.isIncrease && !isZero

    val badgeColor = when {
        isZero -> TextMuted
        inverseColor -> if (isDown) ColorSuccess else ColorWarning
        else -> if (isUp) ColorSuccess else ColorWarning
    }

    val arrow = when {
        isZero -> "="
        isUp -> "↑"
        else -> "↓"
    }

    Box(
        modifier = modifier
            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(0.6.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = arrow,
                color = badgeColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
            Text(
                text = "${delta.formattedPct} $label",
                color = badgeColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }
    }
}



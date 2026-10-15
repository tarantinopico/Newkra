package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppState
import com.example.model.Dose
import com.example.ui.components.ConcentricRings
import com.example.ui.components.DeltaBadge
import com.example.ui.components.PrecisionDial
import com.example.ui.components.SparklineChart
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
import com.example.util.calcDashboardDeltas
import com.example.util.calcDynamicWaterGoal
import com.example.util.calcLimitStreak
import com.example.util.calcReceptorSensitivityScore
import com.example.util.calcStrainDiversityIndex
import com.example.util.dayKey
import com.example.util.formatDurationHoursMin
import com.example.util.formatTime
import com.example.util.r1
import com.example.util.r2
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    appState: AppState,
    onOpenDoseModal: (Dose?) -> Unit,
    onQuickDose: (Double) -> Unit,
    onRepeatLastDose: () -> Unit,
    onQuickWater: (Int) -> Unit,
    onStartTBreak: () -> Unit,
    onFinishTBreak: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = appState.settings
    val doses = appState.doses
    val waterLogs = appState.waterLogs
    val packages = appState.packages
    val strains = appState.strains

    val now = System.currentTimeMillis()
    val todayKeyStr = dayKey(now, settings.dayStartHour)
    val todayDoses = doses.filter { dayKey(it.timestamp, settings.dayStartHour) == todayKeyStr }.sortedByDescending { it.timestamp }
    val todayG = todayDoses.sumOf { it.amountG }
    val todaySpendKc = todayDoses.sumOf { it.cost + it.beverageCost }
    val lastDose = doses.maxByOrNull { it.timestamp }

    // Streaks & Scores
    val limitStreak = calcLimitStreak(doses, settings)
    val receptorScore = calcReceptorSensitivityScore(doses, now, settings)
    val unlockedAchCount = appState.achievements.size
    val diversity = calcStrainDiversityIndex(todayDoses.ifEmpty { doses.takeLast(10) }, strains, settings.diversityWarningThreshold)

    // Cooldown logic
    val cooldownMs = settings.cooldownMin * 60000L
    val timeSinceLastDoseMs = if (lastDose != null) now - lastDose.timestamp else Long.MAX_VALUE
    val cooldownRemainingMs = if (cooldownMs > 0 && timeSinceLastDoseMs < cooldownMs) cooldownMs - timeSinceLastDoseMs else 0L
    val cooldownOk = cooldownRemainingMs <= 0L
    val nextWindowTs = if (lastDose != null && cooldownRemainingMs > 0) lastDose.timestamp + cooldownMs else now

    // Active MIT
    val activeMitMg = calcActiveMitAtTime(doses, now, settings.mitTMaxH, settings.mitHalfLifeAlphaH, settings.mitHalfLifeBetaH)

    // Water calculations
    val todayWaterLogs = waterLogs.filter { dayKey(it.timestamp, settings.dayStartHour) == todayKeyStr }
    val todayWaterMl = todayWaterLogs.sumOf { it.amountMl }
    val dynamicWaterGoalMl = calcDynamicWaterGoal(todayG, settings)

    // Sklad & výdrž ETA
    val totalStockG = packages.filter { it.status != "finished" }.sumOf { it.remainingWeightG }
    // 14-day average daily use
    val past14d = (0..13).map { i -> dayKey(now - i * 86400000L, settings.dayStartHour) }
    val doses14d = doses.filter { past14d.contains(dayKey(it.timestamp, settings.dayStartHour)) }
    val avgDaily14d = doses14d.sumOf { it.amountG } / 14.0
    val stockEtaDays = if (avgDaily14d > 0) (totalStockG / avgDaily14d).roundToInt() else 99

    // 7D shift (% delta today vs past 7-day avg)
    val past7d = (1..7).map { i -> dayKey(now - i * 86400000L, settings.dayStartHour) }
    val avgPast7d = doses.filter { past7d.contains(dayKey(it.timestamp, settings.dayStartHour)) }.sumOf { it.amountG } / 7.0
    val shift7dPct = if (avgPast7d > 0) (((todayG - avgPast7d) / avgPast7d) * 100).roundToInt() else 0

    // Detailed Dashboard Deltas vs Yesterday & 7D Average
    val deltas = calcDashboardDeltas(doses, waterLogs, settings, now)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. QUICK LOG ROW & HUD PILLS
        if (settings.dashShowQuickRow || settings.dashShowHud) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick log chips
                    if (settings.dashShowQuickRow) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            settings.quickDosePresets.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable { onQuickDose(p) }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                        .testTag("quick_dose_${p}")
                                ) {
                                    Text("+${r1(p)}g", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (lastDose != null) {
                                Box(
                                    modifier = Modifier
                                        .background(ColorWarning.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                        .border(1.dp, ColorWarning.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable { onRepeatLastDose() }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                        .testTag("repeat_last_dose")
                                ) {
                                    Text("⧉ ${r1(lastDose.amountG)}g", color = ColorWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (settings.enableHydration) {
                                Box(
                                    modifier = Modifier
                                        .background(ColorWater.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                        .border(1.dp, ColorWater.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable { onQuickWater(settings.waterQuickMl1) }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                        .testTag("quick_water_250")
                                ) {
                                    Text("💧+${settings.waterQuickMl1}", color = ColorWater, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // HUD Pills
                    if (settings.dashShowHud) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(CardSurface2, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Text("🔥 ${limitStreak}d", color = ColorWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (settings.enableReceptorScore) {
                                Box(
                                    modifier = Modifier
                                        .background(CardSurface2, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                ) {
                                    Text("⚡ $receptorScore%", color = if (receptorScore >= 75) ColorSuccess else ColorWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (settings.enableAchievements) {
                                Box(
                                    modifier = Modifier
                                        .background(CardSurface2, RoundedCornerShape(10.dp))
                                        .clickable { onNavigateToAchievements() }
                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                ) {
                                    Text("🏆 $unlockedAchCount/14", color = Color(0xFFFFD60A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. CHYTRÝ ASISTENT (INSIGHT BANNER)
        if (settings.dashShowInsight && settings.enableSmartInsights) {
            item {
                val (insightText, insightBg, insightFg) = when {
                    todayG >= settings.dailyHardLimitG -> Triple(
                        "🛑 STOP limit překročen! Dnes užito ${r1(todayG)} g. Doporučujeme pauzu a hydrataci.",
                        ColorDanger.copy(alpha = 0.18f),
                        ColorDanger
                    )
                    todayG >= settings.dailyLimitG * 0.8 -> Triple(
                        "⚠️ Pozor na limit: Dnes máš ${r1(todayG)} g z ${r1(settings.dailyLimitG)} g limitu.",
                        ColorWarning.copy(alpha = 0.18f),
                        ColorWarning
                    )
                    !cooldownOk -> Triple(
                        "⏳ Aktivní cooldown: Do dalšího okna zbývá ${formatDurationHoursMin(cooldownRemainingMs)}.",
                        Color(0xFFFFCC00).copy(alpha = 0.15f),
                        Color(0xFFFFCC00)
                    )
                    diversity.streak >= settings.diversityWarningThreshold -> Triple(
                        "🔄 Rotace odrůd: Máš za sebou ${diversity.streak}x stejný kmen v řadě (${diversity.dominantStrainName}).",
                        ColorWarning.copy(alpha = 0.15f),
                        ColorWarning
                    )
                    todayDoses.isEmpty() -> Triple(
                        "✨ Čistý start dne: Žádná dávka nezaznamenána. Citlivost receptorů $receptorScore %.",
                        ColorSuccess.copy(alpha = 0.12f),
                        ColorSuccess
                    )
                    else -> Triple(
                        "✅ Optimální tempo: Dnes ${todayDoses.size}x (${r1(todayG)} g). Citlivost $receptorScore %, stabilní stav.",
                        ColorSuccess.copy(alpha = 0.12f),
                        ColorSuccess
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(insightBg, RoundedCornerShape(12.dp))
                        .border(1.dp, insightFg.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(insightText, color = insightFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 3. T-BREAK CARD (Běžící nebo výzva k zahájení)
        if (settings.tbreakActive) {
            item {
                val startedAt = settings.tbreakStartedAt ?: now
                val elapsedMs = max(0L, now - startedAt)
                val goalMs = (settings.tbreakGoalHours * 3600000.0).toLong()
                val progress = (elapsedMs.toFloat() / max(1L, goalMs).toFloat()).coerceIn(0f, 1f)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface1),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorNight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aktivní T-Break pauza 🧘", color = ColorNight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Uběhlo: ${formatDurationHoursMin(elapsedMs)} · Cíl: ${settings.tbreakGoalHours.toInt()}h",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = ColorNight,
                                trackColor = CardSurface2,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onFinishTBreak,
                            colors = ButtonDefaults.buttonColors(containerColor = if (progress >= 1f) ColorSuccess else ColorNight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (progress >= 1f) "Splněno 🎉" else "Ukončit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. HERO SECTION (Dial + Telemetrie - 2 sloupce vedle sebe)
        if (settings.dashShowHero || settings.dashShowTelem) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Hero: Dial
                    if (settings.dashShowHero) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(230.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface1),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (settings.dashboardDialStyle == "concentric") {
                                        ConcentricRings(
                                            currentG = todayG,
                                            limitG = settings.dailyLimitG,
                                            currentCostKc = todaySpendKc,
                                            budgetKc = settings.dailyBudgetKc,
                                            privacyMode = settings.privacyMode
                                        )
                                    } else {
                                        PrecisionDial(
                                            currentG = todayG,
                                            limitG = settings.dailyLimitG,
                                            hardLimitG = settings.dailyHardLimitG,
                                            currentCostKc = todaySpendKc,
                                            budgetKc = settings.dailyBudgetKc,
                                            privacyMode = settings.privacyMode
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DeltaBadge(deltas.deltaGvsYesterday, "včera", inverseColor = true)
                                    DeltaBadge(deltas.deltaGvs7d, "7D Ø", inverseColor = true)
                                }
                            }
                        }
                    }

                    // Right Hero: Telemetrie (Cooldown + MIT Sparkline)
                    if (settings.dashShowTelem) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(230.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface1),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top half: Cooldown
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Cooldown", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (cooldownOk) ColorSuccess.copy(alpha = 0.2f) else ColorWarning.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(1.dp, if (cooldownOk) ColorSuccess else ColorWarning, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (cooldownOk) "Připraveno" else "Zbývá ${formatDurationHoursMin(cooldownRemainingMs)}",
                                                color = if (cooldownOk) ColorSuccess else ColorWarning,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (lastDose != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${lastDose.strainName} (${r1(lastDose.amountG)}g)",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "⏳ Další okno od ${formatTime(nextWindowTs, settings.timeFormat24)}",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Žádná předchozí dávka", color = TextMuted, fontSize = 11.sp)
                                    }
                                }

                                // Middle: Interval Delta Badge if available
                                if (deltas.deltaIntervalVs7d.hasComparison) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Rozestupy", color = TextSecondary, fontSize = 10.sp)
                                        DeltaBadge(deltas.deltaIntervalVs7d, "vs 7D Ø", inverseColor = false)
                                    }
                                }

                                // Bottom half: Active MIT & Sparkline
                                if (settings.showAlkaloids && settings.enablePharmacokinetics) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text("Aktivní MIT", color = TextSecondary, fontSize = 11.sp)
                                            Text(
                                                text = "${r1(activeMitMg)} mg",
                                                color = ColorWater,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (settings.dashboardShowSparklines) {
                                            SparklineChart(
                                                doses = doses,
                                                tMaxH = settings.mitTMaxH,
                                                alphaH = settings.mitHalfLifeAlphaH,
                                                betaH = settings.mitHalfLifeBetaH
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. SECOND PAIR (Timeline + Water Card / Receptors)
        if (settings.dashShowTimeline || settings.dashShowWater) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: 24h Timeline mini
                    if (settings.dashShowTimeline) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface1),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dnešní osa", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    val avgDose = if (todayDoses.isNotEmpty()) todayG / todayDoses.size else 0.0
                                    Text("Ø ${r1(avgDose)}g", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Mini timeline dots
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(CardSurface2, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (todayDoses.isEmpty()) {
                                        Text("Dnes zatím bez záznamu", color = TextMuted, fontSize = 10.sp)
                                    } else {
                                        todayDoses.sortedBy { it.timestamp }.forEach { d ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(parseHexColor(d.strainColor, ColorSuccess), CircleShape)
                                                )
                                                Text(formatTime(d.timestamp, settings.timeFormat24), color = TextMuted, fontSize = 8.sp)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("06:00", color = TextMuted, fontSize = 9.sp)
                                    Text("12:00", color = TextMuted, fontSize = 9.sp)
                                    Text("24:00", color = TextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Right: Water or Receptors
                    if (settings.dashShowWater) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface1),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            if (settings.enableHydration) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Hydratace", color = ColorWater, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (deltas.deltaWaterVsYesterday.hasComparison) {
                                                DeltaBadge(deltas.deltaWaterVsYesterday, "včera", inverseColor = false)
                                            }
                                            Text("$todayWaterMl / $dynamicWaterGoalMl ml", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    val waterProg = (todayWaterMl.toFloat() / max(1, dynamicWaterGoalMl).toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { waterProg },
                                        color = ColorWater,
                                        trackColor = CardSurface2,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(ColorWater.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                                .border(1.dp, ColorWater.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { onQuickWater(settings.waterQuickMl1) }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+${settings.waterQuickMl1}", color = ColorWater, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(ColorWater.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                                .border(1.dp, ColorWater.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { onQuickWater(settings.waterQuickMl2) }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+${settings.waterQuickMl2}", color = ColorWater, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // Receptors & Rotation Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Receptory & Rotace", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("$receptorScore%", color = ColorSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { receptorScore / 100f },
                                        color = ColorSuccess,
                                        trackColor = CardSurface2,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = "${diversity.statusLabel} · ${diversity.streak}x v řadě",
                                        color = if (diversity.statusTier == "good") ColorSuccess else ColorWarning,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. ACTIVITY CARD (Poslední dávky dnes)
        if (settings.dashShowActivity) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface1),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dnešní aktivita", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Zobrazit historii →",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigateToHistory() }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (todayDoses.isEmpty()) {
                            Text("Dnes ještě nebyla zapsána žádná dávka.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            val recentDoses = todayDoses.take(settings.dashRecentDosesCount)
                            recentDoses.forEachIndexed { idx, d ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenDoseModal(d) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(parseHexColor(d.strainColor, ColorSuccess), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(d.strainName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(formatTime(d.timestamp, settings.timeFormat24), color = TextMuted, fontSize = 10.sp)
                                                if (settings.enableBeverages && d.beverageId != "bev_water") {
                                                    Text("· ${d.beverageIcon} ${d.beverageName}", color = TextSecondary, fontSize = 10.sp)
                                                }
                                                if (settings.enableMoodRating && d.rating > 0) {
                                                    Text("· ★${d.rating}", color = Color(0xFFFFD60A), fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val amountStr = if (settings.privacyMode) "••• g" else "${r1(d.amountG)} g"
                                        Text(amountStr, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (settings.showFinances) {
                                            val costStr = if (settings.privacyMode) "••• Kč" else "${d.cost.toInt()} Kč"
                                            Text(costStr, color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                                if (idx < recentDoses.size - 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardSurface2))
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. BOTTOM METRICS BAR (4 dlaždice)
        if (settings.dashShowStats) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile("Skladem", "${r1(totalStockG)}g", "~$stockEtaDays dní", ColorSuccess, Modifier.weight(1f))
                    MetricTile(
                        title = "Útrata",
                        primaryVal = if (settings.showFinances) "${todaySpendKc.toInt()} Kč" else "–",
                        subVal = "z ${settings.dailyBudgetKc.toInt()} Kč",
                        accentColor = ColorWarning,
                        delta = deltas.deltaCostVsYesterday,
                        deltaLabel = "včera",
                        deltaInverse = true,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile("Dnes dávek", "${todayDoses.size}x", if (todayDoses.isNotEmpty()) "Ø ${r1(todayG / todayDoses.size)}g" else "0g", TextPrimary, Modifier.weight(1f))
                    MetricTile(
                        title = "7D Posun",
                        primaryVal = if (shift7dPct >= 0) "+$shift7dPct%" else "$shift7dPct%",
                        subVal = "vs průměr",
                        accentColor = if (shift7dPct > 20) ColorDanger else ColorSuccess,
                        delta = deltas.deltaGvs7d,
                        deltaLabel = "7D Ø",
                        deltaInverse = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun MetricTile(
    title: String,
    primaryVal: String,
    subVal: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    delta: com.example.util.MetricDelta? = null,
    deltaLabel: String = "",
    deltaInverse: Boolean = false
) {
    Box(
        modifier = modifier
            .background(CardSurface1, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(primaryVal, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (delta != null && delta.hasComparison) {
                Spacer(modifier = Modifier.height(3.dp))
                DeltaBadge(delta, deltaLabel, inverseColor = deltaInverse)
            } else {
                Spacer(modifier = Modifier.height(1.dp))
                Text(subVal, color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppState
import com.example.model.Dose
import com.example.ui.components.BezierConsumptionChart
import com.example.ui.components.CircadianHourlyChart
import com.example.ui.components.CircadianMatrix7x24
import com.example.ui.components.CircadianQuadrantsCard
import com.example.ui.components.ContinuousMitCurveChart
import com.example.ui.components.DisciplineMultiRingChart
import com.example.ui.components.DonutChart
import com.example.ui.components.HydrationBalanceChart
import com.example.ui.components.IntervalComplianceBarChart
import com.example.ui.components.IntervalTrendChart
import com.example.ui.components.MitExposureChart
import com.example.ui.components.ReceptorTrendChart
import com.example.ui.components.ShannonRotationDialCard
import com.example.ui.components.StackedFinanceBarChart
import com.example.ui.components.StackedStrainEvolutionChart
import com.example.ui.components.StrainEffectsCard
import com.example.ui.components.TBreakRecoveryProjectionChart
import com.example.ui.components.TaperingTrajectoryChart
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
import com.example.util.ACHIEVEMENTS_LIST
import com.example.util.calcCleanStreak
import com.example.util.calcContinuousMitTimeline
import com.example.util.calcDailyIntervalCompliance
import com.example.util.calcDailyMitExposure
import com.example.util.calcDailyStrainStackedData
import com.example.util.calcDisciplineRingsProgress
import com.example.util.calcDoseDistribution
import com.example.util.calcHydrationDailyBalance
import com.example.util.calcIntervalStreak
import com.example.util.calcLimitStreak
import com.example.util.calcReceptorSensitivityScore
import com.example.util.calcReceptorTrend
import com.example.util.calcStrainDiversityIndex
import com.example.util.calcStrainEffectsMatrix
import com.example.util.calcTBreakProjectionPoints
import com.example.util.calcTaperingTrajectory
import com.example.util.calcWaterStreak
import com.example.util.dayKey
import com.example.util.formatDate
import com.example.util.r1
import com.example.util.r2
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    appState: AppState,
    onStartTBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = appState.settings
    val allDoses = appState.doses
    val strains = appState.strains
    val packages = appState.packages
    val waterLogs = appState.waterLogs

    // Sub-tab: summary, intervals, strains, health, achievements
    var currentSubTab by remember { mutableStateOf(settings.defaultAnalyticsSubTab) }
    var selectedPeriodDays by remember { mutableIntStateOf(30) } // 7, 14, 30, 90, 0 (All)

    val now = System.currentTimeMillis()
    val periodDoses = if (selectedPeriodDays > 0) {
        val cutoff = now - (selectedPeriodDays * 86400000L)
        allDoses.filter { it.timestamp >= cutoff }
    } else allDoses

    // Grouping by day for graphs
    val effectiveDays = if (selectedPeriodDays > 0) selectedPeriodDays else 30
    val dailyList = (0 until effectiveDays).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, settings.dayStartHour)
        val dayDoses = periodDoses.filter { dayKey(it.timestamp, settings.dayStartHour) == dk }
        val g = dayDoses.sumOf { it.amountG }
        val kCost = dayDoses.sumOf { it.cost }
        val bCost = dayDoses.sumOf { it.beverageCost }
        Triple(dk, g, kCost to bCost)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. SUB-TABS SELECTOR
        val subTabs = listOf(
            "summary" to "📊 Souhrn",
            "intervals" to "⏱ Rozestupy",
            "strains" to "🌿 Odrůdy",
            "health" to "💚 Zdraví & Plán",
            "achievements" to "🏆 Úspěchy"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subTabs.forEach { (key, label) ->
                val isSel = currentSubTab == key
                Box(
                    modifier = Modifier
                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else CardSurface1, RoundedCornerShape(10.dp))
                        .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(10.dp))
                        .clickable { currentSubTab = key }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("analytics_tab_$key")
                ) {
                    Text(
                        text = label,
                        color = if (isSel) MaterialTheme.colorScheme.primary else TextSecondary,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. PERIOD SELECTOR (Unless on achievements)
        if (currentSubTab != "achievements") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(7 to "7 dní", 14 to "14 dní", 30 to "30 dní", 90 to "90 dní", 0 to "Vše").forEach { (d, label) ->
                    val isSel = selectedPeriodDays == d
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface1, RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedPeriodDays = d }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (isSel) MaterialTheme.colorScheme.primary else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. TAB CONTENTS
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentSubTab) {
                "summary" -> {
                    // TAB 1: SOUHRN
                    item {
                        // Smart insights
                        if (settings.enableSmartInsights) {
                            SmartInsightsCard(appState, periodDoses)
                        }
                    }

                    item {
                        // Bezier Consumption Chart
                        BezierConsumptionChart(
                            dailyData = dailyList.map { it.first to it.second },
                            limitG = settings.dailyLimitG,
                            hardLimitG = settings.dailyHardLimitG,
                            privacyMode = settings.privacyMode
                        )
                    }

                    if (settings.showAlkaloids) {
                        item {
                            val mitPoints = calcDailyMitExposure(periodDoses, settings, effectiveDays)
                            MitExposureChart(points = mitPoints)
                        }
                    }

                    if (settings.showFinances) {
                        item {
                            // Stacked Finance Chart
                            StackedFinanceBarChart(
                                days = dailyList.map { it.first to it.third },
                                dailyBudgetKc = settings.dailyBudgetKc
                            )
                        }
                    }

                    if (settings.enableReceptorScore) {
                        item {
                            val (recPoints, cleanDays) = calcReceptorTrend(allDoses, settings, effectiveDays)
                            ReceptorTrendChart(points = recPoints, cleanDaysNeeded = cleanDays)
                        }
                    }

                    // Dose Distribution Histogram
                    item {
                        val dist = calcDoseDistribution(periodDoses, MaterialTheme.colorScheme.primary.toString())
                        DoseHistogramCard(dist)
                    }

                    // KPI Grid
                    item {
                        KpiGridCard(periodDoses, settings, packages)
                    }

                    // Donut Chart of Strains
                    item {
                        val strainGroups = periodDoses.groupBy { it.strainId }
                        val shares = strainGroups.map { (sId, dList) ->
                            val str = strains.find { it.id == sId }
                            Triple(str?.name ?: "Druh", dList.sumOf { it.amountG }, str?.color ?: "#30D158")
                        }.sortedByDescending { it.second }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface1),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Rozložení odrůd v období", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    DonutChart(strainShares = shares)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        shares.take(4).forEach { (sName, sG, sCol) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).background(parseHexColor(sCol, ColorSuccess), CircleShape))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("$sName (${r1(sG)}g)", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Circadian Quadrants Card
                    item {
                        CircadianQuadrantsCard(doses = periodDoses)
                    }

                    // Circadian 24h Chart
                    if (settings.enableHourlyDistribution) {
                        item {
                            val hourlyCounts = IntArray(24)
                            val cal = Calendar.getInstance()
                            for (d in periodDoses) {
                                cal.timeInMillis = d.timestamp
                                val h = cal.get(Calendar.HOUR_OF_DAY)
                                hourlyCounts[h]++
                            }
                            CircadianHourlyChart(hourlyCounts)
                        }
                    }
                }

                "intervals" -> {
                    // TAB 2: ROZESTUPY
                    item {
                        IntervalsTabContent(periodDoses, settings)
                    }
                }

                "strains" -> {
                    // TAB 3: ODRŮDY
                    item {
                        StrainsTabContent(periodDoses, strains, settings)
                    }
                }

                "health" -> {
                    // TAB 4: ZDRAVÍ & PLÁN
                    item {
                        HealthTabContent(appState, onStartTBreak)
                    }
                }

                "achievements" -> {
                    // TAB 5: ÚSPĚCHY
                    item {
                        AchievementsTabContent(appState)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// SMART INSIGHTS CARD
@Composable
fun SmartInsightsCard(appState: AppState, periodDoses: List<Dose>) {
    val settings = appState.settings
    val now = System.currentTimeMillis()
    val todayKeyStr = dayKey(now, settings.dayStartHour)
    val todayDoses = periodDoses.filter { dayKey(it.timestamp, settings.dayStartHour) == todayKeyStr }
    val todayG = todayDoses.sumOf { it.amountG }

    val insights = mutableListOf<Pair<String, Color>>()

    if (todayG >= settings.dailyHardLimitG) {
        insights.add("🛑 STOP limit překročen! Dnes užito ${r1(todayG)} g kratomu." to ColorDanger)
    } else if (todayG >= settings.dailyLimitG * 0.8) {
        insights.add("⚠️ Blížíte se dennímu limitu: ${r1(todayG)} g z ${settings.dailyLimitG} g." to ColorWarning)
    }

    val receptorScore = calcReceptorSensitivityScore(periodDoses, now, settings)
    if (receptorScore < 50) {
        insights.add("⚡ Citlivost receptorů klesla pod 50 %. Doporučujeme zahájit T-Break." to ColorWarning)
    } else if (receptorScore >= 85) {
        insights.add("✨ Skvělá citlivost receptorů ($receptorScore %). Tělo efektivně regeneruje." to ColorSuccess)
    }

    val diversity = calcStrainDiversityIndex(periodDoses, appState.strains, settings.diversityWarningThreshold)
    if (diversity.streak >= settings.diversityWarningThreshold) {
        insights.add("🔄 Užíváte stejný kmen ${diversity.streak}x v řadě. Zvažte rotaci odrůdy pro prevenci tolerance." to ColorWarning)
    }

    if (periodDoses.size >= 10) {
        val sorted = periodDoses.sortedBy { it.timestamp }
        var sumH = 0.0
        var count = 0
        for (i in 0 until sorted.size - 1) {
            val diffH = (sorted[i + 1].timestamp - sorted[i].timestamp) / 3600000.0
            if (diffH < 18.0) { // filter sleep gaps
                sumH += diffH
                count++
            }
        }
        if (count > 0) {
            val avgInt = sumH / count
            if (avgInt < settings.intervalComplianceThresholdH) {
                insights.add("⏱️ Průměrný denní rozestup je ${r1(avgInt)} h (cíl je ≥ ${settings.intervalComplianceThresholdH} h)." to ColorWarning)
            } else {
                insights.add("⏱️ Vynikající disciplína rozestupů! Průměrně ${r1(avgInt)} h mezi dávkami." to ColorSuccess)
            }
        }
    }

    if (insights.isEmpty()) {
        insights.add("✅ Všechny ukazatele jsou ve zdravém optimu. Pokračujte v nastaveném rytmu!" to ColorSuccess)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface1),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chytré postřehy z dat", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            insights.take(6).forEach { (text, color) ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.padding(top = 5.dp).size(6.dp).background(color, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text, color = TextPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

// DOSE HISTOGRAM CARD
@Composable
fun DoseHistogramCard(dist: com.example.util.DoseDistributionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface1),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rozdělení velikostí dávek", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Medián: ${dist.median}g · Modus: ${dist.mode}", color = TextSecondary, fontSize = 11.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dist.bins.forEach { b ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(b.label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .background(CardSurface2, RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(b.percentage / 100f)
                                    .height(12.dp)
                                    .background(parseHexColor(b.colorHex, ColorSuccess), RoundedCornerShape(6.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${b.count} (${b.percentage}%)", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
                    }
                }
            }
        }
    }
}

// KPI GRID CARD
@Composable
fun KpiGridCard(periodDoses: List<Dose>, settings: com.example.model.AppSettings, packages: List<com.example.model.KratomPackage>) {
    val totalG = periodDoses.sumOf { it.amountG }
    val daysCount = max(1, periodDoses.map { dayKey(it.timestamp) }.distinct().size)
    val avgDailyG = totalG / daysCount
    val avgDoseG = if (periodDoses.isNotEmpty()) totalG / periodDoses.size else 0.0
    val totalCostKc = periodDoses.sumOf { it.cost + it.beverageCost }
    val projAnnualKg = r1((avgDailyG * 365) / 1000.0)
    val projAnnualKc = (avgDailyG * 365 * settings.defaultPricePerG).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface1),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Klíčové metriky a predikce", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiItem("Celkem g", "${r1(totalG)} g", ColorSuccess, Modifier.weight(1f))
                KpiItem("Denní Ø", "${r1(avgDailyG)} g/d", TextPrimary, Modifier.weight(1f))
                KpiItem("Ø dávka", "${r1(avgDoseG)} g", TextPrimary, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiItem("Útrata", "${totalCostKc.toInt()} Kč", ColorWarning, Modifier.weight(1f))
                KpiItem("Rok kg", "$projAnnualKg kg", ColorWater, Modifier.weight(1f))
                KpiItem("Rok Kč", "$projAnnualKc Kč", ColorWarning, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(CardSurface2, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 9.sp)
        }
    }
}

// TAB 2: ROZESTUPY CONTENT
@Composable
fun IntervalsTabContent(periodDoses: List<Dose>, settings: com.example.model.AppSettings) {
    val sorted = periodDoses.sortedBy { it.timestamp }
    val intervals = ArrayList<Double>()
    for (i in 0 until sorted.size - 1) {
        val diffH = (sorted[i + 1].timestamp - sorted[i].timestamp) / 3600000.0
        intervals.add(r1(diffH))
    }

    val compliantCount = intervals.count { it >= settings.intervalComplianceThresholdH }
    val compliancePct = if (intervals.isNotEmpty()) ((compliantCount.toDouble() / intervals.size) * 100).toInt() else 100
    val avgInt = if (intervals.isNotEmpty()) r1(intervals.average()) else 0.0

    // 7x24 Matrix
    val matrix = Array(7) { IntArray(24) }
    val cal = Calendar.getInstance()
    for (d in periodDoses) {
        cal.timeInMillis = d.timestamp
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        matrix[dayOfWeek][hour]++
    }

    // Continuous Timeline (hourly samples)
    val continuousTimeline = if (settings.enablePharmacokinetics) {
        calcContinuousMitTimeline(periodDoses, settings, periodDays = 7, samplesCount = 120)
    } else emptyList()

    // Daily Compliance Bars
    val dailyCompliance = calcDailyIntervalCompliance(periodDoses, settings, daysCount = 14)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // KPI row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiItem("Průměrný odstup", "${avgInt} h", ColorSuccess, Modifier.weight(1f))
            KpiItem("Disciplína", "$compliancePct %", if (compliancePct >= 80) ColorSuccess else ColorWarning, Modifier.weight(1f))
            KpiItem("Cílový rozestup", "≥ ${settings.intervalComplianceThresholdH.toInt()} h", ColorWater, Modifier.weight(1f))
        }

        // Daily compliance bar chart
        IntervalComplianceBarChart(complianceList = dailyCompliance)

        // Interval trend dual line chart (avg vs min)
        IntervalTrendChart(
            complianceList = dailyCompliance,
            targetThresholdH = settings.intervalComplianceThresholdH
        )

        // Continuous Pharmacokinetic Curve
        if (settings.enablePharmacokinetics && continuousTimeline.isNotEmpty()) {
            ContinuousMitCurveChart(timeline = continuousTimeline)
        }

        // 7x24 Matrix
        CircadianMatrix7x24(matrix = matrix)
    }
}

// TAB 3: ODRŮDY CONTENT
@Composable
fun StrainsTabContent(periodDoses: List<Dose>, strains: List<com.example.model.Strain>, settings: com.example.model.AppSettings) {
    val div = calcStrainDiversityIndex(periodDoses, strains, settings.diversityWarningThreshold)
    val dailyStacked = calcDailyStrainStackedData(periodDoses, strains, daysCount = 14, dayStartHour = settings.dayStartHour)
    val effectsStats = calcStrainEffectsMatrix(periodDoses, strains)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Shannon Rotation Dial Card
        ShannonRotationDialCard(
            diversity = div,
            threshold = settings.diversityWarningThreshold
        )

        // Stacked Daily Strain Evolution
        StackedStrainEvolutionChart(stacks = dailyStacked, strains = strains)

        // Subjective Effects Matrix Card
        if (settings.enableMoodRating) {
            StrainEffectsCard(stats = effectsStats)
        }

        // Strain details list
        val strainGroups = periodDoses.groupBy { it.strainId }
        strainGroups.forEach { (sId, dList) ->
            val str = strains.find { it.id == sId }
            val dotColor = parseHexColor(str?.color ?: "#30D158", ColorSuccess)
            val gSum = dList.sumOf { it.amountG }
            val avgRating = dList.filter { it.rating > 0 }.map { it.rating }.average()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface1),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(str?.name ?: "Druh", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${dList.size} dávek · ${r1(gSum)} g", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    if (!avgRating.isNaN()) {
                        Text("★ ${r1(avgRating)}", color = Color(0xFFFFD60A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// TAB 4: ZDRAVÍ & PLÁN CONTENT
@Composable
fun HealthTabContent(appState: AppState, onStartTBreak: () -> Unit) {
    val settings = appState.settings
    val doses = appState.doses
    val receptorScore = calcReceptorSensitivityScore(doses, System.currentTimeMillis(), settings)
    val tapering = calcTaperingTrajectory(doses, settings, 14)
    val hydrationBalances = calcHydrationDailyBalance(appState.doses, appState.waterLogs, settings, 14)
    val projectionPoints = calcTBreakProjectionPoints(receptorScore, settings.toleranceRecoveryPerDay)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // T-Break Hub
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface1),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("T-Break ovládací hub", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Citlivost receptorů: $receptorScore % · Regenerace: +${settings.toleranceRecoveryPerDay.toInt()}% / čistý den", color = TextSecondary, fontSize = 11.sp)

                if (!settings.tbreakActive) {
                    Button(
                        onClick = onStartTBreak,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorNight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Spustit T-Break (${settings.tbreakGoalHours.toInt()} h) 🧘", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // T-Break Receptor Recovery Projection Curve
        TBreakRecoveryProjectionChart(points = projectionPoints)

        // Hydration Balance and Kratom Compensation Chart
        if (settings.dashShowWater) {
            HydrationBalanceChart(balances = hydrationBalances)
        }

        // Tapering Trajectory Chart (Plan vs Reality)
        TaperingTrajectoryChart(trajectoryResult = tapering)

        // Tapering Plan & Savings Calculator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface1),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tapering – Postupné snižování & Úspory", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Cíl snížení: z ${r1(settings.taperingStartG)}g na ${r1(settings.taperingTargetG)}g / den", color = TextSecondary, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiItem("Ušetřeno g", "${tapering.savedGrams} g", ColorSuccess, Modifier.weight(1f))
                    KpiItem("Ušetřeno Kč", "${tapering.savedMoneyKc.toInt()} Kč", ColorWarning, Modifier.weight(1f))
                    KpiItem("Roční potenciál", "${tapering.annualSavedKc.toInt()} Kč", ColorWater, Modifier.weight(1f))
                }
            }
        }
    }
}

// TAB 5: ACHIEVEMENTS CONTENT
@Composable
fun AchievementsTabContent(appState: AppState) {
    val unlockedMap = appState.achievements
    val unlockedCount = unlockedMap.size
    val totalCount = ACHIEVEMENTS_LIST.size

    val limitStreak = calcLimitStreak(appState.doses, appState.settings)
    val waterStreak = calcWaterStreak(appState.waterLogs, appState.settings)
    val intervalStreak = calcIntervalStreak(appState.doses, appState.settings)
    val cleanStreak = calcCleanStreak(appState.doses, appState.settings)

    val ringsData = calcDisciplineRingsProgress(
        doses = appState.doses,
        waterLogs = appState.waterLogs,
        tbreakHistory = appState.tbreakHistory,
        settings = appState.settings
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Multi-Ring Discipline Chart
        DisciplineMultiRingChart(data = ringsData)

        // Overall Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface1),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Odemčené úspěchy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("$unlockedCount / $totalCount", color = Color(0xFFFFD60A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                LinearProgressIndicator(
                    progress = { unlockedCount.toFloat() / totalCount },
                    color = Color(0xFFFFD60A),
                    trackColor = CardSurface2,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // 4 Streak mini cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KpiItem("V limitu", "${limitStreak}d", ColorWarning, Modifier.weight(1f))
            KpiItem("Čisté dny", "${cleanStreak}d", ColorSuccess, Modifier.weight(1f))
            KpiItem("Voda", "${waterStreak}d", ColorWater, Modifier.weight(1f))
            KpiItem("Rozestupy", "${intervalStreak}x", ColorNight, Modifier.weight(1f))
        }

        // Badges Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ACHIEVEMENTS_LIST.forEach { ach ->
                val unlockTimestamp = unlockedMap[ach.id]
                val isUnlocked = unlockTimestamp != null

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isUnlocked) CardSurface1 else CardSurface2.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ach.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.title,
                                color = if (isUnlocked) TextPrimary else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(ach.description, color = if (isUnlocked) TextSecondary else TextMuted, fontSize = 11.sp)
                        }
                        if (isUnlocked) {
                            Text(
                                text = "Odemčeno\n${formatDate(unlockTimestamp)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        } else {
                            Text("Cíl: ${ach.target}", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

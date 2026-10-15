package com.example.util

import com.example.model.AppSettings
import com.example.model.AppState
import com.example.model.Dose
import com.example.model.KratomPackage
import com.example.model.ReconciliationRecord
import com.example.model.Strain
import com.example.model.WaterLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun r1(v: Double): Double = round(v * 10.0) / 10.0
fun r2(v: Double): Double = round(v * 100.0) / 100.0

fun getDayOffsetMs(dayStartHour: Int): Long {
    val h = dayStartHour.coerceIn(0, 12)
    return h * 3600000L
}

fun dayKey(timestampMs: Long, dayStartHour: Int = 0): String {
    val offset = getDayOffsetMs(dayStartHour)
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestampMs - offset
    }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
}

fun startOfDay(timestampMs: Long, dayStartHour: Int = 0): Long {
    val offset = getDayOffsetMs(dayStartHour)
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestampMs - offset
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis + offset
}

fun formatTime(timestampMs: Long, is24h: Boolean = true): String {
    val pattern = if (is24h) "HH:mm" else "hh:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMs))
}

fun formatDate(timestampMs: Long): String {
    return SimpleDateFormat("d. M. yyyy", Locale.getDefault()).format(Date(timestampMs))
}

fun formatDateTime(timestampMs: Long, is24h: Boolean = true): String {
    val pattern = if (is24h) "d. M. yyyy HH:mm" else "d. M. yyyy hh:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMs))
}

fun formatDurationHoursMin(ms: Long): String {
    val totalMin = max(0L, ms / 60000L)
    val hours = totalMin / 60L
    val mins = totalMin % 60L
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

// 4.2 Dvoufázový farmakokinetický model Mitragyninu
fun calcActiveMitAtTime(
    doses: List<Dose>,
    targetMs: Long,
    tMaxH: Double = 1.2,
    halfLifeAlphaH: Double = 3.8,
    halfLifeBetaH: Double = 24.0
): Double {
    var sumMit = 0.0
    for (d in doses) {
        val deltaMs = targetMs - d.timestamp
        if (deltaMs < 0) continue
        val deltaH = deltaMs / 3600000.0
        if (deltaH > 96.0) continue

        val factor: Double = if (deltaH < tMaxH) {
            val effTMax = max(0.1, tMaxH)
            sin((deltaH / effTMax) * (Math.PI / 2.0))
        } else {
            val tPost = deltaH - tMaxH
            val fAlpha = 0.7 * 0.5.pow(tPost / max(0.5, halfLifeAlphaH))
            val fBeta = 0.3 * 0.5.pow(tPost / max(1.0, halfLifeBetaH))
            fAlpha + fBeta
        }
        sumMit += d.mitMg * factor
    }
    return r2(sumMit)
}

// Estimate peak & clear time for dose dialog
data class MitCurveEstimate(
    val peakMs: Long,
    val clearHours: Double,
    val clearMs: Long
)

fun calcDoseMitEstimate(
    mitMg: Double,
    timestampMs: Long = System.currentTimeMillis(),
    tMaxH: Double = 1.2,
    halfLifeBetaH: Double = 24.0
): MitCurveEstimate {
    val peakMs = timestampMs + (tMaxH * 3600000L).toLong()
    val clearHours = r1(halfLifeBetaH * (ln(max(mitMg, 1.0)) / ln(2.0)))
    val clearMs = timestampMs + (clearHours * 3600000.0).toLong()
    return MitCurveEstimate(peakMs, clearHours, clearMs)
}

// 4.3 Skóre citlivosti receptorů
fun calcReceptorSensitivityScore(
    doses: List<Dose>,
    targetMs: Long = System.currentTimeMillis(),
    settings: AppSettings = AppSettings()
): Int {
    var score = 100.0
    val priorDoses = doses.filter { it.timestamp <= targetMs }.sortedByDescending { it.timestamp }

    // 1. Penalizace za rozestupy (posledních až 6 dávek)
    val recent6 = priorDoses.take(6)
    if (recent6.size >= 2) {
        var intervalSumH = 0.0
        for (i in 0 until recent6.size - 1) {
            val diffH = (recent6[i].timestamp - recent6[i + 1].timestamp) / 3600000.0
            intervalSumH += diffH
        }
        val avgIntervalH = intervalSumH / (recent6.size - 1)
        val pInt = settings.tolerancePenaltyInterval
        if (avgIntervalH < 2.5) {
            score -= pInt
        } else if (avgIntervalH < 4.0) {
            score -= round(0.5 * pInt)
        }
    }

    // 2. Penalizace za denní limit
    val todayKeyStr = dayKey(targetMs, settings.dayStartHour)
    val todayDoses = priorDoses.filter { dayKey(it.timestamp, settings.dayStartHour) == todayKeyStr }
    val todayG = todayDoses.sumOf { it.amountG }
    val limit = settings.dailyLimitG
    val pLim = settings.tolerancePenaltyLimit
    if (limit > 0) {
        if (todayG > limit) {
            score -= pLim
        } else if (todayG > 0.8 * limit) {
            score -= round(0.5 * pLim)
        }
    }

    // 3. Kontrola posledních 7 dnů (Série vs Regenerace)
    var consecutiveDays = 0
    var cleanDays = 0
    val dayKeyDoseMap = mutableMapOf<String, Double>()
    for (d in priorDoses) {
        val dk = dayKey(d.timestamp, settings.dayStartHour)
        dayKeyDoseMap[dk] = (dayKeyDoseMap[dk] ?: 0.0) + d.amountG
    }

    for (i in 0..6) {
        val checkMs = targetMs - (i * 86400000L)
        val dk = dayKey(checkMs, settings.dayStartHour)
        val g = dayKeyDoseMap[dk] ?: 0.0
        if (g > 0) {
            consecutiveDays++
        } else {
            if (i > 0) cleanDays++
        }
    }

    val pStreak = settings.toleranceConsecutiveDaysPenalty
    if (consecutiveDays >= 7) {
        score -= pStreak
    } else if (consecutiveDays >= 4) {
        score -= round(0.5 * pStreak)
    }

    if (cleanDays > 0) {
        score += cleanDays * settings.toleranceRecoveryPerDay
    }

    return score.roundToInt().coerceIn(10, 100)
}

// 4.4 Shannonův index diverzity a rotace odrůd
data class StrainDiversityResult(
    val rotationScorePct: Int,
    val shannonH: Double,
    val streak: Int,
    val dominantStrainName: String,
    val dominantStrainPct: Int,
    val activeStrainsCount: Int,
    val statusLabel: String,
    val statusTier: String // "good" | "mid" | "danger"
)

fun calcStrainDiversityIndex(
    doses: List<Dose>,
    strains: List<Strain> = emptyList(),
    threshold: Int = 3
): StrainDiversityResult {
    if (doses.isEmpty()) {
        return StrainDiversityResult(
            rotationScorePct = 100,
            shannonH = 0.0,
            streak = 0,
            dominantStrainName = "Žádná data",
            dominantStrainPct = 0,
            activeStrainsCount = 0,
            statusLabel = "Výborná rotace",
            statusTier = "good"
        )
    }

    val strainGMap = mutableMapOf<String, Double>()
    var totalG = 0.0
    for (d in doses) {
        strainGMap[d.strainId] = (strainGMap[d.strainId] ?: 0.0) + d.amountG
        totalG += d.amountG
    }

    val k = strainGMap.size
    var hPrime = 0.0
    if (totalG > 0 && k > 1) {
        for ((_, g) in strainGMap) {
            val p = g / totalG
            if (p > 0) {
                hPrime -= p * ln(p)
            }
        }
    }
    val hMax = if (k > 1) ln(k.toDouble()) else 1.0
    val rotationPct = if (k <= 1) 0 else round((hPrime / hMax).coerceIn(0.0, 1.0) * 100.0).toInt()

    // Streak
    val sortedDoses = doses.sortedByDescending { it.timestamp }
    val lastStrainId = sortedDoses.firstOrNull()?.strainId
    var streak = 0
    if (lastStrainId != null) {
        for (d in sortedDoses) {
            if (d.strainId == lastStrainId) streak++ else break
        }
    }

    // Dominant
    val dominantEntry = strainGMap.maxByOrNull { it.value }
    val domStrain = strains.find { it.id == dominantEntry?.key }
    val domName = domStrain?.name ?: (sortedDoses.firstOrNull()?.strainName ?: "Druh")
    val domPct = if (totalG > 0 && dominantEntry != null) ((dominantEntry.value / totalG) * 100).toInt() else 0

    val (label, tier) = when {
        rotationPct < 35 || streak >= threshold + 1 -> "Nízká rotace (riziko)" to "danger"
        rotationPct < 70 || streak >= threshold -> "Střední rotace" to "mid"
        else -> "Výborná rotace" to "good"
    }

    return StrainDiversityResult(
        rotationScorePct = rotationPct,
        shannonH = r2(hPrime),
        streak = streak,
        dominantStrainName = domName,
        dominantStrainPct = domPct,
        activeStrainsCount = k,
        statusLabel = label,
        statusTier = tier
    )
}

// 4.5 Statistické rozdělení velikosti dávek
data class DoseBin(val label: String, val count: Int, val percentage: Int, val colorHex: String)

data class DoseDistributionResult(
    val bins: List<DoseBin>,
    val median: Double,
    val mode: String,
    val stdDev: Double
)

fun calcDoseDistribution(doses: List<Dose>, accentHex: String = "#30D158"): DoseDistributionResult {
    if (doses.isEmpty()) {
        return DoseDistributionResult(emptyList(), 0.0, "-", 0.0)
    }

    var c1 = 0
    var c2 = 0
    var c3 = 0
    var c4 = 0
    var c5 = 0
    val total = doses.size

    for (d in doses) {
        val g = d.amountG
        when {
            g < 1.5 -> c1++
            g < 2.5 -> c2++
            g < 3.5 -> c3++
            g < 5.0 -> c4++
            else -> c5++
        }
    }

    val bins = listOf(
        DoseBin("< 1.5 g", c1, if (total > 0) (c1 * 100 / total) else 0, "#64D2FF"),
        DoseBin("1.5 – 2.4 g", c2, if (total > 0) (c2 * 100 / total) else 0, accentHex),
        DoseBin("2.5 – 3.4 g", c3, if (total > 0) (c3 * 100 / total) else 0, "#FFCC00"),
        DoseBin("3.5 – 4.9 g", c4, if (total > 0) (c4 * 100 / total) else 0, "#FF9F0A"),
        DoseBin("≥ 5.0 g", c5, if (total > 0) (c5 * 100 / total) else 0, "#FF453A")
    )

    val amounts = doses.map { it.amountG }.sorted()
    val median = if (amounts.size % 2 == 1) {
        amounts[amounts.size / 2]
    } else {
        (amounts[amounts.size / 2 - 1] + amounts[amounts.size / 2]) / 2.0
    }

    val maxBin = bins.maxByOrNull { it.count }
    val mode = maxBin?.label ?: "-"

    val mean = amounts.average()
    val variance = amounts.map { (it - mean).pow(2) }.average()
    val stdDev = r2(sqrt(variance))

    return DoseDistributionResult(bins, r1(median), mode, stdDev)
}

// 4.6 Dynamická hydratace
fun calcDynamicWaterGoal(todayG: Double, settings: AppSettings): Int {
    val extra = round(todayG * settings.waterPerGramKratomMl).toInt()
    return settings.dailyWaterGoalMl + extra
}

// 4.7 Tapering – Trajektorie a kalkulačka úspor
data class TaperingDayPoint(val dayIndex: Int, val dateMs: Long, val plannedG: Double, val actualG: Double)

data class TaperingResult(
    val points: List<TaperingDayPoint>,
    val savedGrams: Double,
    val savedMoneyKc: Double,
    val annualSavedGrams: Double,
    val annualSavedKc: Double
)

fun calcTaperingTrajectory(
    doses: List<Dose>,
    settings: AppSettings,
    daysCount: Int = 14
): TaperingResult {
    val startG = settings.taperingStartG
    val targetG = settings.taperingTargetG
    val weeklyRedPct = settings.taperingWeeklyReductionPct
    val fWeek = 1.0 - (weeklyRedPct / 100.0)

    val points = mutableListOf<TaperingDayPoint>()
    val now = System.currentTimeMillis()
    var currentPlanned = startG
    var sumActualG = 0.0

    val dailyTotals = mutableMapOf<String, Double>()
    for (d in doses) {
        val dk = dayKey(d.timestamp, settings.dayStartHour)
        dailyTotals[dk] = (dailyTotals[dk] ?: 0.0) + d.amountG
    }

    for (i in 0 until daysCount) {
        if (i > 0 && i % 7 == 0) {
            currentPlanned = max(targetG, r2(currentPlanned * fWeek))
        }
        val dayTs = now - ((daysCount - 1 - i) * 86400000L)
        val dk = dayKey(dayTs, settings.dayStartHour)
        val actualG = dailyTotals[dk] ?: 0.0
        sumActualG += actualG
        points.add(TaperingDayPoint(i, dayTs, r2(currentPlanned), r2(actualG)))
    }

    val expectedStartSum = startG * daysCount
    val savedGrams = r2(max(0.0, expectedStartSum - sumActualG))
    val priceG = if (settings.defaultPricePerG > 0) settings.defaultPricePerG else 7.0
    val savedMoneyKc = r2(savedGrams * priceG)

    val annualSavedG = r2(max(0.0, startG - targetG) * 365.0)
    val annualSavedKc = round(annualSavedG * priceG)

    return TaperingResult(points, savedGrams, savedMoneyKc, annualSavedG, annualSavedKc)
}

// 4.8 Dopočet spotřebovaného balení (Reconciliation)
fun applyReconciliation(
    pkg: KratomPackage,
    allDoses: List<Dose>,
    targetWeightG: Double,
    method: String = "proportional",
    selectedDoseIds: Set<String>
): Pair<KratomPackage, List<Dose>> {
    val candidateDoses = allDoses.filter { selectedDoseIds.contains(it.id) }
    val s = candidateDoses.sumOf { it.amountG }
    val diff = r2(targetWeightG - s)
    val factor = if (s > 0) targetWeightG / s else 1.0
    val delta = if (candidateDoses.isNotEmpty()) r2(diff / candidateDoses.size) else 0.0

    val origAmounts = mutableMapOf<String, Double>()
    val updatedDoses = allDoses.map { d ->
        if (selectedDoseIds.contains(d.id)) {
            val orig = d.originalAmountG ?: d.amountG
            origAmounts[d.id] = orig
            val newAmount = if (method == "uniform") {
                r2(max(0.1, d.amountG + delta))
            } else {
                r2(d.amountG * factor)
            }
            val calDiff = r2(newAmount - orig)
            val newCost = r2(newAmount * d.pricePerG)
            val newMit = r2(newAmount * d.mitPct * 10.0)
            val newSevenOh = r2(newAmount * d.sevenOhPct * 10.0)

            d.copy(
                amountG = newAmount,
                originalAmountG = orig,
                calibrated = true,
                calibrationDiff = calDiff,
                packageId = pkg.id,
                cost = newCost,
                mitMg = newMit,
                sevenOhMg = newSevenOh
            )
        } else {
            d
        }
    }

    val record = ReconciliationRecord(
        date = System.currentTimeMillis(),
        method = method,
        targetWeightG = targetWeightG,
        diff = diff,
        factor = factor,
        delta = delta,
        doseIds = selectedDoseIds.toList(),
        originalAmounts = origAmounts
    )

    val finishedPkg = pkg.copy(
        status = "finished",
        remainingWeightG = 0.0,
        finishedAt = System.currentTimeMillis(),
        calibrated = true,
        reconciliation = record
    )

    return finishedPkg to updatedDoses
}

fun undoReconciliation(
    pkg: KratomPackage,
    allDoses: List<Dose>
): Pair<KratomPackage, List<Dose>> {
    val rec = pkg.reconciliation ?: return pkg to allDoses
    val origMap = rec.originalAmounts

    val updatedDoses = allDoses.map { d ->
        val origAmount = origMap[d.id]
        if (origAmount != null) {
            val newCost = r2(origAmount * d.pricePerG)
            val newMit = r2(origAmount * d.mitPct * 10.0)
            val newSevenOh = r2(origAmount * d.sevenOhPct * 10.0)
            d.copy(
                amountG = origAmount,
                originalAmountG = null,
                calibrated = false,
                calibrationDiff = null,
                cost = newCost,
                mitMg = newMit,
                sevenOhMg = newSevenOh
            )
        } else {
            d
        }
    }

    val assignedDosesSum = updatedDoses.filter { it.packageId == pkg.id }.sumOf { it.amountG }
    val remaining = max(0.0, pkg.initialWeightG - assignedDosesSum)

    val restoredPkg = pkg.copy(
        status = "active",
        remainingWeightG = r2(remaining),
        finishedAt = null,
        calibrated = false,
        reconciliation = null
    )

    return restoredPkg to updatedDoses
}

// 6.1 Gamifikace – Výpočet sérií
fun calcLimitStreak(doses: List<Dose>, settings: AppSettings): Int {
    if (doses.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val dailyMap = mutableMapOf<String, Double>()
    for (d in doses) {
        val dk = dayKey(d.timestamp, settings.dayStartHour)
        dailyMap[dk] = (dailyMap[dk] ?: 0.0) + d.amountG
    }

    var streak = 0
    for (i in 0..90) {
        val dayTs = now - (i * 86400000L)
        val dk = dayKey(dayTs, settings.dayStartHour)
        val g = dailyMap[dk] ?: 0.0
        if (g > 0) {
            if (g <= settings.dailyLimitG) {
                streak++
            } else {
                break
            }
        }
    }
    return streak
}

fun calcCleanStreak(doses: List<Dose>, settings: AppSettings): Int {
    if (doses.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val dailyMap = mutableMapOf<String, Double>()
    for (d in doses) {
        val dk = dayKey(d.timestamp, settings.dayStartHour)
        dailyMap[dk] = (dailyMap[dk] ?: 0.0) + d.amountG
    }

    var streak = 0
    for (i in 0..60) {
        val dayTs = now - (i * 86400000L)
        val dk = dayKey(dayTs, settings.dayStartHour)
        val g = dailyMap[dk] ?: 0.0
        if (g == 0.0) {
            streak++
        } else {
            break
        }
    }
    return streak
}

fun calcWaterStreak(waterLogs: List<WaterLog>, settings: AppSettings): Int {
    if (waterLogs.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val dailyWater = mutableMapOf<String, Int>()
    for (w in waterLogs) {
        val dk = dayKey(w.timestamp, settings.dayStartHour)
        dailyWater[dk] = (dailyWater[dk] ?: 0) + w.amountMl
    }

    var streak = 0
    for (i in 0..60) {
        val dayTs = now - (i * 86400000L)
        val dk = dayKey(dayTs, settings.dayStartHour)
        val ml = dailyWater[dk] ?: 0
        if (ml >= settings.dailyWaterGoalMl) {
            streak++
        } else {
            break
        }
    }
    return streak
}

fun calcIntervalStreak(doses: List<Dose>, settings: AppSettings): Int {
    val sorted = doses.sortedByDescending { it.timestamp }
    if (sorted.size < 2) return 0
    var streak = 0
    val thresholdH = settings.intervalComplianceThresholdH
    for (i in 0 until sorted.size - 1) {
        val diffH = (sorted[i].timestamp - sorted[i + 1].timestamp) / 3600000.0
        if (diffH >= thresholdH) {
            streak++
        } else {
            break
        }
    }
    return streak
}

data class AchievementDef(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val target: Int
)

val ACHIEVEMENTS_LIST = listOf(
    AchievementDef("first_dose", "🌱", "První záznam", "Uložení 1. dávky", 1),
    AchievementDef("doses_50", "📒", "Pečlivý deník", "Celkem 50 zapsaných dávek", 50),
    AchievementDef("doses_200", "📚", "Archivář", "Celkem 200 zapsaných dávek", 200),
    AchievementDef("streak_3", "🎯", "Rozjezd disciplíny", "3 dny v řadě pod limitem", 3),
    AchievementDef("streak_7", "🔥", "Týden v limitech", "7 dní v řadě pod limitem", 7),
    AchievementDef("streak_30", "👑", "Mistr disciplíny", "30 dní v řadě pod limitem", 30),
    AchievementDef("clean_3", "🌿", "Čistý víkend+", "3 dny v kuse bez dávky", 3),
    AchievementDef("water_7", "💧", "Hydratační rytmus", "7 dní po sobě splněný pitný cíl", 7),
    AchievementDef("strains_5", "🌈", "Kolektor odrůd", "Vyzkoušeno 5 různých druhů", 5),
    AchievementDef("budget_14", "💰", "Spořivá dvoutýdenní", "14 dní v řadě pod rozpočtem Kč", 14),
    AchievementDef("interval_pro", "⏱️", "Pán rozestupů", "10 po sobě jdoucích rozestupů ≥ 4h", 10),
    AchievementDef("tbreak_done", "🧘", "Reset receptorů", "Splněna 1 T-Break pauza", 1),
    AchievementDef("tbreak_2", "🧠", "Veterán pauz", "Splněny 2 T-Break pauzy", 2),
    AchievementDef("backup_pro", "💾", "Data v bezpečí", "Proveden export zálohy", 1)
)

fun checkAchievements(state: AppState): Map<String, Long> {
    val current = state.achievements.toMutableMap()
    val now = System.currentTimeMillis()

    fun unlock(id: String) {
        if (!current.containsKey(id)) {
            current[id] = now
        }
    }

    if (state.doses.isNotEmpty()) unlock("first_dose")
    if (state.doses.size >= 50) unlock("doses_50")
    if (state.doses.size >= 200) unlock("doses_200")

    val limitStreak = calcLimitStreak(state.doses, state.settings)
    if (limitStreak >= 3) unlock("streak_3")
    if (limitStreak >= 7) unlock("streak_7")
    if (limitStreak >= 30) unlock("streak_30")

    val cleanStreak = calcCleanStreak(state.doses, state.settings)
    if (cleanStreak >= 3) unlock("clean_3")

    val waterStreak = calcWaterStreak(state.waterLogs, state.settings)
    if (waterStreak >= 7) unlock("water_7")

    val uniqueStrains = state.doses.map { it.strainId }.distinct().size
    if (uniqueStrains >= 5) unlock("strains_5")

    // Budget streak 14 days
    val dailySpend = mutableMapOf<String, Double>()
    for (d in state.doses) {
        val dk = dayKey(d.timestamp, state.settings.dayStartHour)
        dailySpend[dk] = (dailySpend[dk] ?: 0.0) + d.cost + d.beverageCost
    }
    var budgetDays = 0
    for (i in 0..30) {
        val dayTs = now - (i * 86400000L)
        val dk = dayKey(dayTs, state.settings.dayStartHour)
        val spend = dailySpend[dk] ?: 0.0
        if (spend > 0) {
            if (spend <= state.settings.dailyBudgetKc) {
                budgetDays++
            } else {
                break
            }
        }
    }
    if (budgetDays >= 14) unlock("budget_14")

    val intervalStreak = calcIntervalStreak(state.doses, state.settings)
    if (intervalStreak >= 10) unlock("interval_pro")

    val completedTBreaks = state.tbreakHistory.count { it.completed }
    if (completedTBreaks >= 1) unlock("tbreak_done")
    if (completedTBreaks >= 2) unlock("tbreak_2")

    if (state.settings.lastBackupAt != null) unlock("backup_pro")

    return current
}

// 6.2 Český CSV Export
fun exportCsv(state: AppState): String {
    val sb = StringBuilder()
    sb.append("Datum;Cas;Druh;Mnozstvi_g;Cena_Kc;Napoj;Cena_Napoj_Kc;MIT_mg;Hodnoceni;Efekt;Poznamka\n")
    val dateFmt = SimpleDateFormat("dd. MM. yyyy", Locale.US)
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)

    for (d in state.doses.sortedBy { it.timestamp }) {
        val dateStr = dateFmt.format(Date(d.timestamp))
        val timeStr = timeFmt.format(Date(d.timestamp))
        val strainNameEsc = "\"" + d.strainName.replace("\"", "\"\"") + "\""
        val bevNameEsc = "\"" + d.beverageName.replace("\"", "\"\"") + "\""
        val noteEsc = "\"" + d.note.replace("\"", "\"\"") + "\""

        sb.append("$dateStr;$timeStr;$strainNameEsc;${d.amountG};${d.cost};$bevNameEsc;${d.beverageCost};${d.mitMg};${d.rating};${d.effect};$noteEsc\n")
    }
    return sb.toString()
}

// --- POKROČILÉ ANALYTICKÉ VÝPOČTY PRO VŠECH 5 PODZÁLOŽEK ---

// 1. Denní expozice alkaloidů (MIT & 7-OH)
data class DailyMitPoint(
    val dayKey: String,
    val dateMs: Long,
    val mitMg: Double,
    val sevenOhMg: Double,
    val thresholdMg: Double
)

fun calcDailyMitExposure(
    doses: List<Dose>,
    settings: AppSettings,
    daysCount: Int
): List<DailyMitPoint> {
    val now = System.currentTimeMillis()
    val dailyMap = mutableMapOf<String, Pair<Double, Double>>()
    for (d in doses) {
        val dk = dayKey(d.timestamp, settings.dayStartHour)
        val cur = dailyMap[dk] ?: (0.0 to 0.0)
        dailyMap[dk] = (cur.first + d.mitMg) to (cur.second + d.sevenOhMg)
    }

    val threshold = if (settings.bodyWeightKg > 0) settings.bodyWeightKg * 1.5 else 112.5

    return (0 until daysCount).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, settings.dayStartHour)
        val (mit, sevenOh) = dailyMap[dk] ?: (0.0 to 0.0)
        DailyMitPoint(dk, ts, r1(mit), r2(sevenOh), r1(threshold))
    }
}

// 2. Křivka vývoje citlivosti receptorů
data class ReceptorTrendPoint(
    val dayKey: String,
    val dateMs: Long,
    val score: Int
)

fun calcReceptorTrend(
    doses: List<Dose>,
    settings: AppSettings,
    daysCount: Int
): Pair<List<ReceptorTrendPoint>, Int> {
    val now = System.currentTimeMillis()
    val points = (0 until daysCount).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, settings.dayStartHour)
        val score = calcReceptorSensitivityScore(doses, ts, settings)
        ReceptorTrendPoint(dk, ts, score)
    }

    val currentScore = points.lastOrNull()?.score ?: 100
    val recPerDay = if (settings.toleranceRecoveryPerDay > 0) settings.toleranceRecoveryPerDay else 15.0
    val cleanDaysNeeded = if (currentScore < 90) {
        Math.ceil((90.0 - currentScore) / recPerDay).toInt()
    } else 0

    return points to cleanDaysNeeded
}

// 3. Kontinuální farmakokinetická simulace plazmatické hladiny MIT
data class ContinuousMitPoint(
    val timestampMs: Long,
    val mitMg: Double
)

fun calcContinuousMitTimeline(
    doses: List<Dose>,
    settings: AppSettings,
    periodDays: Int,
    samplesCount: Int = 100
): List<ContinuousMitPoint> {
    val now = System.currentTimeMillis()
    val startMs = now - (periodDays * 86400000L)
    val totalMs = now - startMs
    if (totalMs <= 0) return emptyList()

    val relevantDoses = doses.filter { it.timestamp >= startMs - (96 * 3600000L) && it.timestamp <= now }
    val stepMs = totalMs / samplesCount.toDouble()

    val points = mutableListOf<ContinuousMitPoint>()
    for (i in 0..samplesCount) {
        val t = (startMs + i * stepMs).toLong()
        val mit = calcActiveMitAtTime(
            relevantDoses,
            t,
            settings.mitTMaxH,
            settings.mitHalfLifeAlphaH,
            settings.mitHalfLifeBetaH
        )
        points.add(ContinuousMitPoint(t, mit))
    }
    return points
}

// 4. Denní míra disciplíny rozestupů a minimální interval
data class DailyIntervalCompliance(
    val dayKey: String,
    val dateMs: Long,
    val totalIntervals: Int,
    val compliantCount: Int,
    val compliancePct: Int,
    val avgIntervalH: Double,
    val minIntervalH: Double
)

fun calcDailyIntervalCompliance(
    doses: List<Dose>,
    settings: AppSettings,
    daysCount: Int
): List<DailyIntervalCompliance> {
    val now = System.currentTimeMillis()
    val sorted = doses.sortedBy { it.timestamp }
    val dailyIntervalsMap = mutableMapOf<String, MutableList<Double>>()

    for (i in 0 until sorted.size - 1) {
        val dCurr = sorted[i]
        val dNext = sorted[i + 1]
        val diffH = (dNext.timestamp - dCurr.timestamp) / 3600000.0
        if (diffH <= 24.0) { // skip multi-day breaks
            val dk = dayKey(dNext.timestamp, settings.dayStartHour)
            dailyIntervalsMap.getOrPut(dk) { mutableListOf() }.add(diffH)
        }
    }

    val targetH = settings.intervalComplianceThresholdH

    return (0 until daysCount).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, settings.dayStartHour)
        val ints = dailyIntervalsMap[dk] ?: emptyList()
        val total = ints.size
        val compliant = ints.count { it >= targetH }
        val pct = if (total > 0) ((compliant.toDouble() / total) * 100).roundToInt() else 100
        val avgH = if (total > 0) r1(ints.average()) else 0.0
        val minH = if (total > 0) r1(ints.minOrNull() ?: 0.0) else 0.0

        DailyIntervalCompliance(dk, ts, total, compliant, pct, avgH, minH)
    }
}

// 5. Denní skládaná data podílu odrůd (Stacked Evolution)
data class DailyStrainStack(
    val dayKey: String,
    val dateMs: Long,
    val totalG: Double,
    val strainShares: List<Pair<String, Double>> // strainId -> amountG
)

fun calcDailyStrainStackedData(
    doses: List<Dose>,
    strains: List<Strain>,
    daysCount: Int,
    dayStartHour: Int = 0
): List<DailyStrainStack> {
    val now = System.currentTimeMillis()
    val dailyMap = mutableMapOf<String, MutableMap<String, Double>>()

    for (d in doses) {
        val dk = dayKey(d.timestamp, dayStartHour)
        val subMap = dailyMap.getOrPut(dk) { mutableMapOf() }
        subMap[d.strainId] = (subMap[d.strainId] ?: 0.0) + d.amountG
    }

    return (0 until daysCount).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, dayStartHour)
        val sub = dailyMap[dk] ?: emptyMap()
        val total = sub.values.sum()
        val shares = strains.map { s -> s.id to (sub[s.id] ?: 0.0) }
        DailyStrainStack(dk, ts, r1(total), shares)
    }
}

// 6. Matice účinků a spokojenosti dle odrůd
data class StrainEffectStat(
    val strainId: String,
    val strainName: String,
    val colorHex: String,
    val dosesCount: Int,
    val totalGrams: Double,
    val avgRating: Double,
    val energyCount: Int,
    val focusCount: Int,
    val relaxCount: Int,
    val painCount: Int,
    val sleepCount: Int
)

fun calcStrainEffectsMatrix(
    doses: List<Dose>,
    strains: List<Strain>
): List<StrainEffectStat> {
    val grouped = doses.groupBy { it.strainId }

    return strains.map { str ->
        val dList = grouped[str.id] ?: emptyList()
        val rated = dList.filter { it.rating > 0 }
        val avgR = if (rated.isNotEmpty()) r1(rated.map { it.rating }.average()) else 0.0

        var energy = 0
        var focus = 0
        var relax = 0
        var pain = 0
        var sleep = 0

        for (d in dList) {
            when (d.effect) {
                "energy" -> energy++
                "focus" -> focus++
                "relax" -> relax++
                "pain" -> pain++
                "sleep" -> sleep++
            }
        }

        StrainEffectStat(
            strainId = str.id,
            strainName = str.name,
            colorHex = str.color,
            dosesCount = dList.size,
            totalGrams = r1(dList.sumOf { it.amountG }),
            avgRating = avgR,
            energyCount = energy,
            focusCount = focus,
            relaxCount = relax,
            painCount = pain,
            sleepCount = sleep
        )
    }
}

// 7. Hydratační bilance (reálná voda vs dynamický cíl)
data class DailyHydrationBalance(
    val dayKey: String,
    val dateMs: Long,
    val actualWaterMl: Int,
    val targetWaterMl: Int,
    val balanceMl: Int
)

fun calcHydrationDailyBalance(
    doses: List<Dose>,
    waterLogs: List<WaterLog>,
    settings: AppSettings,
    daysCount: Int
): List<DailyHydrationBalance> {
    val now = System.currentTimeMillis()
    val dailyGMap = mutableMapOf<String, Double>()
    for (d in doses) {
        val dk = dayKey(d.timestamp, settings.dayStartHour)
        dailyGMap[dk] = (dailyGMap[dk] ?: 0.0) + d.amountG
    }

    val dailyWaterMap = mutableMapOf<String, Int>()
    for (w in waterLogs) {
        val dk = dayKey(w.timestamp, settings.dayStartHour)
        dailyWaterMap[dk] = (dailyWaterMap[dk] ?: 0) + w.amountMl
    }

    return (0 until daysCount).reversed().map { i ->
        val ts = now - (i * 86400000L)
        val dk = dayKey(ts, settings.dayStartHour)
        val g = dailyGMap[dk] ?: 0.0
        val actualMl = dailyWaterMap[dk] ?: 0
        val targetMl = calcDynamicWaterGoal(g, settings)
        val diff = actualMl - targetMl
        DailyHydrationBalance(dk, ts, actualMl, targetMl, diff)
    }
}

// 8. T-Break projekční křivka regenerace
data class TBreakProjectionPoint(
    val label: String,
    val dayOffset: Int,
    val projectedScore: Int
)

fun calcTBreakProjectionPoints(
    currentScore: Int,
    recoveryPerDay: Double = 15.0
): List<TBreakProjectionPoint> {
    val offsets = listOf(
        "Teď" to 0,
        "+1d" to 1,
        "+2d" to 2,
        "+3d" to 3,
        "+5d" to 5,
        "+7d" to 7,
        "+14d" to 14
    )
    return offsets.map { (lbl, days) ->
        val rec = currentScore + (days * recoveryPerDay).roundToInt()
        TBreakProjectionPoint(lbl, days, rec.coerceIn(10, 100))
    }
}

// 9. Multi-kruhy disciplíny (Discipline Multi-Ring)
data class DisciplineRingData(
    val limitStreak: Int,
    val limitGoal: Int = 14,
    val completedTBreaks: Int,
    val tbreakGoal: Int = 2,
    val waterStreak: Int,
    val waterGoal: Int = 7,
    val intervalCompliancePct: Int
)

fun calcDisciplineRingsProgress(
    doses: List<Dose>,
    waterLogs: List<WaterLog>,
    tbreakHistory: List<com.example.model.TBreakRecord>,
    settings: AppSettings
): DisciplineRingData {
    val limStreak = calcLimitStreak(doses, settings)
    val watStreak = calcWaterStreak(waterLogs, settings)
    val compBreaks = tbreakHistory.count { it.completed }

    val sorted = doses.sortedBy { it.timestamp }
    var compCount = 0
    var totalInts = 0
    for (i in 0 until sorted.size - 1) {
        val diffH = (sorted[i + 1].timestamp - sorted[i].timestamp) / 3600000.0
        if (diffH <= 24.0) {
            totalInts++
            if (diffH >= settings.intervalComplianceThresholdH) compCount++
        }
    }
    val intPct = if (totalInts > 0) ((compCount.toDouble() / totalInts) * 100).roundToInt() else 100

    return DisciplineRingData(
        limitStreak = limStreak,
        completedTBreaks = compBreaks,
        waterStreak = watStreak,
        intervalCompliancePct = intPct
    )
}

// 10. Výpočet srovnávacích odchylek (Deltas) pro Dashboard
data class MetricDelta(
    val current: Double,
    val previous: Double,
    val diff: Double,
    val diffPct: Double,
    val hasComparison: Boolean,
    val isIncrease: Boolean,
    val formattedPct: String
)

data class DashboardDeltas(
    val todayG: Double,
    val yesterdayG: Double,
    val deltaGvsYesterday: MetricDelta,
    val avg7dG: Double,
    val deltaGvs7d: MetricDelta,

    val todayCost: Double,
    val yesterdayCost: Double,
    val deltaCostVsYesterday: MetricDelta,
    val avg7dCost: Double,
    val deltaCostVs7d: MetricDelta,

    val todayWaterMl: Int,
    val yesterdayWaterMl: Int,
    val deltaWaterVsYesterday: MetricDelta,
    val targetWaterMl: Int,

    val todayAvgIntervalH: Double,
    val avg7dIntervalH: Double,
    val deltaIntervalVs7d: MetricDelta
)

fun makeMetricDelta(current: Double, previous: Double): MetricDelta {
    val hasComp = previous > 0.001
    val diff = current - previous
    val pct = if (hasComp) ((diff / previous) * 100.0) else 0.0
    val roundedPct = round(pct * 10.0) / 10.0
    val isInc = diff > 0.001
    val sign = if (roundedPct > 0) "+" else ""
    val formatted = if (hasComp) "$sign${roundedPct.toInt()}%" else "–"
    return MetricDelta(
        current = r1(current),
        previous = r1(previous),
        diff = r1(diff),
        diffPct = roundedPct,
        hasComparison = hasComp,
        isIncrease = isInc,
        formattedPct = formatted
    )
}

fun calcDashboardDeltas(
    doses: List<Dose>,
    waterLogs: List<WaterLog>,
    settings: AppSettings,
    targetMs: Long = System.currentTimeMillis()
): DashboardDeltas {
    val todayKeyStr = dayKey(targetMs, settings.dayStartHour)
    val yesterdayKeyStr = dayKey(targetMs - 86400000L, settings.dayStartHour)

    val past7Keys = (1..7).map { dayKey(targetMs - (it * 86400000L), settings.dayStartHour) }

    val todayDoses = doses.filter { dayKey(it.timestamp, settings.dayStartHour) == todayKeyStr }
    val yesterdayDoses = doses.filter { dayKey(it.timestamp, settings.dayStartHour) == yesterdayKeyStr }
    val past7Doses = doses.filter { past7Keys.contains(dayKey(it.timestamp, settings.dayStartHour)) }

    val todayG = todayDoses.sumOf { it.amountG }
    val yesterdayG = yesterdayDoses.sumOf { it.amountG }
    val avg7dG = past7Doses.sumOf { it.amountG } / 7.0

    val todayCost = todayDoses.sumOf { it.cost + it.beverageCost }
    val yesterdayCost = yesterdayDoses.sumOf { it.cost + it.beverageCost }
    val avg7dCost = past7Doses.sumOf { it.cost + it.beverageCost } / 7.0

    val todayWaterLogs = waterLogs.filter { dayKey(it.timestamp, settings.dayStartHour) == todayKeyStr }
    val yesterdayWaterLogs = waterLogs.filter { dayKey(it.timestamp, settings.dayStartHour) == yesterdayKeyStr }
    val todayWater = todayWaterLogs.sumOf { it.amountMl }
    val yesterdayWater = yesterdayWaterLogs.sumOf { it.amountMl }
    val targetWater = calcDynamicWaterGoal(todayG, settings)

    // Today intervals
    val sortedToday = todayDoses.sortedBy { it.timestamp }
    val todayIntervals = mutableListOf<Double>()
    for (i in 0 until sortedToday.size - 1) {
        val diffH = (sortedToday[i + 1].timestamp - sortedToday[i].timestamp) / 3600000.0
        if (diffH <= 18.0) todayIntervals.add(diffH)
    }
    val todayAvgInt = if (todayIntervals.isNotEmpty()) todayIntervals.average() else 0.0

    // Past 7D intervals
    val sortedPast7 = past7Doses.sortedBy { it.timestamp }
    val past7Intervals = mutableListOf<Double>()
    for (i in 0 until sortedPast7.size - 1) {
        val diffH = (sortedPast7[i + 1].timestamp - sortedPast7[i].timestamp) / 3600000.0
        if (diffH <= 18.0) past7Intervals.add(diffH)
    }
    val past7AvgInt = if (past7Intervals.isNotEmpty()) past7Intervals.average() else 0.0

    return DashboardDeltas(
        todayG = r1(todayG),
        yesterdayG = r1(yesterdayG),
        deltaGvsYesterday = makeMetricDelta(todayG, yesterdayG),
        avg7dG = r1(avg7dG),
        deltaGvs7d = makeMetricDelta(todayG, avg7dG),

        todayCost = r1(todayCost),
        yesterdayCost = r1(yesterdayCost),
        deltaCostVsYesterday = makeMetricDelta(todayCost, yesterdayCost),
        avg7dCost = r1(avg7dCost),
        deltaCostVs7d = makeMetricDelta(todayCost, avg7dCost),

        todayWaterMl = todayWater,
        yesterdayWaterMl = yesterdayWater,
        deltaWaterVsYesterday = makeMetricDelta(todayWater.toDouble(), yesterdayWater.toDouble()),
        targetWaterMl = targetWater,

        todayAvgIntervalH = r1(todayAvgInt),
        avg7dIntervalH = r1(past7AvgInt),
        deltaIntervalVs7d = makeMetricDelta(todayAvgInt, past7AvgInt)
    )
}


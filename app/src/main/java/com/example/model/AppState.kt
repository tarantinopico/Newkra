package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// --- PALETTE FOR STRAINS ---
val STRAIN_PALETTE = listOf(
    "#30D158", "#34C759", "#007AFF", "#5856D6",
    "#AF52DE", "#FF2D55", "#FF3B30", "#FF9500",
    "#FFCC00", "#8E8E93", "#E5E5EA"
)

data class Strain(
    val id: String = "s_" + UUID.randomUUID().toString().take(8),
    val name: String,
    val pricePerG: Double = 7.0,
    val packageSizeG: Double = 50.0,
    val mitPct: Double = 1.45,
    val sevenOhPct: Double = 0.025,
    val color: String = "#30D158",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("pricePerG", pricePerG)
        put("packageSizeG", packageSizeG)
        put("mitPct", mitPct)
        put("sevenOhPct", sevenOhPct)
        put("color", color)
        put("note", note)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): Strain = Strain(
            id = json.optString("id", "s_" + UUID.randomUUID().toString().take(8)),
            name = json.optString("name", "Neznámý druh"),
            pricePerG = json.optDouble("pricePerG", 7.0),
            packageSizeG = json.optDouble("packageSizeG", 50.0),
            mitPct = json.optDouble("mitPct", 1.45),
            sevenOhPct = json.optDouble("sevenOhPct", 0.025),
            color = json.optString("color", "#30D158"),
            note = json.optString("note", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

data class ReconciliationRecord(
    val date: Long = System.currentTimeMillis(),
    val method: String = "proportional", // "proportional" | "uniform"
    val targetWeightG: Double = 0.0,
    val diff: Double = 0.0,
    val factor: Double = 1.0,
    val delta: Double = 0.0,
    val doseIds: List<String> = emptyList(),
    val originalAmounts: Map<String, Double> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("method", method)
        put("targetWeightG", targetWeightG)
        put("diff", diff)
        put("factor", factor)
        put("delta", delta)
        put("doseIds", JSONArray(doseIds))
        val origObj = JSONObject()
        originalAmounts.forEach { (k, v) -> origObj.put(k, v) }
        put("originalAmounts", origObj)
    }

    companion object {
        fun fromJson(json: JSONObject): ReconciliationRecord {
            val dIds = mutableListOf<String>()
            val doseArr = json.optJSONArray("doseIds")
            if (doseArr != null) {
                for (i in 0 until doseArr.length()) {
                    dIds.add(doseArr.getString(i))
                }
            }
            val origMap = mutableMapOf<String, Double>()
            val origObj = json.optJSONObject("originalAmounts")
            if (origObj != null) {
                val keys = origObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    origMap[k] = origObj.optDouble(k, 0.0)
                }
            }
            return ReconciliationRecord(
                date = json.optLong("date", System.currentTimeMillis()),
                method = json.optString("method", "proportional"),
                targetWeightG = json.optDouble("targetWeightG", 0.0),
                diff = json.optDouble("diff", 0.0),
                factor = json.optDouble("factor", 1.0),
                delta = json.optDouble("delta", 0.0),
                doseIds = dIds,
                originalAmounts = origMap
            )
        }
    }
}

data class KratomPackage(
    val id: String = "pkg_" + UUID.randomUUID().toString().take(8),
    val strainId: String,
    val name: String,
    val initialWeightG: Double,
    val remainingWeightG: Double,
    val costTotal: Double,
    val pricePerG: Double = if (initialWeightG > 0) costTotal / initialWeightG else 8.0,
    val status: String = "active", // "active" | "unopened" | "finished"
    val openedAt: Long? = null,
    val finishedAt: Long? = null,
    val note: String = "",
    val calibrated: Boolean = false,
    val reconciliation: ReconciliationRecord? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("strainId", strainId)
        put("name", name)
        put("initialWeightG", initialWeightG)
        put("remainingWeightG", remainingWeightG)
        put("costTotal", costTotal)
        put("pricePerG", pricePerG)
        put("status", status)
        openedAt?.let { put("openedAt", it) }
        finishedAt?.let { put("finishedAt", it) }
        put("note", note)
        put("calibrated", calibrated)
        reconciliation?.let { put("reconciliation", it.toJson()) }
    }

    companion object {
        fun fromJson(json: JSONObject): KratomPackage = KratomPackage(
            id = json.optString("id", "pkg_" + UUID.randomUUID().toString().take(8)),
            strainId = json.optString("strainId", ""),
            name = json.optString("name", "Balení"),
            initialWeightG = json.optDouble("initialWeightG", 50.0),
            remainingWeightG = json.optDouble("remainingWeightG", 50.0),
            costTotal = json.optDouble("costTotal", 350.0),
            pricePerG = json.optDouble("pricePerG", 7.0),
            status = json.optString("status", "active"),
            openedAt = if (json.has("openedAt") && !json.isNull("openedAt")) json.getLong("openedAt") else null,
            finishedAt = if (json.has("finishedAt") && !json.isNull("finishedAt")) json.getLong("finishedAt") else null,
            note = json.optString("note", ""),
            calibrated = json.optBoolean("calibrated", false),
            reconciliation = json.optJSONObject("reconciliation")?.let { ReconciliationRecord.fromJson(it) }
        )
    }
}

data class Beverage(
    val id: String = "bev_" + UUID.randomUUID().toString().take(8),
    val name: String,
    val icon: String = "🥤",
    val cost: Double = 0.0,
    val color: String = "#0A84FF",
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("icon", icon)
        put("cost", cost)
        put("color", color)
        put("note", note)
    }

    companion object {
        fun fromJson(json: JSONObject): Beverage = Beverage(
            id = json.optString("id", "bev_" + UUID.randomUUID().toString().take(8)),
            name = json.optString("name", "Nápoj"),
            icon = json.optString("icon", "🥤"),
            cost = json.optDouble("cost", 0.0),
            color = json.optString("color", "#0A84FF"),
            note = json.optString("note", "")
        )
    }
}

data class Dose(
    val id: String = "d_" + UUID.randomUUID().toString().take(8),
    val strainId: String,
    val strainName: String,
    val strainColor: String = "#30D158",
    val packageId: String? = null,
    val amountG: Double,
    val originalAmountG: Double? = null,
    val calibrated: Boolean = false,
    val calibrationDiff: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val pricePerG: Double = 7.0,
    val cost: Double = Math.round(amountG * pricePerG * 100.0) / 100.0,
    val beverageId: String = "bev_water",
    val beverageName: String = "Voda",
    val beverageIcon: String = "💧",
    val beverageCost: Double = 0.0,
    val rating: Int = 0,
    val effect: String = "energy", // "energy" | "focus" | "relax" | "pain" | "sleep"
    val mitPct: Double = 1.45,
    val sevenOhPct: Double = 0.025,
    val mitMg: Double = Math.round(amountG * mitPct * 10.0 * 100.0) / 100.0,
    val sevenOhMg: Double = Math.round(amountG * sevenOhPct * 10.0 * 100.0) / 100.0,
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("strainId", strainId)
        put("strainName", strainName)
        put("strainColor", strainColor)
        packageId?.let { put("packageId", it) }
        put("amountG", amountG)
        originalAmountG?.let { put("originalAmountG", it) }
        put("calibrated", calibrated)
        calibrationDiff?.let { put("calibrationDiff", it) }
        put("timestamp", timestamp)
        put("pricePerG", pricePerG)
        put("cost", cost)
        put("beverageId", beverageId)
        put("beverageName", beverageName)
        put("beverageIcon", beverageIcon)
        put("beverageCost", beverageCost)
        put("rating", rating)
        put("effect", effect)
        put("mitPct", mitPct)
        put("sevenOhPct", sevenOhPct)
        put("mitMg", mitMg)
        put("sevenOhMg", sevenOhMg)
        put("note", note)
    }

    companion object {
        fun fromJson(json: JSONObject): Dose {
            val amount = json.optDouble("amountG", 2.0)
            val mitP = json.optDouble("mitPct", 1.45)
            val sevenOhP = json.optDouble("sevenOhPct", 0.025)
            val pPerG = json.optDouble("pricePerG", 7.0)
            val defCost = Math.round(amount * pPerG * 100.0) / 100.0
            val defMit = Math.round(amount * mitP * 10.0 * 100.0) / 100.0
            val defSevenOh = Math.round(amount * sevenOhP * 10.0 * 100.0) / 100.0

            return Dose(
                id = json.optString("id", "d_" + UUID.randomUUID().toString().take(8)),
                strainId = json.optString("strainId", ""),
                strainName = json.optString("strainName", "Kratom"),
                strainColor = json.optString("strainColor", "#30D158"),
                packageId = if (json.has("packageId") && !json.isNull("packageId")) json.getString("packageId") else null,
                amountG = amount,
                originalAmountG = if (json.has("originalAmountG") && !json.isNull("originalAmountG")) json.getDouble("originalAmountG") else null,
                calibrated = json.optBoolean("calibrated", false),
                calibrationDiff = if (json.has("calibrationDiff") && !json.isNull("calibrationDiff")) json.getDouble("calibrationDiff") else null,
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                pricePerG = pPerG,
                cost = json.optDouble("cost", defCost),
                beverageId = json.optString("beverageId", "bev_water"),
                beverageName = json.optString("beverageName", "Voda"),
                beverageIcon = json.optString("beverageIcon", "💧"),
                beverageCost = json.optDouble("beverageCost", 0.0),
                rating = json.optInt("rating", 0),
                effect = json.optString("effect", "energy"),
                mitPct = mitP,
                sevenOhPct = sevenOhP,
                mitMg = json.optDouble("mitMg", defMit),
                sevenOhMg = json.optDouble("sevenOhMg", defSevenOh),
                note = json.optString("note", "")
            )
        }
    }
}

data class WaterLog(
    val id: String = "w_" + UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val amountMl: Int = 250
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("amountMl", amountMl)
    }

    companion object {
        fun fromJson(json: JSONObject): WaterLog = WaterLog(
            id = json.optString("id", "w_" + UUID.randomUUID().toString().take(8)),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            amountMl = json.optInt("amountMl", 250)
        )
    }
}

data class TBreakRecord(
    val id: String = "tb_" + UUID.randomUUID().toString().take(8),
    val startedAt: Long,
    val endedAt: Long = System.currentTimeMillis(),
    val goalHours: Double = 72.0,
    val hoursDone: Double = Math.round(((endedAt - startedAt) / 3600000.0) * 10.0) / 10.0,
    val completed: Boolean = (endedAt - startedAt) >= (goalHours * 3600000.0)
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startedAt", startedAt)
        put("endedAt", endedAt)
        put("goalHours", goalHours)
        put("hoursDone", hoursDone)
        put("completed", completed)
    }

    companion object {
        fun fromJson(json: JSONObject): TBreakRecord = TBreakRecord(
            id = json.optString("id", "tb_" + UUID.randomUUID().toString().take(8)),
            startedAt = json.optLong("startedAt", System.currentTimeMillis()),
            endedAt = json.optLong("endedAt", System.currentTimeMillis()),
            goalHours = json.optDouble("goalHours", 72.0),
            hoursDone = json.optDouble("hoursDone", 0.0),
            completed = json.optBoolean("completed", false)
        )
    }
}

data class AppSettings(
    // 1. Vzhled a téma
    val currency: String = "Kč",
    val theme: String = "emerald", // "dynamic", "emerald", "cyan", "purple", "amber", "rose", "gold", "custom"
    val customAccentHex: String = "#30D158",
    val dashboardDialStyle: String = "precision", // "precision" | "concentric"
    val dashboardShowSparklines: Boolean = true,
    val defaultAnalyticsSubTab: String = "summary",
    val privacyMode: Boolean = false,
    val uiDensity: String = "normal", // "normal" | "compact"
    val fontScale: String = "normal", // "normal" | "large" | "xlarge"
    val startTab: String = "dashboard", // "dashboard" | "stock" | "history" | "analytics"
    val timeFormat24: Boolean = true,
    val weekStartsMonday: Boolean = true,
    val dashRecentDosesCount: Int = 2,

    // 2. Dashboard Builder (9 switches)
    val dashShowQuickRow: Boolean = true,
    val dashShowHud: Boolean = true,
    val dashShowInsight: Boolean = true,
    val dashShowHero: Boolean = true,
    val dashShowTelem: Boolean = true,
    val dashShowTimeline: Boolean = true,
    val dashShowWater: Boolean = true,
    val dashShowActivity: Boolean = true,
    val dashShowStats: Boolean = true,

    // 3. Historie
    val histCompactRows: Boolean = false,
    val histShowFilters: Boolean = true,

    // 4. Modal dialog dávky
    val modalShowPreview: Boolean = true,
    val modalShowMitEstimate: Boolean = true,
    val modalShowNoteTags: Boolean = true,
    val quickLogConfirm: Boolean = false,

    // 5. Limity a rozpočty
    val dailyLimitG: Double = 10.0,
    val dailyHardLimitG: Double = 14.0,
    val dailyBudgetKc: Double = 150.0,
    val monthlyBudgetKc: Double = 4500.0,
    val cooldownMin: Int = 240,
    val intervalComplianceThresholdH: Double = 4.0,
    val haptics: Boolean = true,

    // 6. Farmakokinetika & Alkaloidy
    val bodyWeightKg: Double = 75.0,
    val mitTMaxH: Double = 1.2,
    val mitHalfLifeAlphaH: Double = 3.8,
    val mitHalfLifeBetaH: Double = 24.0,
    val defaultMitPct: Double = 1.45,
    val defaultSevenOhPct: Double = 0.025,

    // 7. Tolerance, Receptory & T-Break
    val toleranceRecoveryPerDay: Double = 15.0,
    val tolerancePenaltyLimit: Double = 30.0,
    val tolerancePenaltyInterval: Double = 35.0,
    val toleranceConsecutiveDaysPenalty: Double = 15.0,
    val tbreakActive: Boolean = false,
    val tbreakStartedAt: Long? = null,
    val tbreakGoalHours: Double = 72.0,

    // 8. Hydratace
    val dailyWaterGoalMl: Int = 2500,
    val waterPerGramKratomMl: Int = 200,
    val waterQuickMl1: Int = 250,
    val waterQuickMl2: Int = 500,

    // 9. Dávkování & Presety
    val defaultDoseG: Double = 2.0,
    val quickDosePresets: List<Double> = listOf(1.0, 2.0, 2.5, 3.0),
    val autoSelectActivePackage: Boolean = true,
    val doseStepDefault: Double = 0.5,

    // 10. Chování & Čas
    val dayStartHour: Int = 0,
    val confirmBeforeDelete: Boolean = true,
    val soundsEnabled: Boolean = false,
    val enableNotifications: Boolean = false,

    // 11. Sklad & Dopočet
    val defaultReconcileMethod: String = "proportional", // "proportional" | "uniform"
    val reconcileAutoIncludeUnassigned: Boolean = false,
    val lowStockWarningG: Double = 20.0,
    val defaultPackageWeightG: Double = 100.0,
    val defaultPricePerG: Double = 8.0,

    // 12. Tapering
    val taperingEnabled: Boolean = false,
    val taperingStartG: Double = 12.0,
    val taperingTargetG: Double = 4.0,
    val taperingWeeklyReductionPct: Double = 10.0,
    val taperingStepG: Double = 0.5,
    val taperingDaysInterval: Int = 3,
    val taperingStartedAt: Long? = null,

    // 13. Moduly & Funkce
    val showAlkaloids: Boolean = true,
    val showFinances: Boolean = true,
    val enableBeverages: Boolean = true,
    val enableHydration: Boolean = true,
    val enableMoodRating: Boolean = true,
    val enableHeatmap: Boolean = true,
    val enableQuickLog: Boolean = true,
    val enablePharmacokinetics: Boolean = true,
    val enableReceptorScore: Boolean = true,
    val enableHourlyDistribution: Boolean = true,
    val enableStrainRotationAdvice: Boolean = true,
    val diversityWarningThreshold: Int = 3,
    val enableAchievements: Boolean = true,
    val enableCalendarView: Boolean = true,
    val enableSmartInsights: Boolean = true,

    // Paměť & Záloha
    val lastStrainId: String? = null,
    val lastAmount: Double = 2.0,
    val lastBeverageId: String = "bev_water",
    val lastBackupAt: Long? = null,
    val autoBackupReminderDays: Int = 7,
    val onboarded: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("currency", currency)
        put("theme", theme)
        put("customAccentHex", customAccentHex)
        put("dashboardDialStyle", dashboardDialStyle)
        put("dashboardShowSparklines", dashboardShowSparklines)
        put("defaultAnalyticsSubTab", defaultAnalyticsSubTab)
        put("privacyMode", privacyMode)
        put("uiDensity", uiDensity)
        put("fontScale", fontScale)
        put("startTab", startTab)
        put("timeFormat24", timeFormat24)
        put("weekStartsMonday", weekStartsMonday)
        put("dashRecentDosesCount", dashRecentDosesCount)

        put("dashShowQuickRow", dashShowQuickRow)
        put("dashShowHud", dashShowHud)
        put("dashShowInsight", dashShowInsight)
        put("dashShowHero", dashShowHero)
        put("dashShowTelem", dashShowTelem)
        put("dashShowTimeline", dashShowTimeline)
        put("dashShowWater", dashShowWater)
        put("dashShowActivity", dashShowActivity)
        put("dashShowStats", dashShowStats)

        put("histCompactRows", histCompactRows)
        put("histShowFilters", histShowFilters)

        put("modalShowPreview", modalShowPreview)
        put("modalShowMitEstimate", modalShowMitEstimate)
        put("modalShowNoteTags", modalShowNoteTags)
        put("quickLogConfirm", quickLogConfirm)

        put("dailyLimitG", dailyLimitG)
        put("dailyHardLimitG", dailyHardLimitG)
        put("dailyBudgetKc", dailyBudgetKc)
        put("monthlyBudgetKc", monthlyBudgetKc)
        put("cooldownMin", cooldownMin)
        put("intervalComplianceThresholdH", intervalComplianceThresholdH)
        put("haptics", haptics)

        put("bodyWeightKg", bodyWeightKg)
        put("mitTMaxH", mitTMaxH)
        put("mitHalfLifeAlphaH", mitHalfLifeAlphaH)
        put("mitHalfLifeBetaH", mitHalfLifeBetaH)
        put("defaultMitPct", defaultMitPct)
        put("defaultSevenOhPct", defaultSevenOhPct)

        put("toleranceRecoveryPerDay", toleranceRecoveryPerDay)
        put("tolerancePenaltyLimit", tolerancePenaltyLimit)
        put("tolerancePenaltyInterval", tolerancePenaltyInterval)
        put("toleranceConsecutiveDaysPenalty", toleranceConsecutiveDaysPenalty)
        put("tbreakActive", tbreakActive)
        tbreakStartedAt?.let { put("tbreakStartedAt", it) }
        put("tbreakGoalHours", tbreakGoalHours)

        put("dailyWaterGoalMl", dailyWaterGoalMl)
        put("waterPerGramKratomMl", waterPerGramKratomMl)
        put("waterQuickMl1", waterQuickMl1)
        put("waterQuickMl2", waterQuickMl2)

        put("defaultDoseG", defaultDoseG)
        put("quickDosePresets", JSONArray(quickDosePresets))
        put("autoSelectActivePackage", autoSelectActivePackage)
        put("doseStepDefault", doseStepDefault)

        put("dayStartHour", dayStartHour)
        put("confirmBeforeDelete", confirmBeforeDelete)
        put("soundsEnabled", soundsEnabled)
        put("enableNotifications", enableNotifications)

        put("defaultReconcileMethod", defaultReconcileMethod)
        put("reconcileAutoIncludeUnassigned", reconcileAutoIncludeUnassigned)
        put("lowStockWarningG", lowStockWarningG)
        put("defaultPackageWeightG", defaultPackageWeightG)
        put("defaultPricePerG", defaultPricePerG)

        put("taperingEnabled", taperingEnabled)
        put("taperingStartG", taperingStartG)
        put("taperingTargetG", taperingTargetG)
        put("taperingWeeklyReductionPct", taperingWeeklyReductionPct)
        put("taperingStepG", taperingStepG)
        put("taperingDaysInterval", taperingDaysInterval)
        taperingStartedAt?.let { put("taperingStartedAt", it) }

        put("showAlkaloids", showAlkaloids)
        put("showFinances", showFinances)
        put("enableBeverages", enableBeverages)
        put("enableHydration", enableHydration)
        put("enableMoodRating", enableMoodRating)
        put("enableHeatmap", enableHeatmap)
        put("enableQuickLog", enableQuickLog)
        put("enablePharmacokinetics", enablePharmacokinetics)
        put("enableReceptorScore", enableReceptorScore)
        put("enableHourlyDistribution", enableHourlyDistribution)
        put("enableStrainRotationAdvice", enableStrainRotationAdvice)
        put("diversityWarningThreshold", diversityWarningThreshold)
        put("enableAchievements", enableAchievements)
        put("enableCalendarView", enableCalendarView)
        put("enableSmartInsights", enableSmartInsights)

        lastStrainId?.let { put("lastStrainId", it) }
        put("lastAmount", lastAmount)
        put("lastBeverageId", lastBeverageId)
        lastBackupAt?.let { put("lastBackupAt", it) }
        put("autoBackupReminderDays", autoBackupReminderDays)
        put("onboarded", onboarded)
    }

    companion object {
        fun fromJson(json: JSONObject): AppSettings {
            val presets = mutableListOf<Double>()
            val pArr = json.optJSONArray("quickDosePresets")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    presets.add(pArr.getDouble(i))
                }
            } else {
                val pStr = json.optString("quickDosePresets", "")
                if (pStr.isNotEmpty()) {
                    pStr.split(",").forEach {
                        it.trim().toDoubleOrNull()?.let { d -> presets.add(d) }
                    }
                }
                if (presets.isEmpty()) {
                    presets.addAll(listOf(1.0, 2.0, 2.5, 3.0))
                }
            }

            return AppSettings(
                currency = json.optString("currency", "Kč"),
                theme = json.optString("theme", "emerald"),
                customAccentHex = json.optString("customAccentHex", "#30D158"),
                dashboardDialStyle = json.optString("dashboardDialStyle", "precision"),
                dashboardShowSparklines = json.optBoolean("dashboardShowSparklines", true),
                defaultAnalyticsSubTab = json.optString("defaultAnalyticsSubTab", "summary"),
                privacyMode = json.optBoolean("privacyMode", false),
                uiDensity = json.optString("uiDensity", "normal"),
                fontScale = json.optString("fontScale", "normal"),
                startTab = json.optString("startTab", "dashboard"),
                timeFormat24 = json.optBoolean("timeFormat24", true),
                weekStartsMonday = json.optBoolean("weekStartsMonday", true),
                dashRecentDosesCount = json.optInt("dashRecentDosesCount", 2),

                dashShowQuickRow = json.optBoolean("dashShowQuickRow", true),
                dashShowHud = json.optBoolean("dashShowHud", true),
                dashShowInsight = json.optBoolean("dashShowInsight", true),
                dashShowHero = json.optBoolean("dashShowHero", true),
                dashShowTelem = json.optBoolean("dashShowTelem", true),
                dashShowTimeline = json.optBoolean("dashShowTimeline", true),
                dashShowWater = json.optBoolean("dashShowWater", true),
                dashShowActivity = json.optBoolean("dashShowActivity", true),
                dashShowStats = json.optBoolean("dashShowStats", true),

                histCompactRows = json.optBoolean("histCompactRows", false),
                histShowFilters = json.optBoolean("histShowFilters", true),

                modalShowPreview = json.optBoolean("modalShowPreview", true),
                modalShowMitEstimate = json.optBoolean("modalShowMitEstimate", true),
                modalShowNoteTags = json.optBoolean("modalShowNoteTags", true),
                quickLogConfirm = json.optBoolean("quickLogConfirm", false),

                dailyLimitG = json.optDouble("dailyLimitG", 10.0),
                dailyHardLimitG = json.optDouble("dailyHardLimitG", 14.0),
                dailyBudgetKc = json.optDouble("dailyBudgetKc", 150.0),
                monthlyBudgetKc = json.optDouble("monthlyBudgetKc", 4500.0),
                cooldownMin = json.optInt("cooldownMin", 240),
                intervalComplianceThresholdH = json.optDouble("intervalComplianceThresholdH", 4.0),
                haptics = json.optBoolean("haptics", true),

                bodyWeightKg = json.optDouble("bodyWeightKg", 75.0),
                mitTMaxH = json.optDouble("mitTMaxH", 1.2),
                mitHalfLifeAlphaH = json.optDouble("mitHalfLifeAlphaH", 3.8),
                mitHalfLifeBetaH = json.optDouble("mitHalfLifeBetaH", 24.0),
                defaultMitPct = json.optDouble("defaultMitPct", 1.45),
                defaultSevenOhPct = json.optDouble("defaultSevenOhPct", 0.025),

                toleranceRecoveryPerDay = json.optDouble("toleranceRecoveryPerDay", 15.0),
                tolerancePenaltyLimit = json.optDouble("tolerancePenaltyLimit", 30.0),
                tolerancePenaltyInterval = json.optDouble("tolerancePenaltyInterval", 35.0),
                toleranceConsecutiveDaysPenalty = json.optDouble("toleranceConsecutiveDaysPenalty", 15.0),
                tbreakActive = json.optBoolean("tbreakActive", false),
                tbreakStartedAt = if (json.has("tbreakStartedAt") && !json.isNull("tbreakStartedAt")) json.getLong("tbreakStartedAt") else null,
                tbreakGoalHours = json.optDouble("tbreakGoalHours", 72.0),

                dailyWaterGoalMl = json.optInt("dailyWaterGoalMl", 2500),
                waterPerGramKratomMl = json.optInt("waterPerGramKratomMl", 200),
                waterQuickMl1 = json.optInt("waterQuickMl1", 250),
                waterQuickMl2 = json.optInt("waterQuickMl2", 500),

                defaultDoseG = json.optDouble("defaultDoseG", 2.0),
                quickDosePresets = presets,
                autoSelectActivePackage = json.optBoolean("autoSelectActivePackage", true),
                doseStepDefault = json.optDouble("doseStepDefault", 0.5),

                dayStartHour = json.optInt("dayStartHour", 0),
                confirmBeforeDelete = json.optBoolean("confirmBeforeDelete", true),
                soundsEnabled = json.optBoolean("soundsEnabled", false),
                enableNotifications = json.optBoolean("enableNotifications", false),

                defaultReconcileMethod = json.optString("defaultReconcileMethod", "proportional"),
                reconcileAutoIncludeUnassigned = json.optBoolean("reconcileAutoIncludeUnassigned", false),
                lowStockWarningG = json.optDouble("lowStockWarningG", 20.0),
                defaultPackageWeightG = json.optDouble("defaultPackageWeightG", 100.0),
                defaultPricePerG = json.optDouble("defaultPricePerG", 8.0),

                taperingEnabled = json.optBoolean("taperingEnabled", false),
                taperingStartG = json.optDouble("taperingStartG", 12.0),
                taperingTargetG = json.optDouble("taperingTargetG", 4.0),
                taperingWeeklyReductionPct = json.optDouble("taperingWeeklyReductionPct", 10.0),
                taperingStepG = json.optDouble("taperingStepG", 0.5),
                taperingDaysInterval = json.optInt("taperingDaysInterval", 3),
                taperingStartedAt = if (json.has("taperingStartedAt") && !json.isNull("taperingStartedAt")) json.getLong("taperingStartedAt") else null,

                showAlkaloids = json.optBoolean("showAlkaloids", true),
                showFinances = json.optBoolean("showFinances", true),
                enableBeverages = json.optBoolean("enableBeverages", true),
                enableHydration = json.optBoolean("enableHydration", true),
                enableMoodRating = json.optBoolean("enableMoodRating", true),
                enableHeatmap = json.optBoolean("enableHeatmap", true),
                enableQuickLog = json.optBoolean("enableQuickLog", true),
                enablePharmacokinetics = json.optBoolean("enablePharmacokinetics", true),
                enableReceptorScore = json.optBoolean("enableReceptorScore", true),
                enableHourlyDistribution = json.optBoolean("enableHourlyDistribution", true),
                enableStrainRotationAdvice = json.optBoolean("enableStrainRotationAdvice", true),
                diversityWarningThreshold = json.optInt("diversityWarningThreshold", 3),
                enableAchievements = json.optBoolean("enableAchievements", true),
                enableCalendarView = json.optBoolean("enableCalendarView", true),
                enableSmartInsights = json.optBoolean("enableSmartInsights", true),

                lastStrainId = if (json.has("lastStrainId") && !json.isNull("lastStrainId")) json.getString("lastStrainId") else null,
                lastAmount = json.optDouble("lastAmount", 2.0),
                lastBeverageId = json.optString("lastBeverageId", "bev_water"),
                lastBackupAt = if (json.has("lastBackupAt") && !json.isNull("lastBackupAt")) json.getLong("lastBackupAt") else null,
                autoBackupReminderDays = json.optInt("autoBackupReminderDays", 7),
                onboarded = json.optBoolean("onboarded", false)
            )
        }
    }
}

data class AppState(
    val settings: AppSettings = AppSettings(),
    val strains: List<Strain> = defaultStrains(),
    val packages: List<KratomPackage> = defaultPackages(),
    val beverages: List<Beverage> = defaultBeverages(),
    val doses: List<Dose> = emptyList(),
    val waterLogs: List<WaterLog> = emptyList(),
    val tbreakHistory: List<TBreakRecord> = emptyList(),
    val achievements: Map<String, Long> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", "kratom-tracker-v3")
        put("settings", settings.toJson())
        put("strains", JSONArray(strains.map { it.toJson() }))
        put("packages", JSONArray(packages.map { it.toJson() }))
        put("beverages", JSONArray(beverages.map { it.toJson() }))
        put("doses", JSONArray(doses.map { it.toJson() }))
        put("waterLogs", JSONArray(waterLogs.map { it.toJson() }))
        put("tbreakHistory", JSONArray(tbreakHistory.map { it.toJson() }))
        val achObj = JSONObject()
        achievements.forEach { (k, v) -> achObj.put(k, v) }
        put("achievements", achObj)
    }

    companion object {
        fun fromJson(json: JSONObject): AppState {
            val settings = json.optJSONObject("settings")?.let { AppSettings.fromJson(it) } ?: AppSettings()

            val strains = mutableListOf<Strain>()
            json.optJSONArray("strains")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { strains.add(Strain.fromJson(it)) }
                }
            }

            val packages = mutableListOf<KratomPackage>()
            json.optJSONArray("packages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { packages.add(KratomPackage.fromJson(it)) }
                }
            }

            val beverages = mutableListOf<Beverage>()
            json.optJSONArray("beverages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { beverages.add(Beverage.fromJson(it)) }
                }
            }

            val doses = mutableListOf<Dose>()
            json.optJSONArray("doses")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { doses.add(Dose.fromJson(it)) }
                }
            }

            val waterLogs = mutableListOf<WaterLog>()
            json.optJSONArray("waterLogs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { waterLogs.add(WaterLog.fromJson(it)) }
                }
            }

            val tbreakHistory = mutableListOf<TBreakRecord>()
            json.optJSONArray("tbreakHistory")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { tbreakHistory.add(TBreakRecord.fromJson(it)) }
                }
            }

            val achievements = mutableMapOf<String, Long>()
            json.optJSONObject("achievements")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    achievements[k] = obj.optLong(k, System.currentTimeMillis())
                }
            }

            // Normalization per migration rules
            if (beverages.none { it.id == "bev_water" }) {
                beverages.add(0, Beverage("bev_water", "Voda", "💧", 0.0, "#0A84FF", "Základní tekutina"))
            }

            var finalSettings = settings
            if (finalSettings.tbreakActive && finalSettings.tbreakStartedAt == null) {
                finalSettings = finalSettings.copy(tbreakActive = false)
            }

            return AppState(
                settings = finalSettings,
                strains = if (strains.isEmpty()) defaultStrains() else strains,
                packages = packages,
                beverages = if (beverages.isEmpty()) defaultBeverages() else beverages,
                doses = doses,
                waterLogs = waterLogs,
                tbreakHistory = tbreakHistory,
                achievements = achievements
            )
        }
    }
}

fun defaultStrains(): List<Strain> = listOf(
    Strain(
        id = "s_gbo",
        name = "Zelený Borneo",
        pricePerG = 8.0,
        packageSizeG = 100.0,
        mitPct = 1.55,
        sevenOhPct = 0.025,
        color = "#30D158",
        note = "Vyvážený střed a čistá nálada"
    ),
    Strain(
        id = "s_bth",
        name = "Bílý Thailand",
        pricePerG = 7.0,
        packageSizeG = 50.0,
        mitPct = 1.45,
        sevenOhPct = 0.02,
        color = "#E5E5EA",
        note = "Ranní energie a soustředění"
    ),
    Strain(
        id = "s_rba",
        name = "Červený Bali",
        pricePerG = 7.0,
        packageSizeG = 50.0,
        mitPct = 1.20,
        sevenOhPct = 0.035,
        color = "#FF453A",
        note = "Večerní zklidnění a úleva"
    ),
    Strain(
        id = "s_yvi",
        name = "Žlutý Vietnam",
        pricePerG = 8.0,
        packageSizeG = 50.0,
        mitPct = 1.35,
        sevenOhPct = 0.02,
        color = "#FF9F0A",
        note = "Pohoda a relax"
    )
)

fun defaultPackages(): List<KratomPackage> = listOf(
    KratomPackage(
        id = "pkg_seed_gbo",
        strainId = "s_gbo",
        name = "Zelený Borneo 100g",
        initialWeightG = 100.0,
        remainingWeightG = 100.0,
        costTotal = 800.0,
        pricePerG = 8.0,
        status = "active",
        openedAt = System.currentTimeMillis() - 86400000L * 2
    ),
    KratomPackage(
        id = "pkg_seed_bth",
        strainId = "s_bth",
        name = "Bílý Thailand 50g",
        initialWeightG = 50.0,
        remainingWeightG = 50.0,
        costTotal = 350.0,
        pricePerG = 7.0,
        status = "unopened"
    )
)

fun defaultBeverages(): List<Beverage> = listOf(
    Beverage(id = "bev_water", name = "Voda", icon = "💧", cost = 0.0, color = "#0A84FF", note = "Základní tekutina"),
    Beverage(id = "bev_orange", name = "Pomerančový džus", icon = "🍊", cost = 15.0, color = "#FF9F0A", note = "Zjemňuje chuť a podporuje vstřebávání"),
    Beverage(id = "bev_grapefruit", name = "Grepový džus", icon = "🍋", cost = 20.0, color = "#FFD60A", note = "Přírodní podpora"),
    Beverage(id = "bev_tea", name = "Teplý bylinný čaj", icon = "🍵", cost = 8.0, color = "#30D158", note = "Šetrný k žaludku"),
    Beverage(id = "bev_soda", name = "Limonáda / Kofola", icon = "🥤", cost = 18.0, color = "#BF5AF2", note = "Sladký nápoj")
)

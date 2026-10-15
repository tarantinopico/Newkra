package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.model.AppSettings
import com.example.model.AppState
import com.example.model.Beverage
import com.example.model.Dose
import com.example.model.KratomPackage
import com.example.model.Strain
import com.example.model.TBreakRecord
import com.example.model.WaterLog
import com.example.model.defaultBeverages
import com.example.model.defaultPackages
import com.example.model.defaultStrains
import com.example.util.AudioFeedback
import com.example.util.applyReconciliation
import com.example.util.checkAchievements
import com.example.util.exportCsv
import com.example.util.r2
import com.example.util.undoReconciliation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.math.max

class AppRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val stateFile = File(context.filesDir, "app_state.json")

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        createNotificationChannel()
        loadState()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kratom Tracker Upozornění",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Upozornění na vypršení cooldownu a T-Breaku"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun loadState() {
        try {
            if (stateFile.exists()) {
                val jsonStr = stateFile.readText()
                val jsonObj = JSONObject(jsonStr)
                val loadedState = AppState.fromJson(jsonObj)
                _state.value = loadedState
            } else {
                // Initialize clean default state
                val initial = AppState(
                    settings = AppSettings(),
                    strains = defaultStrains(),
                    packages = defaultPackages(),
                    beverages = defaultBeverages(),
                    doses = emptyList(),
                    waterLogs = emptyList(),
                    tbreakHistory = emptyList(),
                    achievements = emptyMap()
                )
                _state.value = initial
                saveStateInternal(initial)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = AppState(
                settings = AppSettings(),
                strains = defaultStrains(),
                packages = defaultPackages(),
                beverages = defaultBeverages()
            )
            _state.value = fallback
        }
    }

    private fun persistState() {
        val current = _state.value
        scope.launch {
            saveStateInternal(current)
        }
    }

    private fun saveStateInternal(stateToSave: AppState) {
        try {
            val jsonStr = stateToSave.toJson().toString(2)
            stateFile.writeText(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- DOSE OPERATIONS ---
    fun addDose(dose: Dose) {
        val curr = _state.value
        val updatedDoses = curr.doses + dose

        // Deduct from package if applicable
        val updatedPackages = curr.packages.map { pkg ->
            if (dose.packageId != null && pkg.id == dose.packageId) {
                val newRemaining = max(0.0, r2(pkg.remainingWeightG - dose.amountG))
                val isUnopened = pkg.status == "unopened"
                pkg.copy(
                    remainingWeightG = newRemaining,
                    status = if (isUnopened) "active" else pkg.status,
                    openedAt = if (isUnopened) System.currentTimeMillis() else pkg.openedAt
                )
            } else {
                pkg
            }
        }

        val updatedSettings = curr.settings.copy(
            lastStrainId = dose.strainId,
            lastAmount = dose.amountG,
            lastBeverageId = dose.beverageId
        )

        val newState = curr.copy(
            doses = updatedDoses,
            packages = updatedPackages,
            settings = updatedSettings
        )
        val withAchievements = newState.copy(achievements = checkAchievements(newState))
        _state.value = withAchievements
        persistState()

        if (curr.settings.soundsEnabled) {
            AudioFeedback.playSuccess()
        }
    }

    fun updateDose(oldDose: Dose, newDose: Dose) {
        val curr = _state.value
        val diffG = newDose.amountG - oldDose.amountG

        val updatedPackages = curr.packages.map { pkg ->
            if (newDose.packageId != null && pkg.id == newDose.packageId) {
                val newRem = max(0.0, r2(pkg.remainingWeightG - diffG))
                pkg.copy(remainingWeightG = newRem)
            } else {
                pkg
            }
        }

        val updatedDoses = curr.doses.map { if (it.id == oldDose.id) newDose else it }
        val newState = curr.copy(doses = updatedDoses, packages = updatedPackages)
        val withAchievements = newState.copy(achievements = checkAchievements(newState))
        _state.value = withAchievements
        persistState()
    }

    fun deleteDose(dose: Dose) {
        val curr = _state.value
        // Restore remaining weight in package if assigned
        val updatedPackages = curr.packages.map { pkg ->
            if (dose.packageId != null && pkg.id == dose.packageId) {
                val restored = r2(pkg.remainingWeightG + dose.amountG)
                pkg.copy(remainingWeightG = restored)
            } else {
                pkg
            }
        }

        val updatedDoses = curr.doses.filter { it.id != dose.id }
        val newState = curr.copy(doses = updatedDoses, packages = updatedPackages)
        _state.value = newState
        persistState()
    }

    // --- WATER LOGS ---
    fun addWaterLog(amountMl: Int) {
        val curr = _state.value
        val log = WaterLog(amountMl = amountMl)
        val updatedLogs = curr.waterLogs + log
        val newState = curr.copy(waterLogs = updatedLogs)
        val withAchievements = newState.copy(achievements = checkAchievements(newState))
        _state.value = withAchievements
        persistState()

        if (curr.settings.soundsEnabled) {
            AudioFeedback.playSuccess()
        }
    }

    fun deleteWaterLog(id: String) {
        val curr = _state.value
        val updatedLogs = curr.waterLogs.filter { it.id != id }
        _state.value = curr.copy(waterLogs = updatedLogs)
        persistState()
    }

    // --- PACKAGES ---
    fun addPackage(pkg: KratomPackage) {
        val curr = _state.value
        _state.value = curr.copy(packages = curr.packages + pkg)
        persistState()
    }

    fun updatePackage(pkg: KratomPackage) {
        val curr = _state.value
        _state.value = curr.copy(packages = curr.packages.map { if (it.id == pkg.id) pkg else it })
        persistState()
    }

    fun deletePackage(pkgId: String) {
        val curr = _state.value
        _state.value = curr.copy(packages = curr.packages.filter { it.id != pkgId })
        persistState()
    }

    fun openPackage(pkgId: String) {
        val curr = _state.value
        val updated = curr.packages.map {
            if (it.id == pkgId) it.copy(status = "active", openedAt = System.currentTimeMillis()) else it
        }
        _state.value = curr.copy(packages = updated)
        persistState()
    }

    // --- RECONCILIATION ---
    fun reconcilePackage(
        pkg: KratomPackage,
        targetWeightG: Double,
        method: String,
        selectedDoseIds: Set<String>
    ) {
        val curr = _state.value
        val (finishedPkg, updatedDoses) = applyReconciliation(
            pkg = pkg,
            allDoses = curr.doses,
            targetWeightG = targetWeightG,
            method = method,
            selectedDoseIds = selectedDoseIds
        )
        val updatedPackages = curr.packages.map { if (it.id == pkg.id) finishedPkg else it }
        val newState = curr.copy(packages = updatedPackages, doses = updatedDoses)
        val withAchievements = newState.copy(achievements = checkAchievements(newState))
        _state.value = withAchievements
        persistState()

        if (curr.settings.soundsEnabled) {
            AudioFeedback.playSuccess()
        }
    }

    fun undoPackageReconciliation(pkg: KratomPackage) {
        val curr = _state.value
        val (restoredPkg, updatedDoses) = undoReconciliation(pkg, curr.doses)
        val updatedPackages = curr.packages.map { if (it.id == pkg.id) restoredPkg else it }
        _state.value = curr.copy(packages = updatedPackages, doses = updatedDoses)
        persistState()
    }

    // --- T-BREAK ---
    fun startTBreak(goalHours: Double = 72.0) {
        val curr = _state.value
        val updatedSettings = curr.settings.copy(
            tbreakActive = true,
            tbreakStartedAt = System.currentTimeMillis(),
            tbreakGoalHours = goalHours
        )
        _state.value = curr.copy(settings = updatedSettings)
        persistState()
    }

    fun finishTBreak() {
        val curr = _state.value
        val startedAt = curr.settings.tbreakStartedAt ?: return
        val endedAt = System.currentTimeMillis()
        val record = TBreakRecord(
            startedAt = startedAt,
            endedAt = endedAt,
            goalHours = curr.settings.tbreakGoalHours
        )

        val updatedSettings = curr.settings.copy(
            tbreakActive = false,
            tbreakStartedAt = null
        )
        val updatedHistory = curr.tbreakHistory + record
        val newState = curr.copy(settings = updatedSettings, tbreakHistory = updatedHistory)
        val withAchievements = newState.copy(achievements = checkAchievements(newState))
        _state.value = withAchievements
        persistState()

        if (curr.settings.soundsEnabled) {
            if (record.completed) AudioFeedback.playFanfare() else AudioFeedback.playSuccess()
        }
    }

    // --- SETTINGS ---
    fun updateSettings(newSettings: AppSettings) {
        val curr = _state.value
        val newState = curr.copy(settings = newSettings)
        _state.value = newState
        persistState()
    }

    // --- STRAINS CATALOG ---
    fun addStrain(strain: Strain) {
        val curr = _state.value
        _state.value = curr.copy(strains = curr.strains + strain)
        persistState()
    }

    fun updateStrain(strain: Strain) {
        val curr = _state.value
        _state.value = curr.copy(strains = curr.strains.map { if (it.id == strain.id) strain else it })
        persistState()
    }

    fun deleteStrain(strainId: String): Boolean {
        val curr = _state.value
        if (curr.strains.size <= 1) return false
        _state.value = curr.copy(strains = curr.strains.filter { it.id != strainId })
        persistState()
        return true
    }

    // --- BEVERAGES CATALOG ---
    fun addBeverage(beverage: Beverage) {
        val curr = _state.value
        _state.value = curr.copy(beverages = curr.beverages + beverage)
        persistState()
    }

    fun updateBeverage(beverage: Beverage) {
        val curr = _state.value
        _state.value = curr.copy(beverages = curr.beverages.map { if (it.id == beverage.id) beverage else it })
        persistState()
    }

    fun deleteBeverage(beverageId: String): Boolean {
        if (beverageId == "bev_water") return false
        val curr = _state.value
        _state.value = curr.copy(beverages = curr.beverages.filter { it.id != beverageId })
        persistState()
        return true
    }

    // --- IMPORT / EXPORT & BACKUP ---
    fun exportJsonString(): String {
        val curr = _state.value
        val updatedSettings = curr.settings.copy(lastBackupAt = System.currentTimeMillis())
        val stateWithBackup = curr.copy(settings = updatedSettings)
        val withAch = stateWithBackup.copy(achievements = checkAchievements(stateWithBackup))
        _state.value = withAch
        persistState()
        return withAch.toJson().toString(2)
    }

    fun exportCsvString(): String {
        val curr = _state.value
        val updatedSettings = curr.settings.copy(lastBackupAt = System.currentTimeMillis())
        val stateWithBackup = curr.copy(settings = updatedSettings)
        val withAch = stateWithBackup.copy(achievements = checkAchievements(stateWithBackup))
        _state.value = withAch
        persistState()
        return exportCsv(withAch)
    }

    fun importJsonString(jsonStr: String): Boolean {
        return try {
            val jsonObj = JSONObject(jsonStr)
            val imported = AppState.fromJson(jsonObj)
            _state.value = imported
            persistState()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetAllData() {
        val fresh = AppState(
            settings = AppSettings(),
            strains = defaultStrains(),
            packages = defaultPackages(),
            beverages = defaultBeverages()
        )
        _state.value = fresh
        persistState()
    }

    companion object {
        const val CHANNEL_ID = "kratom_tracker_channel"
    }
}

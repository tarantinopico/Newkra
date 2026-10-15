package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.AppRepository
import com.example.model.AppSettings
import com.example.model.AppState
import com.example.model.Beverage
import com.example.model.Dose
import com.example.model.KratomPackage
import com.example.model.Strain
import com.example.util.r2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application.applicationContext)
    val state: StateFlow<AppState> = repository.state

    // UI Navigation State
    private val _currentTab = MutableStateFlow("dashboard") // dashboard | stock | history | analytics | settings
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _doseModalOpen = MutableStateFlow(false)
    val doseModalOpen: StateFlow<Boolean> = _doseModalOpen.asStateFlow()

    private val _editingDose = MutableStateFlow<Dose?>(null)
    val editingDose: StateFlow<Dose?> = _editingDose.asStateFlow()

    fun navigateTo(tab: String) {
        _currentTab.value = tab
    }

    fun openNewDoseModal() {
        _editingDose.value = null
        _doseModalOpen.value = true
    }

    fun openEditDoseModal(dose: Dose) {
        _editingDose.value = dose
        _doseModalOpen.value = true
    }

    fun closeDoseModal() {
        _doseModalOpen.value = false
        _editingDose.value = null
    }

    fun saveDose(dose: Dose) {
        val old = _editingDose.value
        if (old != null) {
            repository.updateDose(old, dose)
        } else {
            repository.addDose(dose)
        }
        closeDoseModal()
    }

    fun deleteDose(dose: Dose) {
        repository.deleteDose(dose)
        closeDoseModal()
    }

    fun restoreDose(dose: Dose) {
        repository.addDose(dose)
    }

    fun quickDose(amountG: Double) {
        val curr = state.value
        val strain = curr.strains.find { it.id == curr.settings.lastStrainId } ?: curr.strains.first()
        val availPacks = curr.packages.filter { it.strainId == strain.id && it.status != "finished" }
        val pack = if (curr.settings.autoSelectActivePackage) availPacks.find { it.status == "active" } else null
        val bev = curr.beverages.find { it.id == curr.settings.lastBeverageId } ?: curr.beverages.first()

        val pPerG = pack?.pricePerG ?: strain.pricePerG
        val bevCost = if (curr.settings.enableBeverages) bev.cost else 0.0
        val cost = r2(amountG * pPerG + bevCost)
        val mit = r2(amountG * strain.mitPct * 10.0)
        val sevenOh = r2(amountG * strain.sevenOhPct * 10.0)

        val dose = Dose(
            strainId = strain.id,
            strainName = strain.name,
            strainColor = strain.color,
            packageId = pack?.id,
            amountG = amountG,
            pricePerG = pPerG,
            cost = cost,
            beverageId = bev.id,
            beverageName = bev.name,
            beverageIcon = bev.icon,
            beverageCost = bevCost,
            mitPct = strain.mitPct,
            sevenOhPct = strain.sevenOhPct,
            mitMg = mit,
            sevenOhMg = sevenOh
        )
        repository.addDose(dose)
    }

    fun repeatLastDose() {
        val last = state.value.doses.maxByOrNull { it.timestamp } ?: return
        val newDose = last.copy(
            id = "d_" + System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            calibrated = false,
            originalAmountG = null,
            calibrationDiff = null
        )
        repository.addDose(newDose)
    }

    fun quickWater(amountMl: Int) {
        repository.addWaterLog(amountMl)
    }

    fun togglePrivacyMode() {
        val curr = state.value.settings
        repository.updateSettings(curr.copy(privacyMode = !curr.privacyMode))
    }

    // Package operations
    fun addPackage(pkg: KratomPackage) = repository.addPackage(pkg)
    fun updatePackage(pkg: KratomPackage) = repository.updatePackage(pkg)
    fun deletePackage(id: String) = repository.deletePackage(id)
    fun openPackage(id: String) = repository.openPackage(id)
    fun reconcilePackage(pkg: KratomPackage, targetW: Double, method: String, selectedDoseIds: Set<String>) =
        repository.reconcilePackage(pkg, targetW, method, selectedDoseIds)
    fun undoReconciliation(pkg: KratomPackage) = repository.undoPackageReconciliation(pkg)

    // T-Break operations
    fun startTBreak(goalHours: Double = 72.0) = repository.startTBreak(goalHours)
    fun finishTBreak() = repository.finishTBreak()

    // Settings & Catalogs
    fun updateSettings(settings: AppSettings) = repository.updateSettings(settings)
    fun addStrain(strain: Strain) = repository.addStrain(strain)
    fun updateStrain(strain: Strain) = repository.updateStrain(strain)
    fun deleteStrain(id: String) = repository.deleteStrain(id)
    fun addBeverage(beverage: Beverage) = repository.addBeverage(beverage)
    fun updateBeverage(beverage: Beverage) = repository.updateBeverage(beverage)
    fun deleteBeverage(id: String) = repository.deleteBeverage(id)

    // Import/Export
    fun exportJson(): String = repository.exportJsonString()
    fun exportCsv(): String = repository.exportCsvString()
    fun importJson(json: String): Boolean = repository.importJsonString(json)
    fun resetAllData() = repository.resetAllData()
}

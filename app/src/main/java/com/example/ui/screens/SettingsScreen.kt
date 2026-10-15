package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.AppState
import com.example.model.Beverage
import com.example.model.STRAIN_PALETTE
import com.example.model.Strain
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface1
import com.example.ui.theme.CardSurface2
import com.example.ui.theme.ColorDanger
import com.example.ui.theme.ColorSuccess
import com.example.ui.theme.ColorWarning
import com.example.ui.theme.ColorWater
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.parseHexColor
import com.example.util.formatDate
import com.example.util.r1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appState: AppState,
    onUpdateSettings: (AppSettings) -> Unit,
    onAddStrain: (Strain) -> Unit,
    onUpdateStrain: (Strain) -> Unit,
    onDeleteStrain: (String) -> Boolean,
    onAddBeverage: (Beverage) -> Unit,
    onUpdateBeverage: (Beverage) -> Unit,
    onDeleteBeverage: (String) -> Boolean,
    onExportJson: () -> String,
    onExportCsv: () -> String,
    onImportJson: (String) -> Boolean,
    onResetAllData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var settings by remember { mutableStateOf(appState.settings) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Dialogs
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    // Strain/Beverage editing
    var editingStrain by remember { mutableStateOf<Strain?>(null) }
    var showStrainDialog by remember { mutableStateOf(false) }
    var editingBeverage by remember { mutableStateOf<Beverage?>(null) }
    var showBeverageDialog by remember { mutableStateOf(false) }

    fun save(updated: AppSettings) {
        settings = updated
        onUpdateSettings(updated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpět", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AmoledBlack)
            )
        },
        containerColor = AmoledBlack
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. VZHLED & ROZHRANÍ
            item {
                SettingsSectionCard(title = "🎨 Vzhled & Rozhraní") {
                    Text("Motiv a akcentová barva", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val themes = listOf(
                        "emerald" to ("Smaragd" to "#30D158"),
                        "cyan" to ("Tyrkys" to "#64D2FF"),
                        "purple" to ("Fialová" to "#BF5AF2"),
                        "amber" to ("Jantar" to "#FF9F0A"),
                        "rose" to ("Růže" to "#FF375F"),
                        "gold" to ("Zlato" to "#FFD60A"),
                        "dynamic" to ("Material You" to "#3DDC84")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        themes.forEach { (key, pair) ->
                            val (label, hex) = pair
                            val isSel = settings.theme == key
                            Row(
                                modifier = Modifier
                                    .background(if (isSel) parseHexColor(hex).copy(alpha = 0.25f) else CardSurface2, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) parseHexColor(hex) else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { save(settings.copy(theme = key)) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(parseHexColor(hex), CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, color = if (isSel) TextPrimary else TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tachometr style
                    Text("Styl tachometru na ploše", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("precision" to "Precision Dial (28 rysek)", "concentric" to "Kompaktní soustředné kruhy").forEach { (key, label) ->
                            val isSel = settings.dashboardDialStyle == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else CardSurface2, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { save(settings.copy(dashboardDialStyle = key)) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSel) MaterialTheme.colorScheme.primary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsSwitchRow("Mikrografy na ploše", settings.dashboardShowSparklines) { save(settings.copy(dashboardShowSparklines = it)) }
                    SettingsSwitchRow("24hodinový formát času", settings.timeFormat24) { save(settings.copy(timeFormat24 = it)) }
                    SettingsSwitchRow("Týden začíná pondělím", settings.weekStartsMonday) { save(settings.copy(weekStartsMonday = it)) }
                }
            }

            // 2. SESTAVA PLOCHY (DASHBOARD BUILDER - 9 SWITCHŮ)
            item {
                SettingsSectionCard(title = "🧱 Sestava plochy (Dashboard Builder)") {
                    Text("Přizpůsobte si, které karty chcete na hlavní obrazovce vidět:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSwitchRow("Rychlý zápis (čipy)", settings.dashShowQuickRow) { save(settings.copy(dashShowQuickRow = it)) }
                    SettingsSwitchRow("HUD pilulky (série, receptory)", settings.dashShowHud) { save(settings.copy(dashShowHud = it)) }
                    SettingsSwitchRow("Chytrý denní asistent", settings.dashShowInsight) { save(settings.copy(dashShowInsight = it)) }
                    SettingsSwitchRow("Budík limitu (Dial)", settings.dashShowHero) { save(settings.copy(dashShowHero = it)) }
                    SettingsSwitchRow("Telemetrie (cooldown, MIT)", settings.dashShowTelem) { save(settings.copy(dashShowTelem = it)) }
                    SettingsSwitchRow("Dnešní časová osa 24h", settings.dashShowTimeline) { save(settings.copy(dashShowTimeline = it)) }
                    SettingsSwitchRow("Hydratace / Receptory", settings.dashShowWater) { save(settings.copy(dashShowWater = it)) }
                    SettingsSwitchRow("Dnešní aktivita (seznam dávek)", settings.dashShowActivity) { save(settings.copy(dashShowActivity = it)) }
                    SettingsSwitchRow("Spodní statistiky (sklad, útrata)", settings.dashShowStats) { save(settings.copy(dashShowStats = it)) }
                }
            }

            // 3. HISTORIE & DIALOG
            item {
                SettingsSectionCard(title = "📜 Historie a Dialog dávky") {
                    SettingsSwitchRow("Kompaktní řádky v historii", settings.histCompactRows) { save(settings.copy(histCompactRows = it)) }
                    SettingsSwitchRow("Zobrazit filtry v historii", settings.histShowFilters) { save(settings.copy(histShowFilters = it)) }
                    SettingsSwitchRow("Náhled dopadu v dialogu dávky", settings.modalShowPreview) { save(settings.copy(modalShowPreview = it)) }
                    SettingsSwitchRow("Odhad křivky MIT v dialogu", settings.modalShowMitEstimate) { save(settings.copy(modalShowMitEstimate = it)) }
                    SettingsSwitchRow("Rychlé štítky k poznámce", settings.modalShowNoteTags) { save(settings.copy(modalShowNoteTags = it)) }
                    SettingsSwitchRow("Potvrzovat rychlý zápis", settings.quickLogConfirm) { save(settings.copy(quickLogConfirm = it)) }
                }
            }

            // 4. DENNÍ LIMITY A ROZPOČTY
            item {
                SettingsSectionCard(title = "🎯 Denní limity a rozpočty") {
                    SettingsNumberField("Měkký denní limit (g)", settings.dailyLimitG.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(dailyLimitG = v)) }
                    }
                    SettingsNumberField("Tvrdý STOP limit (g)", settings.dailyHardLimitG.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(dailyHardLimitG = v)) }
                    }
                    SettingsNumberField("Denní rozpočet (${settings.currency})", settings.dailyBudgetKc.toInt().toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(dailyBudgetKc = v)) }
                    }
                    SettingsNumberField("Minimální cooldown (min)", settings.cooldownMin.toString()) {
                        it.toIntOrNull()?.let { v -> save(settings.copy(cooldownMin = v)) }
                    }
                    SettingsNumberField("Cíl pro rozestupy (hodin)", settings.intervalComplianceThresholdH.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(intervalComplianceThresholdH = v)) }
                    }
                    SettingsSwitchRow("Haptická odezva (vibrace)", settings.haptics) { save(settings.copy(haptics = it)) }
                    SettingsSwitchRow("Zvuková odezva", settings.soundsEnabled) { save(settings.copy(soundsEnabled = it)) }
                }
            }

            // 5. POKROČILÉ VÝPOČTY, FARMAKOKINETIKA A TOLERANCE
            item {
                SettingsSectionCard(title = "🧬 Pokročilé výpočty, farmakokinetika & tolerance") {
                    Text(
                        text = "Všechny parametry matematických a biologických modelů aplikace:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Farmakokinetika Mitragyninu", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SettingsNumberField("Tělesná hmotnost (kg)", settings.bodyWeightKg.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(bodyWeightKg = v)) }
                    }
                    SettingsNumberField("Nástup maxima Tmax (h)", settings.mitTMaxH.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(mitTMaxH = v)) }
                    }
                    SettingsNumberField("Distribuční poločas Alpha (h)", settings.mitHalfLifeAlphaH.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(mitHalfLifeAlphaH = v)) }
                    }
                    SettingsNumberField("Eliminační poločas Beta (h)", settings.mitHalfLifeBetaH.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(mitHalfLifeBetaH = v)) }
                    }
                    SettingsNumberField("Výchozí MIT v sušině (%)", settings.defaultMitPct.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(defaultMitPct = v)) }
                    }
                    SettingsNumberField("Výchozí 7-OH v sušině (%)", settings.defaultSevenOhPct.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(defaultSevenOhPct = v)) }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Citlivost receptorů & T-Break", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SettingsNumberField("Regenerace za čistý den (%/den)", settings.toleranceRecoveryPerDay.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(toleranceRecoveryPerDay = v)) }
                    }
                    SettingsNumberField("Penalizace za krátký odstup (%)", settings.tolerancePenaltyInterval.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(tolerancePenaltyInterval = v)) }
                    }
                    SettingsNumberField("Penalizace za překročení limitu (%)", settings.tolerancePenaltyLimit.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(tolerancePenaltyLimit = v)) }
                    }
                    SettingsNumberField("Cílový minimální rozestup (h)", settings.intervalComplianceThresholdH.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(intervalComplianceThresholdH = v)) }
                    }
                    SettingsNumberField("Varovný práh rotace stejné odrůdy", settings.diversityWarningThreshold.toString()) {
                        it.toIntOrNull()?.let { v -> save(settings.copy(diversityWarningThreshold = v)) }
                    }
                    SettingsNumberField("Cílová délka T-Break pauzy (h)", settings.tbreakGoalHours.toInt().toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(tbreakGoalHours = v)) }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Hydratační model & kompenzace", color = ColorWater, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SettingsNumberField("Základní denní cíl vody (ml)", settings.dailyWaterGoalMl.toString()) {
                        it.toIntOrNull()?.let { v -> save(settings.copy(dailyWaterGoalMl = v)) }
                    }
                    SettingsNumberField("Kompenzace dehydratace na 1g kratomu (ml)", settings.waterPerGramKratomMl.toString()) {
                        it.toIntOrNull()?.let { v -> save(settings.copy(waterPerGramKratomMl = v)) }
                    }
                    SettingsNumberField("Rychlé tlačítko vody 1 (ml)", settings.waterQuickMl1.toString()) {
                        it.toIntOrNull()?.let { v -> save(settings.copy(waterQuickMl1 = v)) }
                    }
                    SettingsNumberField("Rychlé tlačítko vody 2 (ml)", settings.waterQuickMl2.toString()) {
                        it.toIntOrNull()?.let { v -> save(settings.copy(waterQuickMl2 = v)) }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tapering (plán snižování spotřeby)", color = ColorWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SettingsNumberField("Počáteční denní dávka (g)", settings.taperingStartG.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(taperingStartG = v)) }
                    }
                    SettingsNumberField("Cílová denní dávka (g)", settings.taperingTargetG.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(taperingTargetG = v)) }
                    }
                    SettingsNumberField("Krok snižování dávky (g)", settings.taperingStepG.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(taperingStepG = v)) }
                    }
                    SettingsNumberField("Týdenní redukce (%)", settings.taperingWeeklyReductionPct.toString()) {
                        it.toDoubleOrNull()?.let { v -> save(settings.copy(taperingWeeklyReductionPct = v)) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val reset = settings.copy(
                                bodyWeightKg = 75.0,
                                mitTMaxH = 1.2,
                                mitHalfLifeAlphaH = 3.8,
                                mitHalfLifeBetaH = 24.0,
                                defaultMitPct = 1.45,
                                defaultSevenOhPct = 0.025,
                                toleranceRecoveryPerDay = 15.0,
                                tolerancePenaltyInterval = 35.0,
                                tolerancePenaltyLimit = 30.0,
                                intervalComplianceThresholdH = 4.0,
                                diversityWarningThreshold = 3,
                                dailyWaterGoalMl = 2500,
                                waterPerGramKratomMl = 200,
                                waterQuickMl1 = 250,
                                waterQuickMl2 = 500,
                                taperingStartG = 12.0,
                                taperingTargetG = 4.0,
                                taperingStepG = 0.5,
                                taperingWeeklyReductionPct = 10.0
                            )
                            save(reset)
                            Toast.makeText(context, "Výchozí vědecké konstanty byly obnoveny", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("🔄 Obnovit vědecké výchozí hodnoty modelů", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 6. HLAVNÍ PŘEPÍNAČE MODULŮ
            item {
                SettingsSectionCard(title = "🧩 Zapnuté moduly a funkce") {
                    SettingsSwitchRow("Sledování alkaloidů (MIT / 7-OH)", settings.showAlkaloids) { save(settings.copy(showAlkaloids = it)) }
                    SettingsSwitchRow("Sledování financí (Kč)", settings.showFinances) { save(settings.copy(showFinances = it)) }
                    SettingsSwitchRow("Doprovodné nápoje", settings.enableBeverages) { save(settings.copy(enableBeverages = it)) }
                    SettingsSwitchRow("Sledování hydratace", settings.enableHydration) { save(settings.copy(enableHydration = it)) }
                    SettingsSwitchRow("Hodnocení nálady & účinků", settings.enableMoodRating) { save(settings.copy(enableMoodRating = it)) }
                    SettingsSwitchRow("28denní heatmapa", settings.enableHeatmap) { save(settings.copy(enableHeatmap = it)) }
                    SettingsSwitchRow("Farmakokinetika & Křivky", settings.enablePharmacokinetics) { save(settings.copy(enablePharmacokinetics = it)) }
                    SettingsSwitchRow("Citlivost receptorů & Tolerance", settings.enableReceptorScore) { save(settings.copy(enableReceptorScore = it)) }
                    SettingsSwitchRow("Cirkadiánní 24h rozložení", settings.enableHourlyDistribution) { save(settings.copy(enableHourlyDistribution = it)) }
                    SettingsSwitchRow("Rada k rotaci odrůd", settings.enableStrainRotationAdvice) { save(settings.copy(enableStrainRotationAdvice = it)) }
                    SettingsSwitchRow("Systém úspěchů (gamifikace)", settings.enableAchievements) { save(settings.copy(enableAchievements = it)) }
                    SettingsSwitchRow("Kalendářní měsíční pohled", settings.enableCalendarView) { save(settings.copy(enableCalendarView = it)) }
                }
            }

            // 7. KATALOG ODRŮD KRATOMU
            item {
                SettingsSectionCard(title = "🌿 Katalog odrůd kratomu") {
                    Button(
                        onClick = {
                            editingStrain = null
                            showStrainDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text("+ Přidat nový druh", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    appState.strains.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface2, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(parseHexColor(s.color), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(s.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("${s.pricePerG} Kč/g · ${s.mitPct}% MIT", color = TextMuted, fontSize = 10.sp)
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingStrain = s
                                        showStrainDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val ok = onDeleteStrain(s.id)
                                        if (!ok) Toast.makeText(context, "Nemůžeš smazat jediný zbývající druh", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ColorDanger.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // 8. KATALOG NÁPOJŮ
            if (settings.enableBeverages) {
                item {
                    SettingsSectionCard(title = "🥤 Katalog doprovodných nápojů") {
                        Button(
                            onClick = {
                                editingBeverage = null
                                showBeverageDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text("+ Přidat nový nápoj", color = ColorWater, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        appState.beverages.forEach { b ->
                            val isProtected = b.id == "bev_water"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardSurface2, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(b.icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(b.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text(if (b.cost > 0) "+${b.cost.toInt()} Kč" else "Zdarma", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingBeverage = b
                                            showBeverageDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    if (!isProtected) {
                                        IconButton(
                                            onClick = { onDeleteBeverage(b.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ColorDanger.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // 9. ZÁLOHA A SPRÁVA DAT
            item {
                SettingsSectionCard(title = "💾 Záloha & Správa dat") {
                    Text(
                        text = "Celkem: ${appState.doses.size} dávek · ${appState.packages.size} balení · ${appState.strains.size} druhů",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    val lastBackup = settings.lastBackupAt
                    if (lastBackup != null) {
                        Text("Poslední záloha: ${formatDate(lastBackup)}", color = ColorSuccess, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val jsonStr = onExportJson()
                                clipboardManager.setText(AnnotatedString(jsonStr))
                                Toast.makeText(context, "JSON záloha zkopírována do schránky!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export JSON", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val csvStr = onExportCsv()
                                clipboardManager.setText(AnnotatedString(csvStr))
                                Toast.makeText(context, "CSV export zkopírován do schránky (pro Excel)!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export CSV", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text("Obnovit / Importovat JSON data", color = ColorWater, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorDanger.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, ColorDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Text("Vymazat všechna data", color = ColorDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // IMPORT DIALOG
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import JSON zálohy", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vložte text JSON zálohy ze schránky nebo souboru:", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = CardBorder)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = onImportJson(importJsonText)
                        if (ok) {
                            Toast.makeText(context, "Data úspěšně obnovena!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "Chyba při čtení JSON zálohy", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Importovat", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Zrušit", color = TextSecondary) }
            },
            containerColor = CardSurface1
        )
    }

    // RESET DIALOG
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Vymazat všechna data?", color = ColorDanger, fontWeight = FontWeight.Bold) },
            text = { Text("Tato akce smaže celou historii dávek, balení a vrátí aplikaci do výchozího stavu. Tuto akci nelze vrátit.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetDialog = false
                        Toast.makeText(context, "Data byla vymazána", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)
                ) {
                    Text("Smazat vše", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Zrušit", color = TextSecondary) }
            },
            containerColor = CardSurface1
        )
    }

    // STRAIN DIALOG
    if (showStrainDialog) {
        AddEditStrainDialog(
            initial = editingStrain,
            onDismiss = { showStrainDialog = false },
            onSave = {
                if (editingStrain == null) onAddStrain(it) else onUpdateStrain(it)
                showStrainDialog = false
            }
        )
    }

    // BEVERAGE DIALOG
    if (showBeverageDialog) {
        AddEditBeverageDialog(
            initial = editingBeverage,
            onDismiss = { showBeverageDialog = false },
            onSave = {
                if (editingBeverage == null) onAddBeverage(it) else onUpdateBeverage(it)
                showBeverageDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface1),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun SettingsNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = CardBorder),
            modifier = Modifier.width(90.dp).height(46.dp)
        )
    }
}

// DIALOG: ADD/EDIT STRAIN
@Composable
fun AddEditStrainDialog(initial: Strain?, onDismiss: () -> Unit, onSave: (Strain) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var pricePerG by remember { mutableStateOf(initial?.pricePerG?.toString() ?: "7.0") }
    var mitPct by remember { mutableStateOf(initial?.mitPct?.toString() ?: "1.45") }
    var color by remember { mutableStateOf(initial?.color ?: "#30D158") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nový druh" else "Upravit druh", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Název druhu") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pricePerG, onValueChange = { pricePerG = it }, label = { Text("Kč/g") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = mitPct, onValueChange = { mitPct = it }, label = { Text("% MIT") }, modifier = Modifier.weight(1f))
                }
                Text("Barva", color = TextSecondary, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STRAIN_PALETTE.forEach { hex ->
                        val isSel = color == hex
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(parseHexColor(hex), CircleShape)
                                .border(if (isSel) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Poznámka (volitelná)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val str = Strain(
                            id = initial?.id ?: ("s_" + System.currentTimeMillis()),
                            name = name,
                            pricePerG = pricePerG.toDoubleOrNull() ?: 7.0,
                            mitPct = mitPct.toDoubleOrNull() ?: 1.45,
                            color = color,
                            note = note
                        )
                        onSave(str)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Uložit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit", color = TextSecondary) } },
        containerColor = CardSurface1
    )
}

// DIALOG: ADD/EDIT BEVERAGE
@Composable
fun AddEditBeverageDialog(initial: Beverage?, onDismiss: () -> Unit, onSave: (Beverage) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "🥤") }
    var cost by remember { mutableStateOf(initial?.cost?.toInt()?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nový nápoj" else "Upravit nápoj", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Název nápoje") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("Emoji ikona") }, modifier = Modifier.width(90.dp))
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cena (Kč)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val bev = Beverage(
                            id = initial?.id ?: ("bev_" + System.currentTimeMillis()),
                            name = name,
                            icon = icon,
                            cost = cost.toDoubleOrNull() ?: 0.0
                        )
                        onSave(bev)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Uložit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit", color = TextSecondary) } },
        containerColor = CardSurface1
    )
}

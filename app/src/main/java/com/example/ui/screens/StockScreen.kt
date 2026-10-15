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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppState
import com.example.model.Dose
import com.example.model.KratomPackage
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
import com.example.util.dayKey
import com.example.util.formatDate
import com.example.util.formatTime
import com.example.util.r1
import com.example.util.r2
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    appState: AppState,
    onAddPackage: (KratomPackage) -> Unit,
    onUpdatePackage: (KratomPackage) -> Unit,
    onDeletePackage: (String) -> Unit,
    onOpenPackage: (String) -> Unit,
    onReconcilePackage: (KratomPackage, Double, String, Set<String>) -> Unit,
    onUndoReconciliation: (KratomPackage) -> Unit,
    modifier: Modifier = Modifier
) {
    val packages = appState.packages
    val doses = appState.doses
    val strains = appState.strains
    val settings = appState.settings

    var selectedTab by remember { mutableStateOf("active") } // "active" | "unopened" | "finished"
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<KratomPackage?>(null) }
    var reconcilingPackage by remember { mutableStateOf<KratomPackage?>(null) }

    // Summary calculations
    val totalStockG = packages.filter { it.status != "finished" }.sumOf { it.remainingWeightG }
    val totalStockValueKc = packages.filter { it.status != "finished" }.sumOf {
        if (it.initialWeightG > 0) (it.remainingWeightG / it.initialWeightG) * it.costTotal else 0.0
    }

    val filteredPacks = packages.filter { it.status == selectedTab }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. TOP SUMMARY TILES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardSurface1),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Celkem skladem", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (settings.privacyMode) "••• g" else "${r1(totalStockG)} g",
                        color = ColorSuccess,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (settings.showFinances) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardSurface1),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Hodnota skladu", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.privacyMode) "••• Kč" else "${totalStockValueKc.toInt()} Kč",
                            color = ColorWarning,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. ADD BUTTON & TABS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "active" to "Otevřená (${packages.count { it.status == "active" }})",
                "unopened" to "Zásoba (${packages.count { it.status == "unopened" }})",
                "finished" to "Spotřebovaná (${packages.count { it.status == "finished" }})"
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                tabs.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = selectedTab == key,
                        onClick = { selectedTab = key },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.primary,
                            inactiveContainerColor = CardSurface1,
                            inactiveContentColor = TextSecondary
                        )
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                editingPackage = null
                showAddEditDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp)).testTag("add_package_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Přidat", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ Přidat nové balení", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. PACKAGES LIST
        if (filteredPacks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (selectedTab) {
                        "active" -> "Žádné otevřené balení. Přidejte nové nebo otevřete zásobu."
                        "unopened" -> "Žádné neotevřené zásoby."
                        else -> "Zatím žádná spotřebovaná balení."
                    },
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPacks, key = { it.id }) { pkg ->
                    val strain = strains.find { it.id == pkg.strainId }
                    val strainColor = parseHexColor(strain?.color ?: "#30D158", ColorSuccess)

                    // ETA Calculation
                    val now = System.currentTimeMillis()
                    val past14d = (0..13).map { i -> dayKey(now - i * 86400000L, settings.dayStartHour) }
                    val strainDoses14d = doses.filter { it.strainId == pkg.strainId && past14d.contains(dayKey(it.timestamp, settings.dayStartHour)) }
                    val dailyUse = strainDoses14d.sumOf { it.amountG } / 14.0
                    val etaDays = if (dailyUse > 0) (pkg.remainingWeightG / dailyUse).roundToInt() else 99

                    val assignedDoses = doses.filter { it.packageId == pkg.id }
                    val assignedGrams = assignedDoses.sumOf { it.amountG }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface1),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(strainColor, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(pkg.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(strain?.name ?: "Druh", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingPackage = pkg
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeletePackage(pkg.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ColorDanger.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (pkg.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(pkg.note, color = TextMuted, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Remaining Progress Bar
                            if (pkg.status != "finished") {
                                val remRatio = if (pkg.initialWeightG > 0) (pkg.remainingWeightG / pkg.initialWeightG).toFloat() else 0f
                                val barColor = if (pkg.remainingWeightG < settings.lowStockWarningG) ColorWarning else ColorSuccess

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${r1(pkg.remainingWeightG)} g zbývá", color = barColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Původně ${r1(pkg.initialWeightG)} g", color = TextSecondary, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { remRatio.coerceIn(0f, 1f) },
                                    color = barColor,
                                    trackColor = CardSurface2,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // ETA
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Spotřeba druhu: ${r1(dailyUse)} g/den", color = TextMuted, fontSize = 10.sp)
                                    val etaColor = when {
                                        etaDays <= 7 -> ColorDanger
                                        etaDays <= 14 -> ColorWarning
                                        else -> ColorSuccess
                                    }
                                    Text("Vystačí ≈ $etaDays dní", color = etaColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Stats line
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dávek: ${assignedDoses.size} (${r1(assignedGrams)}g)", color = TextSecondary, fontSize = 11.sp)
                                if (settings.showFinances) {
                                    Text("${pkg.costTotal.toInt()} Kč (${r1(pkg.pricePerG)} Kč/g)", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Actions
                            when (pkg.status) {
                                "active" -> {
                                    Button(
                                        onClick = { reconcilingPackage = pkg },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Balení došlo (Dopočet) ⚖️", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                "unopened" -> {
                                    Button(
                                        onClick = { onOpenPackage(pkg.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Otevřít k odběru 🔓", color = ColorSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                "finished" -> {
                                    if (pkg.calibrated) {
                                        Button(
                                            onClick = { onUndoReconciliation(pkg) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ColorWarning.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = "Zpět", tint = ColorWarning, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Vrátit přepočet (Zpět)", color = ColorWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { reconcilingPackage = pkg },
                                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Přepočítat dávky ⚖️", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // BOTTOMSHEET: RECONCILIATION
    if (reconcilingPackage != null) {
        ReconciliationBottomSheet(
            pkg = reconcilingPackage!!,
            allDoses = doses,
            strains = strains,
            defaultMethod = settings.defaultReconcileMethod,
            onDismiss = { reconcilingPackage = null },
            onApply = { targetW, method, doseIds ->
                onReconcilePackage(reconcilingPackage!!, targetW, method, doseIds)
                reconcilingPackage = null
            }
        )
    }

    // DIALOG: ADD/EDIT PACKAGE
    if (showAddEditDialog) {
        AddEditPackageDialog(
            initialPackage = editingPackage,
            strains = strains,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedPkg ->
                if (editingPackage == null) onAddPackage(savedPkg) else onUpdatePackage(savedPkg)
                showAddEditDialog = false
            }
        )
    }
}

// BOTTOMSHEET RECONCILIATION
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationBottomSheet(
    pkg: KratomPackage,
    allDoses: List<Dose>,
    strains: List<Strain>,
    defaultMethod: String,
    onDismiss: () -> Unit,
    onApply: (targetWeightG: Double, method: String, selectedDoseIds: Set<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var targetWeightText by remember { mutableStateOf(pkg.initialWeightG.toString()) }
    var method by remember { mutableStateOf(defaultMethod) } // "proportional" | "uniform"
    var includeUnassigned by remember { mutableStateOf(false) }

    // Candidate doses
    val directDoses = allDoses.filter { it.packageId == pkg.id }
    val unassignedDoses = allDoses.filter { it.packageId == null && it.strainId == pkg.strainId }

    val candidateDoses = if (includeUnassigned) directDoses + unassignedDoses else directDoses
    val selectedDoseIds = remember { mutableStateListOf<String>().apply { addAll(candidateDoses.map { it.id }) } }

    val targetWeightG = targetWeightText.toDoubleOrNull() ?: pkg.initialWeightG
    val currentSum = candidateDoses.filter { selectedDoseIds.contains(it.id) }.sumOf { it.amountG }
    val diff = r2(targetWeightG - currentSum)
    val factor = if (currentSum > 0) targetWeightG / currentSum else 1.0
    val delta = if (selectedDoseIds.isNotEmpty()) r2(diff / selectedDoseIds.size) else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AmoledBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Dopočet spotřebovaného balení", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "${pkg.name} · Celkový součet odhadů: ${r1(currentSum)} g",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Target weight input
            OutlinedTextField(
                value = targetWeightText,
                onValueChange = { targetWeightText = it },
                label = { Text("Skutečná hmotnost balení (g)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Impact Card
            val (badgeText, badgeBg, badgeFg) = when {
                diff > 0 -> Triple("Dávky se ZVÝŠÍ (+${r1(diff)}g)", ColorSuccess.copy(alpha = 0.2f), ColorSuccess)
                diff < 0 -> Triple("Dávky se SNÍŽÍ (${r1(diff)}g)", ColorWarning.copy(alpha = 0.2f), ColorWarning)
                else -> Triple("Přesná shoda (0g)", ColorWater.copy(alpha = 0.2f), ColorWater)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface1, RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rozdíl dopočtu: ${if (diff > 0) "+$diff" else "$diff"} g", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val diffPct = if (targetWeightG > 0) ((diff / targetWeightG) * 100).roundToInt() else 0
                        Text("Odchylka $diffPct % oproti balení", color = TextSecondary, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(badgeText, color = badgeFg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Method Selector
            Text("Metoda rozpočtení", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (method == "proportional") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface1, RoundedCornerShape(10.dp))
                        .border(1.dp, if (method == "proportional") MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(10.dp))
                        .clickable { method = "proportional" }
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Poměrně", color = if (method == "proportional") TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${r2(factor)}× dle váhy", color = TextMuted, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (method == "uniform") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface1, RoundedCornerShape(10.dp))
                        .border(1.dp, if (method == "uniform") MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(10.dp))
                        .clickable { method = "uniform" }
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Rovnoměrně", color = if (method == "uniform") TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${if (delta >= 0) "+$delta" else "$delta"} g / dávka", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }

            // Unassigned switch if any exist
            if (unassignedDoses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface1, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zahrnout volné dávky druhu (${unassignedDoses.size})", color = TextPrimary, fontSize = 12.sp)
                    Checkbox(
                        checked = includeUnassigned,
                        onCheckedChange = {
                            includeUnassigned = it
                            selectedDoseIds.clear()
                            val newCands = if (it) directDoses + unassignedDoses else directDoses
                            selectedDoseIds.addAll(newCands.map { d -> d.id })
                        },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Doses List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dávky k přepočtu (${selectedDoseIds.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (selectedDoseIds.size == candidateDoses.size) "Odznačit vše" else "Označit vše",
                    color = ColorWater,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (selectedDoseIds.size == candidateDoses.size) {
                            selectedDoseIds.clear()
                        } else {
                            selectedDoseIds.clear()
                            selectedDoseIds.addAll(candidateDoses.map { it.id })
                        }
                    }
                )
            }

            // Candidate Doses with Live Preview
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                candidateDoses.forEach { d ->
                    val isChecked = selectedDoseIds.contains(d.id)
                    val newAmount = if (isChecked) {
                        if (method == "uniform") r2(max(0.1, d.amountG + delta)) else r2(d.amountG * factor)
                    } else d.amountG
                    val diffG = r2(newAmount - d.amountG)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface1, RoundedCornerShape(8.dp))
                            .clickable {
                                if (isChecked) selectedDoseIds.remove(d.id) else selectedDoseIds.add(d.id)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedDoseIds.add(d.id) else selectedDoseIds.remove(d.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Column {
                                Text("${formatDate(d.timestamp)} ${formatTime(d.timestamp)}", color = TextPrimary, fontSize = 12.sp)
                                if (d.packageId == null) {
                                    Text("Volná dávka", color = ColorWarning, fontSize = 10.sp)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("${r1(d.amountG)}g → ${r1(newAmount)}g", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = "${if (diffG >= 0) "+$diffG" else "$diffG"} g",
                                color = if (diffG >= 0) ColorSuccess else ColorWarning,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // Close package without recalibrating doses
                        onApply(targetWeightG, method, emptySet())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pouze uzavřít", color = TextSecondary, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onApply(targetWeightG, method, selectedDoseIds.toSet())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Přepočítat ${selectedDoseIds.size} dávek", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// DIALOG: ADD/EDIT PACKAGE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPackageDialog(
    initialPackage: KratomPackage?,
    strains: List<Strain>,
    onDismiss: () -> Unit,
    onSave: (KratomPackage) -> Unit
) {
    var name by remember { mutableStateOf(initialPackage?.name ?: "") }
    var selectedStrain by remember {
        mutableStateOf(strains.find { it.id == initialPackage?.strainId } ?: strains.first())
    }
    var initialWeightText by remember { mutableStateOf(initialPackage?.initialWeightG?.toString() ?: "100.0") }
    var costTotalText by remember { mutableStateOf(initialPackage?.costTotal?.toString() ?: "800.0") }
    var status by remember { mutableStateOf(initialPackage?.status ?: "active") }
    var note by remember { mutableStateOf(initialPackage?.note ?: "") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface1),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initialPackage == null) "Nové balení" else "Upravit balení",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název / Šarže", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = CardBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                // Strain chips
                Text("Druh kratomu", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    strains.forEach { s ->
                        val isSel = s.id == selectedStrain.id
                        Box(
                            modifier = Modifier
                                .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedStrain = s }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(s.name, color = if (isSel) TextPrimary else TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialWeightText,
                        onValueChange = { initialWeightText = it },
                        label = { Text("Hmotnost (g)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costTotalText,
                        onValueChange = { costTotalText = it },
                        label = { Text("Cena celkem (Kč)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status
                Text("Stav balení", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("active" to "Otevřené", "unopened" to "Zásoba").forEach { (k, label) ->
                        Box(
                            modifier = Modifier
                                .background(if (status == k) ColorSuccess.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(8.dp))
                                .border(1.dp, if (status == k) ColorSuccess else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { status = k }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(label, color = if (status == k) ColorSuccess else TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Poznámka (volitelná)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Zrušit", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = initialWeightText.toDoubleOrNull() ?: 100.0
                            val c = costTotalText.toDoubleOrNull() ?: 800.0
                            val finalPkg = KratomPackage(
                                id = initialPackage?.id ?: ("pkg_" + System.currentTimeMillis()),
                                strainId = selectedStrain.id,
                                name = if (name.isNotBlank()) name else "${selectedStrain.name} ${w.toInt()}g",
                                initialWeightG = w,
                                remainingWeightG = initialPackage?.remainingWeightG ?: w,
                                costTotal = c,
                                pricePerG = if (w > 0) c / w else 8.0,
                                status = status,
                                openedAt = if (status == "active") (initialPackage?.openedAt ?: System.currentTimeMillis()) else null,
                                note = note
                            )
                            onSave(finalPkg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Uložit", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

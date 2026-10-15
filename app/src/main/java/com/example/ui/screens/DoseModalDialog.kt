package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppState
import com.example.model.Beverage
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
import com.example.util.calcDoseMitEstimate
import com.example.util.formatDateTime
import com.example.util.formatTime
import com.example.util.r1
import com.example.util.r2
import java.util.Calendar
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseModalDialog(
    initialDose: Dose? = null,
    appState: AppState,
    onDismiss: () -> Unit,
    onSave: (Dose) -> Unit,
    onDelete: ((Dose) -> Unit)? = null
) {
    val settings = appState.settings
    val strains = appState.strains
    val packages = appState.packages
    val beverages = appState.beverages

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // State initialization
    var amountG by remember { mutableDoubleStateOf(initialDose?.amountG ?: settings.defaultDoseG) }
    var stepSize by remember { mutableDoubleStateOf(settings.doseStepDefault) }

    // Selected strain
    val defaultStrain = strains.find { it.id == settings.lastStrainId } ?: strains.firstOrNull()
    var selectedStrain by remember {
        mutableStateOf(
            if (initialDose != null) strains.find { it.id == initialDose.strainId } ?: defaultStrain ?: strains.first()
            else defaultStrain ?: strains.first()
        )
    }

    // Selected package (filter packages by selected strain)
    val strainPacks = packages.filter { it.strainId == selectedStrain.id && it.status != "finished" }
    var selectedPackageId by remember {
        mutableStateOf(
            initialDose?.packageId ?: if (settings.autoSelectActivePackage) strainPacks.find { it.status == "active" }?.id else null
        )
    }

    // Beverage
    val defaultBev = beverages.find { it.id == settings.lastBeverageId } ?: beverages.first()
    var selectedBeverage by remember {
        mutableStateOf(
            if (initialDose != null) beverages.find { it.id == initialDose.beverageId } ?: defaultBev
            else defaultBev
        )
    }

    // Rating & effect
    var rating by remember { mutableIntStateOf(initialDose?.rating ?: 0) }
    var effect by remember { mutableStateOf(initialDose?.effect ?: "energy") }

    // Timestamp
    var timestampMs by remember { mutableLongStateOf(initialDose?.timestamp ?: System.currentTimeMillis()) }
    var isCustomTime by remember { mutableStateOf(initialDose != null) }

    // Note
    var note by remember { mutableStateOf(initialDose?.note ?: "") }

    // Stepper text input state
    var amountText by remember { mutableStateOf(r1(amountG).toString()) }

    // Update package selection when strain changes
    fun onStrainChanged(strain: Strain) {
        selectedStrain = strain
        val availPacks = packages.filter { it.strainId == strain.id && it.status != "finished" }
        selectedPackageId = if (settings.autoSelectActivePackage) availPacks.find { it.status == "active" }?.id else null
    }

    // Live Calculations
    val activePack = packages.find { it.id == selectedPackageId }
    val pricePerG = activePack?.pricePerG ?: selectedStrain.pricePerG
    val kratomCost = r2(amountG * pricePerG)
    val beverageCost = if (settings.enableBeverages) selectedBeverage.cost else 0.0
    val totalCost = r2(kratomCost + beverageCost)
    val mitMg = r2(amountG * selectedStrain.mitPct * 10.0)
    val sevenOhMg = r2(amountG * selectedStrain.sevenOhPct * 10.0)
    val dosePerKg = if (settings.bodyWeightKg > 0) r2(mitMg / settings.bodyWeightKg) else 0.0
    val remainingInPack = if (activePack != null) max(0.0, r2(activePack.remainingWeightG - amountG)) else null

    val mitEstimate = calcDoseMitEstimate(mitMg, timestampMs, settings.mitTMaxH, settings.mitHalfLifeBetaH)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (initialDose == null) "Nová dávka" else "Upravit dávku",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("dose_modal_cancel")) {
                            Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = TextPrimary)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (amountG <= 0.0) return@Button
                                val finalDose = Dose(
                                    id = initialDose?.id ?: ("d_" + System.currentTimeMillis()),
                                    strainId = selectedStrain.id,
                                    strainName = selectedStrain.name,
                                    strainColor = selectedStrain.color,
                                    packageId = selectedPackageId,
                                    amountG = r2(amountG),
                                    originalAmountG = initialDose?.originalAmountG,
                                    calibrated = initialDose?.calibrated ?: false,
                                    calibrationDiff = initialDose?.calibrationDiff,
                                    timestamp = if (isCustomTime) timestampMs else System.currentTimeMillis(),
                                    pricePerG = pricePerG,
                                    cost = totalCost,
                                    beverageId = if (settings.enableBeverages) selectedBeverage.id else "bev_water",
                                    beverageName = if (settings.enableBeverages) selectedBeverage.name else "Voda",
                                    beverageIcon = if (settings.enableBeverages) selectedBeverage.icon else "💧",
                                    beverageCost = beverageCost,
                                    rating = if (settings.enableMoodRating) rating else 0,
                                    effect = if (settings.enableMoodRating) effect else "energy",
                                    mitPct = selectedStrain.mitPct,
                                    sevenOhPct = selectedStrain.sevenOhPct,
                                    mitMg = mitMg,
                                    sevenOhMg = sevenOhMg,
                                    note = note
                                )
                                onSave(finalDose)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("dose_modal_save")
                        ) {
                            Text("Uložit", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AmoledBlack)
                )
            },
            containerColor = AmoledBlack
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. HERO STEPPER SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface1, RoundedCornerShape(16.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                amountG = max(0.1, r2(amountG - stepSize))
                                amountText = r1(amountG).toString()
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .background(CardSurface2, CircleShape)
                                .border(1.dp, CardBorder, CircleShape)
                                .testTag("stepper_minus")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Ubrat", tint = TextPrimary, modifier = Modifier.size(28.dp))
                        }

                        // Big editable number
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { newVal ->
                                val clean = newVal.replace(',', '.')
                                amountText = clean
                                clean.toDoubleOrNull()?.let { amountG = max(0.0, it) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.width(160.dp).testTag("stepper_input")
                        )

                        IconButton(
                            onClick = {
                                amountG = r2(amountG + stepSize)
                                amountText = r1(amountG).toString()
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .background(CardSurface2, CircleShape)
                                .border(1.dp, CardBorder, CircleShape)
                                .testTag("stepper_plus")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Přidat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }

                    Text("gramů", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Presets row & Step size row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Presets
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            settings.quickDosePresets.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (amountG == p) MaterialTheme.colorScheme.primary else CardSurface2,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            amountG = p
                                            amountText = r1(p).toString()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${r1(p)}g",
                                        color = if (amountG == p) Color.Black else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Stepper step selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.1, 0.25, 0.5, 1.0).forEach { st ->
                                Box(
                                    modifier = Modifier
                                        .background(if (stepSize == st) ColorWater.copy(alpha = 0.25f) else CardSurface2, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (stepSize == st) ColorWater else CardBorder, RoundedCornerShape(6.dp))
                                        .clickable { stepSize = st }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "±$st",
                                        color = if (stepSize == st) ColorWater else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. LIVE IMPACT CARD
                if (settings.modalShowPreview) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface1, RoundedCornerShape(14.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ImpactMetricItem("Cena", if (settings.showFinances) "${totalCost.toInt()} Kč" else "–", ColorWarning)
                            ImpactMetricItem(
                                "Zbude v balení",
                                if (remainingInPack != null) "${r1(remainingInPack)} g" else "volný",
                                if (remainingInPack != null && remainingInPack < settings.lowStockWarningG) ColorDanger else ColorSuccess
                            )
                            ImpactMetricItem("Čistý MIT", if (settings.showAlkaloids) "${r1(mitMg)} mg" else "–", ColorWater)
                            ImpactMetricItem("Dávka/kg", if (settings.showAlkaloids) "${dosePerKg} mg" else "–", TextSecondary)
                        }

                        if (settings.modalShowMitEstimate && settings.showAlkaloids) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardSurface2, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "⏱ Vrchol ~${formatTime(mitEstimate.peakMs, settings.timeFormat24)} · pod 1 mg MIT kolem ${formatTime(mitEstimate.clearMs, settings.timeFormat24)} (~${mitEstimate.clearHours}h)",
                                    color = ColorWater,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 3. STRAIN & PACKAGE SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface1, RoundedCornerShape(14.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text("Druh kratomu", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        strains.forEach { str ->
                            val isSel = str.id == selectedStrain.id
                            val dotColor = parseHexColor(str.color, ColorSuccess)
                            Row(
                                modifier = Modifier
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(10.dp))
                                    .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable { onStrainChanged(str) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(9.dp).background(dotColor, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = str.name,
                                    color = if (isSel) TextPrimary else TextSecondary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Package Dropdown
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    val currentPack = packages.find { it.id == selectedPackageId }
                    val packLabel = when {
                        currentPack != null -> "${currentPack.name} (${r1(currentPack.remainingWeightG)}g zbývá)"
                        else -> "Bez balení (pouze volný odhad)"
                    }

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = packLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Zdrojové balení", color = TextSecondary, fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardBorder,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(CardSurface2)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bez balení (pouze volný odhad)", color = TextPrimary) },
                                onClick = {
                                    selectedPackageId = null
                                    dropdownExpanded = false
                                }
                            )
                            strainPacks.forEach { p ->
                                val isUnopened = p.status == "unopened"
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${p.name} (${r1(p.remainingWeightG)}g)" + if (isUnopened) " [Zásoba]" else "",
                                            color = if (isUnopened) ColorWarning else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedPackageId = p.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. BEVERAGE SELECTION (If enabled)
                if (settings.enableBeverages) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface1, RoundedCornerShape(14.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text("Doprovodný nápoj", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            beverages.forEach { bev ->
                                val isSel = bev.id == selectedBeverage.id
                                Row(
                                    modifier = Modifier
                                        .background(if (isSel) ColorWater.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(10.dp))
                                        .border(1.dp, if (isSel) ColorWater else CardBorder, RoundedCornerShape(10.dp))
                                        .clickable { selectedBeverage = bev }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(bev.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bev.name + if (bev.cost > 0) " (+${bev.cost.toInt()} Kč)" else "",
                                        color = if (isSel) TextPrimary else TextSecondary,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. RATING, EFFECT & TIME
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface1, RoundedCornerShape(14.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    if (settings.enableMoodRating) {
                        Text("Hodnocení a efekt", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Star rating
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (s in 1..5) {
                                val isFilled = s <= rating
                                IconButton(
                                    onClick = { rating = if (rating == s) 0 else s },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Hvězdička $s",
                                        tint = if (isFilled) Color(0xFFFFD60A) else TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Effect chips
                        val effects = listOf(
                            "energy" to "⚡ Energie",
                            "focus" to "🎯 Soustředění",
                            "relax" to "🍃 Relaxace",
                            "pain" to "🛡️ Úleva",
                            "sleep" to "🌙 Spánek"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            effects.forEach { (effKey, effLabel) ->
                                val isSel = effKey == effect
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
                                        .clickable { effect = effKey }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = effLabel,
                                        color = if (isSel) TextPrimary else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Time selection
                    Text("Čas dávky", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (!isCustomTime) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(8.dp))
                                .border(1.dp, if (!isCustomTime) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { isCustomTime = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Právě teď", color = if (!isCustomTime) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isCustomTime) ColorWater.copy(alpha = 0.2f) else CardSurface2, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isCustomTime) ColorWater else CardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    isCustomTime = true
                                    // Open date / time picker
                                    val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(Calendar.YEAR, y)
                                            cal.set(Calendar.MONTH, m)
                                            cal.set(Calendar.DAY_OF_MONTH, d)
                                            TimePickerDialog(
                                                context,
                                                { _, hh, mm ->
                                                    cal.set(Calendar.HOUR_OF_DAY, hh)
                                                    cal.set(Calendar.MINUTE, mm)
                                                    timestampMs = cal.timeInMillis
                                                },
                                                cal.get(Calendar.HOUR_OF_DAY),
                                                cal.get(Calendar.MINUTE),
                                                settings.timeFormat24
                                            ).show()
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCustomTime) formatDateTime(timestampMs, settings.timeFormat24) else "Zadat jiný čas",
                                color = if (isCustomTime) ColorWater else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 6. NOTE & QUICK TAGS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface1, RoundedCornerShape(14.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text("Poznámka", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 160) note = it },
                        placeholder = { Text("Doplňující poznámka...", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CardBorder,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dose_note_input")
                    )

                    if (settings.modalShowNoteTags) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val tags = listOf("Nalačno", "Po jídle", "Pracovní fokus", "Před tréninkem")
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(CardSurface2, RoundedCornerShape(6.dp))
                                        .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                        .clickable {
                                            note = if (note.isEmpty()) tag else "$note, $tag"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+$tag", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // 7. DELETE BUTTON (If editing)
                if (initialDose != null && onDelete != null) {
                    Button(
                        onClick = { onDelete(initialDose) },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorDanger.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("delete_dose_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ColorDanger)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Smazat tuto dávku", color = ColorDanger, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ImpactMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

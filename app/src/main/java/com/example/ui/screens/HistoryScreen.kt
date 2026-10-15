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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppState
import com.example.model.Dose
import com.example.ui.components.Heatmap28Days
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HistoryScreen(
    appState: AppState,
    onOpenDoseModal: (Dose) -> Unit,
    onDeleteDose: (Dose) -> Unit,
    onRestoreDose: (Dose) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = appState.settings
    val allDoses = appState.doses
    val strains = appState.strains

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Filters state
    var selectedStrainId by remember { mutableStateOf<String?>(null) } // null = Vše
    var selectedPeriodDays by remember { mutableIntStateOf(30) } // 7, 30, 90, 0 (0 = Vše)
    var selectedViewMode by remember { mutableStateOf("list") } // "list" | "calendar"
    var searchQuery by remember { mutableStateOf("") }
    var heatmapFilteredDayKey by remember { mutableStateOf<String?>(null) }

    // Calendar month offset
    var monthOffset by remember { mutableIntStateOf(0) }

    val now = System.currentTimeMillis()

    // Apply filtering
    val filteredDoses = allDoses.filter { dose ->
        // Strain filter
        val strainMatch = selectedStrainId == null || dose.strainId == selectedStrainId

        // Period filter
        val periodMatch = if (heatmapFilteredDayKey != null) {
            dayKey(dose.timestamp, settings.dayStartHour) == heatmapFilteredDayKey
        } else if (selectedPeriodDays > 0) {
            val cutoff = now - (selectedPeriodDays * 86400000L)
            dose.timestamp >= cutoff
        } else true

        // Search query
        val searchMatch = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim().lowercase()
            dose.strainName.lowercase().contains(q) ||
            dose.beverageName.lowercase().contains(q) ||
            dose.note.lowercase().contains(q)
        }

        strainMatch && periodMatch && searchMatch
    }.sortedByDescending { it.timestamp }

    // Totals for filtered set
    val totalFilteredG = filteredDoses.sumOf { it.amountG }
    val totalFilteredMit = filteredDoses.sumOf { it.mitMg }
    val totalFilteredCost = filteredDoses.sumOf { it.cost + it.beverageCost }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AmoledBlack
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. 28-DAY CALENDAR HEATMAP
            if (settings.enableHeatmap) {
                item {
                    Heatmap28Days(
                        doses = allDoses,
                        dailyLimitG = settings.dailyLimitG,
                        selectedDayKey = heatmapFilteredDayKey,
                        onSelectDay = { heatmapFilteredDayKey = it },
                        dayStartHour = settings.dayStartHour
                    )
                }
            }

            // 2. FILTERS CARD
            if (settings.histShowFilters) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface1),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Hledat v historii...", color = TextMuted, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Hledat", tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )

                            // Strain Chips
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChipPill(
                                    label = "Všechny druhy",
                                    isSelected = selectedStrainId == null,
                                    onClick = { selectedStrainId = null }
                                )
                                strains.forEach { s ->
                                    val isSel = selectedStrainId == s.id
                                    FilterChipPill(
                                        label = s.name,
                                        dotColor = parseHexColor(s.color, ColorSuccess),
                                        isSelected = isSel,
                                        onClick = { selectedStrainId = if (isSel) null else s.id }
                                    )
                                }
                            }

                            // Period & View Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Period chips
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(7 to "7 dní", 30 to "30 dní", 90 to "90 dní", 0 to "Vše").forEach { (days, label) ->
                                        val isSel = selectedPeriodDays == days && heatmapFilteredDayKey == null
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else CardSurface2, RoundedCornerShape(8.dp))
                                                .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedPeriodDays = days
                                                    heatmapFilteredDayKey = null
                                                }
                                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                        ) {
                                            Text(label, color = if (isSel) MaterialTheme.colorScheme.primary else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // View switcher (List vs Calendar)
                                if (settings.enableCalendarView) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(if (selectedViewMode == "list") MaterialTheme.colorScheme.primaryContainer else CardSurface2, RoundedCornerShape(8.dp))
                                                .clickable { selectedViewMode = "list" }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("☰ Seznam", color = if (selectedViewMode == "list") MaterialTheme.colorScheme.primary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(if (selectedViewMode == "calendar") MaterialTheme.colorScheme.primaryContainer else CardSurface2, RoundedCornerShape(8.dp))
                                                .clickable { selectedViewMode = "calendar" }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("📅 Kalendář", color = if (selectedViewMode == "calendar") MaterialTheme.colorScheme.primary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary line
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredDoses.size} dávek · ${r1(totalFilteredG)} g" + if (settings.showAlkaloids) " (${r1(totalFilteredMit)} mg MIT)" else "",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (settings.showFinances) {
                        Text("${totalFilteredCost.toInt()} Kč", color = ColorWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. VIEW SELECTION (CALENDAR vs LIST)
            if (selectedViewMode == "calendar" && settings.enableCalendarView) {
                item {
                    MonthlyCalendarView(
                        doses = allDoses,
                        dailyLimitG = settings.dailyLimitG,
                        monthOffset = monthOffset,
                        onMonthOffsetChange = { monthOffset = it },
                        weekStartsMonday = settings.weekStartsMonday,
                        onSelectDay = { dayKeyStr ->
                            heatmapFilteredDayKey = dayKeyStr
                            selectedViewMode = "list"
                        }
                    )
                }
            } else {
                // LIST VIEW (Grouped by day)
                if (filteredDoses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Žádné záznamy neodpovídají zvoleným filtrům.", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    val grouped = filteredDoses.groupBy { dayKey(it.timestamp, settings.dayStartHour) }
                    grouped.forEach { (dk, dosesInDay) ->
                        val dayG = dosesInDay.sumOf { it.amountG }
                        val dayCost = dosesInDay.sumOf { it.cost + it.beverageCost }
                        val todayStr = dayKey(now, settings.dayStartHour)
                        val yesterdayStr = dayKey(now - 86400000L, settings.dayStartHour)
                        val dayLabel = when (dk) {
                            todayStr -> "Dnes"
                            yesterdayStr -> "Včera"
                            else -> formatDate(dosesInDay.first().timestamp)
                        }

                        // Day Header
                        item(key = "header_$dk") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dayLabel, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${r1(dayG)} g", color = if (dayG > settings.dailyLimitG) ColorWarning else ColorSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (settings.showFinances) {
                                        Text("${dayCost.toInt()} Kč", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Dose rows
                        items(dosesInDay, key = { it.id }) { dose ->
                            DoseHistoryRow(
                                dose = dose,
                                compact = settings.histCompactRows,
                                showFinances = settings.showFinances,
                                showAlkaloids = settings.showAlkaloids,
                                privacyMode = settings.privacyMode,
                                timeFormat24 = settings.timeFormat24,
                                onClick = { onOpenDoseModal(dose) },
                                onDelete = {
                                    onDeleteDose(dose)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Dávka smazána",
                                            actionLabel = "Vrátit zpět",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            onRestoreDose(dose)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DoseHistoryRow(
    dose: Dose,
    compact: Boolean,
    showFinances: Boolean,
    showAlkaloids: Boolean,
    privacyMode: Boolean,
    timeFormat24: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dose_history_row_${dose.id}"),
        colors = CardDefaults.cardColors(containerColor = CardSurface1),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Dot + Strain + Subtitle
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(parseHexColor(dose.strainColor, ColorSuccess), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(dose.strainName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (dose.calibrated && dose.calibrationDiff != null) {
                            Box(
                                modifier = Modifier
                                    .background(ColorWater.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("⚖️ ${if (dose.calibrationDiff >= 0) "+${dose.calibrationDiff}" else "${dose.calibrationDiff}"}g", color = ColorWater, fontSize = 9.sp)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(formatTime(dose.timestamp, timeFormat24), color = TextMuted, fontSize = 10.sp)
                        if (dose.beverageId != "bev_water") {
                            Text("· ${dose.beverageIcon} ${dose.beverageName}", color = TextSecondary, fontSize = 10.sp)
                        }
                        if (dose.rating > 0) {
                            Text("· ★${dose.rating}", color = Color(0xFFFFD60A), fontSize = 10.sp)
                        }
                        if (showAlkaloids && !compact) {
                            Text("· ${r1(dose.mitMg)} mg MIT", color = ColorWater, fontSize = 10.sp)
                        }
                    }

                    if (dose.note.isNotBlank() && !compact) {
                        Text(dose.note, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            // Right: Grams & Cost & Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    val amountStr = if (privacyMode) "••• g" else "${r1(dose.amountG)} g"
                    Text(amountStr, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (showFinances) {
                        val costStr = if (privacyMode) "••• Kč" else "${dose.cost.toInt()} Kč"
                        Text(costStr, color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ColorDanger.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// MONTHLY CALENDAR VIEW
@Composable
fun MonthlyCalendarView(
    doses: List<Dose>,
    dailyLimitG: Double,
    monthOffset: Int,
    onMonthOffsetChange: (Int) -> Unit,
    weekStartsMonday: Boolean,
    onSelectDay: (String) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val currentMonthTitle = SimpleDateFormat("LLLL yyyy", Locale("cs")).format(cal.time).replaceFirstChar { it.uppercase() }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...

    val offsetDays = if (weekStartsMonday) {
        (firstDayOfWeek + 5) % 7 // Monday = 0
    } else {
        firstDayOfWeek - 1 // Sunday = 0
    }

    val dailyMap = mutableMapOf<String, Double>()
    for (d in doses) {
        val dk = dayKey(d.timestamp)
        dailyMap[dk] = (dailyMap[dk] ?: 0.0) + d.amountG
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface1),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthOffsetChange(monthOffset - 1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Předchozí měsíc", tint = TextPrimary)
                }
                Text(currentMonthTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = { onMonthOffsetChange(monthOffset + 1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Další měsíc", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day headers
            val headers = if (weekStartsMonday) listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne") else listOf("Ne", "Po", "Út", "St", "Čt", "Pá", "So")
            Row(modifier = Modifier.fillMaxWidth()) {
                headers.forEach { h ->
                    Text(h, color = TextMuted, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days grid (up to 6 rows)
            val totalCells = offsetDays + daysInMonth
            val rows = (totalCells + 6) / 7

            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (c in 0..6) {
                        val cellIdx = r * 7 + c
                        val dayNumber = cellIdx - offsetDays + 1

                        if (dayNumber in 1..daysInMonth) {
                            val curCal = Calendar.getInstance().apply {
                                timeInMillis = cal.timeInMillis
                                set(Calendar.DAY_OF_MONTH, dayNumber)
                            }
                            val dk = dayKey(curCal.timeInMillis)
                            val g = dailyMap[dk] ?: 0.0
                            val ratio = if (dailyLimitG > 0) g / dailyLimitG else 0.0
                            val barColor = when {
                                ratio > 1.25 -> ColorDanger
                                ratio > 1.0 -> ColorWarning
                                ratio > 0.0 -> ColorSuccess
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .padding(2.dp)
                                    .background(if (g > 0) CardSurface2 else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { onSelectDay(dk) }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$dayNumber", color = if (g > 0) TextPrimary else TextMuted, fontSize = 10.sp)
                                    if (g > 0) {
                                        Text("${r1(g)}g", color = barColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).height(44.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    dotColor: Color? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else CardSurface2, RoundedCornerShape(8.dp))
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else CardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(7.dp).background(dotColor, CircleShape))
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}

package com.yc.navigator.ui.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import com.yc.navigator.data.Aerodrome
import com.yc.navigator.data.AppSettings
import com.yc.navigator.data.Journal
import com.yc.navigator.data.Leg
import com.yc.navigator.data.Pilot
import com.yc.navigator.data.PilotAssignments
import com.yc.navigator.data.PilotRole
import com.yc.navigator.data.Quantity
import com.yc.navigator.data.SunEntry
import com.yc.navigator.data.UnitSystem
import com.yc.navigator.data.effectiveUnitSystem
import com.yc.navigator.domain.JournalCalculation
import com.yc.navigator.domain.JournalCalculator
import com.yc.navigator.ui.components.ConfirmDialog
import com.yc.navigator.ui.components.DateField
import com.yc.navigator.ui.components.FlatNumberField
import com.yc.navigator.ui.components.HelpDot
import com.yc.navigator.ui.components.clearFocusOnTap
import com.yc.navigator.ui.components.IntField
import com.yc.navigator.ui.components.MoveButtons
import com.yc.navigator.ui.components.NumberField
import com.yc.navigator.ui.components.SectionCard
import com.yc.navigator.ui.components.TextInput
import com.yc.navigator.ui.formatDate
import com.yc.navigator.ui.formatMinutes
import com.yc.navigator.ui.journalTitle
import com.yc.navigator.ui.routeSummary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    journal: Journal,
    pilots: List<Pilot>,
    settings: AppSettings,
    languageTag: String,
    aircraftTypes: List<String>,
    onSaveAircraftType: (String) -> Unit,
    onRemoveAircraftType: (String) -> Unit,
    onJournalChange: (Journal) -> Unit,
    onBack: () -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSharePdf: (Journal) -> Unit,
    onPreview: () -> Unit
) {
    var draft by remember(journal.id) { mutableStateOf(journal) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun commit(updated: Journal) {
        draft = updated
        onJournalChange(updated)
    }

    val units = settings.effectiveUnitSystem()
    val calc = remember(draft) { JournalCalculator.calculate(draft) }
    val windError = remember(calc) { JournalCalculator.windErrorDetected(calc) }

    val expandedLegs = remember(draft.id) {
        mutableStateOf<Set<Long>>(emptySet())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = journalTitle(draft, stringResource(R.string.journal_no_number)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(draft.dateIso, languageTag),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onSharePdf(draft) }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.width(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_pdf))
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_more)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_duplicate)) },
                            onClick = {
                                menuOpen = false
                                onDuplicate(draft.id)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = onPreview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_preview),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clearFocusOnTap(focusManager),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EditorItem { modifier ->
                    SummaryCard(
                        journal = draft,
                        calc = calc,
                        units = units,
                        languageTag = languageTag,
                        windError = windError,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    HeaderCard(
                        journal = draft,
                        pilots = pilots,
                        units = units,
                        languageTag = languageTag,
                        aircraftTypes = aircraftTypes,
                        onSaveAircraftType = onSaveAircraftType,
                        onRemoveAircraftType = onRemoveAircraftType,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    WindCard(
                        journal = draft,
                        calc = calc,
                        units = units,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    WeatherCard(
                        journal = draft,
                        units = units,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    SunCard(
                        journal = draft,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    AerodromeCard(
                        title = stringResource(R.string.section_departure),
                        aerodrome = draft.departure,
                        isDeparture = true,
                        units = units,
                        onChange = { aerodrome ->
                            commit(draft.copy(departure = aerodrome))
                        },
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    AerodromeCard(
                        title = stringResource(R.string.section_arrival),
                        aerodrome = draft.arrival,
                        isDeparture = false,
                        units = units,
                        onChange = { aerodrome ->
                            commit(draft.copy(arrival = aerodrome))
                        },
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    SectionCard(
                        title = stringResource(R.string.section_route),
                        modifier = modifier,
                        collapsible = true
                    ) {
                        Text(
                            text = routeSummary(draft),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        TakeoffRow(
                            journal = draft,
                            calc = calc,
                            units = units,
                            onChange = ::commit
                        )
                        draft.legs.forEachIndexed { index, leg ->
                            Spacer(Modifier.height(12.dp))
                            LegCard(
                                index = index,
                                leg = leg,
                                calc = calc.legs.getOrNull(index),
                                units = units,
                                expanded = leg.id in expandedLegs.value,
                                onToggleExpanded = {
                                    expandedLegs.value = if (leg.id in expandedLegs.value) {
                                        expandedLegs.value - leg.id
                                    } else {
                                        expandedLegs.value + leg.id
                                    }
                                },
                                onLegChange = { updated ->
                                    commit(
                                        draft.copy(
                                            legs = draft.legs.mapIndexed { i, item ->
                                                if (i == index) updated else item
                                            }
                                        )
                                    )
                                },
                                onMoveUp = if (index > 0) {
                                    {
                                        commit(
                                            draft.copy(
                                                legs = draft.legs.toMutableList().apply {
                                                    val current = this[index]
                                                    this[index] = this[index - 1]
                                                    this[index - 1] = current
                                                }
                                            )
                                        )
                                    }
                                } else null,
                                onMoveDown = if (index < draft.legs.lastIndex) {
                                    {
                                        commit(
                                            draft.copy(
                                                legs = draft.legs.toMutableList().apply {
                                                    val current = this[index]
                                                    this[index] = this[index + 1]
                                                    this[index + 1] = current
                                                }
                                            )
                                        )
                                    }
                                } else null,
                                onDelete = {
                                    expandedLegs.value = expandedLegs.value - leg.id
                                    commit(
                                        draft.copy(
                                            legs = draft.legs.filterNot { it.id == leg.id }
                                        )
                                    )
                                }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = {
                                val newLeg = Leg(
                                    id = (draft.legs.maxOfOrNull { it.id } ?: 0L) + 1L
                                )
                                expandedLegs.value = expandedLegs.value + newLeg.id
                                commit(draft.copy(legs = draft.legs + newLeg))
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.action_add_leg))
                            }
                        }
                    }
                }
            }

            item {
                EditorItem { modifier ->
                    LandingCard(
                        journal = draft,
                        calc = calc,
                        units = units,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    FuelCard(
                        journal = draft,
                        calc = calc,
                        units = units,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    ActualCard(
                        journal = draft,
                        units = units,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }

            item {
                EditorItem { modifier ->
                    SignaturesCard(
                        journal = draft,
                        pilots = pilots,
                        languageTag = languageTag,
                        onChange = ::commit,
                        modifier = modifier
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.journal_delete_title),
            text = stringResource(R.string.journal_delete_text),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                onDelete(draft.id)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun SummaryCard(
    journal: Journal,
    calc: JournalCalculation,
    units: UnitSystem,
    languageTag: String,
    windError: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = routeSummary(journal),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(
                            journal.aircraftType,
                            journal.aircraftNumber,
                            formatDate(journal.dateIso, languageTag)
                        ).filter { it.isNotBlank() }.joinToString(" В· "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMetric(
                    label = stringResource(R.string.field_total_distance),
                    value = formattedQuantity(calc.totalDistanceKm, Quantity.DISTANCE, units),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = stringResource(R.string.field_total_time),
                    value = formatMinutes(calc.totalTimeMinutes),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = stringResource(R.string.field_total_fuel),
                    value = formattedQuantity(calc.totalFuelKg, Quantity.MASS, units),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "${stringResource(R.string.field_wind_direction_magnetic)}: " +
                    formattedNumber(calc.windDirectionMagneticDeg, 0) + "°",
                style = MaterialTheme.typography.bodyMedium
            )

            if (windError) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.msg_wind_exceeds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun HeaderCard(
    journal: Journal,
    pilots: List<Pilot>,
    units: UnitSystem,
    languageTag: String,
    aircraftTypes: List<String>,
    onSaveAircraftType: (String) -> Unit,
    onRemoveAircraftType: (String) -> Unit,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    val weather = journal.windWeather
    val pressureDecimals = if (units == UnitSystem.RU) 0 else 2
    SectionCard(
        title = stringResource(R.string.section_header),
        modifier = modifier,
        collapsible = true
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(
                label = stringResource(R.string.field_journal_number),
                value = journal.number,
                onValueChange = { onChange(journal.copy(number = it)) },
                modifier = Modifier.weight(1f)
            )
            BoxDate(
                label = stringResource(R.string.field_date),
                isoDate = journal.dateIso,
                onChange = { onChange(journal.copy(dateIso = it)) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        TextInput(
            label = stringResource(R.string.field_unit),
            value = journal.unit,
            onValueChange = { onChange(journal.copy(unit = it)) }
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AircraftTypeField(
                value = journal.aircraftType,
                types = aircraftTypes,
                onValueChange = { onChange(journal.copy(aircraftType = it)) },
                onSaveType = onSaveAircraftType,
                onRemoveType = onRemoveAircraftType,
                modifier = Modifier.weight(1f)
            )
            TextInput(
                label = stringResource(R.string.field_aircraft_number),
                value = journal.aircraftNumber,
                onValueChange = { onChange(journal.copy(aircraftNumber = it)) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        PilotPickerField(
            roleLabel = stringResource(R.string.field_commander),
            selectedPilotId = journal.pilots.commanderId,
            pilots = pilots,
            onSelect = {
                onChange(journal.copy(pilots = journal.pilots.withRole(PilotRole.COMMANDER, it)))
            }
        )
        Spacer(Modifier.height(8.dp))
        PilotPickerField(
            roleLabel = stringResource(R.string.field_second_pilot),
            selectedPilotId = journal.pilots.secondPilotId,
            pilots = pilots,
            onSelect = {
                onChange(journal.copy(pilots = journal.pilots.withRole(PilotRole.SECOND_PILOT, it)))
            }
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_min_pressure),
                canonicalValue = weather.minPressureMmHg,
                onCanonicalChange = {
                    onChange(journal.copy(windWeather = weather.copy(minPressureMmHg = it)))
                },
                quantity = Quantity.PRESSURE,
                units = units,
                maxDecimals = pressureDecimals,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_min_pressure)
            )
            NumberField(
                label = stringResource(R.string.field_assigned_level),
                canonicalValue = weather.assignedLevelM,
                onCanonicalChange = {
                    onChange(journal.copy(windWeather = weather.copy(assignedLevelM = it)))
                },
                quantity = Quantity.ALTITUDE,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_assigned_level)
            )
        }
    }
}

@Composable
private fun BoxDate(
    label: String,
    isoDate: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DateField(
        label = label,
        isoDate = isoDate,
        onDateChange = onChange,
        modifier = modifier
    )
}

@Composable
private fun WindCard(
    journal: Journal,
    calc: JournalCalculation,
    units: UnitSystem,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    val weather = journal.windWeather

    fun update(transform: (com.yc.navigator.data.WindWeather) -> com.yc.navigator.data.WindWeather) {
        onChange(journal.copy(windWeather = transform(weather)))
    }

    SectionCard(
        title = stringResource(R.string.section_wind_weather),
        modifier = modifier,
        collapsible = true
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(
                label = stringResource(R.string.field_point),
                value = weather.point,
                onValueChange = { value -> update { w -> w.copy(point = value) } },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = stringResource(R.string.field_height),
                canonicalValue = weather.heightM,
                onCanonicalChange = { update { w -> w.copy(heightM = it) } },
                quantity = Quantity.ALTITUDE,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_wind_direction),
                canonicalValue = weather.windDirectionTrue,
                onCanonicalChange = { update { w -> w.copy(windDirectionTrue = it) } },
                quantity = Quantity.NONE,
                units = units,
                maxDecimals = 0,
                unitText = "°",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = stringResource(R.string.field_wind_speed),
                canonicalValue = weather.windSpeedKmh,
                onCanonicalChange = { update { w -> w.copy(windSpeedKmh = it) } },
                quantity = Quantity.SPEED,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_magnetic_declination),
                canonicalValue = weather.magneticDeclination,
                onCanonicalChange = { update { w -> w.copy(magneticDeclination = it) } },
                quantity = Quantity.NONE,
                units = units,
                maxDecimals = 1,
                unitText = "°",
                modifier = Modifier.weight(1f)
            )
            ValueField(
                label = stringResource(R.string.field_wind_direction_magnetic),
                value = degreesText(
                    formattedQuantity(calc.windDirectionMagneticDeg, Quantity.NONE, units, 0)
                ),
                modifier = Modifier.weight(1f),
                highlight = true,
                help = stringResource(R.string.hint_wind_direction_magnetic)
            )
        }
    }
}

@Composable
private fun WeatherCard(
    journal: Journal,
    units: UnitSystem,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    val weather = journal.windWeather
    val pressureDecimals = if (units == UnitSystem.RU) 0 else 2
    val degree = "°"

    fun update(transform: (com.yc.navigator.data.WindWeather) -> com.yc.navigator.data.WindWeather) {
        onChange(journal.copy(windWeather = transform(weather)))
    }

    SectionCard(
        title = stringResource(R.string.section_weather),
        modifier = modifier,
        collapsible = true
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.field_weather_valid),
                    style = MaterialTheme.typography.titleMedium
                )
                HelpDot(
                    title = stringResource(R.string.field_weather_valid),
                    help = stringResource(R.string.hint_weather_valid)
                )
            }
            IntField(
                label = "",
                value = weather.weatherValidHours,
                onValueChange = { update { w -> w.copy(weatherValidHours = it) } },
                suffix = stringResource(R.string.unit_hour),
                modifier = Modifier.width(92.dp)
            )
            IntField(
                label = "",
                value = weather.weatherValidMinutes,
                onValueChange = { update { w -> w.copy(weatherValidMinutes = it) } },
                suffix = stringResource(R.string.unit_min),
                modifier = Modifier.width(104.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                WeatherTableRow(
                    left = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_landing_course),
                            value = weather.landingCourseMagnetic,
                            onChange = { update { w -> w.copy(landingCourseMagnetic = it) } },
                            quantity = Quantity.NONE,
                            units = units,
                            maxDecimals = 0,
                            unitText = degree,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_landing_course)
                        )
                    },
                    right = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_friction),
                            value = weather.frictionCoefficient,
                            onChange = { update { w -> w.copy(frictionCoefficient = it) } },
                            quantity = Quantity.NONE,
                            units = units,
                            maxDecimals = 2,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_friction)
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                WeatherTableRow(
                    left = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_declination),
                            value = weather.surfaceWindDirection,
                            onChange = { update { w -> w.copy(surfaceWindDirection = it) } },
                            quantity = Quantity.NONE,
                            units = units,
                            maxDecimals = 0,
                            unitText = degree,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_surface_wind_direction)
                        )
                    },
                    right = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_decision_height),
                            value = weather.decisionHeightM,
                            onChange = { update { w -> w.copy(decisionHeightM = it) } },
                            quantity = Quantity.ALTITUDE,
                            units = units,
                            maxDecimals = 0,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_decision_height)
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                WeatherTableRow(
                    left = {
                        WeatherTableCell(
                            label = "U",
                            value = weather.surfaceWindSpeedKmh,
                            onChange = { update { w -> w.copy(surfaceWindSpeedKmh = it) } },
                            quantity = Quantity.SPEED,
                            units = units,
                            maxDecimals = 0,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_surface_wind_speed)
                        )
                    },
                    right = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_visibility),
                            value = weather.visibilityKm,
                            onChange = { update { w -> w.copy(visibilityKm = it) } },
                            quantity = Quantity.DISTANCE,
                            units = units,
                            maxDecimals = 0,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_visibility)
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                WeatherTableRow(
                    left = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_pressure),
                            value = weather.pressureMmHg,
                            onChange = { update { w -> w.copy(pressureMmHg = it) } },
                            quantity = Quantity.PRESSURE,
                            units = units,
                            maxDecimals = pressureDecimals,
                            unitText = if (units == UnitSystem.RU) "мм" else "inHg",
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_pressure)
                        )
                    },
                    right = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_obstacle_height),
                            value = weather.obstacleHeightM,
                            onChange = { update { w -> w.copy(obstacleHeightM = it) } },
                            quantity = Quantity.ALTITUDE,
                            units = units,
                            maxDecimals = 0,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_obstacle_height)
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                WeatherTableRow(
                    left = {
                        WeatherTableCell(
                            label = stringResource(R.string.field_temperature),
                            value = weather.temperatureC,
                            onChange = { update { w -> w.copy(temperatureC = it) } },
                            quantity = Quantity.TEMPERATURE,
                            units = units,
                            maxDecimals = 0,
                            modifier = Modifier.weight(1f),
                            help = stringResource(R.string.hint_temperature)
                        )
                    },
                    right = {
                        Box(Modifier.weight(1f))
                    }
                )
            }
        }
    }
}

@Composable
private fun WeatherTableRow(
    left: @Composable RowScope.() -> Unit,
    right: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        left()
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        right()
    }
}

@Composable
private fun WeatherTableCell(
    label: String,
    value: Double?,
    onChange: (Double?) -> Unit,
    quantity: Quantity,
    units: UnitSystem,
    maxDecimals: Int,
    modifier: Modifier = Modifier,
    unitText: String? = null,
    help: String? = null
) {
    Row(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(64.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = if (help != null) 18.dp else 0.dp),
                maxLines = 2
            )
            if (help != null) {
                HelpDot(
                    title = label,
                    help = help,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        FlatNumberField(
            canonicalValue = value,
            onCanonicalChange = onChange,
            quantity = quantity,
            units = units,
            maxDecimals = maxDecimals,
            unitText = unitText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SunCard(
    journal: Journal,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.section_sun),
        modifier = modifier,
        collapsible = true
    ) {
        if (journal.sunEntries.isEmpty()) {
            Text(
                text = stringResource(R.string.section_sun),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            journal.sunEntries.forEachIndexed { index, entry ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextInput(
                        label = stringResource(R.string.field_sun_point),
                        value = entry.point,
                        onValueChange = { value ->
                            onChange(
                                journal.copy(
                                    sunEntries = journal.sunEntries.mapIndexed { i, item ->
                                        if (i == index) item.copy(point = value) else item
                                    }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        onChange(
                            journal.copy(
                                sunEntries = journal.sunEntries.filterIndexed { i, _ -> i != index }
                            )
                        )
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput(
                        label = stringResource(R.string.field_sunrise),
                        value = entry.sunrise,
                        onValueChange = { value ->
                            onChange(
                                journal.copy(
                                    sunEntries = journal.sunEntries.mapIndexed { i, item ->
                                        if (i == index) item.copy(sunrise = value) else item
                                    }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextInput(
                        label = stringResource(R.string.field_sunset),
                        value = entry.sunset,
                        onValueChange = { value ->
                            onChange(
                                journal.copy(
                                    sunEntries = journal.sunEntries.mapIndexed { i, item ->
                                        if (i == index) item.copy(sunset = value) else item
                                    }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextInput(
                        label = stringResource(R.string.field_sun_sum),
                        value = entry.sum,
                        onValueChange = { value ->
                            onChange(
                                journal.copy(
                                    sunEntries = journal.sunEntries.mapIndexed { i, item ->
                                        if (i == index) item.copy(sum = value) else item
                                    }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index != journal.sunEntries.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        if (journal.sunEntries.size < 5) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                onChange(
                    journal.copy(
                        sunEntries = journal.sunEntries + SunEntry(
                            id = (journal.sunEntries.maxOfOrNull { it.id } ?: 0L) + 1L
                        )
                    )
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_add_sun))
            }
        }
    }
}

@Composable
private fun AerodromeCard(
    title: String,
    aerodrome: Aerodrome,
    isDeparture: Boolean,
    units: UnitSystem,
    onChange: (Aerodrome) -> Unit,
    modifier: Modifier = Modifier
) {
    val pressureDecimals = if (units == UnitSystem.RU) 0 else 2

    SectionCard(title = title, modifier = modifier, collapsible = true) {
        TextInput(
            label = stringResource(
                if (isDeparture) R.string.section_departure else R.string.section_arrival
            ),
            value = aerodrome.name,
            onValueChange = { onChange(aerodrome.copy(name = it)) }
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_declination),
                canonicalValue = aerodrome.declination,
                onCanonicalChange = { onChange(aerodrome.copy(declination = it)) },
                quantity = Quantity.NONE,
                units = units,
                maxDecimals = 1,
                unitText = "°",
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_aerodrome_declination)
            )
            NumberField(
                label = stringResource(R.string.field_wind_speed),
                canonicalValue = aerodrome.windSpeedKmh,
                onCanonicalChange = { onChange(aerodrome.copy(windSpeedKmh = it)) },
                quantity = Quantity.SPEED,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_p0),
                canonicalValue = aerodrome.pressureMmHg,
                onCanonicalChange = { onChange(aerodrome.copy(pressureMmHg = it)) },
                quantity = Quantity.PRESSURE,
                units = units,
                maxDecimals = pressureDecimals,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_p0)
            )
            NumberField(
                label = stringResource(R.string.field_t0),
                canonicalValue = aerodrome.temperatureC,
                onCanonicalChange = { onChange(aerodrome.copy(temperatureC = it)) },
                quantity = Quantity.TEMPERATURE,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_t0)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_sah),
                canonicalValue = aerodrome.sahPercent,
                onCanonicalChange = { onChange(aerodrome.copy(sahPercent = it)) },
                quantity = Quantity.NONE,
                units = units,
                maxDecimals = 1,
                unitText = "%",
                modifier = Modifier.weight(1f),
                help = if (isDeparture) null else stringResource(R.string.hint_sah)
            )
            NumberField(
                label = stringResource(
                    if (isDeparture) R.string.field_takeoff_mass else R.string.field_landing_mass
                ),
                canonicalValue = aerodrome.massKg,
                onCanonicalChange = { onChange(aerodrome.copy(massKg = it)) },
                quantity = Quantity.MASS,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f),
                help = stringResource(
                    if (isDeparture) R.string.hint_takeoff_mass else R.string.hint_landing_mass
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        if (isDeparture) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    label = stringResource(R.string.field_takeoff_speed),
                    canonicalValue = aerodrome.speedKmh,
                    onCanonicalChange = { onChange(aerodrome.copy(speedKmh = it)) },
                    quantity = Quantity.SPEED,
                    units = units,
                    maxDecimals = 0,
                    modifier = Modifier.weight(1f),
                    help = stringResource(R.string.hint_takeoff_speed)
                )
                NumberField(
                    label = stringResource(R.string.field_takeoff_run),
                    canonicalValue = aerodrome.runM,
                    onCanonicalChange = { onChange(aerodrome.copy(runM = it)) },
                    quantity = Quantity.ALTITUDE,
                    units = units,
                    maxDecimals = 0,
                    modifier = Modifier.weight(1f),
                    help = stringResource(R.string.hint_takeoff_run)
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    label = stringResource(R.string.field_approach_speed),
                    canonicalValue = aerodrome.approachSpeedKmh,
                    onCanonicalChange = { onChange(aerodrome.copy(approachSpeedKmh = it)) },
                    quantity = Quantity.SPEED,
                    units = units,
                    maxDecimals = 0,
                    modifier = Modifier.weight(1f),
                    help = stringResource(R.string.hint_approach_speed)
                )
                NumberField(
                    label = stringResource(R.string.field_landing_speed),
                    canonicalValue = aerodrome.landingSpeedKmh,
                    onCanonicalChange = { onChange(aerodrome.copy(landingSpeedKmh = it)) },
                    quantity = Quantity.SPEED,
                    units = units,
                    maxDecimals = 0,
                    modifier = Modifier.weight(1f),
                    help = stringResource(R.string.hint_landing_speed)
                )
            }
            Spacer(Modifier.height(10.dp))
            NumberField(
                label = stringResource(R.string.field_landing_run),
                canonicalValue = aerodrome.landingRunM,
                onCanonicalChange = { onChange(aerodrome.copy(landingRunM = it)) },
                quantity = Quantity.ALTITUDE,
                units = units,
                maxDecimals = 0,
                help = stringResource(R.string.hint_landing_run)
            )
        }
        Spacer(Modifier.height(10.dp))
        NumberField(
            label = stringResource(R.string.field_safe_circle),
            canonicalValue = aerodrome.safeCircleHeightM,
            onCanonicalChange = { onChange(aerodrome.copy(safeCircleHeightM = it)) },
            quantity = Quantity.ALTITUDE,
            units = units,
            maxDecimals = 0,
            help = stringResource(R.string.hint_safe_circle)
        )
    }
}

@Composable
private fun TakeoffRow(
    journal: Journal,
    calc: JournalCalculation,
    units: UnitSystem,
    onChange: (Journal) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.field_takeoff),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = journal.departure.name.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IntField(
                label = stringResource(R.string.field_time),
                value = journal.takeoffTimeMinutes,
                onValueChange = { onChange(journal.copy(takeoffTimeMinutes = it)) },
                suffix = stringResource(R.string.unit_min),
                modifier = Modifier.weight(1f)
            )
            ValueField(
                label = stringResource(R.string.field_fuel_calc),
                value = formattedQuantity(calc.takeoffFuelKg, Quantity.MASS, units),
                modifier = Modifier.weight(1f),
                highlight = true
            )
        }
    }
}

@Composable
private fun LandingCard(
    journal: Journal,
    calc: JournalCalculation,
    units: UnitSystem,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.field_landing),
        modifier = modifier,
        collapsible = true
    ) {
        TextInput(
            label = stringResource(R.string.field_landing_point),
            value = journal.landingPoint,
            onValueChange = { onChange(journal.copy(landingPoint = it)) }
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IntField(
                label = stringResource(R.string.field_landing_time),
                value = journal.landingTimeMinutes,
                onValueChange = { onChange(journal.copy(landingTimeMinutes = it)) },
                suffix = stringResource(R.string.unit_min),
                modifier = Modifier.weight(1f)
            )
            ValueField(
                label = stringResource(R.string.field_fuel_calc),
                value = formattedQuantity(calc.landingFuelKg, Quantity.MASS, units),
                modifier = Modifier.weight(1f),
                highlight = true
            )
        }
    }
}

@Composable
private fun LegCard(
    index: Int,
    leg: Leg,
    calc: com.yc.navigator.domain.LegCalculation?,
    units: UnitSystem,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onLegChange: (Leg) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${stringResource(R.string.field_leg)} ${index + 1}" +
                            if (leg.route.isNotBlank()) " В· ${leg.route}" else "",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${stringResource(R.string.field_distance)}: " +
                                formattedQuantity(leg.distanceKm, Quantity.DISTANCE, units),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stringResource(R.string.field_time)}: " +
                                formatMinutes(calc?.timeMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MoveButtons(onMoveUp = onMoveUp, onMoveDown = onMoveDown)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        label = stringResource(R.string.field_relief_height),
                        canonicalValue = leg.reliefHeightM,
                        onCanonicalChange = { onLegChange(leg.copy(reliefHeightM = it)) },
                        quantity = Quantity.ALTITUDE,
                        units = units,
                        maxDecimals = 0,
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_relief_height)
                    )
                    NumberField(
                        label = stringResource(R.string.field_obstacle_height),
                        canonicalValue = leg.obstacleHeightM,
                        onCanonicalChange = { onLegChange(leg.copy(obstacleHeightM = it)) },
                        quantity = Quantity.ALTITUDE,
                        units = units,
                        maxDecimals = 0,
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_leg_obstacle_height)
                    )
                }
                Spacer(Modifier.height(10.dp))
                ValueField(
                    label = stringResource(R.string.field_safe_alt),
                    value = formattedQuantity(calc?.safeAltitudeM, Quantity.ALTITUDE, units, 0),
                    highlight = true,
                    help = stringResource(R.string.hint_safe_alt)
                )
                Spacer(Modifier.height(10.dp))
                TextInput(
                    label = stringResource(R.string.field_route),
                    value = leg.route,
                    onValueChange = { onLegChange(leg.copy(route = it)) },
                    help = stringResource(R.string.hint_route)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        label = stringResource(R.string.field_magnetic_track),
                        canonicalValue = leg.magneticTrack,
                        onCanonicalChange = { onLegChange(leg.copy(magneticTrack = it)) },
                        quantity = Quantity.NONE,
                        units = units,
                        maxDecimals = 0,
                        unitText = "°",
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_magnetic_track)
                    )
                    ValueField(
                        label = stringResource(R.string.field_magnetic_heading),
                        value = "${formattedQuantity(calc?.magneticCourseDeg, Quantity.NONE, units, 0)}°",
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_magnetic_heading)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        label = stringResource(R.string.field_true_airspeed),
                        canonicalValue = leg.trueAirspeedKmh,
                        onCanonicalChange = { onLegChange(leg.copy(trueAirspeedKmh = it)) },
                        quantity = Quantity.SPEED,
                        units = units,
                        maxDecimals = 0,
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_true_airspeed)
                    )
                    ValueField(
                        label = stringResource(R.string.field_ground_speed),
                        value = formattedQuantity(calc?.groundSpeedKmh, Quantity.SPEED, units, 0),
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_ground_speed)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        label = stringResource(R.string.field_distance),
                        canonicalValue = leg.distanceKm,
                        onCanonicalChange = { onLegChange(leg.copy(distanceKm = it)) },
                        quantity = Quantity.DISTANCE,
                        units = units,
                        maxDecimals = 0,
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_distance)
                    )
                    ValueField(
                        label = stringResource(R.string.field_time),
                        value = formatMinutes(calc?.timeMinutes),
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_time)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ValueField(
                        label = stringResource(R.string.field_fuel_calc),
                        value = formattedQuantity(calc?.fuelRemainingKg, Quantity.MASS, units),
                        modifier = Modifier.weight(1f),
                        highlight = true,
                        help = stringResource(R.string.hint_fuel_calc)
                    )
                    NumberField(
                        label = stringResource(R.string.field_fuel_actual),
                        canonicalValue = leg.fuelActualKg,
                        onCanonicalChange = { onLegChange(leg.copy(fuelActualKg = it)) },
                        quantity = Quantity.MASS,
                        units = units,
                        maxDecimals = 1,
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_fuel_actual)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField(
                        label = stringResource(R.string.field_calc_time),
                        value = leg.calcTimeMinutes,
                        onValueChange = { onLegChange(leg.copy(calcTimeMinutes = it)) },
                        suffix = stringResource(R.string.unit_min),
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_calc_time)
                    )
                    IntField(
                        label = stringResource(R.string.field_actual_time),
                        value = leg.actualTimeMinutes,
                        onValueChange = { onLegChange(leg.copy(actualTimeMinutes = it)) },
                        suffix = stringResource(R.string.unit_min),
                        modifier = Modifier.weight(1f),
                        help = stringResource(R.string.hint_actual_time)
                    )
                }
                Spacer(Modifier.height(10.dp))
                NumberField(
                    label = stringResource(R.string.field_flight_level),
                    canonicalValue = leg.flightLevelM,
                    onCanonicalChange = { onLegChange(leg.copy(flightLevelM = it)) },
                    quantity = Quantity.ALTITUDE,
                    units = units,
                    maxDecimals = 0,
                    help = stringResource(R.string.hint_flight_level)
                )
                Spacer(Modifier.height(10.dp))
                TextInput(
                    label = stringResource(R.string.field_notes),
                    value = leg.notes,
                    onValueChange = { onLegChange(leg.copy(notes = it)) },
                    singleLine = false,
                    help = stringResource(R.string.hint_notes)
                )
            }
        }
    }
}

@Composable
private fun FuelCard(
    journal: Journal,
    calc: JournalCalculation,
    units: UnitSystem,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    val fuel = journal.fuel

    SectionCard(
        title = stringResource(R.string.section_fuel),
        modifier = modifier,
        collapsible = true
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_fuel_ground),
                canonicalValue = fuel.groundKg,
                onCanonicalChange = { onChange(journal.copy(fuel = fuel.copy(groundKg = it))) },
                quantity = Quantity.MASS,
                units = units,
                maxDecimals = 1,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_fuel_ground)
            )
            NumberField(
                label = stringResource(R.string.field_fuel_takeoff_landing),
                canonicalValue = fuel.takeoffLandingKg,
                onCanonicalChange = { onChange(journal.copy(fuel = fuel.copy(takeoffLandingKg = it))) },
                quantity = Quantity.MASS,
                units = units,
                maxDecimals = 1,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_fuel_takeoff_landing)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_fuel_reserve),
                canonicalValue = fuel.reserveKg,
                onCanonicalChange = { onChange(journal.copy(fuel = fuel.copy(reserveKg = it))) },
                quantity = Quantity.MASS,
                units = units,
                maxDecimals = 1,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_fuel_reserve)
            )
            NumberField(
                label = stringResource(R.string.field_hourly_consumption),
                canonicalValue = fuel.hourlyConsumptionKg,
                onCanonicalChange = { onChange(journal.copy(fuel = fuel.copy(hourlyConsumptionKg = it))) },
                quantity = Quantity.FUEL_RATE,
                units = units,
                maxDecimals = 1,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_fuel_hourly)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_fuel_density),
                canonicalValue = fuel.density,
                onCanonicalChange = { onChange(journal.copy(fuel = fuel.copy(density = it))) },
                quantity = Quantity.NONE,
                units = units,
                maxDecimals = 2,
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_fuel_density)
            )
            IntField(
                label = stringResource(R.string.field_landing_time_param),
                value = fuel.landingTimeMinutes,
                onValueChange = { onChange(journal.copy(fuel = fuel.copy(landingTimeMinutes = it))) },
                suffix = stringResource(R.string.unit_min),
                modifier = Modifier.weight(1f),
                help = stringResource(R.string.hint_landing_time_param)
            )
        }
        Spacer(Modifier.height(12.dp))
        ValueField(
            label = stringResource(R.string.field_total_fuel),
            value = formattedQuantity(calc.totalFuelKg, Quantity.MASS, units),
            highlight = true,
            help = stringResource(R.string.hint_total_fuel)
        )
    }
}

@Composable
private fun ActualCard(
    journal: Journal,
    units: UnitSystem,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    val actual = journal.actual

    SectionCard(
        title = stringResource(R.string.section_actual),
        modifier = modifier,
        collapsible = true
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IntField(
                label = stringResource(R.string.field_actual_flight_time),
                value = actual.actualTimeMinutes,
                onValueChange = { onChange(journal.copy(actual = actual.copy(actualTimeMinutes = it))) },
                suffix = stringResource(R.string.unit_min),
                modifier = Modifier.weight(1f)
            )
            IntField(
                label = stringResource(R.string.field_night_time),
                value = actual.nightTimeMinutes,
                onValueChange = { onChange(journal.copy(actual = actual.copy(nightTimeMinutes = it))) },
                suffix = stringResource(R.string.unit_min),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.field_go_around),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_distance_s),
                canonicalValue = actual.goAroundDistanceKm,
                onCanonicalChange = { onChange(journal.copy(actual = actual.copy(goAroundDistanceKm = it))) },
                quantity = Quantity.DISTANCE,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = stringResource(R.string.field_fuel_q),
                canonicalValue = actual.goAroundFuelKg,
                onCanonicalChange = { onChange(journal.copy(actual = actual.copy(goAroundFuelKg = it))) },
                quantity = Quantity.MASS,
                units = units,
                maxDecimals = 1,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.field_diversion),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.field_diversion_s),
                canonicalValue = actual.diversionDistanceKm,
                onCanonicalChange = { onChange(journal.copy(actual = actual.copy(diversionDistanceKm = it))) },
                quantity = Quantity.DISTANCE,
                units = units,
                maxDecimals = 0,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = stringResource(R.string.field_diversion_q),
                canonicalValue = actual.diversionFuelKg,
                onCanonicalChange = { onChange(journal.copy(actual = actual.copy(diversionFuelKg = it))) },
                quantity = Quantity.MASS,
                units = units,
                maxDecimals = 1,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        TextInput(
            label = stringResource(R.string.field_notes),
            value = journal.notes,
            onValueChange = { onChange(journal.copy(notes = it)) },
            singleLine = false
        )
    }
}

@Composable
private fun SignaturesCard(
    journal: Journal,
    pilots: List<Pilot>,
    languageTag: String,
    onChange: (Journal) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.section_signatures),
        modifier = modifier,
        collapsible = true
    ) {
        PilotPickerField(
            roleLabel = stringResource(R.string.field_duty_navigator),
            selectedPilotId = journal.pilots.dutyNavigatorId,
            pilots = pilots,
            onSelect = {
                onChange(journal.copy(pilots = journal.pilots.withRole(PilotRole.DUTY_NAVIGATOR, it)))
            }
        )
        Spacer(Modifier.height(8.dp))
        PilotPickerField(
            roleLabel = stringResource(R.string.field_squadron_navigator),
            selectedPilotId = journal.pilots.squadronNavigatorId,
            pilots = pilots,
            onSelect = {
                onChange(journal.copy(pilots = journal.pilots.withRole(PilotRole.SQUADRON_NAVIGATOR, it)))
            }
        )
        Spacer(Modifier.height(10.dp))
        DateField(
            label = stringResource(R.string.field_preflight_date),
            isoDate = journal.preflightDateIso.ifBlank {
                journal.dateIso.ifBlank { LocalDate.now().toString() }
            },
            onDateChange = { onChange(journal.copy(preflightDateIso = it)) }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.field_preflight_check),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}




private fun degreesText(value: String): String = if (value == "—") "—" else "$value°"

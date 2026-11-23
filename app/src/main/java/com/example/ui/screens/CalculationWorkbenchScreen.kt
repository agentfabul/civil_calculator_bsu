package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.calc.CalcModule
import com.example.domain.calc.CalcRegistry
import com.example.domain.calc.CalcResult
import com.example.domain.calc.ExampleCase
import com.example.domain.calc.InputFieldSpec
import com.example.domain.calc.InputValueType
import com.example.domain.calc.UnitSystem
import com.example.ui.WorkbenchDraft
import com.example.ui.components.CalcStatusBadge
import com.example.ui.components.EngineeringSafeguardNotice
import com.example.ui.components.EquationStepCard
import com.example.ui.components.NatureBadge
import com.example.ui.components.UnitSystemSelector
import com.example.ui.components.UtilizationMeter
import com.example.ui.components.ValidationIssueBanner
import com.example.ui.theme.MonoMetricStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationWorkbenchScreen(
    draft: WorkbenchDraft,
    isBusy: Boolean,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onUnitSwitch: (UnitSystem) -> Unit,
    onStandardSwitch: (String) -> Unit,
    onInputChange: (String, String) -> Unit,
    onAssumptionChange: (String, String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRevisionNoteChange: (String) -> Unit,
    onLoadExample: (ExampleCase) -> Unit,
    onSaveRevision: () -> Unit,
    onExportReport: () -> Unit
) {
    BackHandler { onBack() }

    val module = remember(draft.moduleId) { CalcRegistry.byId(draft.moduleId) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExamplesSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("workbench_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (draft.existingCalcId != null) {
                                "Editing Rev ${draft.existingRevisionNumber} → Save as Rev ${draft.existingRevisionNumber + 1}"
                            } else {
                                "New Calculation • ${module.category.shortLabel}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showExamplesSheet = true },
                        modifier = Modifier.testTag("examples_button")
                    ) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Examples")
                    }
                    IconButton(
                        onClick = onExportReport,
                        enabled = draft.result != null,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("export_report_button")
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = "Export Calculation Report")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        draft.result?.let { res ->
                            Text(
                                text = res.status.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = res.governingCheck,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onSaveRevision,
                        enabled = !isBusy && (draft.result?.isValid == true),
                        modifier = Modifier.testTag("save_revision_button")
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (draft.existingCalcId == null) "Save" else "Save Rev ${draft.existingRevisionNumber + 1}")
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 840.dp
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    InputsAndAssumptionsPane(
                        module = module,
                        draft = draft,
                        onTitleChange = onTitleChange,
                        onUnitSwitch = onUnitSwitch,
                        onStandardSwitch = onStandardSwitch,
                        onInputChange = onInputChange,
                        onAssumptionChange = onAssumptionChange,
                        onNotesChange = onNotesChange,
                        onRevisionNoteChange = onRevisionNoteChange,
                        modifier = Modifier.weight(0.48f).fillMaxHeight()
                    )
                    ResultsBreakdownPane(
                        module = module,
                        result = draft.result,
                        modifier = Modifier.weight(0.52f).fillMaxHeight()
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("1. Inputs & Assumptions") },
                            modifier = Modifier.testTag("workbench_tab_inputs")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("2. Results & Equations") },
                            modifier = Modifier.testTag("workbench_tab_results")
                        )
                    }
                    if (selectedTab == 0) {
                        InputsAndAssumptionsPane(
                            module = module,
                            draft = draft,
                            onTitleChange = onTitleChange,
                            onUnitSwitch = onUnitSwitch,
                            onStandardSwitch = onStandardSwitch,
                            onInputChange = onInputChange,
                            onAssumptionChange = onAssumptionChange,
                            onNotesChange = onNotesChange,
                            onRevisionNoteChange = onRevisionNoteChange,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ResultsBreakdownPane(
                            module = module,
                            result = draft.result,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showExamplesSheet) {
        ModalBottomSheet(onDismissRequest = { showExamplesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Verified Example Cases", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Load a reference engineering case to inspect inputs, equations, and expected outcomes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                module.examples().forEach { ex ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(ex.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ex.description, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Expected: ${ex.expectedNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                showExamplesSheet = false
                                onLoadExample(ex)
                            }) {
                                Text("Load into Workbench")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputsAndAssumptionsPane(
    module: CalcModule,
    draft: WorkbenchDraft,
    onTitleChange: (String) -> Unit,
    onUnitSwitch: (UnitSystem) -> Unit,
    onStandardSwitch: (String) -> Unit,
    onInputChange: (String, String) -> Unit,
    onAssumptionChange: (String, String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRevisionNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val inputSpecs = remember(module, draft.unitSystem, draft.standardId) {
        module.inputSpecs(draft.unitSystem, draft.standardId)
    }
    val assumptionSpecs = remember(module, draft.unitSystem, draft.standardId) {
        module.assumptionSpecs(draft.unitSystem, draft.standardId)
    }
    val activeStd = module.supportedStandards.firstOrNull { it.id == draft.standardId }
        ?: module.supportedStandards.first()

    var assumptionsExpanded by remember { mutableStateOf(true) }
    val errorByField = remember(draft.result) {
        draft.result?.issues
            ?.filter { it.fieldKey != null }
            ?.associateBy { it.fieldKey!! }
            ?: emptyMap()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        item {
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Calculation Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("calc_title_input")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Unit System", style = MaterialTheme.typography.labelLarge)
                UnitSystemSelector(
                    selected = draft.unitSystem,
                    onSelect = onUnitSwitch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Design Code / Standard Method", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (module.supportedStandards.size > 1) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = "${activeStd.name} (${activeStd.edition})",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                module.supportedStandards.forEach { std ->
                                    DropdownMenuItem(
                                        text = { Text("${std.name} (${std.edition})") },
                                        onClick = {
                                            expanded = false
                                            onStandardSwitch(std.id)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${activeStd.name} (${activeStd.edition})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeStd.methodSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Guided Engineering Inputs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(inputSpecs, key = { it.key }) { spec ->
            val currentVal = draft.inputs[spec.key].orEmpty()
            val issue = errorByField[spec.key]
            InputSpecField(
                spec = spec,
                unitSystem = draft.unitSystem,
                value = currentVal,
                issueMessage = issue?.message,
                isError = issue?.severity == com.example.domain.calc.IssueSeverity.ERROR,
                onValueChange = { onInputChange(spec.key, it) }
            )
        }

        if (assumptionSpecs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Explicit Editable Assumptions (${assumptionSpecs.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(
                                onClick = { assumptionsExpanded = !assumptionsExpanded },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (assumptionsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = "Toggle assumptions"
                                )
                            }
                        }
                        if (assumptionsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            assumptionSpecs.forEach { aSpec ->
                                val v = draft.assumptions[aSpec.key] ?: aSpec.defaultValue
                                OutlinedTextField(
                                    value = v,
                                    onValueChange = { onAssumptionChange(aSpec.key, it) },
                                    label = { Text("${aSpec.label} (${aSpec.unit})") },
                                    supportingText = { Text(aSpec.description) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .testTag("assumption_${aSpec.key}")
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = draft.notes,
                onValueChange = onNotesChange,
                label = { Text("Calculation Notes (Included in Report)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = draft.revisionNote,
                onValueChange = onRevisionNoteChange,
                label = { Text("Revision Tag / Change Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSpecField(
    spec: InputFieldSpec,
    unitSystem: UnitSystem,
    value: String,
    issueMessage: String?,
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    val unit = spec.unitFor(unitSystem)
    if (spec.valueType == InputValueType.CHOICE) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(spec.label) },
                supportingText = { Text(issueMessage ?: spec.helperText) },
                isError = isError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .testTag("input_${spec.key}")
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                spec.choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice) },
                        onClick = {
                            expanded = false
                            onValueChange(choice)
                        }
                    )
                }
            }
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (unit == "-") spec.label else "${spec.label} [$unit]") },
            trailingIcon = {
                if (unit != "-") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            supportingText = { Text(issueMessage ?: spec.helperText) },
            isError = isError,
            singleLine = spec.valueType == InputValueType.NUMBER,
            keyboardOptions = if (spec.valueType == InputValueType.NUMBER) {
                KeyboardOptions(keyboardType = KeyboardType.Decimal)
            } else {
                KeyboardOptions.Default
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_${spec.key}")
        )
    }
}

@Composable
private fun ResultsBreakdownPane(
    module: CalcModule,
    result: CalcResult?,
    modifier: Modifier = Modifier
) {
    if (result == null) return
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalcStatusBadge(status = result.status)
                        NatureBadge(nature = result.nature)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = result.governingCheck,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    result.standard?.let { std ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Standard: ${std.name} (${std.edition})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    result.utilizationRatio?.takeIf { it.isFinite() }?.let { util ->
                        Spacer(modifier = Modifier.height(12.dp))
                        UtilizationMeter(ratio = util, label = "Governing Demand / Capacity Utilization")
                    }
                }
            }
        }

        if (result.issues.isNotEmpty()) {
            items(result.issues) { issue ->
                ValidationIssueBanner(issue = issue)
            }
        }

        if (result.summaryMetrics.isNotEmpty()) {
            item {
                Text(
                    text = "Key Outputs (${result.unitSystem.label})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.summaryMetrics.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { metric ->
                                Surface(
                                    color = if (metric.highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = metric.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = metric.formattedValue,
                                                style = MonoMetricStyle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (metric.unit.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = metric.unit,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (result.steps.isNotEmpty()) {
            item {
                Text(
                    text = "Transparent Calculation Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(result.steps, key = { it.stepNumber }) { step ->
                EquationStepCard(step = step)
            }
        }

        if (module.engineeringNotes.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Engineering Notes", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        module.engineeringNotes.forEach { n ->
                            Text("• $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            EngineeringSafeguardNotice(scopeItems = result.scopeAndLimitations)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

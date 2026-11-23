package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.data.firebase.CalculationRevision
import com.example.domain.calc.CalcFmt
import com.example.domain.calc.CalcStatus
import com.example.ui.UiState
import com.example.ui.components.CalcStatusBadge
import com.example.ui.theme.MonoEquationStyle
import com.example.ui.theme.StatusWarnContainer
import com.example.ui.theme.StatusWarnText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionCompareScreen(
    revisionsState: UiState<List<CalculationRevision>>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("compare_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Compare Calculation Revisions") }
            )
        }
    ) { innerPadding ->
        when (revisionsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                Text(revisionsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                val revs = revisionsState.data
                if (revs.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No revision snapshots saved for this calculation yet.")
                    }
                } else {
                    var idxA by remember(revs) { mutableIntStateOf(if (revs.size > 1) 1 else 0) }
                    var idxB by remember(revs) { mutableIntStateOf(0) }
                    val revA = revs[idxA.coerceIn(revs.indices)]
                    val revB = revs[idxB.coerceIn(revs.indices)]

                    val allInputKeys = remember(revA, revB) {
                        (revA.inputs.keys + revB.inputs.keys).distinct().sorted()
                    }
                    val allAssumptionKeys = remember(revA, revB) {
                        (revA.assumptions.keys + revB.assumptions.keys).distinct().sorted()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RevisionPicker(
                                    label = "Baseline (Rev A)",
                                    revisions = revs,
                                    selectedIndex = idxA,
                                    onSelect = { idxA = it },
                                    modifier = Modifier.weight(1f)
                                )
                                RevisionPicker(
                                    label = "Target (Rev B)",
                                    revisions = revs,
                                    selectedIndex = idxB,
                                    onSelect = { idxB = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Status & Governing Check Comparison", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Rev ${revA.revisionNumber}", style = MaterialTheme.typography.labelLarge)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            CalcStatusBadge(CalcStatus.fromCode(revA.status))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(revA.governingCheck, style = MaterialTheme.typography.bodySmall)
                                            revA.utilizationRatio?.let {
                                                Text("Util: ${CalcFmt.num(it, 3)}", style = MonoEquationStyle)
                                            }
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("Rev ${revB.revisionNumber}", style = MaterialTheme.typography.labelLarge)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            CalcStatusBadge(CalcStatus.fromCode(revB.status))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(revB.governingCheck, style = MaterialTheme.typography.bodySmall)
                                            revB.utilizationRatio?.let { uB ->
                                                val deltaStr = revA.utilizationRatio?.let { uA ->
                                                    val d = uB - uA
                                                    " (${if (d >= 0) "+" else ""}${CalcFmt.num(d, 3)})"
                                                } ?: ""
                                                Text("Util: ${CalcFmt.num(uB, 3)}$deltaStr", style = MonoEquationStyle)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Input Parameter Diffs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        items(allInputKeys) { key ->
                            val vA = revA.inputs[key] ?: "—"
                            val vB = revB.inputs[key] ?: "—"
                            DiffRowCard(label = key, valueA = vA, valueB = vB, changed = vA != vB)
                        }

                        if (allAssumptionKeys.isNotEmpty()) {
                            item {
                                Text("Assumptions Diffs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            items(allAssumptionKeys) { key ->
                                val vA = revA.assumptions[key] ?: "—"
                                val vB = revB.assumptions[key] ?: "—"
                                DiffRowCard(label = key, valueA = vA, valueB = vB, changed = vA != vB)
                            }
                        }

                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Rev ${revB.revisionNumber} Primary Outputs", style = MaterialTheme.typography.titleSmall)
                                    revB.primaryOutputs.forEach { out ->
                                        Text("• $out", style = MonoEquationStyle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevisionPicker(
    label: String,
    revisions: List<CalculationRevision>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = revisions[selectedIndex.coerceIn(revisions.indices)]
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "Rev ${selected.revisionNumber} (${selected.status})",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            revisions.forEachIndexed { i, rev ->
                DropdownMenuItem(
                    text = { Text("Rev ${rev.revisionNumber}: ${rev.revisionNote.ifBlank { rev.status }}") },
                    onClick = {
                        expanded = false
                        onSelect(i)
                    }
                )
            }
        }
    }
}

@Composable
private fun DiffRowCard(
    label: String,
    valueA: String,
    valueB: String,
    changed: Boolean
) {
    Surface(
        color = if (changed) StatusWarnContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.4f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (changed) {
                    Text("CHANGED", style = MaterialTheme.typography.labelSmall, color = StatusWarnText, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(valueA, style = MonoEquationStyle, modifier = Modifier.weight(0.3f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(valueB, style = MonoEquationStyle, fontWeight = if (changed) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(0.3f))
        }
    }
}

package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.EngineeringProject
import com.example.data.firebase.SavedCalculation
import com.example.domain.calc.CalcCategory
import com.example.domain.calc.CalcModule
import com.example.domain.calc.CalcModuleId
import com.example.domain.calc.CalcRegistry
import com.example.domain.calc.CalcStatus
import com.example.ui.UiState
import com.example.ui.components.CalcStatusBadge
import com.example.ui.components.NatureBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    project: EngineeringProject?,
    calculationsState: UiState<List<SavedCalculation>>,
    onBack: () -> Unit,
    onStartNewCalc: (EngineeringProject, CalcModuleId) -> Unit,
    onOpenSavedCalc: (SavedCalculation) -> Unit,
    onDuplicateCalc: (SavedCalculation) -> Unit,
    onCompareCalc: (SavedCalculation) -> Unit,
    onDeleteCalc: (String, String) -> Unit,
    onExportBackup: (EngineeringProject, (String) -> Unit) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<CalcCategory?>(null) }
    var pendingDeleteCalc by remember { mutableStateOf<SavedCalculation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("workspace_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Projects")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = project?.name ?: "Project Workspace",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (project != null) {
                            Text(
                                text = listOf(project.clientName, project.location)
                                    .filter { it.isNotBlank() }
                                    .ifEmpty { listOf("${project.defaultUnitSystem} • ${project.defaultStandard}") }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (project != null) {
                        IconButton(
                            onClick = {
                                onExportBackup(project) { json ->
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "CivilSpec Backup - ${project.name}")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Backup Project JSON"))
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("export_backup_button")
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = "Export Project JSON Backup")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        val count = (calculationsState as? UiState.Success)?.data?.size ?: 0
                        Text("Saved Calculations ($count)")
                    },
                    modifier = Modifier.testTag("tab_saved_calcs")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("New Calculation (6 Modules)") },
                    modifier = Modifier.testTag("tab_module_catalog")
                )
            }

            if (project == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == 0) {
                when (calculationsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(calculationsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val calcs = calculationsState.data
                        if (calcs.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No Saved Calculations Yet", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Launch one of the 6 civil engineering modules to compute, review equations, and save revision snapshots.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { selectedTab = 1 },
                                    modifier = Modifier.testTag("browse_modules_button")
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse 6 Calculation Modules")
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(calcs, key = { it.id }) { calc ->
                                    SavedCalculationCard(
                                        calc = calc,
                                        onOpen = { onOpenSavedCalc(calc) },
                                        onDuplicate = { onDuplicateCalc(calc) },
                                        onCompare = { onCompareCalc(calc) },
                                        onDelete = { pendingDeleteCalc = calc }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { selectedTab = 1 },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Another Calculation")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All (12)") }
                            )
                        }
                        items(CalcCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.shortLabel) }
                            )
                        }
                    }

                    val modules = remember(selectedCategory) {
                        if (selectedCategory == null) CalcRegistry.modules
                        else CalcRegistry.byCategory(selectedCategory!!)
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(modules, key = { it.id.id }) { mod ->
                            ModuleCatalogCard(
                                module = mod,
                                onClick = { onStartNewCalc(project, mod.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    pendingDeleteCalc?.let { calc ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCalc = null },
            title = { Text("Delete Calculation?") },
            text = { Text("Delete \"${calc.title}\" and all its revision snapshots?") },
            confirmButton = {
                Button(onClick = {
                    pendingDeleteCalc = null
                    onDeleteCalc(calc.projectId, calc.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCalc = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SavedCalculationCard(
    calc: SavedCalculation,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onCompare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("saved_calc_card_${calc.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalcStatusBadge(status = CalcStatus.fromCode(calc.latestStatus))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Rev ${calc.latestRevisionNumber} • ${calc.unitSystem}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = calc.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = calc.latestSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCompare) {
                    Icon(Icons.AutoMirrored.Outlined.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare")
                }
                TextButton(onClick = onDuplicate) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Duplicate")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete calculation")
                }
            }
        }
    }
}

@Composable
private fun ModuleCatalogCard(
    module: CalcModule,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("module_card_${module.id.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = module.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                NatureBadge(nature = module.nature)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = module.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = module.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Standards: ${module.supportedStandards.joinToString(" | ") { it.name }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

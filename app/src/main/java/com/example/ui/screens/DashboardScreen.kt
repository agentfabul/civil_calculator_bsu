package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.EngineeringProject
import com.example.data.firebase.UserProfile
import com.example.domain.calc.CalcCategory
import com.example.domain.calc.CalcModuleId
import com.example.domain.calc.CalcRegistry
import com.example.domain.calc.StandardFamily
import com.example.domain.calc.UnitSystem
import com.example.ui.UiState
import com.example.ui.components.EngineeringSafeguardNotice
import com.example.ui.theme.BatStateCrimson
import com.example.ui.theme.BatStateCrimsonDeep
import com.example.ui.theme.BatStateGoldBright
import com.example.ui.theme.BatStateGoldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userProfileState: UiState<UserProfile?>,
    projectsState: UiState<List<EngineeringProject>>,
    selectedProjectId: String?,
    onSelectProjectPane: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onCreateProject: (String, String, String, String, UnitSystem, StandardFamily) -> Unit,
    onDeleteProject: (String) -> Unit,
    onQuickLaunchModule: (EngineeringProject, CalcModuleId) -> Unit,
    onOpenProfile: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingDeleteProject by remember { mutableStateOf<EngineeringProject?>(null) }

    val profile = (userProfileState as? UiState.Success)?.data
    val defaultUnit = UnitSystem.fromCode(profile?.preferredUnitSystem)
    val defaultStd = StandardFamily.fromCode(profile?.preferredStandard)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_fg),
                            contentDescription = "App logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CivilSpec Workspace",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile?.displayName?.ifBlank { "Registered Engineer" } ?: "Engineering Projects",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Engineer Profile & Preferences"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_project_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New Project")
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 840.dp

            val projects = (projectsState as? UiState.Success)?.data.orEmpty()
            val filtered = remember(projects, searchQuery) {
                if (searchQuery.isBlank()) projects
                else projects.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.clientName.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true)
                }
            }

            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationRailItem(
                            selected = true,
                            onClick = {},
                            icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = "Projects") },
                            label = { Text("Projects") }
                        )
                        NavigationRailItem(
                            selected = false,
                            onClick = { showCreateDialog = true },
                            icon = { Icon(Icons.Outlined.Add, contentDescription = "New Project") },
                            label = { Text("New") }
                        )
                        NavigationRailItem(
                            selected = false,
                            onClick = onOpenProfile,
                            icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = "Profile") },
                            label = { Text("Profile") }
                        )
                    }

                    // Left pane: Project List
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        ProjectSearchHeader(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onNewProject = { showCreateDialog = true }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ProjectListContent(
                            projectsState = projectsState,
                            filteredProjects = filtered,
                            selectedProjectId = selectedProjectId ?: filtered.firstOrNull()?.id,
                            onClickProject = { proj ->
                                onSelectProjectPane(proj.id)
                            },
                            onOpenProject = onOpenProject,
                            onRequestDelete = { pendingDeleteProject = it },
                            onCreateProjectClick = { showCreateDialog = true }
                        )
                    }

                    // Right pane: Quick Module Launch & Overview
                    val activeProj = filtered.firstOrNull { it.id == selectedProjectId } ?: filtered.firstOrNull()
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    ) {
                        if (activeProj != null) {
                            ExpandedProjectOverviewPane(
                                project = activeProj,
                                onOpenFullWorkspace = { onOpenProject(activeProj.id) },
                                onQuickLaunch = { modId -> onQuickLaunchModule(activeProj, modId) }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Create or select a project on the left to launch engineering calculations.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        HeroDashboardCard(
                            projectCount = projects.size,
                            onCreateClick = { showCreateDialog = true }
                        )
                    }
                    item {
                        ProjectSearchHeader(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onNewProject = { showCreateDialog = true }
                        )
                    }
                    when (projectsState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is UiState.Error -> {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = projectsState.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                        is UiState.Success -> {
                            if (filtered.isEmpty()) {
                                item {
                                    EmptyProjectsCard(onCreateClick = { showCreateDialog = true })
                                }
                            } else {
                                items(filtered, key = { it.id }) { proj ->
                                    ProjectCard(
                                        project = proj,
                                        isSelected = false,
                                        onClick = { onOpenProject(proj.id) },
                                        onDeleteClick = { pendingDeleteProject = proj }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        EngineeringSafeguardNotice(
                            scopeItems = listOf(
                                "Calculations are organized by project with immutable revision snapshots.",
                                "Always confirm applicable local building codes, geotechnical reports, and site conditions."
                            )
                        )
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            initialUnit = defaultUnit,
            initialStandard = defaultStd,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, client, loc, notes, unit, std ->
                showCreateDialog = false
                onCreateProject(name, client, loc, notes, unit, std)
            }
        )
    }

    pendingDeleteProject?.let { proj ->
        AlertDialog(
            onDismissRequest = { pendingDeleteProject = null },
            title = { Text("Delete Project?") },
            text = {
                Text("Delete \"${proj.name}\" and all saved calculations and revisions? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteProject = null
                        onDeleteProject(proj.id)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroDashboardCard(
    projectCount: Int,
    onCreateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_blueprint_header),
            contentDescription = "Engineering blueprint banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(BatStateCrimsonDeep.copy(alpha = 0.94f), BatStateCrimson.copy(alpha = 0.80f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$projectCount Active ${if (projectCount == 1) "Project" else "Projects"}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "6 engineering modules • Transparent formulas • Revision control",
                    style = MaterialTheme.typography.bodySmall,
                    color = BatStateGoldContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onCreateClick,
                border = BorderStroke(1.5.dp, BatStateGoldBright),
                modifier = Modifier.testTag("hero_new_project_button")
            ) {
                Text("New Project", color = BatStateGoldBright, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProjectSearchHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNewProject: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search projects, clients, or locations...") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_search_input")
    )
}

@Composable
private fun ProjectListContent(
    projectsState: UiState<List<EngineeringProject>>,
    filteredProjects: List<EngineeringProject>,
    selectedProjectId: String?,
    onClickProject: (EngineeringProject) -> Unit,
    onOpenProject: (String) -> Unit,
    onRequestDelete: (EngineeringProject) -> Unit,
    onCreateProjectClick: () -> Unit
) {
    when (projectsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Text(projectsState.message, color = MaterialTheme.colorScheme.error)
        is UiState.Success -> {
            if (filteredProjects.isEmpty()) {
                EmptyProjectsCard(onCreateClick = onCreateProjectClick)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredProjects, key = { it.id }) { proj ->
                        ProjectCard(
                            project = proj,
                            isSelected = proj.id == selectedProjectId,
                            onClick = {
                                onClickProject(proj)
                                onOpenProject(proj.id)
                            },
                            onDeleteClick = { onRequestDelete(proj) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: EngineeringProject,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (project.clientName.isNotBlank() || project.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = listOf(project.clientName, project.location).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${project.calculationCount} calcs",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${project.defaultUnitSystem} • ${if (project.defaultStandard == "EUROCODE") "EC" else "ACI/ASCE"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete project",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandedProjectOverviewPane(
    project: EngineeringProject,
    onOpenFullWorkspace: () -> Unit,
    onQuickLaunch: (CalcModuleId) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.headlineSmall)
                    if (project.notes.isNotBlank()) {
                        Text(project.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = onOpenFullWorkspace) {
                    Text("Open Project")
                }
            }
        }
        CalcCategory.entries.forEach { cat ->
            val mods = CalcRegistry.byCategory(cat)
            item {
                Text(
                    text = cat.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(mods, key = { it.id.id }) { mod ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickLaunch(mod.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(mod.name, style = MaterialTheme.typography.titleSmall)
                        Text(mod.shortDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProjectsCard(onCreateClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = "No projects",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("No Engineering Projects Yet", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create a project to organize structural checks, concrete takeoffs, foundation sizing, hydrology, and survey reductions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                modifier = Modifier.testTag("empty_create_project_button")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Project")
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    initialUnit: UnitSystem,
    initialStandard: StandardFamily,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, UnitSystem, StandardFamily) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(initialUnit) }
    var std by remember { mutableStateOf(initialStandard) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Engineering Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name *") },
                    placeholder = { Text("e.g., Oak Street Culvert & Footing") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("project_name_input")
                )
                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Client / Owner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Site Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Scope Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Default Unit System", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UnitSystem.entries.forEachIndexed { i, u ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, UnitSystem.entries.size),
                            selected = unit == u,
                            onClick = { unit = u }
                        ) {
                            Text(u.label)
                        }
                    }
                }
                Text("Default Design Code Family", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    StandardFamily.entries.forEachIndexed { i, s ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, StandardFamily.entries.size),
                            selected = std == s,
                            onClick = { std = s }
                        ) {
                            Text(if (s == StandardFamily.ACI_ASCE) "ACI / ASCE" else "Eurocode")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, client, location, notes, unit, std) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

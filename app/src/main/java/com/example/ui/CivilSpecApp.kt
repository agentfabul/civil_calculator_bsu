package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOutUser
import com.example.data.firebase.CivilSpecRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalculationWorkbenchScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ProjectWorkspaceScreen
import com.example.ui.screens.ReportPreviewScreen
import com.example.ui.screens.VersionCompareScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CivilSpecApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedCivilSpecContent(user = user)
        }
    }
}

@Composable
private fun AuthenticatedCivilSpecContent(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val vm: CivilSpecViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CivilSpecViewModel(
                    repository = CivilSpecRepository(db),
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "",
                    userEmail = user.email ?: ""
                )
            }
        }
    )

    val screen by vm.currentScreen.collectAsStateWithLifecycle()
    val profileState by vm.userProfileState.collectAsStateWithLifecycle()
    val projectsState by vm.projectsState.collectAsStateWithLifecycle()
    val selectedProjectId by vm.selectedProjectId.collectAsStateWithLifecycle()
    val calculationsState by vm.calculationsState.collectAsStateWithLifecycle()
    val revisionsState by vm.revisionsState.collectAsStateWithLifecycle()
    val workbench by vm.workbench.collectAsStateWithLifecycle()
    val isBusy by vm.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearSnackbar()
        }
    }

    val activeProject = remember(projectsState, selectedProjectId) {
        (projectsState as? UiState.Success)?.data?.firstOrNull { it.id == selectedProjectId }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val activeScreen = screen) {
                is CivilScreen.Dashboard -> DashboardScreen(
                    userProfileState = profileState,
                    projectsState = projectsState,
                    selectedProjectId = selectedProjectId,
                    onSelectProjectPane = vm::selectProjectForPane,
                    onOpenProject = vm::openProject,
                    onCreateProject = vm::createProject,
                    onDeleteProject = vm::deleteProject,
                    onQuickLaunchModule = vm::startNewCalculation,
                    onOpenProfile = { vm.navigateTo(CivilScreen.ProfileSettings) }
                )

                is CivilScreen.ProjectWorkspace -> {
                    val proj = (projectsState as? UiState.Success)?.data
                        ?.firstOrNull { it.id == activeScreen.projectId }
                    ProjectWorkspaceScreen(
                        project = proj,
                        calculationsState = calculationsState,
                        onBack = { vm.navigateBack() },
                        onStartNewCalc = vm::startNewCalculation,
                        onOpenSavedCalc = vm::openSavedCalculation,
                        onDuplicateCalc = vm::duplicateCalculation,
                        onCompareCalc = vm::openVersionCompare,
                        onDeleteCalc = vm::deleteCalculation,
                        onExportBackup = vm::exportProjectBackup
                    )
                }

                is CivilScreen.Workbench -> CalculationWorkbenchScreen(
                    draft = workbench,
                    isBusy = isBusy,
                    onBack = { vm.navigateBack() },
                    onTitleChange = vm::updateTitle,
                    onUnitSwitch = vm::switchUnitSystem,
                    onStandardSwitch = vm::switchStandard,
                    onInputChange = vm::updateInput,
                    onAssumptionChange = vm::updateAssumption,
                    onNotesChange = vm::updateNotes,
                    onRevisionNoteChange = vm::updateRevisionNote,
                    onLoadExample = vm::loadExampleCase,
                    onSaveRevision = vm::saveCurrentRevision,
                    onExportReport = {
                        vm.openReportForCurrentDraft(activeProject, user)
                    }
                )

                is CivilScreen.VersionCompare -> VersionCompareScreen(
                    revisionsState = revisionsState,
                    onBack = { vm.navigateBack() }
                )

                is CivilScreen.ReportPreview -> ReportPreviewScreen(
                    title = activeScreen.title,
                    reportText = activeScreen.reportText,
                    onBack = { vm.navigateBack() },
                    onCopied = { vm.showMessage("Calculation report copied to clipboard") }
                )

                is CivilScreen.ProfileSettings -> ProfileSettingsScreen(
                    profileState = profileState,
                    userEmail = user.email ?: "",
                    isBusy = isBusy,
                    onBack = { vm.navigateBack() },
                    onSaveProfile = vm::updateProfile,
                    onSignOut = {
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }
    }
}

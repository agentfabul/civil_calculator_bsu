package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CalculationRevision
import com.example.data.firebase.CivilSpecRepository
import com.example.data.firebase.EngineeringProject
import com.example.data.firebase.SavedCalculation
import com.example.data.firebase.UserProfile
import com.example.data.firebase.unwrapFirestoreException
import com.example.domain.calc.CalcModuleId
import com.example.domain.calc.CalcRegistry
import com.example.domain.calc.CalcResult
import com.example.domain.calc.ExampleCase
import com.example.domain.calc.ReportFormatter
import com.example.domain.calc.StandardFamily
import com.example.domain.calc.UnitSystem
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface CivilScreen {
    data object Dashboard : CivilScreen
    data class ProjectWorkspace(val projectId: String) : CivilScreen
    data class Workbench(
        val projectId: String,
        val moduleId: CalcModuleId,
        val calcId: String? = null
    ) : CivilScreen
    data class VersionCompare(val projectId: String, val calcId: String) : CivilScreen
    data class ReportPreview(
        val title: String,
        val reportText: String
    ) : CivilScreen
    data object ProfileSettings : CivilScreen
}

data class WorkbenchDraft(
    val projectId: String = "",
    val existingCalcId: String? = null,
    val existingRevisionNumber: Long = 0L,
    val moduleId: CalcModuleId = CalcModuleId.STRUCTURAL_BEAM,
    val title: String = "",
    val unitSystem: UnitSystem = UnitSystem.SI,
    val standardId: String = "ACI_318_19",
    val inputs: Map<String, String> = emptyMap(),
    val assumptions: Map<String, String> = emptyMap(),
    val notes: String = "",
    val revisionNote: String = "",
    val result: CalcResult? = null
)

class CivilSpecViewModel(
    private val repository: CivilSpecRepository,
    private val currentUserId: String,
    userDisplayName: String,
    userEmail: String
) : ViewModel() {

    private val _screenStack = MutableStateFlow<List<CivilScreen>>(listOf(CivilScreen.Dashboard))
    val currentScreen: StateFlow<CivilScreen> = _screenStack
        .map { it.last() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CivilScreen.Dashboard)

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    private val _selectedCalcId = MutableStateFlow<String?>(null)

    private val _workbench = MutableStateFlow(WorkbenchDraft())
    val workbench: StateFlow<WorkbenchDraft> = _workbench.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreException(error)
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val projectsState: StateFlow<UiState<List<EngineeringProject>>> = repository.observeProjects(currentUserId)
        .map<List<EngineeringProject>, UiState<List<EngineeringProject>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreException(error)
            Log.w(TAG, "Error observing projects", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load projects"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val calculationsState: StateFlow<UiState<List<SavedCalculation>>> = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeCalculations(currentUserId, projectId)
                    .map<List<SavedCalculation>, UiState<List<SavedCalculation>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreException(error)
                        Log.w(TAG, "Error observing calculations", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load calculations"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val revisionsState: StateFlow<UiState<List<CalculationRevision>>> = _selectedCalcId
        .flatMapLatest { calcId ->
            val projectId = _selectedProjectId.value
            if (projectId.isNullOrBlank() || calcId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeRevisions(currentUserId, projectId, calcId)
                    .map<List<CalculationRevision>, UiState<List<CalculationRevision>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreException(error)
                        Log.w(TAG, "Error observing revisions", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load revisions"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfileExists(userDisplayName, userEmail)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun navigateTo(screen: CivilScreen) {
        _screenStack.update { stack -> stack + screen }
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        if (current.size <= 1) return false
        _screenStack.update { it.dropLast(1) }
        return true
    }

    fun openProject(projectId: String) {
        _selectedProjectId.value = projectId
        navigateTo(CivilScreen.ProjectWorkspace(projectId))
    }

    fun selectProjectForPane(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun createProject(
        name: String,
        clientName: String,
        location: String,
        notes: String,
        unitSystem: UnitSystem,
        standardFamily: StandardFamily
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createProject(name, clientName, location, notes, unitSystem, standardFamily)
            _isBusy.value = false
            res.onSuccess { newId ->
                _snackbarMessage.value = "Project created"
                openProject(newId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not create project"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteProject(projectId)
            _isBusy.value = false
            res.onSuccess {
                if (_selectedProjectId.value == projectId) _selectedProjectId.value = null
                _screenStack.value = listOf(CivilScreen.Dashboard)
                _snackbarMessage.value = "Project deleted"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun startNewCalculation(project: EngineeringProject, moduleId: CalcModuleId) {
        val module = CalcRegistry.byId(moduleId)
        val unitSys = UnitSystem.fromCode(project.defaultUnitSystem)
        val prefFamily = StandardFamily.fromCode(project.defaultStandard)
        val std = module.supportedStandards.firstOrNull { it.family == prefFamily }
            ?: module.supportedStandards.first()
        val defaults = module.defaultInputs(unitSys, std.id)
        val defaultAssumptions = module.assumptionSpecs(unitSys, std.id).associate { it.key to it.defaultValue }
        val draft = WorkbenchDraft(
            projectId = project.id,
            existingCalcId = null,
            existingRevisionNumber = 0L,
            moduleId = moduleId,
            title = module.name,
            unitSystem = unitSys,
            standardId = std.id,
            inputs = defaults,
            assumptions = defaultAssumptions,
            notes = "",
            revisionNote = "Initial calculation"
        )
        _selectedProjectId.value = project.id
        _selectedCalcId.value = null
        updateDraftAndEvaluate(draft)
        navigateTo(CivilScreen.Workbench(project.id, moduleId, null))
    }

    fun openSavedCalculation(calc: SavedCalculation) {
        val moduleId = CalcModuleId.fromId(calc.moduleId) ?: return
        val module = CalcRegistry.byId(moduleId)
        val unitSys = UnitSystem.fromCode(calc.unitSystem)
        val stdId = calc.standardId.ifBlank { module.supportedStandards.first().id }
        val draft = WorkbenchDraft(
            projectId = calc.projectId,
            existingCalcId = calc.id,
            existingRevisionNumber = calc.latestRevisionNumber,
            moduleId = moduleId,
            title = calc.title,
            unitSystem = unitSys,
            standardId = stdId,
            inputs = calc.latestInputs,
            assumptions = calc.latestAssumptions,
            notes = calc.latestNotes,
            revisionNote = "Revision ${calc.latestRevisionNumber + 1}"
        )
        _selectedProjectId.value = calc.projectId
        _selectedCalcId.value = calc.id
        updateDraftAndEvaluate(draft)
        navigateTo(CivilScreen.Workbench(calc.projectId, moduleId, calc.id))
    }

    fun openVersionCompare(calc: SavedCalculation) {
        _selectedProjectId.value = calc.projectId
        _selectedCalcId.value = calc.id
        navigateTo(CivilScreen.VersionCompare(calc.projectId, calc.id))
    }

    fun updateTitle(title: String) {
        _workbench.update { it.copy(title = title) }
    }

    fun updateNotes(notes: String) {
        _workbench.update { it.copy(notes = notes) }
    }

    fun updateRevisionNote(note: String) {
        _workbench.update { it.copy(revisionNote = note) }
    }

    fun updateInput(key: String, value: String) {
        val next = _workbench.value.let { d ->
            d.copy(inputs = d.inputs + (key to value))
        }
        updateDraftAndEvaluate(next)
    }

    fun updateAssumption(key: String, value: String) {
        val next = _workbench.value.let { d ->
            d.copy(assumptions = d.assumptions + (key to value))
        }
        updateDraftAndEvaluate(next)
    }

    fun switchUnitSystem(newUnit: UnitSystem) {
        val current = _workbench.value
        if (current.unitSystem == newUnit) return
        val module = CalcRegistry.byId(current.moduleId)
        val newAssumptions = module.assumptionSpecs(newUnit, current.standardId)
            .associate { it.key to it.defaultValue }
        val next = current.copy(
            unitSystem = newUnit,
            inputs = module.defaultInputs(newUnit, current.standardId),
            assumptions = newAssumptions
        )
        updateDraftAndEvaluate(next)
    }

    fun switchStandard(standardId: String) {
        val current = _workbench.value
        val module = CalcRegistry.byId(current.moduleId)
        val newAssumptions = module.assumptionSpecs(current.unitSystem, standardId)
            .associate { it.key to (current.assumptions[it.key] ?: it.defaultValue) }
        val next = current.copy(standardId = standardId, assumptions = newAssumptions)
        updateDraftAndEvaluate(next)
    }

    fun loadExampleCase(example: ExampleCase) {
        val current = _workbench.value
        val module = CalcRegistry.byId(current.moduleId)
        val baseAssumptions = module.assumptionSpecs(example.unitSystem, example.standardId)
            .associate { it.key to (example.assumptions[it.key] ?: it.defaultValue) }
        val next = current.copy(
            title = example.title,
            unitSystem = example.unitSystem,
            standardId = example.standardId,
            inputs = example.inputs,
            assumptions = baseAssumptions
        )
        updateDraftAndEvaluate(next)
        _snackbarMessage.value = "Loaded example: ${example.title}"
    }

    private fun updateDraftAndEvaluate(draft: WorkbenchDraft) {
        val module = CalcRegistry.byId(draft.moduleId)
        val eval = module.evaluate(draft.unitSystem, draft.standardId, draft.inputs, draft.assumptions)
        _workbench.value = draft.copy(result = eval)
    }

    fun saveCurrentRevision() {
        val draft = _workbench.value
        val result = draft.result ?: return
        if (!result.isValid) {
            _snackbarMessage.value = "Resolve red validation errors before saving."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCalculationRevision(
                projectId = draft.projectId,
                existingCalcId = draft.existingCalcId,
                existingRevisionNumber = draft.existingRevisionNumber,
                title = draft.title.ifBlank { result.moduleName },
                revisionNote = draft.revisionNote,
                notes = draft.notes,
                rawInputs = draft.inputs,
                result = result
            )
            _isBusy.value = false
            res.onSuccess { (calcId, revNum) ->
                _selectedCalcId.value = calcId
                _workbench.update {
                    it.copy(
                        existingCalcId = calcId,
                        existingRevisionNumber = revNum,
                        revisionNote = "Revision ${revNum + 1}"
                    )
                }
                _snackbarMessage.value = "Saved Revision $revNum"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save calculation"
            }
        }
    }

    fun duplicateCalculation(calc: SavedCalculation) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.duplicateCalculation(calc.projectId, calc)
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Duplicated \"${calc.title}\""
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Duplicate failed"
            }
        }
    }

    fun deleteCalculation(projectId: String, calcId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteCalculation(projectId, calcId)
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Calculation deleted"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun openReportForCurrentDraft(project: EngineeringProject?, user: FirebaseUser?) {
        val draft = _workbench.value
        val result = draft.result ?: return
        val module = CalcRegistry.byId(draft.moduleId)
        val profile = (userProfileState.value as? UiState.Success)?.data
        val engineerName = profile?.displayName?.ifBlank { user?.displayName.orEmpty() }
            ?: user?.displayName
            ?: user?.email
            ?: "Registered Engineer"
        val revNum = if (draft.existingRevisionNumber > 0) draft.existingRevisionNumber.toInt() else 1
        val text = ReportFormatter.formatCalculationReport(
            projectName = project?.name ?: "Untitled Project",
            projectLocation = project?.location ?: "",
            calculationTitle = draft.title.ifBlank { module.name },
            revisionNumber = revNum,
            engineerName = engineerName,
            module = module,
            result = result,
            rawInputs = draft.inputs,
            notes = draft.notes
        )
        navigateTo(CivilScreen.ReportPreview(draft.title.ifBlank { module.name }, text))
    }

    fun exportProjectBackup(project: EngineeringProject, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.exportProjectBackupJson(project)
            _isBusy.value = false
            res.onSuccess { json ->
                onReady(json)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Export failed"
            }
        }
    }

    fun updateProfile(
        displayName: String,
        organization: String,
        licenseOrTitle: String,
        unitSystem: UnitSystem,
        standardFamily: StandardFamily
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateUserProfile(displayName, organization, licenseOrTitle, unitSystem, standardFamily)
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Engineering profile updated"
                navigateBack()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Profile update failed"
            }
        }
    }

    private companion object {
        const val TAG = "CivilSpecVM"
    }
}

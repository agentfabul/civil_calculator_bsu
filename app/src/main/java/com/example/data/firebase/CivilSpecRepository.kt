package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.domain.calc.CalcResult
import com.example.domain.calc.SOFTWARE_VERSION
import com.example.domain.calc.StandardFamily
import com.example.domain.calc.UnitSystem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class CivilSpecRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- User Profile / Registration ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileExists(
        displayName: String,
        email: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Civil Engineer" }.take(100)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(120),
                    "organization" to "",
                    "licenseOrTitle" to "",
                    "preferredUnitSystem" to UnitSystem.SI.code,
                    "preferredStandard" to StandardFamily.ACI_ASCE.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        organization: String,
        licenseOrTitle: String,
        preferredUnitSystem: UnitSystem,
        preferredStandard: StandardFamily
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val cleanName = displayName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "organization" to organization.trim().take(120),
                    "licenseOrTitle" to licenseOrTitle.trim().take(80),
                    "preferredUnitSystem" to preferredUnitSystem.code,
                    "preferredStandard" to preferredStandard.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Projects ---

    fun observeProjects(userId: String): Flow<List<EngineeringProject>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EngineeringProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProject(
        name: String,
        clientName: String,
        location: String,
        notes: String,
        defaultUnitSystem: UnitSystem,
        defaultStandard: StandardFamily
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        var docPath = "users/$uid/projects"
        return try {
            val cleanName = name.trim().take(120)
            require(cleanName.isNotEmpty()) { "Project name is required." }
            val ref = db.collection("users").document(uid).collection("projects").document()
            docPath = ref.path
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "clientName" to clientName.trim().take(120),
                "location" to location.trim().take(120),
                "notes" to notes.trim().take(1000),
                "defaultUnitSystem" to defaultUnitSystem.code,
                "defaultStandard" to defaultStandard.code,
                "calculationCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/projects/$projectId"
        return try {
            require(projectId.isNotBlank())
            val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
            // Delete child calculations and revisions cleanly
            val calcs = projRef.collection("calculations").get().await()
            for (calcDoc in calcs.documents) {
                val revs = calcDoc.reference.collection("revisions").get().await()
                for (revDoc in revs.documents) {
                    revDoc.reference.delete().await()
                }
                calcDoc.reference.delete().await()
            }
            projRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Saved Calculations & Version Revisions ---

    fun observeCalculations(userId: String, projectId: String): Flow<List<SavedCalculation>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val path = "users/$uid/projects/$projectId/calculations"
        emitAll(
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("calculations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedCalculation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRevisions(
        userId: String,
        projectId: String,
        calcId: String
    ): Flow<List<CalculationRevision>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID mismatch" }
        val path = "users/$uid/projects/$projectId/calculations/$calcId/revisions"
        emitAll(
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("calculations").document(calcId)
                .collection("revisions")
                .orderBy("revisionNumber", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CalculationRevision::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCalculationRevision(
        projectId: String,
        existingCalcId: String?,
        existingRevisionNumber: Long,
        title: String,
        revisionNote: String,
        notes: String,
        rawInputs: Map<String, String>,
        result: CalcResult
    ): Result<Pair<String, Long>> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        var currentPath = "users/$uid/projects/$projectId/calculations"
        return try {
            require(projectId.isNotBlank()) { "Project ID is required." }
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Calculation title is required." }
            require(result.isValid) { "Fix validation errors before saving revision." }

            val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
            val calcsCol = projRef.collection("calculations")
            val isNew = existingCalcId.isNullOrBlank()
            val calcRef = if (isNew) calcsCol.document() else calcsCol.document(existingCalcId)
            val nextRev = if (isNew) 1L else (existingRevisionNumber + 1L).coerceAtLeast(1L)

            val boundedInputs = rawInputs.entries.take(30).associate { (k, v) -> k.take(40) to v.take(200) }
            val boundedAssumptions = result.resolvedAssumptions.entries.take(20).associate { (k, v) -> k.take(40) to v.take(100) }
            val summaryText = result.primarySummaryText.take(300)
            val util = result.utilizationRatio?.takeIf { it.isFinite() && it >= 0.0 }?.coerceAtMost(9999.0)

            currentPath = calcRef.path
            if (isNew) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "projectId" to projectId,
                    "moduleId" to result.moduleId.id,
                    "title" to cleanTitle,
                    "unitSystem" to result.unitSystem.code,
                    "standardId" to (result.standard?.id ?: "DEFAULT"),
                    "latestRevisionNumber" to nextRev,
                    "latestStatus" to result.status.code,
                    "latestSummary" to summaryText,
                    "latestInputs" to boundedInputs,
                    "latestAssumptions" to boundedAssumptions,
                    "latestNotes" to notes.trim().take(1000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (util != null) {
                    createPayload["latestUtilization"] = util
                }
                calcRef.set(createPayload).await()
                projRef.update(
                    mapOf(
                        "calculationCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "unitSystem" to result.unitSystem.code,
                    "standardId" to (result.standard?.id ?: "DEFAULT"),
                    "latestRevisionNumber" to nextRev,
                    "latestStatus" to result.status.code,
                    "latestSummary" to summaryText,
                    "latestInputs" to boundedInputs,
                    "latestAssumptions" to boundedAssumptions,
                    "latestNotes" to notes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (util != null) {
                    updatePayload["latestUtilization"] = util
                }
                calcRef.update(updatePayload).await()
                projRef.update("updatedAt", FieldValue.serverTimestamp()).await()
            }

            // Create immutable revision snapshot
            val revRef = calcRef.collection("revisions").document()
            currentPath = revRef.path
            val primaryOutputs = result.summaryMetrics.take(20).map {
                "${it.label}: ${it.formattedValue} ${it.unit}".trim().take(150)
            }
            val warningList = result.issues.take(20).map {
                "[${it.severity.name}] ${it.title}: ${it.message}".take(200)
            }
            val revPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "projectId" to projectId,
                "calcId" to calcRef.id,
                "revisionNumber" to nextRev,
                "revisionNote" to revisionNote.trim().take(500),
                "moduleId" to result.moduleId.id,
                "title" to cleanTitle,
                "unitSystem" to result.unitSystem.code,
                "standardId" to (result.standard?.id ?: "DEFAULT"),
                "standardLabel" to (result.standard?.let { "${it.name} (${it.edition})" } ?: "Standard").take(120),
                "status" to result.status.code,
                "governingCheck" to result.governingCheck.take(200),
                "inputs" to boundedInputs,
                "assumptions" to boundedAssumptions,
                "primaryOutputs" to primaryOutputs,
                "warnings" to warningList,
                "softwareVersion" to SOFTWARE_VERSION,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (util != null) {
                revPayload["utilizationRatio"] = util
            }
            revRef.set(revPayload).await()

            Result.success(calcRef.id to nextRev)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun duplicateCalculation(
        projectId: String,
        source: SavedCalculation
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/projects/$projectId/calculations"
        return try {
            val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
            val newCalcRef = projRef.collection("calculations").document()
            val copyTitle = "${source.title} (Copy)".take(140)
            val createPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "projectId" to projectId,
                "moduleId" to source.moduleId,
                "title" to copyTitle,
                "unitSystem" to source.unitSystem,
                "standardId" to source.standardId,
                "latestRevisionNumber" to 1L,
                "latestStatus" to source.latestStatus,
                "latestSummary" to source.latestSummary,
                "latestInputs" to source.latestInputs,
                "latestAssumptions" to source.latestAssumptions,
                "latestNotes" to source.latestNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            source.latestUtilization?.let { createPayload["latestUtilization"] = it }
            newCalcRef.set(createPayload).await()

            // Seed Rev 1 snapshot on duplicate so history is intact
            val revRef = newCalcRef.collection("revisions").document()
            val revPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "projectId" to projectId,
                "calcId" to newCalcRef.id,
                "revisionNumber" to 1L,
                "revisionNote" to "Duplicated from ${source.title} (Rev ${source.latestRevisionNumber})",
                "moduleId" to source.moduleId,
                "title" to copyTitle,
                "unitSystem" to source.unitSystem,
                "standardId" to source.standardId,
                "standardLabel" to source.standardId,
                "status" to source.latestStatus,
                "governingCheck" to source.latestSummary.take(200),
                "inputs" to source.latestInputs,
                "assumptions" to source.latestAssumptions,
                "primaryOutputs" to listOf(source.latestSummary),
                "warnings" to emptyList<String>(),
                "softwareVersion" to SOFTWARE_VERSION,
                "createdAt" to FieldValue.serverTimestamp()
            )
            source.latestUtilization?.let { revPayload["utilizationRatio"] = it }
            revRef.set(revPayload).await()

            projRef.update(
                mapOf(
                    "calculationCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(newCalcRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCalculation(projectId: String, calcId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/projects/$projectId/calculations/$calcId"
        return try {
            val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
            val calcRef = projRef.collection("calculations").document(calcId)
            val revs = calcRef.collection("revisions").get().await()
            for (rev in revs.documents) {
                rev.reference.delete().await()
            }
            calcRef.delete().await()
            val projSnap = projRef.get().await()
            val currentCount = projSnap.getLong("calculationCount") ?: 1L
            projRef.update(
                mapOf(
                    "calculationCount" to (currentCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun exportProjectBackupJson(project: EngineeringProject): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/projects/${project.id}/calculations"
        return try {
            val calcsSnap = db.collection("users").document(uid)
                .collection("projects").document(project.id)
                .collection("calculations").get().await()
            val calcs = calcsSnap.toObjects(SavedCalculation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val calcsArr = JSONArray()
            for (c in calcs) {
                val revsSnap = db.collection("users").document(uid)
                    .collection("projects").document(project.id)
                    .collection("calculations").document(c.id)
                    .collection("revisions").orderBy("revisionNumber", Query.Direction.ASCENDING)
                    .get().await()
                val revs = revsSnap.toObjects(CalculationRevision::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val revsArr = JSONArray()
                for (r in revs) {
                    revsArr.put(JSONObject().apply {
                        put("revisionNumber", r.revisionNumber)
                        put("revisionNote", r.revisionNote)
                        put("status", r.status)
                        put("governingCheck", r.governingCheck)
                        put("utilizationRatio", r.utilizationRatio)
                        put("standardLabel", r.standardLabel)
                        put("inputs", JSONObject(r.inputs))
                        put("assumptions", JSONObject(r.assumptions))
                        put("primaryOutputs", JSONArray(r.primaryOutputs))
                        put("warnings", JSONArray(r.warnings))
                        put("softwareVersion", r.softwareVersion)
                        put("createdAt", (r.createdAt ?: Timestamp.now()).toDate().toString())
                    })
                }
                calcsArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("moduleId", c.moduleId)
                    put("title", c.title)
                    put("unitSystem", c.unitSystem)
                    put("standardId", c.standardId)
                    put("latestRevisionNumber", c.latestRevisionNumber)
                    put("latestStatus", c.latestStatus)
                    put("latestSummary", c.latestSummary)
                    put("latestNotes", c.latestNotes)
                    put("revisions", revsArr)
                })
            }
            val root = JSONObject().apply {
                put("backupFormat", "CivilSpec-ProjectBackup-v1")
                put("softwareVersion", SOFTWARE_VERSION)
                put("exportedAt", Timestamp.now().toDate().toString())
                put("project", JSONObject().apply {
                    put("id", project.id)
                    put("name", project.name)
                    put("clientName", project.clientName)
                    put("location", project.location)
                    put("notes", project.notes)
                    put("defaultUnitSystem", project.defaultUnitSystem)
                    put("defaultStandard", project.defaultStandard)
                })
                put("calculations", calcsArr)
            }
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }
}

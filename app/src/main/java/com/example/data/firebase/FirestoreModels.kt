package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val organization: String = "",
    val licenseOrTitle: String = "",
    val preferredUnitSystem: String = "SI",
    val preferredStandard: String = "ACI_ASCE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EngineeringProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val clientName: String = "",
    val location: String = "",
    val notes: String = "",
    val defaultUnitSystem: String = "SI",
    val defaultStandard: String = "ACI_ASCE",
    val calculationCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedCalculation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val moduleId: String = "",
    val title: String = "",
    val unitSystem: String = "SI",
    val standardId: String = "",
    val latestRevisionNumber: Long = 1L,
    val latestStatus: String = "ESTIMATE",
    val latestSummary: String = "",
    val latestUtilization: Double? = null,
    val latestInputs: Map<String, String> = emptyMap(),
    val latestAssumptions: Map<String, String> = emptyMap(),
    val latestNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CalculationRevision(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val calcId: String = "",
    val revisionNumber: Long = 1L,
    val revisionNote: String = "",
    val moduleId: String = "",
    val title: String = "",
    val unitSystem: String = "SI",
    val standardId: String = "",
    val standardLabel: String = "",
    val status: String = "ESTIMATE",
    val governingCheck: String = "",
    val utilizationRatio: Double? = null,
    val inputs: Map<String, String> = emptyMap(),
    val assumptions: Map<String, String> = emptyMap(),
    val primaryOutputs: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val softwareVersion: String = "1.0.0-mvp",
    @ServerTimestamp val createdAt: Timestamp? = null
)

package com.powerplant.firesurvey.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A physical extinguisher. [id] is the exact text encoded in its QR label. */
@Entity(tableName = "extinguishers")
data class Extinguisher(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val type: String,
    val capacity: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One survey of an extinguisher by an operator. */
@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = Extinguisher::class,
            parentColumns = ["id"],
            childColumns = ["extinguisherId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("extinguisherId"), Index("timestamp")],
)
data class Inspection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val extinguisherId: String,
    val timestamp: Long,
    val inspector: String,
    /** [InspectionStatus.name] */
    val status: String,
    /** Comma-separated [Issue.name] values; empty when status is OK. */
    val issues: String,
    val notes: String,
) {
    val statusEnum: InspectionStatus get() = InspectionStatus.fromDb(status)
    val issueList: List<Issue> get() = Issue.parse(issues)
}

/** Extinguisher plus the result of its most recent inspection (if any). */
data class ExtinguisherSummary(
    @Embedded val extinguisher: Extinguisher,
    val lastTimestamp: Long?,
    val lastStatus: String?,
    val lastIssues: String?,
    val lastInspector: String?,
) {
    val lastStatusEnum: InspectionStatus? get() = lastStatus?.let(InspectionStatus::fromDb)
    val lastIssueList: List<Issue> get() = Issue.parse(lastIssues)
}

enum class InspectionStatus(val label: String) {
    OK("OK"),
    NOT_OK("Not OK");

    companion object {
        fun fromDb(value: String): InspectionStatus = entries.firstOrNull { it.name == value } ?: NOT_OK
    }
}

/** Conditions an operator can report when an extinguisher is not OK. */
enum class Issue(val label: String) {
    CORRODED("Corroded / rusted"),
    DAMAGED("Physically damaged / dented"),
    NEEDS_CASING("Needs casing / cabinet"),
    LOW_PRESSURE("Low pressure (gauge not in green)"),
    PIN_SEAL("Safety pin or tamper seal missing/broken"),
    HOSE_NOZZLE("Hose or nozzle damaged / blocked"),
    LABEL("Label / instructions missing or unreadable"),
    OBSTRUCTED("Access obstructed / not visible"),
    NEEDS_REFILL("Used or needs refill / recharge"),
    SERVICE_DUE("Service / hydrostatic test overdue"),
    MOUNTING("Bracket / mounting damaged"),
    MISSING("Extinguisher missing from location"),
    OTHER("Other (describe in notes)");

    companion object {
        fun parse(value: String?): List<Issue> =
            value.orEmpty().split(',').mapNotNull { code -> entries.firstOrNull { it.name == code.trim() } }

        fun join(issues: Collection<Issue>): String = issues.sortedBy { it.ordinal }.joinToString(",") { it.name }
    }
}

val ExtinguisherTypes = listOf(
    "Dry chemical powder (ABC)",
    "CO2",
    "Foam (AFFF)",
    "Water",
    "Wet chemical",
    "Clean agent (FM-200 / Halotron)",
    "Other",
)

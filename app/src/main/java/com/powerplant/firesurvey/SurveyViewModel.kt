package com.powerplant.firesurvey

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerplant.firesurvey.data.AppDatabase
import com.powerplant.firesurvey.data.Extinguisher
import com.powerplant.firesurvey.data.ExtinguisherSummary
import com.powerplant.firesurvey.data.Inspection
import com.powerplant.firesurvey.data.InspectionStatus
import com.powerplant.firesurvey.data.Issue
import com.powerplant.firesurvey.util.Prefs
import com.powerplant.firesurvey.util.Sharing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class SurveyViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).surveyDao()
    private val prefs = Prefs(app)

    /** null until the first database emission, so the UI can show a loading state. */
    val summaries: StateFlow<List<ExtinguisherSummary>?> =
        dao.observeSummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _inspectorName = MutableStateFlow(prefs.inspectorName)
    val inspectorName: StateFlow<String> = _inspectorName.asStateFlow()

    fun setInspectorName(name: String) {
        prefs.inspectorName = name
        _inspectorName.value = name.trim()
    }

    fun extinguisher(id: String): Flow<Extinguisher?> = dao.observeExtinguisher(id)

    fun inspections(id: String): Flow<List<Inspection>> = dao.observeInspections(id)

    suspend fun getExtinguisher(id: String): Extinguisher? = dao.getExtinguisher(id)

    /** @return an error message, or null on success. */
    suspend fun saveExtinguisher(extinguisher: Extinguisher, isNew: Boolean): String? {
        if (extinguisher.id.isBlank()) return "QR code / ID is required"
        if (extinguisher.name.isBlank()) return "Name is required"
        return if (isNew) {
            if (dao.getExtinguisher(extinguisher.id) != null) {
                "An extinguisher with code \"${extinguisher.id}\" already exists"
            } else {
                dao.insertExtinguisher(extinguisher)
                null
            }
        } else {
            dao.updateExtinguisher(extinguisher)
            null
        }
    }

    suspend fun deleteExtinguisher(extinguisher: Extinguisher) = dao.deleteExtinguisher(extinguisher)

    suspend fun saveInspection(
        extinguisherId: String,
        status: InspectionStatus,
        issues: Set<Issue>,
        notes: String,
        inspector: String,
    ) {
        setInspectorName(inspector)
        dao.insertInspection(
            Inspection(
                extinguisherId = extinguisherId,
                timestamp = System.currentTimeMillis(),
                inspector = inspector.trim(),
                status = status.name,
                issues = if (status == InspectionStatus.OK) "" else Issue.join(issues),
                notes = notes.trim(),
            )
        )
    }

    suspend fun exportCsv(context: Context) {
        Sharing.shareInspectionsCsv(context, dao.allExtinguishers(), dao.allInspections())
    }
}

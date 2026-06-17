package com.qleverty.cue4a

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CueViewModel(application: Application) : AndroidViewModel(application) {

    val projects = mutableStateListOf<Project>()
    var activeProjectIdx by mutableIntStateOf(0)
    var settings by mutableStateOf(Settings())
    var identity by mutableStateOf(DeviceIdentity())

    var ddOpen            by mutableStateOf(false)
    var addingTask        by mutableStateOf(false)
    var taskInput         by mutableStateOf("")
    var projectAdding     by mutableStateOf(false)
    var projectInput      by mutableStateOf("")
    var projectColorIdx   by mutableIntStateOf(0)
    var showDeleteConfirm by mutableStateOf<Int?>(null)

    init {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val loadedIdentity  = withContext(Dispatchers.IO) { loadOrCreateIdentity(ctx) }
            val loadedSettings  = withContext(Dispatchers.IO) { loadSettings(ctx) }
            var loadedProjects  = withContext(Dispatchers.IO) { loadAllProjects(ctx) }

            if (loadedProjects.isEmpty()) {
                val def = withContext(Dispatchers.IO) { createDefaultProject(ctx) }
                loadedProjects = listOf(def)
            }

            identity = loadedIdentity
            settings = loadedSettings
            projects.addAll(loadedProjects)

            val lastId = loadedSettings.local.lastProjectId
            val idx = lastId
                ?.let { id -> loadedProjects.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
                ?: 0
            activeProjectIdx = idx

            val resolvedId = projects.getOrNull(activeProjectIdx)?.id
            if (resolvedId != null && resolvedId != lastId) {
                saveLocalSetting { copy(lastProjectId = resolvedId) }
            }
        }
    }

    private fun ctx() = getApplication<Application>()

    val activeProject: Project?
        get() = projects.getOrNull(activeProjectIdx)

    private fun updateProject(updated: Project) {
        val idx = projects.indexOfFirst { it.id == updated.id }
        if (idx < 0) return
        projects[idx] = updated
        viewModelScope.launch(Dispatchers.IO) { saveProject(ctx(), updated) }
    }

    private fun saveLocalSetting(update: LocalSettings.() -> LocalSettings) {
        val newSettings = settings.copy(local = settings.local.update())
        settings = newSettings
        viewModelScope.launch(Dispatchers.IO) { saveSettings(ctx(), newSettings) }
    }

    fun saveSharedSetting(update: SharedSettings.() -> SharedSettings) {
        val newSettings = settings.copy(shared = settings.shared.update())
        settings = newSettings
        viewModelScope.launch(Dispatchers.IO) { saveSettings(ctx(), newSettings) }
    }

    fun switchToProject(idx: Int) {
        if (idx == activeProjectIdx) return
        activeProjectIdx = idx
        saveLocalSetting { copy(lastProjectId = projects[idx].id) }
    }

    fun commitTaskInput() {
        val text = taskInput.trim()
        taskInput = ""
        addingTask = false
        if (text.isEmpty()) return
        val proj = activeProject ?: return
        updateProject(proj.addTask(text, settings.shared))
    }

    fun cancelTaskInput() {
        taskInput  = ""
        addingTask = false
    }

    fun completeMain() {
        val proj = activeProject ?: return
        updateProject(proj.completeMain())
    }

    fun promoteSub(taskId: String) {
        val proj = activeProject ?: return
        updateProject(proj.promoteSub(taskId))
    }

    fun deleteSub(taskId: String) {
        val proj = activeProject ?: return
        updateProject(proj.deleteSub(taskId))
    }

    fun startProjectAdding() {
        projectAdding   = true
        projectInput    = ""
        projectColorIdx = 0
    }

    fun cancelProjectAdding() {
        projectAdding   = false
        projectInput    = ""
        projectColorIdx = 0
    }

    fun cycleProjectColor() {
        projectColorIdx = (projectColorIdx + 1) % PROJECT_PALETTE.size
    }

    fun commitProjectInput() {
        val name  = projectInput.trim()
        val color = PROJECT_PALETTE[projectColorIdx]
        projectInput    = ""
        projectAdding   = false
        projectColorIdx = 0
        if (name.isEmpty()) return
        val proj = Project(name = name, color = color, colorHex = colorToHex(color))
        projects.add(proj)
        viewModelScope.launch(Dispatchers.IO) { saveProject(ctx(), proj) }
        switchToProject(projects.size - 1)
        ddOpen = false
    }

    fun requestDeleteProject(idx: Int) {
        if (projects.size <= 1) return
        showDeleteConfirm = idx
    }

    fun confirmDeleteProject() {
        val idx = showDeleteConfirm ?: return
        showDeleteConfirm = null
        if (idx !in projects.indices) return
        val proj = projects[idx]
        viewModelScope.launch(Dispatchers.IO) { deleteProjectFile(ctx(), proj) }
        projects.removeAt(idx)
        val newActive = when {
            activeProjectIdx > idx                         -> activeProjectIdx - 1
            activeProjectIdx == idx && idx >= projects.size -> (projects.size - 1).coerceAtLeast(0)
            else                                           -> activeProjectIdx
        }
        activeProjectIdx = newActive
        saveLocalSetting { copy(lastProjectId = projects.getOrNull(newActive)?.id) }
        ddOpen = false
    }

    fun cancelDeleteProject() {
        showDeleteConfirm = null
    }

    fun closeDropdown() {
        ddOpen        = false
        projectAdding = false
        projectInput  = ""
        projectColorIdx = 0
    }
}
package com.qleverty.cue4a

import androidx.compose.ui.graphics.Color
import java.util.UUID

val PROJECT_PALETTE = listOf(
    Color(0xFFDC3232),
    Color(0xFFF97316),
    Color(0xFFEAB308),
    Color(0xFF22C55E),
    Color(0xFF14B8A6),
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
    Color(0xFFEC4899),
    Color(0xFF8B5A2B),
    Color(0xFF282828),
    Color(0xFFD2D2D2),
)

data class TaskData(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val orderKey: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis() / 1000,
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Color,
    val colorHex: String,
    val main: LinkedHashMap<String, TaskData> = LinkedHashMap(),
    val subs: LinkedHashMap<String, TaskData> = LinkedHashMap(),
    val createdAt: Long = System.currentTimeMillis() / 1000,
) {
    fun mainTask(): TaskData? = main.values.firstOrNull()
    fun mainText(): String? = mainTask()?.text
    fun subsSorted(): List<TaskData> = subs.values.sortedWith(
        compareBy({ it.orderKey }, { it.createdAt }, { it.id })
    )
}

data class LocalSettings(
    val lastProjectId: String? = null,
)

data class SharedSettings(
    val newTaskPosEnd: Boolean = true,
    val replaceMain: Boolean = false,
    val resetOnStartup: Boolean = false,
)

data class Settings(
    val local: LocalSettings = LocalSettings(),
    val shared: SharedSettings = SharedSettings(),
)

data class DeviceIdentity(
    val deviceId: String = UUID.randomUUID().toString(),
    val deviceName: String = android.os.Build.MODEL,
)

fun Project.addTask(text: String, settings: SharedSettings): Project {
    val newMain = LinkedHashMap(main)
    val newSubs = LinkedHashMap(subs)
    val task = TaskData(text = text)

    if (newMain.isEmpty()) {
        newMain[task.id] = task
        return copy(main = newMain)
    }

    if (settings.replaceMain) {
        val old = newMain.values.first()
        newMain.clear()
        newMain[task.id] = task
        val demoted = old.copy(orderKey = if (settings.newTaskPosEnd)
            (newSubs.values.maxOfOrNull { it.orderKey } ?: 0.0) + 1000.0
        else
            (newSubs.values.minOfOrNull { it.orderKey } ?: 0.0) - 1000.0
        )
        if (settings.newTaskPosEnd) newSubs[demoted.id] = demoted
        else {
            val tmp = LinkedHashMap<String, TaskData>()
            tmp[demoted.id] = demoted
            tmp.putAll(newSubs)
            return copy(main = newMain, subs = tmp)
        }
        return copy(main = newMain, subs = newSubs)
    }

    val insertKey = if (settings.newTaskPosEnd)
        (newSubs.values.maxOfOrNull { it.orderKey } ?: 0.0) + 1000.0
    else
        (newSubs.values.minOfOrNull { it.orderKey } ?: 0.0) - 1000.0

    val keyed = task.copy(orderKey = insertKey)
    if (settings.newTaskPosEnd) {
        newSubs[keyed.id] = keyed
    } else {
        val tmp = LinkedHashMap<String, TaskData>()
        tmp[keyed.id] = keyed
        tmp.putAll(newSubs)
        return copy(subs = tmp)
    }
    return copy(subs = newSubs)
}

fun Project.completeMain(): Project {
    val newMain = LinkedHashMap(main)
    val newSubs = LinkedHashMap(subs)
    newMain.clear()
    if (newSubs.isNotEmpty()) {
        val next = newSubs.values.minByOrNull { it.orderKey }!!
        newSubs.remove(next.id)
        newMain[next.id] = next
    }
    return copy(main = newMain, subs = newSubs)
}

fun Project.promoteSub(taskId: String): Project {
    val sub = subs[taskId] ?: return this
    val newMain = LinkedHashMap(main)
    val newSubs = LinkedHashMap(subs)
    newSubs.remove(taskId)
    val demoteKey = (newSubs.values.minOfOrNull { it.orderKey } ?: 0.0) - 1000.0
    val oldMain = newMain.values.firstOrNull()
    newMain.clear()
    newMain[sub.id] = sub
    if (oldMain != null) {
        val demoted = oldMain.copy(orderKey = demoteKey)
        val tmp = LinkedHashMap<String, TaskData>()
        tmp[demoted.id] = demoted
        tmp.putAll(newSubs)
        return copy(main = newMain, subs = tmp)
    }
    return copy(main = newMain, subs = newSubs)
}

fun Project.deleteSub(taskId: String): Project {
    val newSubs = LinkedHashMap(subs)
    newSubs.remove(taskId)
    return copy(subs = newSubs)
}
package com.qleverty.cue4a

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun hexToColor(hex: String): Color? {
    val h = hex.trimStart('#')
    if (h.length != 6) return null
    return try {
        Color(
            h.substring(0, 2).toInt(16) / 255f,
            h.substring(2, 4).toInt(16) / 255f,
            h.substring(4, 6).toInt(16) / 255f,
        )
    } catch (_: Exception) { null }
}

fun colorToHex(c: Color): String {
    val r = (c.red   * 255).toInt().coerceIn(0, 255)
    val g = (c.green * 255).toInt().coerceIn(0, 255)
    val b = (c.blue  * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

private fun projectsDir(context: Context) =
    File(context.filesDir, "projects").also { it.mkdirs() }

private fun settingsFile(context: Context) =
    File(context.filesDir, "settings.json")

private fun identityFile(context: Context) =
    File(context.filesDir, "identity.json")

private fun atomicWrite(dest: File, content: String) {
    val tmp = File(dest.parent, "${dest.name}.tmp")
    tmp.writeText(content)
    tmp.renameTo(dest)
}

private fun taskToJson(t: TaskData) = JSONObject().apply {
    put("id",         t.id)
    put("text",       t.text)
    put("order_key",  t.orderKey)
    put("created_at", t.createdAt)
}

private fun taskFromJson(o: JSONObject) = TaskData(
    id        = o.optString("id", java.util.UUID.randomUUID().toString()),
    text      = o.getString("text"),
    orderKey  = o.optDouble("order_key", 0.0),
    createdAt = o.optLong("created_at", System.currentTimeMillis() / 1000),
)

private fun taskMapToJson(map: LinkedHashMap<String, TaskData>): JSONArray =
    JSONArray().also { arr -> map.values.forEach { arr.put(taskToJson(it)) } }

private fun taskMapFromJson(arr: JSONArray): LinkedHashMap<String, TaskData> =
    LinkedHashMap<String, TaskData>().also { map ->
        for (i in 0 until arr.length()) {
            val t = taskFromJson(arr.getJSONObject(i))
            map[t.id] = t
        }
    }

fun saveProject(context: Context, project: Project) {
    val json = JSONObject().apply {
        put("ver",        2)
        put("name",       project.name)
        put("color",      project.colorHex)
        put("created_at", project.createdAt)
        put("last_edited", System.currentTimeMillis() / 1000)
        put("tasks", JSONObject().apply {
            put("main", taskMapToJson(project.main))
            put("subs", taskMapToJson(project.subs))
        })
    }
    atomicWrite(File(projectsDir(context), "${project.id}.json"), json.toString())
}

fun deleteProjectFile(context: Context, project: Project) {
    File(projectsDir(context), "${project.id}.json").delete()
}

fun loadAllProjects(context: Context): List<Project> {
    val dir = projectsDir(context)
    if (!dir.exists()) return emptyList()
    return dir.listFiles()
        ?.filter { it.name.endsWith(".json") && !it.name.endsWith(".json.tmp") }
        ?.mapNotNull { file ->
            try {
                val o     = JSONObject(file.readText())
                val hex   = o.getString("color")
                val color = hexToColor(hex) ?: return@mapNotNull null
                val tasks = o.getJSONObject("tasks")
                Project(
                    id        = file.nameWithoutExtension,
                    name      = o.getString("name"),
                    color     = color,
                    colorHex  = hex,
                    main      = taskMapFromJson(tasks.getJSONArray("main")),
                    subs      = taskMapFromJson(tasks.getJSONArray("subs")),
                    createdAt = o.optLong("created_at", 0),
                )
            } catch (_: Exception) { null }
        }
        ?: emptyList()
}

fun createDefaultProject(context: Context): Project {
    val color = Color(0xFF4A90D9)
    return Project(name = "Cue", color = color, colorHex = colorToHex(color))
        .also { saveProject(context, it) }
}

fun saveSettings(context: Context, settings: Settings) {
    val json = JSONObject().apply {
        put("new_task_pos_end",  settings.shared.newTaskPosEnd)
        put("replace_main",      settings.shared.replaceMain)
        put("reset_on_startup",  settings.shared.resetOnStartup)
        settings.local.lastProjectId?.let { put("last_project_id", it) }
    }
    atomicWrite(settingsFile(context), json.toString())
}

fun loadSettings(context: Context): Settings {
    return try {
        val o = JSONObject(settingsFile(context).readText())
        Settings(
            local  = LocalSettings(
                lastProjectId = if (o.has("last_project_id")) o.getString("last_project_id") else null,
            ),
            shared = SharedSettings(
                newTaskPosEnd  = o.optBoolean("new_task_pos_end",  true),
                replaceMain    = o.optBoolean("replace_main",       false),
                resetOnStartup = o.optBoolean("reset_on_startup",  false),
            ),
        )
    } catch (_: Exception) { Settings() }
}

fun loadOrCreateIdentity(context: Context): DeviceIdentity {
    val file = identityFile(context)
    if (file.exists()) {
        try {
            val o = JSONObject(file.readText())
            return DeviceIdentity(
                deviceId   = o.getString("device_id"),
                deviceName = o.getString("device_name"),
            )
        } catch (_: Exception) {}
    }
    val identity = DeviceIdentity()
    val json = JSONObject().apply {
        put("device_id",   identity.deviceId)
        put("device_name", identity.deviceName)
    }
    atomicWrite(file, json.toString())
    return identity
}
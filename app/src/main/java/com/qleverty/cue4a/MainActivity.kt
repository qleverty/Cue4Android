package com.qleverty.cue4a

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

// ── Colors ──────────────────────────────────────────────────────────────────
private val BG       = Color(0xFF09090B)
private val Surface  = Color(0xFF09090B)  // rgba(255,255,255,.04) on BG
private val Border   = Color(0x12FFFFFF)  // rgba(255,255,255,.07)
private val Sep      = Color(0x0FFFFFFF)  // rgba(255,255,255,.06)
private val TextCol  = Color(0xFFE8E8EA)
private val Muted    = Color(0x61FFFFFF)  // rgba(255,255,255,.38)
private val SurfaceC = Color(0x0AFFFFFF)  // .04 alpha

data class Project(val name: String, val color: Color)

private val projects = listOf(
    Project("Cue",      Color(0xFF4A90D9)),
    Project("Работа",   Color(0xFF22C55E)),
    Project("Личное",   Color(0xFFF97316)),
    Project("Здоровье", Color(0xFF14B8A6)),
)

// Blend for "done" button matching JS logic (35% project hue + 65% green hue)
private fun doneColors(c: Color): Triple<Color, Color, Color> {
    // simplified: just tint toward green
    val bg     = Color(0xFF1A2B1E).copy(alpha = 1f)
    val border = Color(0xFF3A6B44).copy(alpha = 0.5f)
    val text   = Color(0xFF8DB89A)
    // We blend the actual color subtly
    val blended = Color(
        red   = c.red   * 0.35f + 0.13f * 0.65f,
        green = c.green * 0.35f + 0.76f * 0.65f,
        blue  = c.blue  * 0.35f + 0.27f * 0.65f,
    )
    val bgFinal     = blended.copy(alpha = 1f).let {
        Color(it.red * 0.22f + 0f, it.green * 0.22f + 0f, it.blue * 0.22f + 0f)
    }
    val borderFinal = blended.copy(alpha = 0.5f).let {
        Color(it.red * 0.38f * 2f, it.green * 0.38f * 2f, it.blue * 0.38f * 2f, 0.5f)
    }
    val textFinal = Color(
        red   = blended.red   * 0.72f + 0.1f,
        green = blended.green * 0.72f + 0.1f,
        blue  = blended.blue  * 0.72f + 0.1f,
    )
    return Triple(bgFinal, borderFinal, textFinal)
}

// ── Entry ────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CueApp() }
    }
}

@Composable
fun CueApp() {
    var activeProject by remember { mutableStateOf(0) }
    var ddOpen by remember { mutableStateOf(false) }

    val proj = projects[activeProject]
    val glow = proj.color.copy(alpha = 0.09f)
    val (doneBg, doneBorder, doneText) = remember(activeProject) { doneColors(proj.color) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BG)
            // close dropdown on outside tap
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { if (ddOpen) ddOpen = false }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Project picker button + dropdown
                Box {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                ddOpen = !ddOpen
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Ring
                        Box(
                            Modifier
                                .size(28.dp)
                                .border(2.dp, Color(0xD9FFFFFF), CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(proj.color)
                            )
                        }
                        Text(
                            text = proj.name,
                            color = TextCol,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.15.sp
                        )
                        // Caret
                        val caretAngle by animateFloatAsState(
                            if (ddOpen) 180f else 0f,
                            animationSpec = tween(200), label = "caret"
                        )
                        CaretIcon(
                            Modifier.graphicsLayer { rotationZ = caretAngle },
                            tint = Muted
                        )
                    }

                    // Dropdown
                    if (ddOpen) {
                        Dropdown(
                            active = activeProject,
                            onSelect = { i ->
                                activeProject = i
                                ddOpen = false
                            },
                            modifier = Modifier
                                .padding(top = 46.dp)
                                .zIndex(10f)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { /* consume */ }
                        )
                    }
                }

                // Settings
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceC)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
                    contentAlignment = Alignment.Center
                ) {
                    SettingsIcon(tint = Color.White.copy(alpha = 0.55f))
                }
            }

            // ── Main area ──────────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Main task card
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                        .background(SurfaceC)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glow, Color.Transparent),
                                    center = Offset(size.width * 0.3f, 0f),
                                    radius = size.width * 0.7f
                                ),
                                radius = size.width * 0.7f,
                                center = Offset(size.width * 0.3f, 0f)
                            )
                        }
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "ГЛАВНАЯ ЗАДАЧА",
                            color = Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "Дочитать «Мастер и Маргарита» до конца недели",
                            color = TextCol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 25.sp
                        )
                        // Done button
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, doneBorder, RoundedCornerShape(10.dp))
                                .background(doneBg)
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CheckIcon(tint = doneText.copy(alpha = 0.7f))
                            Text("Выполнено", color = doneText, fontSize = 13.sp)
                        }
                    }
                }

                // Subtasks card
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Border, RoundedCornerShape(18.dp))
                        .background(SurfaceC)
                ) {
                    Text(
                        "ДАЛЕЕ",
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                    )

                    val subs = listOf(
                        "Купить молоко и хлеб",
                        "Записаться к стоматологу на осмотр",
                        "Позвонить маме",
                        "Оплатить интернет"
                    )

                    subs.forEachIndexed { i, text ->
                        SubItem(text)
                        if (i < subs.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .height(1.dp)
                                    .background(Sep)
                            )
                        }
                    }
                }

                // Add button
                Row(
                    Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlusIcon(tint = Muted)
                    Text("Добавить задачу", color = Muted, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SubItem(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0x33FFFFFF)))
        Text(
            text,
            color = Color(0xB3FFFFFF),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            contentAlignment = Alignment.Center
        ) {
            XIcon(tint = Color(0x73FFFFFF))
        }
    }
}

@Composable
fun Dropdown(active: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Color(0xFF141416))
    ) {
        projects.forEachIndexed { i, p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(p.color))
                Text(p.name, color = TextCol, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (i == active) CheckIcon(tint = p.color.copy(alpha = 0.6f))
            }
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(Sep))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlusIcon(tint = Muted, size = 14)
            Text("Новый проект", color = Muted, fontSize = 13.sp)
        }
    }
}

// ── Inline SVG icons via Canvas/Path ─────────────────────────────────────────
@Composable
fun CaretIcon(modifier: Modifier = Modifier, tint: Color) {
    androidx.compose.foundation.Canvas(modifier.size(12.dp)) {
        val s = size
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width * 0.17f, s.height * 0.33f)
            lineTo(s.width * 0.5f,  s.height * 0.67f)
            lineTo(s.width * 0.83f, s.height * 0.33f)
        }
        drawPath(p, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.5.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        ))
    }
}

@Composable
fun CheckIcon(tint: Color, size: Int = 14) {
    androidx.compose.foundation.Canvas(Modifier.size(size.dp)) {
        val s = size.dp.toPx()
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(s * 0.11f, s * 0.5f)
            lineTo(s * 0.43f, s * 0.82f)
            lineTo(s * 0.89f, s * 0.18f)
        }
        drawPath(p, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.6.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        ))
    }
}

@Composable
fun XIcon(tint: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(10.dp)) {
        val s = size
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        val p1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width * 0.1f, s.height * 0.1f)
            lineTo(s.width * 0.9f, s.height * 0.9f)
        }
        val p2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width * 0.9f, s.height * 0.1f)
            lineTo(s.width * 0.1f, s.height * 0.9f)
        }
        drawPath(p1, tint, style = stroke)
        drawPath(p2, tint, style = stroke)
    }
}

@Composable
fun PlusIcon(tint: Color, size: Int = 14) {
    androidx.compose.foundation.Canvas(Modifier.size(size.dp)) {
        val s = this.size
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        val v = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width / 2f, s.height * 0.07f)
            lineTo(s.width / 2f, s.height * 0.93f)
        }
        val h = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width * 0.07f, s.height / 2f)
            lineTo(s.width * 0.93f, s.height / 2f)
        }
        drawPath(v, tint, style = stroke)
        drawPath(h, tint, style = stroke)
    }
}

@Composable
fun SettingsIcon(tint: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
        val s = size
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        val cx = s.width / 2f; val cy = s.height / 2f
        val r1 = s.width * 0.139f  // inner circle
        val r2 = s.width * 0.278f  // outer dots distance

        // center circle
        drawCircle(tint, radius = r1, center = Offset(cx, cy), style = stroke)

        // 8 spokes
        val angles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
        val innerR = r1 + 2.dp.toPx()
        val outerR = r2 + 2.dp.toPx()
        angles.forEach { deg ->
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val cos = kotlin.math.cos(rad); val sin = kotlin.math.sin(rad)
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx + cos * innerR, cy + sin * innerR)
                lineTo(cx + cos * outerR, cy + sin * outerR)
            }
            drawPath(p, tint, style = stroke)
        }
    }
}
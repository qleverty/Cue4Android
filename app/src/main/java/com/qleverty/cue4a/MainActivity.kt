package com.qleverty.cue4a

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay

private val BG      = Color(0xFF09090B)
private val Border  = Color(0x12FFFFFF)
private val Sep     = Color(0x0FFFFFFF)
private val TextCol = Color(0xFFE8E8EA)
private val Muted   = Color(0x61FFFFFF)
private val Surface = Color(0x0AFFFFFF)

private fun doneColors(c: Color): Triple<Color, Color, Color> {
    val b  = Color(c.red * 0.35f + 0.13f * 0.65f, c.green * 0.35f + 0.76f * 0.65f, c.blue * 0.35f + 0.27f * 0.65f)
    val bg = Color(b.red * 0.22f, b.green * 0.22f, b.blue * 0.22f)
    return Triple(
        bg,
        Color(b.red * 0.76f, b.green * 0.76f, b.blue * 0.76f, 0.5f),
        Color(b.red * 0.72f + 0.1f, b.green * 0.72f + 0.1f, b.blue * 0.72f + 0.1f),
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vm = ViewModelProvider(this)[CueViewModel::class.java]
        setContent { CueApp(vm) }
    }
}

@Composable
fun CueApp(vm: CueViewModel) {
    val proj = vm.activeProject
    var showSettingsNotice by remember { mutableStateOf(false) }

    BackHandler(enabled = vm.ddOpen) { vm.closeDropdown() }

    Box(
        Modifier
            .fillMaxSize()
            .background(BG)
            .noRippleClick { if (vm.ddOpen) vm.closeDropdown() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    ProjectPickerButton(vm)
                    if (vm.ddOpen) {
                        Popup(
                            alignment  = Alignment.TopStart,
                            properties = PopupProperties(focusable = vm.projectAdding)
                        ) {
                            Box(Modifier.noRippleClick { vm.closeDropdown() }) {
                                ProjectDropdown(
                                    vm       = vm,
                                    modifier = Modifier
                                        .padding(start = 8.dp, top = 48.dp)
                                        .zIndex(100f)
                                        .noRippleClick {}
                                )
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.dp, Border, CircleShape)
                        .background(Surface)
                        .noRippleClick { showSettingsNotice = true },
                    contentAlignment = Alignment.Center
                ) {
                    SettingsIcon(tint = Color.White.copy(alpha = 0.55f))
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (proj != null) {
                    MainTaskCard(proj, vm)
                    SubtasksCard(proj, vm)
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Surface)
                    )
                }
            }
        }
    }

    vm.showDeleteConfirm?.let { idx ->
        DeleteConfirmDialog(
            name      = vm.projects.getOrNull(idx)?.name ?: "",
            onConfirm = { vm.confirmDeleteProject() },
            onDismiss = { vm.cancelDeleteProject() },
        )
    }

    if (showSettingsNotice) {
        SettingsNoticeDialog(onDismiss = { showSettingsNotice = false })
    }
}

@Composable
private fun ProjectPickerButton(vm: CueViewModel) {
    val proj = vm.activeProject
    val caretAngle by animateFloatAsState(
        targetValue   = if (vm.ddOpen) 180f else 0f,
        animationSpec = tween(200),
        label         = "caret"
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .noRippleClick { if (vm.ddOpen) vm.closeDropdown() else vm.ddOpen = true }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProjectLogo(color = proj?.color ?: Color(0xFF4A90D9))
        Text(proj?.name ?: "", color = TextCol, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp)
        CaretIcon(modifier = Modifier.graphicsLayer { rotationZ = caretAngle }, tint = Muted)
    }
}

@Composable
private fun MainTaskCard(proj: Project, vm: CueViewModel) {
    val glow = proj.color.copy(alpha = 0.09f)
    val (doneBg, doneBorder, doneText) = remember(proj.color) { doneColors(proj.color) }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .background(Surface)
            .drawBehind {
                drawCircle(
                    brush  = Brush.radialGradient(listOf(glow, Color.Transparent), Offset(size.width * 0.3f, 0f), size.width * 0.7f),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.3f, 0f),
                )
            }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ГЛАВНАЯ ЗАДАЧА", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
            if (proj.main.isNotEmpty()) {
                Text(proj.mainText() ?: "", color = TextCol, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, doneBorder, RoundedCornerShape(10.dp))
                        .background(doneBg)
                        .noRippleClick { vm.completeMain() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CheckIcon(tint = doneText.copy(alpha = 0.7f))
                    Text("Выполнено", color = doneText, fontSize = 13.sp)
                }
            } else {
                Text("Нет активных задач", color = Muted, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SubtasksCard(proj: Project, vm: CueViewModel) {
    val sorted = remember(proj.subs) { proj.subsSorted() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .background(Surface)
    ) {
        Text(
            "ПРОЧИЕ",
            color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)
        )
        if (sorted.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(sorted, key = { it.id }) { task ->
                    SubItem(
                        text      = task.text,
                        onPromote = { vm.promoteSub(task.id) },
                        onDelete  = { vm.deleteSub(task.id) },
                    )
                    if (task.id != sorted.last().id) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(Sep))
                    }
                }
            }
        }
        AddTaskRow(vm)
    }
}

@Composable
private fun AddTaskRow(vm: CueViewModel) {
    val focusRequester = remember { FocusRequester() }
    if (vm.addingTask) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        BasicTextField(
            value           = vm.taskInput,
            onValueChange   = { vm.taskInput = it },
            modifier        = Modifier.fillMaxWidth().focusRequester(focusRequester).padding(horizontal = 14.dp, vertical = 10.dp),
            textStyle       = TextStyle(color = TextCol, fontSize = 14.sp),
            cursorBrush     = SolidColor(TextCol),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.commitTaskInput() }),
            decorationBox   = { inner ->
                Box {
                    if (vm.taskInput.isEmpty()) Text("Новое задание...", color = Muted, fontSize = 14.sp)
                    inner()
                }
            }
        )
    } else {
        Row(
            Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .noRippleClick { vm.addingTask = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlusIcon(tint = Muted)
            Text("Добавить задачу", color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun SubItem(text: String, onPromote: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .noRippleClick { onPromote() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0x33FFFFFF)))
        Text(text, color = Color(0xB3FFFFFF), fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).noRippleClick { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            XIcon(tint = Color(0x73FFFFFF))
        }
    }
}

@Composable
fun ProjectDropdown(vm: CueViewModel, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Color(0xFF141416))
    ) {
        val listHeight = 48.dp * minOf(vm.projects.size, 6)
        LazyColumn(modifier = Modifier.height(listHeight)) {
            items(vm.projects, key = { it.id }) { p ->
                val idx = vm.projects.indexOf(p)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .noRippleClick { vm.switchToProject(idx); vm.closeDropdown() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(p.color))
                    Text(p.name, color = TextCol, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (idx == vm.activeProjectIdx) CheckIcon(tint = p.color.copy(alpha = 0.6f))
                    if (vm.projects.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).noRippleClick { vm.requestDeleteProject(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            XIcon(tint = Color(0x73FFFFFF))
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(Sep))
        if (vm.projectAdding) {
            LaunchedEffect(vm.projectAdding) {
                delay(50)
                focusRequester.requestFocus()
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PROJECT_PALETTE[vm.projectColorIdx])
                        .noRippleClick { vm.cycleProjectColor(); focusRequester.requestFocus() }
                )
                BasicTextField(
                    value           = vm.projectInput,
                    onValueChange   = { vm.projectInput = it },
                    modifier        = Modifier.weight(1f).focusRequester(focusRequester),
                    textStyle       = TextStyle(color = TextCol, fontSize = 14.sp),
                    cursorBrush     = SolidColor(TextCol),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.commitProjectInput() }),
                    decorationBox   = { inner ->
                        Box {
                            if (vm.projectInput.isEmpty()) Text("Название...", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).noRippleClick { vm.cancelProjectAdding() },
                    contentAlignment = Alignment.Center
                ) {
                    XIcon(tint = Muted)
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .noRippleClick { vm.startProjectAdding() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlusIcon(tint = Muted, size = 14)
                Text("Новый проект", color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF18181B))
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Удалить проект", color = TextCol, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("Удалить проект «$name»? Это действие нельзя отменить.", color = Color(0xB3FFFFFF), fontSize = 14.sp, lineHeight = 20.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp))
                        .background(Surface).noRippleClick { onDismiss() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Отмена", color = TextCol, fontSize = 14.sp) }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x55FF4444), RoundedCornerShape(12.dp))
                        .background(Color(0xFF7F1D1D)).noRippleClick { onConfirm() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Удалить", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun SettingsNoticeDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF18181B))
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ого, разраб чмо и ещё не сделал окно настроек, чтоб открывать его по этой кнопке. Подождите ещё немного, пожалуйста. Весь отдел программистов активно трудится над этим.", color = TextCol, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp))
                    .background(Surface).noRippleClick { onDismiss() }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Хайп", color = TextCol, fontSize = 14.sp) }
        }
    }
}

private fun Modifier.noRippleClick(onClick: () -> Unit) = this.clickable(
    indication = null,
    interactionSource = MutableInteractionSource(),
    onClick = onClick,
)

@Composable
private fun ProjectLogo(color: Color) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Image(
            painter          = painterResource(R.drawable.cue_center),
            contentDescription = null,
            modifier         = Modifier.fillMaxSize(),
            colorFilter      = ColorFilter.tint(color, BlendMode.SrcIn)
        )
        Image(
            painter          = painterResource(R.drawable.cue_contour),
            contentDescription = null,
            modifier         = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CaretIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier.size(12.dp)) {
        drawPath(Path().apply {
            moveTo(size.width * 0.17f, size.height * 0.33f)
            lineTo(size.width * 0.5f,  size.height * 0.67f)
            lineTo(size.width * 0.83f, size.height * 0.33f)
        }, tint, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CheckIcon(tint: Color, size: Int = 14) {
    Canvas(Modifier.size(size.dp)) {
        val s = size.dp.toPx()
        drawPath(Path().apply {
            moveTo(s * 0.11f, s * 0.5f)
            lineTo(s * 0.43f, s * 0.82f)
            lineTo(s * 0.89f, s * 0.18f)
        }, tint, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun XIcon(tint: Color) {
    Canvas(Modifier.size(10.dp)) {
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(size.width * 0.1f, size.height * 0.1f); lineTo(size.width * 0.9f, size.height * 0.9f) }, tint, style = stroke)
        drawPath(Path().apply { moveTo(size.width * 0.9f, size.height * 0.1f); lineTo(size.width * 0.1f, size.height * 0.9f) }, tint, style = stroke)
    }
}

@Composable
fun PlusIcon(tint: Color, size: Int = 14) {
    Canvas(Modifier.size(size.dp)) {
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(this@Canvas.size.width / 2f, this@Canvas.size.height * 0.07f); lineTo(this@Canvas.size.width / 2f, this@Canvas.size.height * 0.93f) }, tint, style = stroke)
        drawPath(Path().apply { moveTo(this@Canvas.size.width * 0.07f, this@Canvas.size.height / 2f); lineTo(this@Canvas.size.width * 0.93f, this@Canvas.size.height / 2f) }, tint, style = stroke)
    }
}

@Composable
fun SettingsIcon(tint: Color) {
    Canvas(Modifier.size(18.dp)) {
        val stroke  = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        val cx      = size.width / 2f
        val cy      = size.height / 2f
        val r1      = size.width * 0.139f
        val r2      = size.width * 0.278f + 2.dp.toPx()
        drawCircle(tint, radius = r1, center = Offset(cx, cy), style = stroke)
        listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).forEach { deg ->
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val cos = kotlin.math.cos(rad)
            val sin = kotlin.math.sin(rad)
            val inner = r1 + 2.dp.toPx()
            drawPath(Path().apply { moveTo(cx + cos * inner, cy + sin * inner); lineTo(cx + cos * r2, cy + sin * r2) }, tint, style = stroke)
        }
    }
}
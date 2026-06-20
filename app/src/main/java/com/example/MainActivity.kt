package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TimeManagerViewModel
import com.example.viewmodel.TimeManagerViewModelFactory
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup local Room database securely
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "time-manager-database"
        ).fallbackToDestructiveMigration().build()

        val repository = SessionRepository(db.focusSessionDao())

        setContent {
            MyApplicationTheme {
                TimeManagerApp(repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimeManagerApp(
    repository: SessionRepository,
    viewModel: TimeManagerViewModel = viewModel(factory = TimeManagerViewModelFactory(repository))
) {
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val todaySessions by viewModel.todaySessions.collectAsStateWithLifecycle()

    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showManualLogDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Navigation and Calendar state
    var currentTab by remember { mutableStateOf(1) } // 0: Focus Timer, 1: Google Calendar Schedule Board
    var selectedDayOffset by remember { mutableStateOf(0) } // 0 = today, -1 = yesterday, etc.

    // State for local dynamic greeting banner
    var systemTimeText by remember { mutableStateOf("") }
    var greetingText by remember { mutableStateOf("您好") }

    // Populate demo schedule plans once on first launch so the user gets Google Schedule style visuals immediately
    LaunchedEffect(allSessions) {
        if (allSessions.isEmpty()) {
            viewModel.populateDemoPlans()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            greetingText = when (hour) {
                in 5..10 -> "早上好"
                in 11..13 -> "中午好"
                in 14..18 -> "下午好"
                in 19..22 -> "晚上好"
                else -> "夜深了"
            }
            systemTimeText = SimpleDateFormat("E, d MMM H:mm:ss", Locale.CHINESE).format(now.time)
            delay(1000L)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Elegant Material 3 Styled Bottom Navigation Bar with fluid capsule transitions
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                // Tab 0: Focus Timer with glowing icon
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        val scale by animateFloatAsState(
                            targetValue = if (currentTab == 0) 1.25f else 1f,
                            animationSpec = spring(dampingRatio = 0.5f)
                        )
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Rounded.Timer else Icons.Rounded.HourglassBottom,
                            contentDescription = "专注计时",
                            modifier = Modifier.scale(scale)
                        )
                    },
                    label = { Text("极速专注", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Tab 1: Google Calendar Schedule Planner
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        val scale by animateFloatAsState(
                            targetValue = if (currentTab == 1) 1.25f else 1f,
                            animationSpec = spring(dampingRatio = 0.5f)
                        )
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Rounded.EventNote else Icons.Rounded.CalendarToday,
                            contentDescription = "时间规划",
                            modifier = Modifier.scale(scale)
                        )
                    },
                    label = { Text("时间规划表", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Fluid Slide & Fade animations when toggling between Focus Screen and Google Schedule Board
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                }
            ) { tab ->
                when (tab) {
                    0 -> {
                        FocusTimerScreen(
                            viewModel = viewModel,
                            todaySessions = todaySessions,
                            allSessions = allSessions,
                            greetingText = greetingText,
                            systemTimeText = systemTimeText,
                            onClearAll = { showClearConfirmDialog = true },
                            onManualLog = { showManualLogDialog = true },
                            onCustomDuration = { showCustomDurationDialog = true }
                        )
                    }
                    1 -> {
                        GoogleScheduleScreen(
                            viewModel = viewModel,
                            allSessions = allSessions,
                            greetingText = greetingText,
                            systemTimeText = systemTimeText,
                            selectedOffset = selectedDayOffset,
                            onDaySelected = { selectedDayOffset = it },
                            onClearAll = { showClearConfirmDialog = true },
                            onGoToFocusTab = {
                                currentTab = 0
                            }
                        )
                    }
                }
            }

            // Confetti Overlay celebrations
            if (viewModel.showCelebrationDialog) {
                ConfettiCelebration()
            }
        }
    }

    // --- CELEBRATION MODAL ---
    if (viewModel.showCelebrationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showCelebrationDialog = false },
            confirmButton = {
                Button(
                    onClick = { viewModel.showCelebrationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("继续前行", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = "祝贺",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "专注规划大获成功！",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                val inspirationalQuote = remember {
                    val quotes = listOf(
                        "专注是卓越的代名词。你刚才做得太棒了！",
                        "每一分专注，都是在计划下一步更好的自己。",
                        "自律带来真正的自由，保持规划，高效专注！",
                        "合抱之木，生于毫末；九层之台，起于累土。祝贺你攻克这块日程模块！"
                    )
                    quotes[Random().nextInt(quotes.size)]
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val durationMins = if (viewModel.lastCompletedSessionDuration != 0) viewModel.lastCompletedSessionDuration / 60 else 25
                    Text(
                        text = "您刚刚高效地完成了「${viewModel.selectedCategory}」长达 $durationMins 分钟的日历任务！",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "“$inspirationalQuote”",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- DIALOG: CUSTOM TIMER DURATION ---
    if (showCustomDurationDialog) {
        var minutesInput by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = minutesInput.toIntOrNull()
                        if (mins != null && mins in 1..180) {
                            viewModel.setTimerDuration(mins)
                            showCustomDurationDialog = false
                        } else {
                            inputError = true
                        }
                    }
                ) {
                    Text("确 定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("取 消")
                }
            },
            title = { Text("自定义单次专注时长", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("请输入专注分钟数 (范围: 1 - 180 分钟):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = {
                            minutesInput = it
                            inputError = false
                        },
                        placeholder = { Text("例如 25") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError,
                        supportingText = {
                            if (inputError) {
                                  Text("请输入有效数字范围 (1-180)", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // --- DIALOG: QUICK MANUAL SESSIONS ENGINE ---
    if (showManualLogDialog) {
        var minutesInput by remember { mutableStateOf("25") }
        var noteInput by remember { mutableStateOf("") }
        var categorySelected by remember { mutableStateOf("工作") }
        var errorOccurred by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualLogDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = minutesInput.toIntOrNull()
                        if (mins != null && mins > 0) {
                            viewModel.logManualSession(categorySelected, mins, noteInput)
                            showManualLogDialog = false
                        } else {
                            errorOccurred = true
                        }
                    }
                ) {
                    Text("登 记", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLogDialog = false }) {
                    Text("取 消")
                }
            },
            title = { Text("极速时间手动登记", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("刚刚完成了事情但忘记开计时器？在此手动日志保存：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    // Display category selector in sheets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("工作", "学习", "运动", "创意", "生活").forEach { cat ->
                            val isChosen = categorySelected == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { categorySelected = cat }
                                    .border(
                                        width = 1.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = {
                            minutesInput = it
                            errorOccurred = false
                        },
                        label = { Text("时长 (分钟)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = errorOccurred,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("专注备注信息") },
                        placeholder = { Text("写个小标题, 例如'阅读一章节'") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // --- DIALOG: RECONCILIATION CLEAR ALL CONFIRMATION ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取 消")
                }
            },
            title = { Text("请确认清空记录", fontWeight = FontWeight.Bold) },
            text = { Text("此操作将永久抹除所有本地的专注和时间管理数据，无法挽回。是否继续？", fontSize = 14.sp) },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ==========================================
// SCREEN 0: QUICK FOCUS TIMER SCREEN (PRO POMODORO VIEW WITH BREATHING RING)
// ==========================================
@Composable
fun FocusTimerScreen(
    viewModel: TimeManagerViewModel,
    todaySessions: List<FocusSession>,
    allSessions: List<FocusSession>,
    greetingText: String,
    systemTimeText: String,
    onClearAll: () -> Unit,
    onManualLog: () -> Unit,
    onCustomDuration: () -> Unit
) {
    // Pulsing aura animation for the running timer ring
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // HEADER ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "时",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
                Column {
                    Text(
                        text = "极速专注计时器",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "$greetingText，今日已专注 ${todaySessions.filter { it.durationSeconds > 0 }.size} 次",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            IconButton(
                onClick = onClearAll,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "清空数据",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Progress Meter
            item {
                val goalSessions = 4
                val completedToday = todaySessions.filter { it.durationSeconds > 0 }.size
                val percent = if (completedToday >= goalSessions) 100 else (completedToday * 100 / goalSessions)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .scale(if (viewModel.isTimerRunning) pulseScale else 1f)
                        ) {
                            val remainingRatio = completedToday.toFloat() / goalSessions.toFloat()
                            val sweepAngle = (if (remainingRatio > 1f) 1f else remainingRatio) * 360f

                            val circleTrackColor = if (isSystemInDarkTheme()) Color(0xFF4D3230) else Color(0xFFE5D1D0)
                            val circleProgressColor = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFF8B413E)
                            val circleTextColor = if (isSystemInDarkTheme()) Color(0xFFFFDAD8) else Color(0xFF410002)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = circleTrackColor,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = circleProgressColor,
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = circleTextColor
                                )
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val textTitleColor = if (isSystemInDarkTheme()) Color(0xFFFFDAD8) else Color(0xFF410002)
                            val textSubColor = if (isSystemInDarkTheme()) Color(0xFFD8C2C0) else Color(0xFF534341)

                            Text(
                                text = "今日专注完成度",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textTitleColor
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (completedToday >= goalSessions)
                                    "太棒了！已达成今日专注大圆满！"
                                else "保持专注！再完成 ${goalSessions - completedToday} 次专注即可达到今日目标。",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = textSubColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // CIRCULAR countdown timer with animated progress
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(240.dp)
                                .padding(8.dp)
                        ) {
                            val remainingRatio = if (viewModel.totalDurationSeconds > 0) {
                                viewModel.secondsRemaining.toFloat() / viewModel.totalDurationSeconds.toFloat()
                            } else 1f

                            val sweepAngle = remainingRatio * 360f

                            val trackColor = MaterialTheme.colorScheme.surfaceVariant
                            val primaryTeal = MaterialTheme.colorScheme.primary
                            val accentSecondary = MaterialTheme.colorScheme.secondary

                            // Pulsing glowing ring backing the active timer
                            if (viewModel.isTimerRunning) {
                                Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
                                    drawCircle(
                                        color = primaryTeal.copy(alpha = 0.08f),
                                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = trackColor,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )

                                drawArc(
                                    brush = Brush.linearGradient(
                                        colors = listOf(primaryTeal, accentSecondary)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Middle timer data
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = getCategoryIcon(viewModel.selectedCategory),
                                    contentDescription = "当前分类",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatSecondsToTime(viewModel.secondsRemaining),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = (-1).sp
                                    ),
                                    color = if (viewModel.isTimerRunning) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.isTimerRunning) "正在高效专注中" else "准备就绪",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (viewModel.isTimerRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (viewModel.isTimerRunning) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.resetTimer() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "重置",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Button(
                                onClick = {
                                    if (viewModel.isTimerRunning) {
                                        viewModel.pauseTimer()
                                    } else {
                                        viewModel.startTimer()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.isTimerRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.background
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(56.dp)
                                    .width(160.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (viewModel.isTimerRunning) "暂停" else "开始"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (viewModel.isTimerRunning) " 暂停专注 " else " 开始专注 ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            IconButton(
                                onClick = onManualLog,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NoteAdd,
                                    contentDescription = "手动记录",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Duration shortcuts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val presets = listOf(5, 15, 25, 45, 60)
                            presets.forEach { min ->
                                val isSelected = viewModel.totalDurationSeconds == min * 60
                                SuggestionChip(
                                    onClick = { viewModel.setTimerDuration(min) },
                                    label = { Text("${min}分", fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        labelColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        enabled = true
                                    )
                                )
                            }

                            SuggestionChip(
                                onClick = onCustomDuration,
                                label = { Text("自定义", fontWeight = FontWeight.Bold) },
                                icon = {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Category select row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "选择专注方向",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.categories.take(5).forEach { category ->
                            val isSelected = viewModel.selectedCategory == category
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { viewModel.selectCategory(category) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = getCategoryIcon(category),
                                        contentDescription = category,
                                        tint = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.currentNote,
                        onValueChange = { viewModel.updateNote(it) },
                        placeholder = { Text("写个小目标 (例如: 读英语书/开发代码)") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "手写目标",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Stats row
            item {
                val stats = getSessionsStats(todaySessions)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "今日统计",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${stats.totalMinutes} 分钟",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "今日专注时长",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${todaySessions.filter { it.durationSeconds > 0 }.size} 次",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "今日专注周期",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // History Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "完成历史记录",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            val completedList = allSessions.filter { it.durationSeconds > 0 }
            if (completedList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassEmpty,
                            contentDescription = "空记录",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "尚无专注记录。在上方设置分类，启动专注吧！",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            } else {
                items(completedList, key = { it.id }) { session ->
                    HistoryLogItem(
                        session = session,
                        onDelete = { viewModel.deleteSession(session) }
                    )
                }
            }
        }
    }
}


// ==========================================
// SCREEN 1: GOOGLE CALENDAR SCHEDULE LIST TIME-TABLE VIEW (每日规划时间规划表)
// ==========================================
@Composable
fun GoogleScheduleScreen(
    viewModel: TimeManagerViewModel,
    allSessions: List<FocusSession>,
    greetingText: String,
    systemTimeText: String,
    selectedOffset: Int,
    onDaySelected: (Int) -> Unit,
    onClearAll: () -> Unit,
    onGoToFocusTab: () -> Unit
) {
    // Dropdown or expand layout state
    var showAddPlanField by remember { mutableStateOf(false) }

    // Adder parameters
    var planNote by remember { mutableStateOf("") }
    var planCategory by remember { mutableStateOf("工作") }
    var planMins by remember { mutableStateOf(30) }
    var planHour by remember { mutableStateOf(10) }

    // Week dates
    val weekDays = remember { generateCalendarDays() }
    val activeDay = weekDays.firstOrNull { it.offset == selectedOffset } ?: weekDays[2]

    // Filtering items happening on the active calendar day
    val activeDaySlots = allSessions.filter { session ->
        val cal = Calendar.getInstance().apply { timeInMillis = session.timestamp }
        cal.get(Calendar.YEAR) == activeDay.year && cal.get(Calendar.DAY_OF_YEAR) == activeDay.dayOfYear
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP GOOGLE CALENDAR BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val monthName = remember(activeDay) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, selectedOffset)
                    SimpleDateFormat("yyyy年 MMMM", Locale.CHINESE).format(cal.time)
                }
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Google 日程习惯规划",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClearAll,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "清空数据",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }

        // GOOGLE CALENDAR HORIZONTAL WEEK SLIDER (With springy size & padding animations)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { day ->
                val isSelected = selectedOffset == day.offset
                
                // Animate chip scale & elevation spring
                val elevationAnimation by animateDpAsState(
                    targetValue = if (isSelected) 4.dp else 0.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
                
                Card(
                    onClick = { onDaySelected(day.offset) },
                    modifier = Modifier
                        .width(48.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevationAnimation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = day.dayOfWeek,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                        Text(
                            text = day.dateNum.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                        )
                        
                        // Tiny bottom red indicator for current today
                        if (day.offset == 0) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF8B413E)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // COLLAPSIBLE COLLAPSING PLANNER FORM
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddPlanField = !showAddPlanField },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (showAddPlanField) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (showAddPlanField) "正在规划日程..." else "➕ 规划新的一天 (快捷添加计划)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showAddPlanField = !showAddPlanField }) {
                                Icon(
                                    imageVector = if (showAddPlanField) Icons.Rounded.Close else Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (showAddPlanField) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Enter Title note
                            OutlinedTextField(
                                value = planNote,
                                onValueChange = { planNote = it },
                                label = { Text("规划任务标题") },
                                placeholder = { Text("例如：下午工作茶话会/跑圈...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Hour selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "计划时辰：",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val hoursDemo = listOf(9, 11, 14, 16, 19, 21)
                                    hoursDemo.forEach { h ->
                                        val isChosen = planHour == h
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .clickable { planHour = h }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = String.format("%02d:00", h),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChosen) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Category chips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                viewModel.categories.take(5).forEach { cat ->
                                    val isChosen = planCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { planCategory = cat }
                                            .border(
                                                width = 1.dp,
                                                color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Plan mins selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val minsPresets = listOf(15, 25, 45, 60)
                                minsPresets.forEach { min ->
                                    val isChosen = planMins == min
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isChosen) MaterialTheme.colorScheme.tertiary else Color.Transparent
                                            )
                                            .clickable { planMins = min }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${min}分钟",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChosen) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Save Button
                            Button(
                                onClick = {
                                    if (planNote.isNotBlank()) {
                                        viewModel.logPlannedSession(
                                            category = planCategory,
                                            durationMinutes = planMins,
                                            note = planNote,
                                            dateOffsetDays = selectedOffset,
                                            hourOfDay = planHour
                                        )
                                        // Reset
                                        planNote = ""
                                        showAddPlanField = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("保存计划并发布到时间轴", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // TIMELINE AGENDA AXIS GROUP LIST
            // Hours segments from 08:00 to 22:00
            val hoursRange = (8..22)

            if (activeDaySlots.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventAvailable,
                            contentDescription = "暂无日程",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "该天没有任何日程安排",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击上方「➕ 规划新的一天」创建第一个时间表模块吧！",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                hoursRange.forEach { hr ->
                    // Get all slots belonging to this specific HourOfDay
                    val slotsForThisHour = activeDaySlots.filter { session ->
                        val cal = Calendar.getInstance().apply { timeInMillis = session.timestamp }
                        cal.get(Calendar.HOUR_OF_DAY) == hr
                    }

                    if (slotsForThisHour.isNotEmpty()) {
                        item(key = "hour_label_$hr") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour Tag Left side
                                Box(
                                    modifier = Modifier.width(55.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = String.format("%02d:00", hr),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Right side: beautiful hair-line connector
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                )
                            }
                        }

                        // Plot items of this hour
                        items(slotsForThisHour, key = { "slot_${it.id}" }) { slot ->
                            GoogleAgendaItemCard(
                                session = slot,
                                onCheckToggle = {
                                    if (slot.durationSeconds < 0) {
                                        viewModel.completePlannedSession(slot)
                                    }
                                },
                                onDelete = {
                                    viewModel.deleteSession(slot)
                                },
                                onShortCutFocus = {
                                    viewModel.selectCategory(slot.category)
                                    viewModel.updateNote(slot.note)
                                    onGoToFocusTab()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- GOOGLE AGENDA EVENT ITEM CARD ( Pastel Color Card blocks ) ---
@Composable
fun GoogleAgendaItemCard(
    session: FocusSession,
    onCheckToggle: () -> Unit,
    onDelete: () -> Unit,
    onShortCutFocus: () -> Unit
) {
    val isCompleted = session.durationSeconds > 0
    val durationMins = if (session.durationSeconds < 0) -session.durationSeconds / 60 else session.durationSeconds / 60
    
    // Retrieve calendar colors
    val schemeColors = getScheduleColors(session.category)
    val cardBg = if (isSystemInDarkTheme()) schemeColors.bgDark else schemeColors.bgLight
    val textMain = if (isSystemInDarkTheme()) schemeColors.textDark else schemeColors.textLight
    val stripeHighlight = schemeColors.accent

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 75.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Thick Colored Stripe/Stripe Highlight (like Google Calendar)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(64.dp)
                    .background(stripeHighlight)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Checkbox or Completed circle badge with springy scale pops on check triggers
                    if (!isCompleted) {
                        IconButton(
                            onClick = onCheckToggle,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "点击完成计划",
                                tint = textMain,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // Fully completed checked dot
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(stripeHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "完成印记",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = session.note,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textMain,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = if (isCompleted) {
                                MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = session.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMain,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(stripeHighlight.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                            Text(
                                text = if (isCompleted) "已专注 $durationMins 分钟" else "计划限时 $durationMins 分钟",
                                fontSize = 11.sp,
                                color = textMain.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Delete handle
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "取消日程",
                        tint = textMain.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Details expanded panel
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompleted) "🎉 这一专注计划在今天大功告成！" else "💡 快速启动专注流程以实践此规划日程",
                    fontSize = 11.sp,
                    color = textMain.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                
                if (!isCompleted) {
                    Button(
                        onClick = onShortCutFocus,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = stripeHighlight,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("去计时 ⏱️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ==========================================
// CONFETTI PARTICLES POPPING CELEBRATION
// ==========================================
@Composable
fun ConfettiCelebration() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    
    // Animate coordinates for dynamic floats
    val confettiOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti1"
    )
    val confettiOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti2"
    )
    val confettiFall by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiFall"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti pieces
        listOf(
            Color(0xFFEA4335) to 50.dp to 100.dp,
            Color(0xFF4285F4) to 150.dp to 50.dp,
            Color(0xFFFBBC05) to 250.dp to 150.dp,
            Color(0xFF34A853) to 300.dp to 180.dp,
            Color(0xFF8B413E) to 80.dp to 220.dp,
            Color(0xFF9334E6) to 200.dp to 300.dp
        ).forEachIndexed { index, data ->
            val color = data.first.first
            val xStart = data.first.second
            val yStart = data.second

            val xOffset = if (index % 2 == 0) confettiOffset1 else confettiOffset2
            
            Box(
                modifier = Modifier
                    .offset(x = xStart + xOffset.dp, y = yStart + confettiFall.dp)
                    .size(if (index % 3 == 0) 12.dp else 8.dp)
                    .clip(if (index % 2 == 0) CircleShape else RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}


// ==========================================
// DATE DATA STRUCTURE FOR GOOGLE CALENDAR
// ==========================================
data class CalendarDay(
    val dayOfWeek: String,
    val dateNum: Int,
    val offset: Int,
    val year: Int,
    val dayOfYear: Int
)

fun generateCalendarDays(): List<CalendarDay> {
    val list = mutableListOf<CalendarDay>()
    val daysOfWeekChinese = listOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
    for (offset in -2..4) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK)
        list.add(
            CalendarDay(
                dayOfWeek = daysOfWeekChinese[dayOfWeekIndex],
                dateNum = cal.get(Calendar.DAY_OF_MONTH),
                offset = offset,
                year = cal.get(Calendar.YEAR),
                dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            )
        )
    }
    return list
}


// ==========================================
// GOOGLE PALETTE COLOR CHIPS MAPPINGS
// ==========================================
data class ScheduleColors(
    val bgLight: Color,
    val textLight: Color,
    val bgDark: Color,
    val textDark: Color,
    val accent: Color
)

fun getScheduleColors(category: String): ScheduleColors {
    return when (category) {
        "工作" -> ScheduleColors(
            bgLight = Color(0xFFE8F0FE), textLight = Color(0xFF1967D2),
            bgDark = Color(0xFF1F355C), textDark = Color(0xFF8AB4F8),
            accent = Color(0xFF4285F4)
        )
        "学习" -> ScheduleColors(
            bgLight = Color(0xFFE6F4EA), textLight = Color(0xFF137333),
            bgDark = Color(0xFF1D3B26), textDark = Color(0xFF81C995),
            accent = Color(0xFF34A853)
        )
        "运动" -> ScheduleColors(
            bgLight = Color(0xFFFEF7E0), textLight = Color(0xFFB06000),
            bgDark = Color(0xFF3C2F12), textDark = Color(0xFFFDD663),
            accent = Color(0xFFFBBC05)
        )
        "创意" -> ScheduleColors(
            bgLight = Color(0xFFF3E8FD), textLight = Color(0xFF7627BB),
            bgDark = Color(0xFF341B4D), textDark = Color(0xFFD7AEFB),
            accent = Color(0xFF9334E6)
        )
        "生活" -> ScheduleColors(
            bgLight = Color(0xFFFCE8E6), textLight = Color(0xFFC5221F),
            bgDark = Color(0xFF3E1F1C), textDark = Color(0xFFF28B82),
            accent = Color(0xFFEA4335)
        )
        else -> ScheduleColors(
            bgLight = Color(0xFFF1F3F4), textLight = Color(0xFF5F6368),
            bgDark = Color(0xFF2C2C2C), textDark = Color(0xFFBDC1C6),
            accent = Color(0xFF7F8C8D)
        )
    }
}


// --- LOGS LAYOUT CARD ITEM ---
@Composable
fun HistoryLogItem(
    session: FocusSession,
    onDelete: () -> Unit
) {
    val durationMinutes = session.durationSeconds / 60
    val durationString = if (durationMinutes > 0) "$durationMinutes 分钟" else "${session.durationSeconds} 秒"

    val calendar = Calendar.getInstance().apply { timeInMillis = session.timestamp }
    val formattedTime = SimpleDateFormat("H:mm", Locale.getDefault()).format(calendar.time)
    val isToday = isTodayTimestamp(session.timestamp)
    val dateString = if (isToday) "今天 $formattedTime" else {
        SimpleDateFormat("yyyy-MM-dd H:mm", Locale.getDefault()).format(calendar.time)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category icon circle badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(session.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = session.note,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = session.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                        Text(
                            text = dateString,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = durationString,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "删除该日志",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


// --- UTILITIES & HELPERS ---

fun formatSecondsToTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "工作" -> Icons.Rounded.Terminal
        "学习" -> Icons.Rounded.Book
        "运动" -> Icons.Rounded.FitnessCenter
        "创意" -> Icons.Rounded.Palette
        "生活" -> Icons.Rounded.Coffee
        "备忘" -> Icons.Rounded.EditNote
        else -> Icons.Rounded.Timer
    }
}

fun isTodayTimestamp(timestamp: Long): Boolean {
    val current = Calendar.getInstance()
    val logTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    return current.get(Calendar.YEAR) == logTime.get(Calendar.YEAR) &&
            current.get(Calendar.DAY_OF_YEAR) == logTime.get(Calendar.DAY_OF_YEAR)
}

data class TodaySessionStats(
    val totalMinutes: Int
)

fun getSessionsStats(sessions: List<FocusSession>): TodaySessionStats {
    val totalSecs = sessions.sumOf { it.durationSeconds }
    return TodaySessionStats(
        totalMinutes = totalSecs / 60
    )
}

// Extension to get colors safely in dark mode
val ColorScheme.textMutedDark: Color
    @Composable
    get() = Color(0xFF64748B)

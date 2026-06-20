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

    // State for local dynamic greeting banner
    var systemTimeText by remember { mutableStateOf("") }
    var greetingText by remember { mutableStateOf("您好") }

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
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // 1. HEADER SECTION
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
                            .background(MaterialTheme.colorScheme.tertiary)
                            .clickable { /* Profile info */ },
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
                            text = "时间管理器",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "$greetingText，今日专注 ${todaySessions.size} 次",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                IconButton(
                    onClick = { showClearConfirmDialog = true },
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
                // 1.5 Hero State: Daily Progress Card
                item {
                    val goalSessions = 4
                    val completedToday = todaySessions.size
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
                                modifier = Modifier.size(76.dp)
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
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val textTitleColor = if (isSystemInDarkTheme()) Color(0xFFFFDAD8) else Color(0xFF410002)
                                val textSubColor = if (isSystemInDarkTheme()) Color(0xFFD8C2C0) else Color(0xFF534341)

                                Text(
                                    text = "每日专注进度",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textTitleColor
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (completedToday >= goalSessions) 
                                        "太棒了！已达成今日专注目标！" 
                                        else "保持专注！再完成 ${goalSessions - completedToday} 个任务即可达到今日目标。",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textSubColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. TIMER CARD & CIRCULAR RING
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
                            // Circular Indicator Visualizer
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

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Draw background track
                                    drawCircle(
                                        color = trackColor,
                                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Draw animated progress track with elegant gradient
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

                                // Interactive display numbers inside ring
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
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

                            // Controls Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Reset Button
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

                                // Play & Pause Button
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
                                            fontSize = 16.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                // Manual log or quick add button
                                IconButton(
                                    onClick = { showManualLogDialog = true },
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

                            // Custom Preset Selection Chips
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

                                // Custom Entry Minute button
                                SuggestionChip(
                                    onClick = { showCustomDurationDialog = true },
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

                // 3. TARGET FOCUS AND NOTE INPUT PANEL
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "选择您的专注方向与任务",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Categories chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.categories.forEach { category ->
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
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
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

                        // Focus Session note text indicator
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

                // 4. STATS SUMMARY SECTION
                item {
                    val stats = getSessionsStats(todaySessions)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "今日专注面板",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Total hours today Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccessTime,
                                        contentDescription = "总时长",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${stats.totalMinutes} 分钟",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "今日总专注时长",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // High count today Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Assessment,
                                        contentDescription = "频次",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${todaySessions.size} 次",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "高效周期数",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. HISTORY TRACK LOGS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "专注历史记录",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                if (allSessions.isEmpty()) {
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
                                tint = MaterialTheme.colorScheme.textMutedDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "尚无专注记录，挑选上方分类开始你的第一段专注吧！",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                } else {
                    items(allSessions, key = { it.id }) { session ->
                        HistoryLogItem(
                            session = session,
                            onDelete = { viewModel.deleteSession(session) }
                        )
                    }
                }
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
                        text = "专注太棒了！",
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
                        "每一分专注，都是在雕刻更好的自己。",
                        "自律带来真正的自由，保持专注，继续前行！",
                        "合抱之木，生于毫末；九层之台，起于累土。祝贺你完成这次专注！"
                    )
                    quotes[Random().nextInt(quotes.size)]
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "您刚刚高效地专注于「${viewModel.selectedCategory}」长达 ${viewModel.lastCompletedSessionDuration / 60} 分钟！",
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

package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.theme.*
import com.example.viewmodel.OrbitTab
import com.example.viewmodel.OrbitViewModel
import com.example.viewmodel.TaskTabMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun OrbitApp(viewModel: OrbitViewModel) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val confettiEvent by viewModel.confettiEvent.collectAsStateWithLifecycle()

    OrbitBaseBackground {
        if (userName == null) {
            OnboardingScreen(onNameEntered = { viewModel.registerUser(it) })
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        OrbitBottomNavBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.setTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Header with Cute Rabbit companion "INFINITE"
                        OrbitHeaderSection(viewModel = viewModel)

                        // Main Tab Content Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith
                                            fadeOut(animationSpec = tween(200))
                                },
                                label = "tab_fade"
                            ) { tab ->
                                when (tab) {
                                    OrbitTab.Tasks -> TasksAndDumpScreen(viewModel)
                                    OrbitTab.Write -> WriteScreen(viewModel)
                                    OrbitTab.Calendar -> CalendarScreen(viewModel)
                                    OrbitTab.Bin -> BinScreen(viewModel)
                                }
                            }
                        }
                    }
                }

                // Overlay confetti on top of everything when triggered
                ConfettiCelebrationOverlay(
                    isActive = confettiEvent,
                    onFinished = { viewModel.dismissConfetti() }
                )
            }
        }
    }
}

// --- ONBOARDING / NAME ASKING SCREEN ---
@Composable
fun OnboardingScreen(onNameEntered: (String) -> Unit) {
    var nameInput by remember { mutableStateFlowOf("") }
    var showError by remember { mutableStateFlowOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing star / galaxy shape
        Box(
            modifier = Modifier
                .size(120.dp)
                .drawBehind {
                    drawCircle(
                        color = OrbitPrimary.copy(alpha = 0.25f),
                        radius = size.minDimension / 1.5f
                    )
                    drawCircle(
                        color = OrbitAccent.copy(alpha = 0.3f),
                        radius = size.minDimension / 2.5f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Draw a simplified rabbit face inline
            Canvas(modifier = Modifier.size(70.dp)) {
                // Ears
                drawOval(
                    color = RabbitCream,
                    topLeft = Offset(15f, 0f),
                    size = androidx.compose.ui.geometry.Size(25f, 85f)
                )
                drawOval(
                    color = RabbitPink,
                    topLeft = Offset(20f, 15f),
                    size = androidx.compose.ui.geometry.Size(15f, 55f)
                )

                drawOval(
                    color = RabbitCream,
                    topLeft = Offset(60f, 0f),
                    size = androidx.compose.ui.geometry.Size(25f, 85f)
                )
                drawOval(
                    color = RabbitPink,
                    topLeft = Offset(65f, 15f),
                    size = androidx.compose.ui.geometry.Size(15f, 55f)
                )

                // Head
                drawCircle(color = RabbitCream, radius = 45f, center = Offset(50f, 90f))
                // Eyes
                drawCircle(color = RabbitTextColor, radius = 6f, center = Offset(32f, 85f))
                drawCircle(color = RabbitTextColor, radius = 6f, center = Offset(68f, 85f))
                // Nose
                drawCircle(color = RabbitRed, radius = 4f, center = Offset(50f, 98f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Orbit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OrbitText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A calm personal space to organize your busy mind. Before we begin, what should I call you?",
            style = MaterialTheme.typography.bodyLarge,
            color = OrbitText.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = {
                nameInput = it
                if (showError && it.isNotBlank()) showError = false
            },
            placeholder = { Text("Enter your name...", color = OrbitText.copy(alpha = 0.5f)) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = OrbitCard,
                unfocusedContainerColor = OrbitCard,
                focusedBorderColor = OrbitPrimary,
                unfocusedBorderColor = OrbitPrimary.copy(alpha = 0.4f),
                focusedTextColor = OrbitText,
                unfocusedTextColor = OrbitText
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("onboarding_name_input")
        )

        if (showError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter a warm name to proceed.",
                color = RabbitRed,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nameInput.trim().isNotBlank()) {
                    onNameEntered(nameInput.trim())
                } else {
                    showError = true
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrbitPrimary),
            modifier = Modifier
                .height(48.dp)
                .width(200.dp)
                .testTag("onboarding_start_button")
        ) {
            Text("Enter Orbit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}


// --- SUBTLE COSMIC SPACE BACKGROUND DRAWING ---
@Composable
fun OrbitBaseBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitBackground)
            .drawBehind {
                // Nebula glow 1 (Lavender - Primary)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(OrbitPrimary.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.25f),
                        radius = size.minDimension * 0.5f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.25f),
                    radius = size.minDimension * 0.5f
                )

                // Nebula glow 2 (Peach - Accent)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(OrbitAccent.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.75f),
                        radius = size.minDimension * 0.6f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.75f),
                    radius = size.minDimension * 0.6f
                )

                // Simple subtle stars (sparkling coordinate system)
                val stars = listOf(
                    Offset(0.12f, 0.1f), Offset(0.85f, 0.15f),
                    Offset(0.35f, 0.42f), Offset(0.72f, 0.38f),
                    Offset(0.2f, 0.65f), Offset(0.9f, 0.55f),
                    Offset(0.55f, 0.82f), Offset(0.15f, 0.92f)
                )

                stars.forEach { percentOffset ->
                    val x = percentOffset.x * size.width
                    val y = percentOffset.y * size.height

                    // Draw tiny starry sparkles
                    drawCircle(
                        color = OrbitPrimary.copy(alpha = 0.3f),
                        radius = 4f,
                        center = Offset(x, y)
                    )
                    // Core point
                    drawCircle(
                        color = Color.White,
                        radius = 1.5f,
                        center = Offset(x, y)
                    )
                }

                // Constellation lines (dashed, very soft)
                drawPath(
                    path = Path().apply {
                        moveTo(0.12f * size.width, 0.1f * size.height)
                        lineTo(0.35f * size.width, 0.42f * size.height)
                        lineTo(0.2f * size.width, 0.65f * size.height)
                    },
                    color = OrbitPrimary.copy(alpha = 0.05f),
                    style = Stroke(
                        width = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f), 0f
                        )
                    )
                )

                drawPath(
                    path = Path().apply {
                        moveTo(0.85f * size.width, 0.15f * size.height)
                        lineTo(0.72f * size.width, 0.38f * size.height)
                        lineTo(0.9f * size.width, 0.55f * size.height)
                    },
                    color = OrbitAccent.copy(alpha = 0.05f),
                    style = Stroke(
                        width = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f), 0f
                        )
                    )
                )
            }
    ) {
        content()
    }
}


// --- COMPANION SECTION (HEADER SPEECH BUBBLE) ---
@Composable
fun OrbitHeaderSection(viewModel: OrbitViewModel) {
    val rabbitMessage by viewModel.rabbitMessage.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()

    // Floating/bobbing translation animation for Infinite the Rabbit
    val infiniteTransition = rememberInfiniteTransition(label = "rabbit_bob")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rabbit_bob"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Upper banner with logo and streak count badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Orbit",
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = OrbitPrimary,
                letterSpacing = 1.sp
            )

            // Cozy Streak fire 🔥 badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrbitAccent.copy(alpha = 0.12f))
                    .border(1.dp, OrbitAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔥", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (streakCount > 0) "$streakCount days" else "0 days",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrbitText
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cute Vector Rabbit Canvas with Hover / Click interaction
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = bobbingOffset.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.triggerRandomEncouragement()
                    }
                    .testTag("rabbit_infinite_clickable")
            ) {
                RabbitVectorDrawing()
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Speech Bubble Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrbitCard)
                    .border(1.dp, OrbitPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "INFINITE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrbitPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rabbitMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OrbitText,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


// --- CUTE VECTOR RABBIT (CANVAS DRAWING) ---
@Composable
fun RabbitVectorDrawing() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Outer glow
        drawCircle(
            color = RabbitPink.copy(alpha = 0.15f),
            radius = width * 0.45f,
            center = Offset(width * 0.5f, height * 0.55f)
        )

        // ears
        // Left Ear outer
        drawOval(
            color = RabbitCream,
            topLeft = Offset(width * 0.28f, height * 0.05f),
            size = androidx.compose.ui.geometry.Size(width * 0.16f, height * 0.52f)
        )
        // Left Ear inner
        drawOval(
            color = RabbitPink,
            topLeft = Offset(width * 0.32f, height * 0.16f),
            size = androidx.compose.ui.geometry.Size(width * 0.08f, height * 0.34f)
        )

        // Right Ear outer (slightly rotated/tighter)
        drawOval(
            color = RabbitCream,
            topLeft = Offset(width * 0.53f, height * 0.05f),
            size = androidx.compose.ui.geometry.Size(width * 0.16f, height * 0.52f)
        )
        // Right Ear inner
        drawOval(
            color = RabbitPink,
            topLeft = Offset(width * 0.57f, height * 0.16f),
            size = androidx.compose.ui.geometry.Size(width * 0.08f, height * 0.34f)
        )

        // Cozy Space Helmet Ring (Background)
        drawCircle(
            color = RabbitLavender.copy(alpha = 0.4f),
            radius = width * 0.35f,
            center = Offset(width * 0.5f, height * 0.62f),
            style = Stroke(width = 4f)
        )

        // Head Base
        drawCircle(
            color = RabbitCream,
            radius = width * 0.28f,
            center = Offset(width * 0.5f, height * 0.62f)
        )

        // Cheek blush (peach)
        drawCircle(
            color = RabbitPeach,
            radius = width * 0.06f,
            center = Offset(width * 0.32f, height * 0.65f)
        )
        drawCircle(
            color = RabbitPeach,
            radius = width * 0.06f,
            center = Offset(width * 0.68f, height * 0.65f)
        )

        // Eyes (Dark circles + sparkles)
        drawCircle(
            color = RabbitTextColor,
            radius = width * 0.045f,
            center = Offset(width * 0.38f, height * 0.57f)
        )
        drawCircle(
            color = RabbitTextColor,
            radius = width * 0.045f,
            center = Offset(width * 0.62f, height * 0.57f)
        )
        // Eye Sparkles
        drawCircle(
            color = Color.White,
            radius = width * 0.015f,
            center = Offset(width * 0.36f, height * 0.55f)
        )
        drawCircle(
            color = Color.White,
            radius = width * 0.015f,
            center = Offset(width * 0.60f, height * 0.55f)
        )

        // Nose (Cute Reddish Pink Triangle/Oval)
        drawCircle(
            color = RabbitRed,
            radius = width * 0.03f,
            center = Offset(width * 0.5f, height * 0.64f)
        )

        // Tiny Smile / whiskers outline
        drawPath(
            path = Path().apply {
                moveTo(width * 0.46f, height * 0.68f)
                quadraticTo(width * 0.48f, height * 0.71f, width * 0.5f, height * 0.69f)
                quadraticTo(width * 0.52f, height * 0.71f, width * 0.54f, height * 0.68f)
            },
            color = RabbitTextColor,
            style = Stroke(width = 3f)
        )
        
        // Star decorative emblem on cheek
        drawPath(
            path = Path().apply {
                moveTo(width * 0.25f, height * 0.51f)
                lineTo(width * 0.27f, height * 0.54f)
                lineTo(width * 0.30f, height * 0.54f)
                lineTo(width * 0.28f, height * 0.56f)
                lineTo(width * 0.29f, height * 0.59f)
                lineTo(width * 0.25f, height * 0.57f)
                lineTo(width * 0.21f, height * 0.59f)
                lineTo(width * 0.22f, height * 0.56f)
                lineTo(width * 0.20f, height * 0.54f)
                lineTo(width * 0.23f, height * 0.54f)
                close()
            },
            color = RabbitLavender
        )
    }
}


// --- TAB NAVIGATION BAR ---
@Composable
fun OrbitBottomNavBar(currentTab: OrbitTab, onTabSelected: (OrbitTab) -> Unit) {
    NavigationBar(
        containerColor = OrbitCard,
        tonalElevation = 6.dp,
        modifier = Modifier.border(1.dp, OrbitPrimary.copy(alpha = 0.1f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = currentTab == OrbitTab.Tasks,
            onClick = { onTabSelected(OrbitTab.Tasks) },
            icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Tasks") },
            label = { Text("Tasks", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrbitPrimary,
                selectedTextColor = OrbitPrimary,
                indicatorColor = OrbitPrimary.copy(alpha = 0.15f),
                unselectedIconColor = OrbitText.copy(alpha = 0.5f),
                unselectedTextColor = OrbitText.copy(alpha = 0.5f)
            )
        )

        NavigationBarItem(
            selected = currentTab == OrbitTab.Write,
            onClick = { onTabSelected(OrbitTab.Write) },
            icon = { Icon(Icons.Outlined.EditNote, contentDescription = "Write") },
            label = { Text("Write", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrbitPrimary,
                selectedTextColor = OrbitPrimary,
                indicatorColor = OrbitPrimary.copy(alpha = 0.15f),
                unselectedIconColor = OrbitText.copy(alpha = 0.5f),
                unselectedTextColor = OrbitText.copy(alpha = 0.5f)
            )
        )

        NavigationBarItem(
            selected = currentTab == OrbitTab.Calendar,
            onClick = { onTabSelected(OrbitTab.Calendar) },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar") },
            label = { Text("Calendar", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrbitPrimary,
                selectedTextColor = OrbitPrimary,
                indicatorColor = OrbitPrimary.copy(alpha = 0.15f),
                unselectedIconColor = OrbitText.copy(alpha = 0.5f),
                unselectedTextColor = OrbitText.copy(alpha = 0.5f)
            )
        )

        NavigationBarItem(
            selected = currentTab == OrbitTab.Bin,
            onClick = { onTabSelected(OrbitTab.Bin) },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Bin") },
            label = { Text("Bin", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrbitPrimary,
                selectedTextColor = OrbitPrimary,
                indicatorColor = OrbitPrimary.copy(alpha = 0.15f),
                unselectedIconColor = OrbitText.copy(alpha = 0.5f),
                unselectedTextColor = OrbitText.copy(alpha = 0.5f)
            )
        )
    }
}


// --- SCREEN 1: TASKS & BRAIN DUMP TAB ---
@Composable
fun TasksAndDumpScreen(viewModel: OrbitViewModel) {
    val taskTabMode by viewModel.taskTabMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Toggle bar at the top (Tasks vs Brain Dump)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OrbitCard)
                .border(1.dp, OrbitPrimary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (taskTabMode == TaskTabMode.MyList) OrbitPrimary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { viewModel.setTaskTabMode(TaskTabMode.MyList) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Active List",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (taskTabMode == TaskTabMode.MyList) OrbitPrimary else OrbitText
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (taskTabMode == TaskTabMode.BrainDump) OrbitPrimary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { viewModel.setTaskTabMode(TaskTabMode.BrainDump) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Brain Dump",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (taskTabMode == TaskTabMode.BrainDump) OrbitPrimary else OrbitText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = taskTabMode,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState == TaskTabMode.BrainDump) width else -width } togetherWith
                        slideOutHorizontally { width -> if (targetState == TaskTabMode.BrainDump) -width else width }
            },
            label = "tasks_modes"
        ) { mode ->
            when (mode) {
                TaskTabMode.MyList -> ActiveTasksListView(viewModel)
                TaskTabMode.BrainDump -> BrainDumpInputView(viewModel)
            }
        }
    }
}


// --- ACTIVE TASKS VIEW SCREEN ---
@Composable
fun ActiveTasksListView(viewModel: OrbitViewModel) {
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    var showAddTaskDialog by remember { mutableStateFlowOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeTasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = "Empty",
                    tint = OrbitPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your space is entirely clear.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrbitText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add tasks manually or use Brain Dump to untangle thoughts.",
                    fontSize = 12.sp,
                    color = OrbitText.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = activeTasks, key = { it.id }) { task ->
                    TaskRowCard(
                        task = task,
                        onComplete = { viewModel.completeTask(task) },
                        onDelete = { viewModel.softDeleteTask(task) }
                    )
                }
            }
        }

        // FAB to add task manually
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = OrbitAccent,
            contentColor = OrbitText,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 4.dp)
                .testTag("add_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { name, note, date ->
                viewModel.createTask(name, note, date)
                showAddTaskDialog = false
            }
        )
    }
}


// --- SWIPE/DRAG ACTIVE TASK ITEM CARD ---
@Composable
fun TaskRowCard(
    task: TaskEntity,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    // Elegant slide translation on complete/delete
    var slideOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = slideOffset, label = "swipe_offset")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (slideOffset < -150f) {
                            // Completed swiping left
                            onComplete()
                        } else if (slideOffset > 150f) {
                            // Swiped right -> move to Bin
                            onDelete()
                        }
                        slideOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // constrain list sliding boundaries
                        slideOffset = (slideOffset + dragAmount).coerceIn(-300f, 300f)
                    }
                )
            }
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .clip(RoundedCornerShape(16.dp))
            .background(OrbitCard)
            .border(1.dp, OrbitPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("task_item_${task.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(OrbitSecondary.copy(alpha = 0.3f))
                    .clickable { onComplete() }
                    .border(2.dp, OrbitPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // inner empty circle
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OrbitText
                )
                if (!task.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.note,
                        fontSize = 12.sp,
                        color = OrbitText.copy(alpha = 0.6f)
                    )
                }
                if (!task.dueDate.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Due Date",
                            tint = OrbitPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.dueDate,
                            fontSize = 11.sp,
                            color = OrbitPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Trash delete button shortcut
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = OrbitText.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// --- ADD TASK DIALOGUE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String?, date: String?) -> Unit
) {
    var taskName by remember { mutableStateFlowOf("") }
    var taskNote by remember { mutableStateFlowOf("") }
    var selectedDateStr by remember { mutableStateFlowOf<String?>(null) }
    var showDatePicker by remember { mutableStateFlowOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Quick Task",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OrbitText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrbitPrimary,
                        unfocusedBorderColor = OrbitPrimary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_dialog_name_input")
                )

                OutlinedTextField(
                    value = taskNote,
                    onValueChange = { taskNote = it },
                    label = { Text("Optional note...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrbitPrimary,
                        unfocusedBorderColor = OrbitPrimary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showDatePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OrbitSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Pick day",
                        tint = OrbitText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedDateStr != null) "Plan: $selectedDateStr" else "Pin to a calendar date",
                        color = OrbitText,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotBlank()) {
                        onConfirm(taskName, taskNote.ifBlank { null }, selectedDateStr)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrbitPrimary)
            ) {
                Text("Orbit Task", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OrbitText)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        selectedDateStr = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("Select", color = OrbitPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = OrbitText) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


// --- BRAIN DUMP VIEW ---
@Composable
fun BrainDumpInputView(viewModel: OrbitViewModel) {
    var rawTextInput by remember { mutableStateFlowOf("") }
    val isOrganizing by viewModel.isOrganizing.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrbitCard)
            .border(1.dp, OrbitPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Pour Your Mind Out",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = OrbitText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Type any random thoughts, reminders, or items in a raw paragraph. Our AI companion will magically sort them into elegant task cards.",
            fontSize = 12.sp,
            color = OrbitText.copy(alpha = 0.6f),
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = rawTextInput,
            onValueChange = { rawTextInput = it },
            placeholder = {
                Text(
                    "e.g., Buy chemistry books, call Grandma tonight, write daily poetry, and submit math assignments yesterday.",
                    fontSize = 13.sp,
                    color = OrbitText.copy(alpha = 0.4f)
                )
            },
            minLines = 6,
            maxLines = 10,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = OrbitBackground,
                unfocusedContainerColor = OrbitBackground,
                focusedBorderColor = OrbitPrimary,
                unfocusedBorderColor = OrbitSecondary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("brain_dump_text_field")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isOrganizing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = OrbitPrimary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Untangling star calculations...",
                    color = OrbitPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = {
                    if (rawTextInput.isNotBlank()) {
                        viewModel.organizeRawParagraph(rawTextInput)
                        rawTextInput = ""
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrbitPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("brain_dump_organize_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Organize with Magical AI", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// --- SCREEN 2: WRITE TAB (JOURNAL/SCRAPBOOK) ---
@Composable
fun WriteScreen(viewModel: OrbitViewModel) {
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    var showCreateNoteDialog by remember { mutableStateFlowOf(false) }
    var selectedNoteForEdit by remember { mutableStateFlowOf<NoteEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Cosmic Notebook",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OrbitText
                )
                TextButton(onClick = { showCreateNoteDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add Entry", color = OrbitPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "Empty notes",
                        tint = OrbitPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "An unwritten chapter in space.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrbitText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Store thoughts, poems, quotes, and observations here.",
                        fontSize = 12.sp,
                        color = OrbitText.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeNotes) { note ->
                        NoteCard(
                            note = note,
                            onClick = { selectedNoteForEdit = note },
                            onDelete = { viewModel.softDeleteNote(note) }
                        )
                    }
                }
            }
        }

        // On Note click or New Entry, open custom dialog
        if (showCreateNoteDialog) {
            NoteEditDialog(
                onDismiss = { showCreateNoteDialog = false },
                onSave = { title, content, img, links ->
                    viewModel.createNote(title, content, img, links)
                    showCreateNoteDialog = false
                }
            )
        }

        if (selectedNoteForEdit != null) {
            NoteEditDialog(
                existingNote = selectedNoteForEdit,
                onDismiss = { selectedNoteForEdit = null },
                onSave = { title, content, img, links ->
                    selectedNoteForEdit?.let { existing ->
                        viewModel.updateNote(existing.copy(title = title, content = content, imageUrl = img, links = links))
                    }
                    selectedNoteForEdit = null
                }
            )
        }
    }
}


// --- NOTE INDIVIDUAL ITEM CARD ---
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OrbitCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, OrbitPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Note Image Support (either custom pick URL or cozy celestial preset vector drawing)
            if (!note.imageUrl.isNullOrBlank()) {
                if (note.imageUrl.startsWith("content://") || note.imageUrl.startsWith("file://") || note.imageUrl.startsWith("http")) {
                    AsyncImage(
                        model = note.imageUrl,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrbitSecondary.copy(alpha = 0.2f))
                    )
                } else {
                    // Celestial space illustration preset backgrounds based on selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (note.imageUrl) {
                                    "Stellar Nebula" -> OrbitPrimary
                                    "Quiet Constellation" -> OrbitSecondary
                                    "Dreamy Crescent Moon" -> OrbitAccent
                                    else -> RabbitPink
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (note.imageUrl) {
                                "Stellar Nebula" -> Icons.Default.Cloud
                                "Quiet Constellation" -> Icons.Default.StarOutline
                                "Dreamy Crescent Moon" -> Icons.Default.Brightness3
                                else -> Icons.Default.FavoriteBorder
                            },
                            contentDescription = null,
                            tint = OrbitText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = note.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = OrbitText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.content,
                fontSize = 12.sp,
                color = OrbitText.copy(alpha = 0.7f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            if (!note.links.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val linkList = note.links.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                linkList.firstOrNull()?.let { firstLink ->
                    Text(
                        text = "🔗 $firstLink",
                        fontSize = 10.sp,
                        color = OrbitPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (firstLink.startsWith("http")) firstLink else "https://$firstLink"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("NoteCard", "Failed to open link: $firstLink", e)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.createdTime)),
                    fontSize = 9.sp,
                    color = OrbitText.copy(alpha = 0.4f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = RabbitRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


// --- JOURNAL ENTRY AND EDITING DIALOGUE ---
@Composable
fun NoteEditDialog(
    existingNote: NoteEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, img: String?, links: String?) -> Unit
) {
    var title by remember { mutableStateFlowOf(existingNote?.title ?: "") }
    var content by remember { mutableStateFlowOf(existingNote?.content ?: "") }
    var imageUrl by remember { mutableStateFlowOf(existingNote?.imageUrl) }
    var linksInput by remember { mutableStateFlowOf(existingNote?.links ?: "") }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                // Grant persistable permission to URI
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w("NoteEditDialog", "Could not grant persistable path permission: ${e.message}")
            }
            imageUrl = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingNote != null) "Edit Entry" else "New Cosmic Verse",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OrbitText
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrbitPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_edit_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write your thoughts...", fontSize = 12.sp) },
                    minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrbitPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = linksInput,
                    onValueChange = { linksInput = it },
                    label = { Text("Links / URLs (comma separated)...", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrbitPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Image support row
                Text("Vibe Illustration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrbitText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Stellar Nebula", "Quiet Constellation", "Dreamy Crescent Moon").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (imageUrl == preset) OrbitPrimary.copy(alpha = 0.3f) else OrbitSecondary.copy(alpha = 0.1f))
                                .border(
                                    2.dp,
                                    if (imageUrl == preset) OrbitPrimary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { imageUrl = preset }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = OrbitText)
                        }
                    }
                }

                Button(
                    onClick = { photoPickerLauncher.launch(arrayOf("image/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = OrbitSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Photo, contentDescription = "Add Picture", tint = OrbitText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (imageUrl?.startsWith("content://") == true) "Pick Custom Image ✓" else "Choose Picture from Device",
                        color = OrbitText,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(title, content, imageUrl, linksInput.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrbitPrimary)
            ) {
                Text("Confirm Journal", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OrbitText)
            }
        }
    )
}


// --- SCREEN 3: CALENDAR MONTHLY PLANNER ---
@Composable
fun CalendarScreen(viewModel: OrbitViewModel) {
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarTasks by viewModel.calendarTasks.collectAsStateWithLifecycle()
    val usageDates by viewModel.usageDates.collectAsStateWithLifecycle()

    var currentYearMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showAddTaskDialog by remember { mutableStateFlowOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    time = currentYearMonth.time
                    add(Calendar.MONTH, -1)
                }
                currentYearMonth = cal
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Prev Month", tint = OrbitText)
            }

            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            Text(
                text = monthFormat.format(currentYearMonth.time),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OrbitText
            )

            IconButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    time = currentYearMonth.time
                    add(Calendar.MONTH, 1)
                }
                currentYearMonth = cal
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month", tint = OrbitText)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Simple Custom Grid Calendar View
        MonthlyCalendarGrid(
            calendarInstance = currentYearMonth,
            selectedDate = selectedDate,
            activeTasks = activeTasks,
            usageDates = usageDates,
            onDateSelected = { dateString -> viewModel.selectDate(dateString) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Day tasks list header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Plans for $selectedDate",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = OrbitText
            )
            IconButton(
                onClick = { showAddTaskDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add for this day", tint = OrbitPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (calendarTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No specific events or tasks planned for this date.",
                    fontSize = 12.sp,
                    color = OrbitText.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(calendarTasks) { task ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OrbitCard)
                            .border(1.dp, OrbitPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OrbitPrimary,
                                    uncheckedColor = OrbitText.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = OrbitText,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (!task.note.isNullOrBlank()) {
                                    Text(
                                        text = task.note,
                                        fontSize = 11.sp,
                                        color = OrbitText.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.softDeleteTask(task) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RabbitRed.copy(0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { name, note, _ ->
                viewModel.createTask(name, note, selectedDate)
                showAddTaskDialog = false
            }
        )
    }
}


@Composable
fun MonthlyCalendarGrid(
    calendarInstance: Calendar,
    selectedDate: String,
    activeTasks: List<TaskEntity>,
    usageDates: Set<String>,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Month Calculations
    val gridDays = remember(calendarInstance) {
        val days = mutableListOf<Date?>()
        val cal = Calendar.getInstance().apply {
            time = calendarInstance.time
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // Days before month starts (padding)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        for (i in 1 until firstDayOfWeek) {
            days.add(null)
        }

        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..totalDaysInMonth) {
            days.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }

    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(OrbitCard).padding(12.dp)) {
        // Row of weekday headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrbitText.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid listing cal dates
        val rows = gridDays.chunked(7)
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val dateStr = sdf.format(date)
                        val isSelected = dateStr == selectedDate
                        val hasTasks = activeTasks.any { it.dueDate == dateStr }
                        val hasUsedApp = usageDates.contains(dateStr)

                        val calendarCal = Calendar.getInstance().apply { time = date }
                        val dayNum = calendarCal.get(Calendar.DAY_OF_MONTH).toString()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) OrbitPrimary else Color.Transparent)
                                .clickable { onDateSelected(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNum,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else OrbitText
                                    )
                                    if (hasUsedApp) {
                                        Spacer(modifier = Modifier.width(1.dp))
                                        Text(text = "🔥", fontSize = 10.sp)
                                    }
                                }
                                if (hasTasks) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else OrbitAccent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- SCREEN 4: BIN & HISTORY TAB ---
@Composable
fun BinScreen(viewModel: OrbitViewModel) {
    val binTasks by viewModel.binTasks.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()

    var showAboutDialog by remember { mutableStateFlowOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cosmic Bin & Story",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OrbitText
            )

            // Special interactive button to open Annie's about dialog!
            Button(
                onClick = { showAboutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = OrbitSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(34.dp).testTag("about_us_button")
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "About Creator", tint = RabbitRed, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("About Orbit", color = OrbitText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Completed tasks and deleted notes disintegrate slowly. Tasks disappear after 48 hours.",
            fontSize = 12.sp,
            color = OrbitText.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tab views or lists of Bin
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (binTasks.isEmpty() && deletedNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No debris floating in your bin.",
                            fontSize = 13.sp,
                            color = OrbitText.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Recently completed tasks section
            if (binTasks.isNotEmpty()) {
                item {
                    Text("Tasks in Transit", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OrbitPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(binTasks) { task ->
                    val hrsRemaining = if (task.completedTime != null) {
                        val fortyEightHoursMs = 48 * 60 * 60 * 1000L
                        val timePassed = System.currentTimeMillis() - task.completedTime
                        val remainingMs = fortyEightHoursMs - timePassed
                        val remainingHours = remainingMs / (1000 * 60 * 60)
                        if (remainingHours > 0) "$remainingHours hrs" else "Expired"
                    } else "Soon"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(OrbitCard)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (task.isCompleted) OrbitSecondary else RabbitPeach
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.name,
                                    fontSize = 13.sp,
                                    color = OrbitText,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = "Disintegrates in $hrsRemaining",
                                    fontSize = 10.sp,
                                    color = OrbitText.copy(alpha = 0.4f)
                                )
                            }
                            TextButton(onClick = { viewModel.restoreTask(task) }) {
                                Text("Restore", color = OrbitPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.permanentlyDeleteTask(task) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = RabbitRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Deleted notes section
            if (deletedNotes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Deleted Journal Pages", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OrbitPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(deletedNotes) { note ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(OrbitCard)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = OrbitAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, fontSize = 13.sp, color = OrbitText, fontWeight = FontWeight.Bold)
                                Text(note.content, fontSize = 11.sp, color = OrbitText.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { viewModel.restoreNote(note) }) {
                                Text("Restore", color = OrbitPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.permanentlyDeleteNote(note) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = RabbitRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutUsDialog(onDismiss = { showAboutDialog = false })
    }
}


// --- CREATOR LETTER DIALOGUE (APANSHULA ANI'S BIOGRAPHY & LINKEDIN) ---
@Composable
fun AboutUsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val linkedInUrl = "https://www.linkedin.com/in/apanshula-ani-6a624b36a?utm_source=share_via&utm_content=profile&utm_medium=member_android"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(color = OrbitAccent, radius = size.width / 2f)
                }
                Text("About Orbit", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrbitText)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture Placeholder - Elegant warm custom avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(OrbitPrimary, OrbitAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AA",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                    // Decorative tiny moon/star
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(OrbitSecondary)
                            .align(Alignment.BottomEnd)
                            .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Hi, I'm Apanshula Ani.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = OrbitPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = """
Orbit began as something I wished existed.

I wanted a quiet place to keep my thoughts, tasks, notes, and ideas without the pressure of traditional productivity apps. So I decided to build it myself.

Orbit is designed to be simple, gentle, and always there when you need it.

Outside of this project, I enjoy technology, STEM, writing, creativity, and building things that help people.

Thank you for visiting Orbit.
                        """.trimIndent(),
                        fontSize = 12.sp,
                        color = OrbitText.copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Justify
                    )
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(OrbitPrimary.copy(alpha = 0.15f)))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Connect with me at:",
                        fontSize = 12.sp,
                        color = OrbitText.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A66C2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("linkedin_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Custom elegant vector/canvas LinkedIn icon
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "in",
                                    color = Color(0xFF0A66C2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Apanshula Ani on LinkedIn",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = OrbitPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}


// Simple helper for stateflow creation
fun <T> mutableStateFlowOf(value: T): MutableState<T> {
    return mutableStateOf(value)
}

@Composable
fun ConfettiCelebrationOverlay(
    isActive: Boolean,
    onFinished: () -> Unit
) {
    if (!isActive) return

    // Dismiss automatically after 1.8 seconds
    LaunchedEffect(Unit) {
        delay(1800)
        onFinished()
    }

    // Capture multiple floating confetti particles
    val particles = remember {
        List(35) {
            ConfettiParticle(
                emoji = listOf("🎉", "🥳", "✨", "⭐", "🦄", "🌸", "🍬", "🍭", "💖", "⚡").random(),
                startX = (30..70).random() / 100f, // Center-ish horizontal start
                startY = 0.5f,
                angle = (200..340).random().toDouble(), // Upwards spreading angle
                speed = (8..24).random() / 10f,
                scale = (8..18).random() / 10f,
                rotationSpeed = (-15..15).random()
            )
        }
    }

    // Drive an entry and decay transition
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = true }
    val transition = rememberTransition(transitionState, label = "confetti")

    // Animate factor from 0.0 to 1.0
    val progress by transition.animateFloat(
        transitionSpec = { tween(1800, easing = LinearEasing) },
        label = "progress"
    ) { if (it) 1f else 0f }

    val alpha by transition.animateFloat(
        transitionSpec = {
            keyframes {
                1f at 0
                1f at 1200
                0f at 1800
            }
        },
        label = "alpha"
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.15f * alpha))
            .pointerInput(Unit) {}, // Consume clicks so user doesn't accidentally click active elements during celebration
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            // Draw each animated particle
            particles.forEach { particle ->
                val radian = Math.toRadians(particle.angle)
                val distance = progress * heightPx * 0.7f * particle.speed

                val currentX = (particle.startX * widthPx) + (Math.cos(radian) * distance).toFloat()
                val currentY = (particle.startY * heightPx) - (Math.sin(radian) * distance).toFloat() + (progress * progress * 300f * particle.speed)

                val rotation = progress * 360f * particle.rotationSpeed

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                currentX.coerceIn(0f, widthPx).roundToInt(),
                                currentY.coerceIn(0f, heightPx).roundToInt()
                            )
                        }
                        .graphicsLayer(
                            scaleX = particle.scale * progress.coerceIn(0.1f, 1f),
                            scaleY = particle.scale * progress.coerceIn(0.1f, 1f),
                            rotationZ = rotation,
                            alpha = alpha
                        )
                ) {
                    Text(text = particle.emoji, fontSize = 24.sp)
                }
            }

            // Central delightful congratulations popup
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = progress.coerceIn(0.1f, 1f),
                        scaleY = progress.coerceIn(0.1f, 1f),
                        alpha = alpha
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(OrbitCard)
                    .border(2.dp, OrbitAccent, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✨ Delightful! ✨",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = OrbitPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Task completed successfully",
                        fontSize = 12.sp,
                        color = OrbitText.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

data class ConfettiParticle(
    val emoji: String,
    val startX: Float,
    val startY: Float,
    val angle: Double,
    val speed: Float,
    val scale: Float,
    val rotationSpeed: Int
)

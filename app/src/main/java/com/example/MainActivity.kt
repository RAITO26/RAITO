package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.IntentFilter
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.example.ui.theme.MyApplicationTheme
import com.example.data.TeleguardSettings
import com.example.utils.AppHider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private fun Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val app = applicationContext as? SyncApplication
            val repository = app?.repository ?: run {
                val db = com.example.data.TeleguardDatabase.getInstance(applicationContext)
                com.example.data.TeleguardRepository(db.teleguardDao)
            }
            val factory = MainViewModel.Factory(repository)
            val viewModel: MainViewModel = viewModel(factory = factory)
            
            val settingsState by viewModel.settings.collectAsState()
            
            // Periodically monitor background processes state
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.forceEnableComponents(context)
                while(true) {
                    viewModel.checkServiceStatus(context)
                    delay(3000)
                }
            }

            val currentLang = AppLanguage.fromCode(settingsState.languageCode)
            MyApplicationTheme(
                darkTheme = settingsState.isDarkTheme,
                lang = currentLang
            ) {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    AnimeSplashScreen(
                        lang = currentLang,
                        onAnimationFinished = { showSplash = false }
                    )
                } else {
                    RaitoMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// ----------------------------------------------------
// Premium 5-Second Custom Anime/Cybertech Opening Splash
// ----------------------------------------------------
@Composable
fun AnimeCharacterAndHologram(
    modifier: Modifier = Modifier,
    breathingGlow: Float,
    rotationAngle: Float,
    eyeYScale: Float
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2

        scale(scale = 1.30f, pivot = Offset(cx, cy)) {
            // --- PROC-DRAW ANIME MASTERMIND "RAITO" ---
        // 1. Neck definition (Scaled up and centered)
        val neckPath = Path().apply {
            moveTo(cx - 30f, cy + 50f)
            lineTo(cx + 30f, cy + 50f)
            lineTo(cx + 35f, cy + 110f)
            lineTo(cx - 35f, cy + 110f)
            close()
        }
        drawPath(neckPath, Color(0xFFFFD1A9)) // Skin tone base
        // Neck shadow
        val neckShadowPath = Path().apply {
            moveTo(cx - 30f, cy + 50f)
            lineTo(cx + 30f, cy + 50f)
            lineTo(cx + 10f, cy + 75f)
            lineTo(cx - 10f, cy + 75f)
            close()
        }
        drawPath(neckShadowPath, Color(0xFFE5A885))

        // 2. High-collared cyber mastermind coat
        val collarPath = Path().apply {
            moveTo(cx - 70f, cy + 80f)
            lineTo(cx + 70f, cy + 80f)
            lineTo(cx + 90f, cy + 140f)
            lineTo(cx - 90f, cy + 140f)
            close()
        }
        drawPath(collarPath, Color(0xFF131117))
        drawPath(collarPath, Color(0xFFA855F7).copy(alpha = 0.6f), style = Stroke(width = 4f))

        // Left and right shoulders coat detail
        val innerShirtPath = Path().apply {
            moveTo(cx - 20f, cy + 80f)
            lineTo(cx + 20f, cy + 80f)
            lineTo(cx, cy + 110f)
            close()
        }
        drawPath(innerShirtPath, Color(0xFF06B6D4)) // Neon cyan tie/inner shirt accent

        // 3. V-shaped sleek anime face definition (scaled up and prominent)
        val facePath = Path().apply {
            moveTo(cx - 75f, cy - 50f)
            cubicTo(cx - 75f, cy, cx - 55f, cy + 30f, cx, cy + 65f)
            cubicTo(cx + 55f, cy + 30f, cx + 75f, cy, cx + 75f, cy - 50f)
            close()
        }
        drawPath(facePath, Color(0xFFFFE0BD)) // Lighter skin highlight
        
        // Face cheeks blush/shading
        drawPath(
            facePath,
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFFFDA4AF).copy(alpha = 0.15f)),
                startY = cy - 30f,
                endY = cy + 40f
            )
        )

        // Ears
        val leftEar = Path().apply {
            moveTo(cx - 75f, cy - 30f)
            cubicTo(cx - 90f, cy - 25f, cx - 90f, cy, cx - 74f, cy + 10f)
            close()
        }
        drawPath(leftEar, Color(0xFFFFD1A9))
        val rightEar = Path().apply {
            moveTo(cx + 75f, cy - 30f)
            cubicTo(cx + 90f, cy - 25f, cx + 90f, cy, cx + 74f, cy + 10f)
            close()
        }
        drawPath(rightEar, Color(0xFFFFD1A9))

        // Cybernetic Cheek circuit tattoo (Left cheek)
        val circuitPath = Path().apply {
            moveTo(cx - 50f, cy + 10f)
            lineTo(cx - 35f, cy + 20f)
            lineTo(cx - 35f, cy + 35f)
        }
        drawPath(circuitPath, Color(0xFF06B6D4).copy(alpha = 0.8f * breathingGlow), style = Stroke(width = 2.5f))
        drawCircle(Color(0xFF22D3EE).copy(alpha = breathingGlow), 4f, Offset(cx - 35f, cy + 35f))

        // Delicate nose & cyberpunk mouth lines
        // Nose pointer
        val nosePath = Path().apply {
            moveTo(cx - 3f, cy + 8f)
            lineTo(cx, cy + 11f)
            lineTo(cx + 4f, cy + 8f)
        }
        drawPath(nosePath, Color(0xFFE5A885), style = Stroke(width = 2f))

        // Cyberpunk slight grin mouth
        val mouthPath = Path().apply {
            moveTo(cx - 18f, cy + 30f)
            cubicTo(cx - 8f, cy + 34f, cx + 8f, cy + 34f, cx + 18f, cy + 30f)
        }
        drawPath(mouthPath, Color(0xFF1E1B24), style = Stroke(width = 3f))
        // Neon lip line overlay
        drawPath(mouthPath, Color(0xFFA855F7).copy(alpha = 0.4f * breathingGlow), style = Stroke(width = 1f))

        // 4. Heterochromia Eyes (Sun/Moon cybernetic theme) with Eye-Blinking Core
        val eyeWidth = 28f
        val eyeHeightMax = 18f
        val animatedEyeHeight = eyeHeightMax * eyeYScale

        // Screen-Left (Right Eye): Sun-Theme golden yellow neon eye
        val rightEyeCenter = Offset(cx - 28f, cy - 8f)
        if (eyeYScale > 0.2f) {
            // EYEBALL OUTER RIM
            drawOval(
                color = Color(0xFFFFB000),
                topLeft = Offset(rightEyeCenter.x - eyeWidth / 2, rightEyeCenter.y - animatedEyeHeight / 2),
                size = Size(eyeWidth, animatedEyeHeight),
                style = Stroke(width = 2.2f)
            )
            // PUPIL / IRIS
            drawCircle(
                color = Color(0xFFFBBF24), // Sun Gold pupil
                radius = 8f * eyeYScale,
                center = rightEyeCenter
            )
            // Cybernetic internal target lines
            drawLine(
                color = Color(0xFFFFF5C2).copy(alpha = breathingGlow),
                start = Offset(rightEyeCenter.x - 12f, rightEyeCenter.y),
                end = Offset(rightEyeCenter.x + 12f, rightEyeCenter.y),
                strokeWidth = 1.2f
            )
            // GLOW
            drawCircle(
                color = Color(0xFFFFD166).copy(alpha = 0.5f * breathingGlow),
                radius = 16f,
                center = rightEyeCenter
            )
        } else {
            // CLOSED BLINKING EYE LINE
            val closedEyePath = Path().apply {
                moveTo(rightEyeCenter.x - eyeWidth / 2 - 2f, rightEyeCenter.y)
                quadraticTo(rightEyeCenter.x, rightEyeCenter.y + 4f, rightEyeCenter.x + eyeWidth / 2 + 2f, rightEyeCenter.y)
            }
            drawPath(closedEyePath, Color(0xFF0F172A), style = Stroke(width = 3.5f))
        }

        // Screen-Right (Left Eye): Moon-Theme cyan/blue neon eye
        val leftEyeCenter = Offset(cx + 28f, cy - 8f)
        if (eyeYScale > 0.2f) {
            // EYEBALL OUTER RIM
            drawOval(
                color = Color(0xFF00ADB5),
                topLeft = Offset(leftEyeCenter.x - eyeWidth / 2, leftEyeCenter.y - animatedEyeHeight / 2),
                size = Size(eyeWidth, animatedEyeHeight),
                style = Stroke(width = 2.2f)
            )
            // PUPIL / IRIS
            drawCircle(
                color = Color(0xFF06B6D4), // Moon Cyan pupil
                radius = 8f * eyeYScale,
                center = leftEyeCenter
            )
            // Lunar orbit internal indicator
            drawCircle(
                color = Color(0xFFE2F9FF).copy(alpha = breathingGlow),
                radius = 3.5f,
                center = Offset(leftEyeCenter.x + 4f, leftEyeCenter.y - 3f)
            )
            // GLOW
            drawCircle(
                color = Color(0xFF67E8F9).copy(alpha = 0.5f * breathingGlow),
                radius = 16f,
                center = leftEyeCenter
            )
        } else {
            // CLOSED BLINKING EYE LINE
            val closedEyePath = Path().apply {
                moveTo(leftEyeCenter.x - eyeWidth / 2 - 2f, leftEyeCenter.y)
                quadraticTo(leftEyeCenter.x, leftEyeCenter.y + 4f, leftEyeCenter.x + eyeWidth / 2 + 2f, leftEyeCenter.y)
            }
            drawPath(closedEyePath, Color(0xFF0F172A), style = Stroke(width = 3.5f))
        }

        // Eyebrows (Slanted, expressive, high-definition)
        val eyebrowYOffset = if (eyeYScale > 0.2f) 0f else 3f
        val leftEyebrow = Path().apply {
            moveTo(cx - 45f, cy - 22f + eyebrowYOffset)
            quadraticTo(cx - 30f, cy - 25f + eyebrowYOffset, cx - 12f, cy - 18f + eyebrowYOffset)
        }
        drawPath(leftEyebrow, Color(0xFF374151), style = Stroke(width = 3.5f))

        val rightEyebrow = Path().apply {
            moveTo(cx + 12f, cy - 18f + eyebrowYOffset)
            quadraticTo(cx + 30f, cy - 25f + eyebrowYOffset, cx + 45f, cy - 22f + eyebrowYOffset)
        }
        drawPath(rightEyebrow, Color(0xFF374151), style = Stroke(width = 3.5f))

        // Subtle tech eyepiece over right eye (cybernetic tactical mastermind glass HUD)
        drawLine(
            color = Color(0xFF06B6D4).copy(alpha = 0.7f),
            start = Offset(cx - 44f, cy - 8f),
            end = Offset(cx - 10f, cy - 8f),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF06B6D4).copy(alpha = 0.7f),
            start = Offset(cx - 44f, cy - 8f),
            end = Offset(cx - 52f, cy - 40f),
            strokeWidth = 1.5f
        )

        // 5. Massive detailed flowing platinum silver hair locks
        // Layer 1: Shadow Hair (Deep violet-indigo shadows for elite dimensional hair)
        val shadowHairPath = Path().apply {
            moveTo(cx - 82f, cy - 50f)
            cubicTo(cx - 95f, cy - 120f, cx + 95f, cy - 120f, cx + 82f, cy - 50f)
            lineTo(cx + 60f, cy + 15f)
            lineTo(cx + 42f, cy - 20f)
            lineTo(cx + 20f, cy + 12f)
            lineTo(cx + 5f, cy - 30f)
            lineTo(cx - 15f, cy + 15f)
            lineTo(cx - 35f, cy - 30f)
            lineTo(cx - 55f, cy + 10f)
            close()
        }
        drawPath(shadowHairPath, Color(0xFF4338CA).copy(alpha = 0.45f)) // Deep indigo hair tint

        // Layer 2: Core Platinum Silver Hair Locks overlapping perfectly over face
        val hairColor = Color(0xFFFAFAFA) // Premium silver-white
        val hairOutline = Color(0xFFCBD5E1)

        val hairPath1 = Path().apply {
            // Head crown
            moveTo(cx - 80f, cy - 45f)
            cubicTo(cx - 85f, cy - 125f, cx + 85f, cy - 125f, cx + 80f, cy - 45f)
            // Left flank strand
            lineTo(cx + 70f, cy + 10f)
            lineTo(cx + 55f, cy - 15f)
            // Right-cheek lock
            lineTo(cx + 40f, cy + 25f)
            lineTo(cx + 34f, cy - 10f)
            // Deep forehead bangs hanging elegantly between eyes
            lineTo(cx + 12f, cy + 28f) // Deep bang lock right
            lineTo(cx + 3f, cy - 20f)
            lineTo(cx - 8f, cy + 34f)  // Center lock hangs lowest over nose
            lineTo(cx - 15f, cy - 15f)
            lineTo(cx - 35f, cy + 20f)  // Left lock
            lineTo(cx - 42f, cy - 12f)
            lineTo(cx - 68f, cy + 5f)
            close()
        }
        drawPath(hairPath1, hairColor)
        drawPath(hairPath1, hairOutline.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

        // Layer 3: Dynamic flying hair highlights
        val highlightBangs = Path().apply {
            moveTo(cx - 5f, cy - 60f)
            lineTo(cx - 12f, cy + 5f)
            lineTo(cx - 20f, cy - 45f)
            moveTo(cx + 15f, cy - 55f)
            lineTo(cx + 8f, cy + 10f)
            lineTo(cx, cy - 45f)
        }
        drawPath(highlightBangs, Color.White, style = Stroke(width = 2.5f))
        } // end scale block
    }
}

@Composable
fun AnimeSplashScreen(
    lang: AppLanguage,
    onAnimationFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    
    // Snappy and smooth breathing size animation
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Fast and smooth fade-in / fade-out sequence
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1400) // Beautiful 1.4 seconds presentation (snappy and super responsive)
        visible = false
        delay(300)  // Wait for fadeOut animation to finish
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D0E)), // Solid, clean premium dark background
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.93f, animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Circular Raito Avatar with subtle breathing size and a beautiful neon cyan/purple sweep gradient border
                Image(
                    painter = painterResource(id = R.drawable.raito_launcher_asset),
                    contentDescription = "RAITO",
                    modifier = Modifier
                        .size(240.dp)
                        .scale(scaleAnim)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF8E24AA), Color(0xFF00E5FF))
                            ),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // App name styling customized uniquely for each language with high-quality fonts and spacing
                when (lang) {
                    AppLanguage.PERSIAN -> {
                        Text(
                            text = "رایتو",
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = Color(0xFF00E5FF), // Cyber cyan
                            letterSpacing = 0.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "RAITO SECURE SYSTEM",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    AppLanguage.ENGLISH -> {
                        Text(
                            text = "RAITO",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 44.sp,
                            color = Color.White,
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "INTELLIGENT SECURE CORE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF8E24AA).copy(alpha = 0.85f),
                            letterSpacing = 3.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    AppLanguage.RUSSIAN -> {
                        Text(
                            text = "RAITO",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            color = Color(0xFFEF4444), // Accent Red
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ИНТЕЛЛЕКТУАЛЬНЫЙ КОМПЛЕКС",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    AppLanguage.CHINESE -> {
                        Text(
                            text = "RAITO",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp,
                            color = Color(0xFFF59E0B), // Warm amber
                            letterSpacing = 5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "智能安全控制系统",
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Subtle glowing progress light
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF00E5FF), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// ----------------------------------------------------
// Premium Dual-Tier Dashboard Screen
// ----------------------------------------------------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RaitoMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val logList by viewModel.logs.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()

    val currentLang = AppLanguage.fromCode(settingsState.languageCode)

    // Override the general Layout Direction dynamically based on language choice!
    val layoutDirection = currentLang.direction

    // Track active navigation tab
    var activeTab by remember { mutableStateOf(0) } // 0: CONTROL CORE, 1: TELEMETRY LOGS, 2: POLICIES

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                RaitoTopBar(
                    lang = currentLang,
                    isDark = settingsState.isDarkTheme,
                    onLangChanged = { viewModel.updateLanguage(it.code) },
                    onThemeToggle = { viewModel.toggleTheme(it) }
                )
            },
            bottomBar = {
                RaitoBottomBar(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it },
                    lang = currentLang
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        // Base under-layer: subtle gradient for a premium dual-tier look
                        Brush.radialGradient(
                            colors = if (settingsState.isDarkTheme) {
                                listOf(Color(0xFFD0BCFF).copy(alpha = 0.08f), Color.Transparent)
                            } else {
                                listOf(Color(0xFF6750A4).copy(alpha = 0.05f), Color.Transparent)
                            },
                            center = Offset(400f, 400f),
                            radius = 1200f
                        )
                    )
            ) {
                // Main dynamic visual transition between tabs
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            initialOffsetX = { if (activeTab > initialState) it else -it }
                        ) + fadeIn(animationSpec = tween(220)) with
                                slideOutHorizontally(
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    targetOffsetX = { if (activeTab > initialState) -it else it }
                                ) + fadeOut(animationSpec = tween(220))
                    },
                    label = "main_navigation"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> ControlCoreTab(
                            viewModel = viewModel,
                            settings = settingsState,
                            isRunning = isRunning,
                            lang = currentLang
                        )
                        1 -> TelemetryLogsTab(
                            viewModel = viewModel,
                            logs = logList,
                            lang = currentLang
                        )
                        2 -> AppPoliciesTab(
                            lang = currentLang
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Custom Top Menu Bar Layout
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaitoTopBar(
    lang: AppLanguage,
    isDark: Boolean,
    onLangChanged: (AppLanguage) -> Unit,
    onThemeToggle: (Boolean) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = Translations.get("app_title", lang),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = Translations.get("caption_bar", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        },
        actions = {
            // EXQUISITE CUSTOM CANVAS SUN & MOON THEME TOGGLE
            CustomSunMoonToggle(
                isDark = isDark,
                onToggle = onThemeToggle,
                modifier = Modifier
                    .size(42.dp)
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // GLOBE ICON REGISTRATION DROP DOWN
            Box {
                IconButton(onClick = { expandedMenu = !expandedMenu }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language Selection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AppLanguage.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = option.flag, fontSize = 18.sp)
                                    Text(
                                        text = option.label,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (lang == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                onLangChanged(option)
                                expandedMenu = false
                            },
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}

// ----------------------------------------------------
// Custom Sun/Moon Canvas Component (Legendary Accent)
// ----------------------------------------------------
@Composable
fun CustomSunMoonToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Elastic spring animation for the background/rotation progression
    val animProgress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f, // Elastic bouncy spring
            stiffness = Spring.StiffnessLow
        ),
        label = "mode_morph_spring"
    )

    // Fluid-shift gradient background colors
    val containerBgStart by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1E1B4B) else Color(0xFFFEF3C7), // Royal Navy vs Pastel Gold Cream
        animationSpec = tween(700),
        label = "bg_start"
    )
    val containerBgEnd by animateColorAsState(
        targetValue = if (isDark) Color(0xFF311042) else Color(0xFFFDE047), // Deep Purple vs Warm Solar Amber
        animationSpec = tween(700),
        label = "bg_end"
    )

    // Stationery centered circle layout (touch target size 48dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                Brush.linearGradient(listOf(containerBgStart, containerBgEnd)),
                CircleShape
            )
            .border(
                1.5.dp,
                if (isDark) Color(0xFFA855F7).copy(alpha = 0.5f) else Color(0xFFEAB308).copy(alpha = 0.6f),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggle(!isDark) },
        contentAlignment = Alignment.Center
    ) {
        // Draw the custom morphing shapes on a centered canvas of size 28.dp
        Canvas(modifier = Modifier.size(28.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width * 0.42f

            // Apply 360-degree rotation based on progression
            rotate(degrees = animProgress * 360f, pivot = Offset(cx, cy)) {
                
                // --- LIGHT MODE (SUN - PREMIUM GOLD DETAILS) ---
                if (animProgress < 0.99f) {
                    val sunAlpha = 1f - animProgress
                    
                    // Outer luxury golden corona glow
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = 0.35f * sunAlpha),
                        radius = radius * 1.3f,
                        center = Offset(cx, cy)
                    )

                    // Draw sleek radiant spikes
                    for (angle in 0 until 360 step 45) {
                        rotate(degrees = angle.toFloat(), pivot = Offset(cx, cy)) {
                            val rayPath = Path().apply {
                                moveTo(cx - 2.5f, cy - radius * 1.55f)
                                lineTo(cx + 2.5f, cy - radius * 1.55f)
                                lineTo(cx, cy - radius * 1.05f)
                                close()
                            }
                            drawPath(path = rayPath, color = Color(0xFFF59E0B).copy(alpha = sunAlpha)) // Intense amber
                        }
                    }
                    
                    // Core magnificent sun body
                    drawCircle(
                        color = Color(0xFFEA580C).copy(alpha = sunAlpha), // Sunset orange core
                        radius = radius * 0.85f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = sunAlpha), // Shimmering Gold inner body
                        radius = radius * 0.6f,
                        center = Offset(cx, cy)
                    )
                }

                // --- DARK MODE (MOON - PLATINUM CRESCENT & GOLD STARS) ---
                if (animProgress > 0.01f) {
                    val moonAlpha = animProgress

                    // 3 Tiny glittering luxury stellar elements in empty background spaces
                    drawCircle(Color(0xFFFCD34D).copy(alpha = 0.9f * moonAlpha), 1.8f, Offset(cx - radius * 0.9f, cy - radius * 0.6f))
                    drawCircle(Color(0xFFFEF08A).copy(alpha = 0.8f * moonAlpha), 1.2f, Offset(cx + radius * 0.8f, cy - radius * 0.9f))
                    drawCircle(Color(0xFFFFD700).copy(alpha = 0.95f * moonAlpha), 1.5f, Offset(cx + radius * 1.0f, cy + radius * 0.5f))
                    
                    // Draw crescent moon using cut-out subtract logic
                    val moonPath = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius))
                    }
                    
                    // Cut out offset is stationary when fully dark
                    val cutoutOffset = radius * 0.65f
                    val cutoutPath = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(
                            cx - radius + cutoutOffset, 
                            cy - radius - cutoutOffset * 0.25f, 
                            cx + radius + cutoutOffset, 
                            cy + radius
                        ))
                    }

                    val crescentPath = Path.combine(
                        operation = androidx.compose.ui.graphics.PathOperation.Difference,
                        path1 = moonPath,
                        path2 = cutoutPath
                    )

                    // Draw background crescent glow
                    drawPath(
                        path = crescentPath,
                        color = Color(0xFFA5B4FC).copy(alpha = 0.3f * moonAlpha)
                    )

                    // Draw foreground metallic platinum celestial body
                    drawPath(
                        path = crescentPath,
                        color = Color(0xFFF8FAFC).copy(alpha = moonAlpha)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 0: CONTROL CORE (Dual-Tier Dashboard Area)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCoreTab(
    viewModel: MainViewModel,
    settings: TeleguardSettings,
    isRunning: Boolean,
    lang: AppLanguage
) {
    val context = LocalContext.current
    var botTokenInput by remember(settings.botToken) { mutableStateOf(settings.botToken) }
    var chatIdInput by remember(settings.chatId) { mutableStateOf(settings.chatId) }

    // Intercept permissions state
    var hasNotifPermission by remember { mutableStateOf(false) }
    var hasSmsPermission by remember { mutableStateOf(false) }
    var hasCallsPermission by remember { mutableStateOf(false) }
    var hasContactsPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var hasBatteryExemption by remember { mutableStateOf(false) }

    val currentBatteryText = remember(context) {
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val statusVal = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
            val status = when (statusVal) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Idle"
                else -> "Active"
            }
            "$pct% ($status)"
        } catch (e: Exception) {
            "85% Active"
        }
    }

    val currentStorageText = remember {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val freeBlocks = stat.availableBlocksLong
            val freeBytes = freeBlocks * blockSize
            val gb = freeBytes / (1024.0 * 1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.1f GB Free", gb)
        } catch (e: Exception) {
            "12.4 GB Free"
        }
    }

    val currentNetworkText = remember(context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNet = cm.activeNetwork
                    val caps = cm.getNetworkCapabilities(activeNet)
                    if (caps != null) {
                        when {
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                            else -> "Online"
                        }
                    } else {
                        "Offline"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val netInfo = cm.activeNetworkInfo
                    if (netInfo?.isConnected == true) netInfo.typeName else "Offline"
                }
            } else {
                "Online"
            }
        } catch (e: Exception) {
            "Online"
        }
    }

    val checkPermissions = {
        // Notification channel listener check
        val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        hasNotifPermission = listeners != null && listeners.contains(context.packageName)

        // Read/Receive SMS Check
        hasSmsPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        // Calls Check
        hasCallsPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        // Contacts Check
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        // Storage Check
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Battery Whitelisting check
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        hasBatteryExemption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermission = results[android.Manifest.permission.RECEIVE_SMS] == true &&
                results[android.Manifest.permission.READ_SMS] == true
    }

    val callsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallsPermission = isGranted
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // Scrollable Dashboard Content
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // ----------------- TIER 1: CORE SERVICE STATUS HERO PANEL -----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(12.dp, RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .shadow(
                                        if (isRunning) 6.dp else 0.dp,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (isRunning) "Foreground Service Active" else "Foreground Service Inactive",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (isRunning) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STICKY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inner Grid for Connected & Forwarding Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bot Status Box
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "BOT STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRunning && settings.botToken.isNotEmpty()) "Connected" else "Disconnected",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Forwarding Box
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "FORWARDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRunning) "Enabled" else "Disabled",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pulsating state circle
                    ServiceBreathingRing(isRunning = isRunning)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isRunning) {
                            Translations.get("status_running", lang)
                        } else {
                            Translations.get("status_stopped", lang)
                        },
                        color = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (botTokenInput.isEmpty()) {
                                    Toast.makeText(context, "Please configure bot token parameter first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.startRaitoService(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = if (settings.isDarkTheme) Color(0xFF381E72) else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isRunning
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translations.get("btn_start", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.stopRaitoService(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            enabled = isRunning
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translations.get("btn_stop", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.killApplication(context) {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    activity.finishAffinity()
                                    activity.finishAndRemoveTask()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = "Force Kill")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "💥 خروج کامل و توقف اجباری رایتو" else "💥 Complete Exit & Force Kill",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "💡 آموزش و راهنمای توقف اجباری:" else "💡 Force Close Educational Guide:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    "چه زمانی از این دکمه استفاده کنیم؟\n" +
                                    "این دکمه را هنگامی بفشارید که می‌خواهید فرآیند پایش پس‌زمینه و سرویس رایتو را به صورت ۱۰۰٪ خاموش و بی‌اثر کنید تا رم، باتری یا CPU دستگاه شما کاملاً آزاد شود و هیچگونه پیامک یا اعلانی پایش نگردد.\n\n" +
                                    "تفاوت آن با توقف معمولی چیست؟\n" +
                                    "در توقف معمولی، سیستم ممکن است سرویس پس‌زمینه را مجدداً زنده کند؛ اما با لمس این دکمه، برنامه وارد خواب زمستانی عمیق (Complete Sleep) و توقف اجباری واقعی می‌شود.\n\n" +
                                    "چگونه دوباره برنامه را روشن کنیم؟\n" +
                                    "کافی است دوباره به صفحه اصلی گوشی آمده و روی آیکون برنامه رایتو ضربه بزنید تا همه سرویس‌ها بلافاصله به صورت ایمن راه‌اندازی و فعال شوند."
                                } else {
                                    "When to use this action?\n" +
                                    "Trigger this command when you want to put RAITO into deep sleep mode, freeing up 100% of RAM, CPU, and device battery. No SMS or notification forwarding will occur after this.\n\n" +
                                    "How to boot up again?\n" +
                                    "Simply open the app again manually from the settings or app launcher to revive all monitoring services instantly."
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // ----------------- TIER 1.5: GOOGLE PLAY PROTECT WARNING REASSURANCE CARD -----------------
        item {
            val isDark = settings.isDarkTheme
            val bannerBg = if (isDark) androidx.compose.ui.graphics.Color(0xFF1B2E1C) else androidx.compose.ui.graphics.Color(0xFFE8F5E9)
            val bannerBorder = if (isDark) androidx.compose.ui.graphics.Color(0xFF2E7D32).copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color(0xFF81C784).copy(alpha = 0.6f)
            val bannerTitleTint = if (isDark) androidx.compose.ui.graphics.Color(0xFF81C784) else androidx.compose.ui.graphics.Color(0xFF2E7D32)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                border = BorderStroke(1.5.dp, bannerBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Shield,
                                contentDescription = null,
                                tint = bannerTitleTint
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "🛡️ امنیت رایتو و خطای سپر ایمنی (Play Protect)" else "🛡️ RAITO Privacy & Play Protect Warning",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = bannerTitleTint
                            )
                        }
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "اگر هنگام نصب یا اجرا با هشدار «سپر ایمنی گوگل پلی (Google Play Protect)» یا «برنامه ناشناخته یا مخرب مسدود شد» مواجه شده‌اید، کاملاً طبیعی بوده و جای هیچ نگرانی نیست! رایتو به هیچ عنوان بدافزار نیست و امنیت شما تضمین شده است.\n\n" +
                                    "❓ چرا این هشدار دریافت می‌شود؟\n" +
                                    "به دلیل اینکه رایتو یک ابزار مدیریت و پایش از راه دور مقتدر و اختصاصی (پایش پیامک‌ها و اعلان‌های شخصی و مدیریت فایل‌ها) است و در فروشگاه عمومی گوگل پلی استور ثبت نشده است، سپر ایمنی سیستم‌عامل اندروید به صورت پیش‌فرض به درخواست‌های دسترسی آن حساسیت نشان داده و سد امنیتی ایجاد می‌کند. اطلاعات شما فقط و فقط به چت خصوصی تلگرام خودتان فرستاده می‌شود و هیچ سرور واسطی وجود ندارد.\n\n" +
                                    "🔧 نحوه نصب موفق و بدون دغدغه:\n" +
                                    "۱. هنگام نصب و مشاهده پاپ‌آپ سپر ایمنی، روی گزینه «جزئیات بیشتر» (More details) ضربه بزنید.\n" +
                                    "۲. سپس گزینه «در هر صورت نصب شود» (Install anyway) را انتخاب کنید تا با موفقیت فعال‌سازی نهایی شود.\n" +
                                    "۳. برای عدم دریافت این هشدار، می‌توانید وارد برنامه Play Store شده، از منوی کاربری خود بخش Play Protect را باز کرده و از چرخ‌دنده بالا سپر ایمنی را به صورت موقت خاموش کنید."
                        } else {
                            "If you encounter a 'Blocked by Play Protect' or 'Unrecognized application' popup during installation, this is completely normal and expected. RAITO is NOT malware and your privacy is fully locked and secure.\n\n" +
                                    "❓ Why this warning?\n" +
                                    "Because RAITO employs active background syncing and custom notifications reading to send directly to your Telegram, and is not distributed publicly via Google Play Store, Google Protect flags its package automatically. Your communication data is secure and serverless.\n\n" +
                                    "🔧 How to bypass & install:\n" +
                                    "1. Click 'More details' or details expansion accordion inside the popup.\n" +
                                    "2. Select 'Install anyway' to complete the installation of the service.\n" +
                                    "3. Alternatively, you can disable Google Play Protect scanning inside Play Store settings."
                        },
                        fontSize = 11.5.sp,
                        color = if (isDark) androidx.compose.ui.graphics.Color(0xFFE0E0E0) else androidx.compose.ui.graphics.Color(0xFF2E3B2E),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // ----------------- TIER 2: TELEGRAM BOT INTEGRATION CONFIG CARD -----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(6.dp, RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "⚙️ Parameters Catalog Configuration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Token input
                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        label = { Text(Translations.get("bot_token_label", lang)) },
                        placeholder = { Text(Translations.get("bot_token_placeholder", lang)) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Chat ID input
                    OutlinedTextField(
                        value = chatIdInput,
                        onValueChange = { chatIdInput = it },
                        label = { Text(Translations.get("chat_id_label", lang)) },
                        placeholder = { Text(Translations.get("chat_id_placeholder", lang)) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Chat ID interactive tooltip info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = Translations.get("chat_id_tip", lang),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Start
                        )
                    }

                    // Save Trigger Button
                    Button(
                        onClick = {
                            viewModel.saveConfig(botTokenInput, chatIdInput, context)
                            Toast.makeText(context, Translations.get("config_saved", lang), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = if (settings.isDarkTheme) Color(0xFF381E72) else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save Parameters")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Credentials",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ----------------- TIER 3: INDIVIDUAL SYSTEM PERMISSIONS MODULES -----------------

        // Refresh permissions checker card (Request 3)
        item {
            val isDark = settings.isDarkTheme
            val cardBg = if (isDark) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color(0xFFF3E8FF)
            val cardBorder = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color(0xFFE2D5F5)
            val titleColor = if (isDark) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF4A148C)
            val descColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else androidx.compose.ui.graphics.Color(0xFF4A148C).copy(alpha = 0.8f)
            val refreshIconBg = if (isDark) MaterialTheme.colorScheme.background else androidx.compose.ui.graphics.Color.White
            val refreshIconTint = if (isDark) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF6750A4)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clickable {
                        checkPermissions()
                        android.widget.Toast.makeText(
                            context,
                            if (lang == AppLanguage.PERSIAN) "🔄 وضعیت تمامی دسترسی‌ها با موفقیت رفرش و بروز شد!" else "🔄 Permission states re-scanned and synchronized!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.5.dp, cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(refreshIconBg, CircleShape)
                            .border(1.dp, cardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = refreshIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "🔄 بررسی مجدد و رفرش وضعیت دسترسی‌ها" else "🔄 Re-check & Refresh Permissions",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = titleColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "برای اسکن مجدد وضعیت دسترسی‌هایی که به برنامه داده‌اید ضربه بزنید" else "Tap here to instantly refresh and scan granted permission details",
                            fontSize = 10.5.sp,
                            color = descColor,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        
        // 1. Notification Sync Access Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "🔔 دسترسی همگام‌سازی اعلان‌ها" else "🔔 Notification Listener Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (hasNotifPermission) {
                                if (lang == AppLanguage.PERSIAN) "✅ فعال شده" else "✅ ACTIVE"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "❌ غیرفعال" else "❌ DISABLED"
                            },
                            color = if (hasNotifPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "این دسترسی به رایتو امکان می‌دهد تا اعلان‌های دریافتی از پیام‌رسان‌ها (مانند تلگرام، واتس‌اپ و بقیه برنامه‌ها) را مستقیماً شنود کرده و به ربات تلگرام شخصی شما همگام‌سازی کند."
                        } else {
                            "Allows RAITO to read and dispatch notification alerts from active messaging channels straight to your Telegram bot."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    if (!hasNotifPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    "🔧 راهنمای فعال‌سازی گام‌به‌گام:\n" +
                                    "۱. روی دکمه «فعال‌سازی دسترسی اعلان» در زیر کلیک کنید.\n" +
                                    "۲. در صفحه باز شده، برنامه «رایتو (RAITO)» را در لیست پیدا کنید.\n" +
                                    "۳. سوییچ دسترسی آن را فعال کرده و در پنجره هشدار گزینه Allow یا تایید را بزنید تا دسترسی برقرار شود."
                                } else {
                                    "🔧 How to enable:\n" +
                                    "1. Press the button below.\n" +
                                    "2. Find 'RAITO' in the system settings screen.\n" +
                                    "3. Turn on the switch and press Allow to authorize notification sync."
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            intentToNotificationAccess(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasNotifPermission) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasNotifPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                if (hasNotifPermission) "تنظیم مجدد دسترسی اعلان" else "فعال‌سازی دسترسی اعلان"
                            } else {
                                "Configure Notification Sync"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. SMS Interception Access Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "✉️ دسترسی پایش پیامک‌ها" else "✉️ SMS Recipient & Sync Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (hasSmsPermission) {
                                if (lang == AppLanguage.PERSIAN) "✅ فعال شده" else "✅ ACTIVE"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "❌ غیرفعال" else "❌ DISABLED"
                            },
                            color = if (hasSmsPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "این دسترسی برای خواندن و دریافت پیامک‌های ورودی دستگاه است، تا دیگر پیامک‌های اداری، بانکی یا کدهای تایید دو مرحله‌ای ورودی به دستگاه خود را از راه دور روی تلگرام مانیتور کنید."
                        } else {
                            "Allows the application to fetch incoming SMS alerts and sync security code verifications seamlessly to your account."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    if (!hasSmsPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    "🔧 راهنمای فعال‌سازی گام‌به‌گام:\n" +
                                    "۱. روی دکمه «تایید دسترسی پیامک» در زیر ضربه بزنید.\n" +
                                    "۲. در پنجره مجوز که توسط اندروید نشان داده می‌شود، گزینه Allow یا «اجازه دادن» را انتخاب کنید.\n" +
                                    "۳. اگر پنجره باز نشد، به تنظیمات اطلاعات برنامه (App Info) شیفت کرده و در بخش Permissions، سوییچ SMS را به حالت مجاز (Allowed) تغییر دهید."
                                } else {
                                    "🔧 How to enable:\n" +
                                    "1. Press the verification button below.\n" +
                                    "2. Confirm 'Allow' inside the native Android permission query drawer."
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val list = arrayOf(
                                android.Manifest.permission.RECEIVE_SMS,
                                android.Manifest.permission.READ_SMS
                            )
                            smsLauncher.launch(list)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSmsPermission) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasSmsPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                if (hasSmsPermission) "تایید مجدد دسترسی پیامک" else "تایید دسترسی پیامک"
                            } else {
                                "Configure SMS Permissions"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Call History Access Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "📞 دسترسی گزارش‌ تماس‌ها" else "📞 Access Call Logs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (hasCallsPermission) {
                                if (lang == AppLanguage.PERSIAN) "✅ فعال شده" else "✅ ACTIVE"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "❌ غیرفعال" else "❌ DISABLED"
                            },
                            color = if (hasCallsPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "دسترسی به لاگ تماس‌ها برای ثبت شماره و تاریخچه تماس‌های ورودی، خروجی و از دست رفته دستگاه است، تا بتوانید آمار وضعیت تماس‌ها را از باتری تلگرام پایش کنید."
                        } else {
                            "Required to sync incoming, outgoing, and missed call statistics seamlessly to the master controller."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    if (!hasCallsPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    "🔧 راهنمای فعال‌سازی گام‌به‌گام:\n" +
                                    "۱. دکمه «تایید دسترسی تاریخچه تماس» در زیر را بفشارید.\n" +
                                    "۲. در کادر باز شده، مجوز دسترسی به Call Log را تایید (Allow) کنید تا برنامه به تاریخچه تماس‌ها متصل گردد."
                                } else {
                                    "🔧 How to enable:\n" +
                                    "1. Press the configurations button below.\n" +
                                    "2. Grant 'Call Log' authorization when prompted by Android."
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            callsLauncher.launch(android.Manifest.permission.READ_CALL_LOG)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasCallsPermission) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasCallsPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                if (hasCallsPermission) "تایید مجدد دسترسی تماس" else "تایید دسترسی تماس"
                            } else {
                                "Configure Call Permissions"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Contacts Access Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "👥 دسترسی مخاطبین دستگاه" else "👥 Access Contacts List",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (hasContactsPermission) {
                                if (lang == AppLanguage.PERSIAN) "✅ فعال شده" else "✅ ACTIVE"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "❌ غیرفعال" else "❌ DISABLED"
                            },
                            color = if (hasContactsPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "دسترسی مخاطبین برای همگام‌سازی نام مخاطبین در زمان دریافت اعلان‌ها و پیامک‌ها استفاده می‌شود تا به جای نمایش شماره خالی، نام ذخیره شده را تشخیص دهید."
                        } else {
                            "Allows the application to cross-reference incoming SMS numbers with your saved on-device contact book names."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    if (!hasContactsPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    "🔧 راهنمای فعال‌سازی گام‌به‌گام:\n" +
                                    "۱. دکمه «تایید دسترسی مخاطبان» در زیر را کلیک کنید.\n" +
                                    "۲. در منوی اندروید گزینه Allow (یا «اجازه دادن») را برای ورود به کاتالوگ مخاطبین کلیک کنید."
                                } else {
                                    "🔧 How to enable:\n" +
                                    "1. Click the configured settings trigger below.\n" +
                                    "2. Validate contact accessibility in the native dialog."
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            contactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasContactsPermission) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasContactsPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                if (hasContactsPermission) "تایید مجدد دسترسی مخاطبان" else "تایید دسترسی مخاطبان"
                            } else {
                                "Configure Contacts Permissions"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4.5 Storage & Files Access Card (Requirement 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "📁 دسترسی فایل‌ها و مدیر حافظه" else "📁 Files & All Storage Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (hasStoragePermission) {
                                if (lang == AppLanguage.PERSIAN) "✅ فعال شده" else "✅ ACTIVE"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "❌ غیرفعال" else "❌ DISABLED"
                            },
                            color = if (hasStoragePermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "این دسترسی به رایتو امکان می‌دهد تا فایل‌های ذخیره شده روی دستگاه شما (مانند موزیک‌ها، ویدیوها و تصاویر) را بازیابی کرده و در مدیریت فایل هوشمند ربات تلگرام نمایش دهد."
                        } else {
                            "Allows the application file explorer module to index and retrieve photos, audios, and download directories securely inside your Telegram bot."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    if (!hasStoragePermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    "🔧 راهنمای فعال‌سازی گام‌به‌گام:\n" +
                                    "۱. دکمه «تایید دسترسی فایل‌ها» در زیر را بفشارید.\n" +
                                    "۲. در منوی اندروید، گزینه All Files Access (یا دسترسی مدیریت تمامی فایل‌ها) را روشن کنید."
                                } else {
                                    "🔧 How to enable:\n" +
                                    "1. Press the authorize file explorer trigger below.\n" +
                                    "2. Toggle ON 'All Files Access' inside Android settings panel."
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: java.lang.Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        context.startActivity(intent)
                                    } catch (ex: java.lang.Exception) {
                                        android.widget.Toast.makeText(context, "Error opening settings", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                storageLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasStoragePermission) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                            contentColor = if (hasStoragePermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                if (hasStoragePermission) "تایید مجدد دسترسی فایل‌ها" else "تایید دسترسی فایل‌ها"
                            } else {
                                "Configure Storage Permissions"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Battery & App System Info Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "🔋 بهینه‌سازی مصرف باتری" else "🔋 Battery Settings optimization",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (hasBatteryExemption) {
                                if (lang == AppLanguage.PERSIAN) "✅ معاف شده" else "✅ UNRESTRICTED"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "⚠️ محدود شده" else "⚠️ RESTRICTED"
                            },
                            color = if (hasBatteryExemption) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) {
                            "اندروید به صورت خودکار فعالیت برنامه‌های پس‌زمینه را متوقف می‌کند. رایتو کله پایش خود و کاربری همگام‌ساز خود را در ثانیه به باتری متصل می‌کند، پس با قرار دادن باتری برنامه در حالت Unrestricted، جلوی خاموشی ناگهانی رایتو در پس‌زمینه را بگیرید."
                        } else {
                            "Allows the polling service to execute in background background indefinitely without sleep throttling parameters."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Please open system settings and configure battery and permissions manually.", Toast.LENGTH_LONG).show()
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "تنظیمات اطلاعات برنامه رایتو" else "App Info Details settings",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "برای تنظیم مستقیم باتری و بررسی جزئیات فعالیت سرویس." else "Change parameters manually.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // ----------------- TIER 4: HARDWARE LIVING TELEMETRY DATA PANEL -----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(6.dp, RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📱 " + Translations.get("stats_title", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.6.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HardwareMetricBox(
                            icon = Icons.Default.Info,
                            title = Translations.get("stats_battery", lang),
                            details = currentBatteryText,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HardwareMetricBox(
                            icon = Icons.Default.Folder,
                            title = Translations.get("stats_storage", lang),
                            details = currentStorageText,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        HardwareMetricBox(
                            icon = Icons.Default.Language,
                            title = Translations.get("stats_network", lang),
                            details = currentNetworkText,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun intentToNotificationAccess(context: Context) {
    try {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        Toast.makeText(context, "Find and enable RAITO in the listener menu list.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "System manual redirection failed. Open Settings > Notification Listener.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun HardwareMetricBox(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    details: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = details,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ServiceBreathingRing(isRunning: Boolean) {
    val duration = if (isRunning) 1400 else 0
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (isRunning) {
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val pulseAlpha by if (isRunning) {
        transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val activeColor = MaterialTheme.colorScheme.secondary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Box(contentAlignment = Alignment.Center) {
        // Glowing Background pulse
        Box(
            modifier = Modifier
                .size(76.dp)
                .drawBehind {
                    if (isRunning) {
                        drawCircle(
                            color = activeColor.copy(alpha = pulseAlpha),
                            radius = (size.width / 2) * pulseScale
                        )
                    }
                }
        )

        // Core Ring
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    if (isRunning) activeColor else inactiveColor,
                    CircleShape
                )
                .border(2.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Status Ring",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ----------------------------------------------------
// Tab 1: TELEMETRY OP LOGS CONSOLE AREA
// ----------------------------------------------------
@Composable
fun TelemetryLogsTab(
    viewModel: MainViewModel,
    logs: List<com.example.data.ActivityLog>,
    lang: AppLanguage
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .shadow(6.dp, RoundedCornerShape(32.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📡 " + Translations.get("console_title", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.6.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total cached sync logs: ${logs.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Purge action triggers
                Button(
                    onClick = { viewModel.clearTelemetryLogs() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Flush Console")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = Translations.get("btn_clear_logs", lang), fontSize = 11.7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Export card for report generation (Requirement 5)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "📊 خروجی گزارشات نوتیفیکیشن‌ها (TXT)" else "📊 Export Notification Reports (TXT)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "ذخیره گزارش تمامی پیام‌ها در پوشه دانلودها" else "Save all notification logs to Downloads folder",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            try {
                                val file = java.io.File(context.getExternalFilesDir(null), "notifications_log.txt")
                                var lines: List<String> = if (file.exists()) file.readLines() else emptyList()
                                if (lines.isEmpty()) {
                                    val publicFile = java.io.File(android.os.Environment.getExternalStorageDirectory(), "Documents/RaitoLogs/notifications_log.txt")
                                    if (publicFile.exists()) {
                                        lines = publicFile.readLines()
                                    }
                                }
                                
                                if (lines.isNotEmpty()) {
                                    val packageGroups = java.util.LinkedHashMap<String, MutableList<String>>()
                                    val packageManager = context.packageManager

                                    for (line in lines) {
                                        if (line.trim().isEmpty()) continue

                                        val packageMarker = "[Package: "
                                        val startIndex = line.indexOf(packageMarker)
                                        val packageName = if (startIndex != -1) {
                                            val endIndex = line.indexOf("]", startIndex + packageMarker.length)
                                            if (endIndex != -1) {
                                                line.substring(startIndex + packageMarker.length, endIndex).trim()
                                            } else {
                                                "System/Unknown"
                                            }
                                        } else {
                                            "System/Unknown"
                                        }
                                        
                                        if (!packageGroups.containsKey(packageName)) {
                                            packageGroups[packageName] = mutableListOf()
                                        }
                                        packageGroups[packageName]?.add(line)
                                    }

                                    if (packageGroups.isEmpty()) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (lang == AppLanguage.PERSIAN) "هیچ گزارشی یافت نشد." else "No records found.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        val sb = java.lang.StringBuilder()
                                        sb.append("=========================================================================\n")
                                        sb.append(if (lang == AppLanguage.PERSIAN) "📊 گزارش تجمیعی نوتیفیکیشن‌ها - رایتو\n" else "📊 UNIFIED CLASSIFIED NOTIFICATIONS REPORT\n")
                                        sb.append("Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
                                        sb.append("=========================================================================\n\n")

                                        for ((pkg, list) in packageGroups) {
                                            val appLabel = try {
                                                if (pkg == "System/Unknown" || pkg == "System & Other Alerts") {
                                                    if (lang == AppLanguage.PERSIAN) "سیستم و متفرقه" else "System Alerts & Miscellaneous"
                                                } else {
                                                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                                                    packageManager.getApplicationLabel(appInfo).toString()
                                                }
                                            } catch (e: Exception) {
                                                pkg
                                            }
                                            
                                            sb.append("=========================================================================\n")
                                            sb.append("📱 APP: $appLabel ($pkg)\n")
                                            sb.append("=========================================================================\n")
                                            
                                            for (origLine in list) {
                                                var cleanedLine = origLine
                                                val marker1 = "[Package: $pkg] "
                                                val marker2 = "[Package: $pkg]"
                                                if (cleanedLine.contains(marker1)) {
                                                    cleanedLine = cleanedLine.replace(marker1, "")
                                                } else if (cleanedLine.contains(marker2)) {
                                                    cleanedLine = cleanedLine.replace(marker2, "")
                                                }
                                                sb.append("  • $cleanedLine\n")
                                            }
                                            sb.append("\n\n")
                                        }

                                        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                        if (!downloadDir.exists()) {
                                            downloadDir.mkdirs()
                                        }
                                        val exportFile = java.io.File(downloadDir, "raito_notifications_classified_all.txt")
                                        exportFile.writeText(sb.toString(), Charsets.UTF_8)

                                        android.widget.Toast.makeText(
                                            context, 
                                            if (lang == AppLanguage.PERSIAN) "گزارش با موفقیت در پوشه دانلودها ذخیره شد:\n${exportFile.name}" else "Classified report stored in Downloads directory:\n${exportFile.name}", 
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context, 
                                        if (lang == AppLanguage.PERSIAN) "گزارش خالی است." else "No records cached to export.", 
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "📥 خروجی" else "📥 Export",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Export card for system telemetry logs (Room DB)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "📊 خروجی کل رویدادهای سیستم (System Logs TXT)" else "📊 Export All System Logs (TXT)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "ذخیره فایل گزارش سیستم در پوشه دانلودها" else "Save system diagnostic logs as text file",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            try {
                                if (logs.isNotEmpty()) {
                                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    if (!downloadDir.exists()) {
                                        downloadDir.mkdirs()
                                    }
                                    val exportFile = java.io.File(downloadDir, "raito_system_logs.txt")
                                    val sb = java.lang.StringBuilder()
                                    sb.append("--- RAITO SYSTEM DIAGNOSTICS LOG HISTORY ---\n")
                                    sb.append("Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
                                    logs.forEach { log ->
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        val timeStr = sdf.format(java.util.Date(log.timestamp))
                                        sb.append("[$timeStr] [${log.type}] [${log.status}] ${log.message}\n")
                                    }
                                    exportFile.writeText(sb.toString())
                                    
                                    android.widget.Toast.makeText(
                                        context, 
                                        if (lang == AppLanguage.PERSIAN) "گزارش با موفقیت در پوشه دانلودها ذخیره شد:\n${exportFile.name}" else "Logs stored in Downloads directory:\n${exportFile.name}", 
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context, 
                                        if (lang == AppLanguage.PERSIAN) "هیچ رویدادی وجود ندارد." else "No records to export.", 
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "📥 خروجی سیستم" else "📥 Export Logs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Logs Scrollable Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(24.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    RoundedCornerShape(24.dp)
                )
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No Logs",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Translations.get("no_logs", lang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        LogLineItem(log = log)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun LogLineItem(log: com.example.data.ActivityLog) {
    val isSystemDark = isSystemInDarkTheme()
    val accentColor = when (log.type) {
        "NOTIF_FORWARD" -> MaterialTheme.colorScheme.primary
        "SMS_FORWARD" -> Color(0xFF818CF8)
        "BOT_COMMAND" -> MaterialTheme.colorScheme.secondary
        "BOT_STATUS" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val typeLabel = when (log.type) {
        "NOTIF_FORWARD" -> "📬 NOTIF"
        "SMS_FORWARD" -> "💬 SMS"
        "BOT_COMMAND" -> "🤖 CMD"
        "BOT_STATUS" -> "⚙️ POL"
        else -> "📡 SYS"
    }

    val timeStr = remember(log.timestamp) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        formatter.format(Date(log.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                RoundedCornerShape(14.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Tag badge
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = typeLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = timeStr,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "•",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    text = log.status,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (log.status == "SUCCESS") MaterialTheme.colorScheme.secondary else accentColor.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------
// Tab 2: HELP & APP POLICIES AREA
// ----------------------------------------------------
@Composable
fun AppPoliciesTab(lang: AppLanguage) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(6.dp, RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ℹ️ " + Translations.get("tab_policy", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    // EXQUISITE AND ACCURATE DYNAMIC LOCALIZED TEXT REPRESENTATIONS
                    Text(
                        text = Translations.get("policy_text", lang),
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        textAlign = if (lang == AppLanguage.PERSIAN) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic email contact action trigger
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:Sinanetguard@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "RAITO Bot Developer Inquiries")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email applications available.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = if (isSystemDark) Color(0xFF381E72) else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email Direct Link")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Send Developer Mail", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Navigation Items & Bottom Selection Settings (Curved & Floating)
// ----------------------------------------------------
@Composable
fun RaitoBottomBar(activeTab: Int, onTabSelected: (Int) -> Unit, lang: AppLanguage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding() // Handled floating safe region beautifully
    ) {
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp), // curved
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            val customNavColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            NavigationBar(
                containerColor = Color.Transparent,
                windowInsets = WindowInsets(0,0,0,0), // clean custom inset handling
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.testTag("tab_control_core"),
                    colors = customNavColors,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Dashboard Control"
                        )
                    },
                    label = {
                        Text(
                            text = Translations.get("tab_dashboard", lang),
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.2.sp
                        )
                    }
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.testTag("tab_live_logs"),
                    colors = customNavColors,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Live Logs"
                        )
                    },
                    label = {
                        Text(
                            text = Translations.get("tab_logs", lang),
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.2.sp
                        )
                    }
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { onTabSelected(2) },
                    modifier = Modifier.testTag("tab_help_policies"),
                    colors = customNavColors,
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Default.Info else Icons.Outlined.Info,
                            contentDescription = "Help"
                        )
                    },
                    label = {
                        Text(
                            text = Translations.get("tab_policy", lang),
                            fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.2.sp
                        )
                    }
                )
            }
        }
    }
}

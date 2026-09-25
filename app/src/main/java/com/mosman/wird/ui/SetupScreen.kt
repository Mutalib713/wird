package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.R
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.mushaf.MushafDownloadService
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

/**
 * First-run Onboarding flow redesigned into an authentic 10-step journey (0 to 9):
 * - Step 0: Animated Atmospheric Splash (auto-advances upon 2.4s sweep ring, tap anywhere, no Begin button)
 * - Step 1: Why Wird? (The problem: rigid 1 Juz/day, broken streak shame. What is a Wird: presence over pressure)
 * - Step 2: Adapts to Your Learning Style (African & Ghanaian madrasa reverse tradition, mushaf vs memory, switch anytime)
 * - Step 3: 4 Core Features (Daily Wird portion, separated Sūrahs free reading, Whisper AI voice auditor, AI Chat companion)
 * - Step 4: Let's Set Up Your Wird Gateway (100% offline guarantee, step-by-step or quick start)
 * - Step 5: What should we call you? (readerName, private and local, skippable)
 * - Step 6: Reading Method & Direction (Mushaf vs Memory; Forward vs Reverse Madrasa, with switch anytime notes)
 * - Step 7: Starting Position (Searchable Sūrahs + quick presets: Juz 'Amma / Page 582, Beg / Page 1, Ya-Sin / Page 442)
 * - Step 8: Starting Ayah & Daily Target (Open in Qur'an page viewer, ayah number, half/1/2 pages or custom verses)
 * - Step 9: Blessing Du'a & Launch ("Bismillah, [Name]", du'a blessing, configured profile card, launch to Toolkit Tour)
 *
 * Implements Sacred Rule 6 & 9: Google Material / Lucide style vector drawables exclusively. Zero raw emojis.
 */
@Composable
fun SetupScreen(
    onDone: (
        page: Int,
        unitsPerDay: Int,
        startVerse: Pair<Int, Int>?,
        name: String?,
        mode: ReadingMode,
        direction: ReadingDirection,
    ) -> Unit,
) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }

    // State collected across steps
    var readerName by remember { mutableStateOf<String?>(null) }
    var readingMode by remember { mutableStateOf(ReadingMode.READING) }
    var readingDirection by remember { mutableStateOf(ReadingDirection.TOWARDS_FATIHAH) }
    var chosenSurah by remember { mutableStateOf<Surah?>(SurahIndex.byNumber(78)) } // Default Juz 'Amma
    var startAyah by remember { mutableIntStateOf(1) }
    var startAyahText by remember { mutableStateOf("1") }
    var startPage by remember { mutableIntStateOf(582) } // Default Page 582
    var dailyUnits by remember { mutableIntStateOf(2) } // 1 page (2 units)
    var isCustomTarget by remember { mutableStateOf(false) }
    var customVersesText by remember { mutableStateOf("10") }

    // Full-screen Mushaf Page Picker modal
    var viewingMushafPage by remember { mutableStateOf(false) }

    // Handle back button across steps
    BackHandler(enabled = viewingMushafPage || currentStep > 0) {
        if (viewingMushafPage) {
            viewingMushafPage = false
        } else if (currentStep > 1) {
            currentStep--
        } else if (currentStep == 1) {
            currentStep = 0
        }
    }

    if (viewingMushafPage) {
        // Full screen "Open in Qur'an" page viewer
        FullMushafPagePicker(
            initialPage = startPage,
            onAyahPicked = { s, a, p ->
                val surah = SurahIndex.byNumber(s)
                if (surah != null) {
                    chosenSurah = surah
                }
                startAyah = a
                startAyahText = a.toString()
                startPage = p
                viewingMushafPage = false
            },
            onClose = { viewingMushafPage = false },
        )
        return
    }

    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(groundColor)
            .statusBarsPadding()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        if (currentStep == 0) {
            Step0AnimatedSplash(
                onComplete = { currentStep = 1 },
            )
        } else {
            Spacer(Modifier.height(10.dp))

            // Top Stepper Navigation Row (Steps 1 to 9)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clayPill(
                            shape = RoundedCornerShape(999.dp),
                            backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFECE5D8),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "STEP $currentStep OF 9",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFC9A24B),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentStep == 5) {
                        TextButton(onClick = { currentStep = 6 }) {
                            Text(
                                text = "Skip",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (currentStep > 1) {
                        TextButton(onClick = { currentStep-- }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Back",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 9-Capsule Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (stepIndex in 1..9) {
                    val filled = stepIndex < currentStep
                    val current = stepIndex == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    current -> Color(0xFFC9A24B)
                                    filled -> if (isDark) Color(0xFF245847) else Color(0xFF2D6B52)
                                    else -> if (isDark) Color(0xFF182820) else Color(0xFFDDD7C8)
                                }
                            ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Step Content Body
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    1 -> Step1WhyWird(
                        onNext = { currentStep = 2 },
                    )
                    2 -> Step2LearningStyle(
                        onNext = { currentStep = 3 },
                    )
                    3 -> Step3CoreFeatures(
                        onNext = { currentStep = 4 },
                    )
                    4 -> Step4Gateway(
                        onPersonalize = { currentStep = 5 },
                        onQuickStart = {
                            chosenSurah = SurahIndex.byNumber(78) // Juz 'Amma An-Naba
                            startPage = 582
                            startAyah = 1
                            startAyahText = "1"
                            dailyUnits = 2 // 1 page
                            readingMode = ReadingMode.READING
                            readingDirection = ReadingDirection.TOWARDS_FATIHAH
                            currentStep = 9
                        },
                    )
                    5 -> Step5Name(
                        initialName = readerName,
                        onNext = {
                            readerName = it
                            currentStep = 6
                        },
                    )
                    6 -> Step6MethodAndDirection(
                        selectedMode = readingMode,
                        onSelectMode = { readingMode = it },
                        selectedDirection = readingDirection,
                        onSelectDirection = { readingDirection = it },
                        onNext = { currentStep = 7 },
                    )
                    7 -> Step7ReadingPosition(
                        chosenSurah = chosenSurah,
                        startPage = startPage,
                        onSurahPicked = { surah, page ->
                            chosenSurah = surah
                            startPage = page
                            startAyah = 1
                            startAyahText = "1"
                            currentStep = 8
                        },
                        onNext = { currentStep = 8 },
                    )
                    8 -> Step8AyahAndTarget(
                        surah = chosenSurah ?: SurahIndex.byNumber(78)!!,
                        startAyah = startAyah,
                        startAyahText = startAyahText,
                        onAyahTextChange = { text ->
                            startAyahText = text
                            val a = text.toIntOrNull()
                            val max = chosenSurah?.verses ?: 7
                            if (a != null && a in 1..max) {
                                startAyah = a
                                coroutineScope.launch {
                                    repo.pageOfVerse(chosenSurah?.number ?: 78, a)?.let { p ->
                                        startPage = p
                                    }
                                }
                            }
                        },
                        dailyUnits = dailyUnits,
                        onDailyUnitsChange = {
                            dailyUnits = it
                            isCustomTarget = false
                        },
                        isCustomTarget = isCustomTarget,
                        customVerses = customVersesText,
                        onCustomVersesChange = { text ->
                            customVersesText = text
                            isCustomTarget = true
                            val count = text.toIntOrNull() ?: 10
                            dailyUnits = when {
                                count <= 5 -> 1
                                count <= 10 -> 2
                                count <= 20 -> 4
                                else -> ((count / 10) * 2).coerceIn(1, 40)
                            }
                        },
                        onOpenMushaf = { viewingMushafPage = true },
                        onFinish = {
                            currentStep = 9
                        },
                    )
                    9 -> Step9Blessing(
                        readerName = readerName,
                        startPage = startPage,
                        chosenSurah = chosenSurah,
                        startAyah = startAyah,
                        dailyUnits = dailyUnits,
                        readingMode = readingMode,
                        readingDirection = readingDirection,
                        onLaunch = {
                            val versePair = chosenSurah?.number?.let { s -> s to startAyah }
                            onDone(startPage, dailyUnits, versePair, readerName, readingMode, readingDirection)
                        },
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 0: ANIMATED ATMOSPHERIC SPLASH (Auto-advances, No Begin Button)
// ============================================================================
@Composable
private fun Step0AnimatedSplash(
    onComplete: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    val emerald = Color(0xFF8ED676)

    // Animated Sweep Progress (0f to 1f over 2400ms)
    val sweepProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
        )
        delay(120)
        onComplete()
    }

    // Breathing Aura Infinite Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_scale",
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onComplete, // Tap anywhere advances immediately
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Background breathing aura
        Box(
            modifier = Modifier
                .size((280 * auraScale).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            emerald.copy(alpha = 0.22f * auraAlpha),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            // Rosette / Emblem with circular sweep ring
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Background & progress ring canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.5.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth),
                    )

                    // Sweep progress arc (-90 deg start)
                    drawArc(
                        brush = Brush.linearGradient(listOf(emerald, gold)),
                        startAngle = -90f,
                        sweepAngle = 360f * sweepProgress.value,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth * 1.15f, cap = StrokeCap.Round),
                    )
                }

                // Central emblem
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clayCard(
                            shape = CircleShape,
                            backgroundColor = if (isDark) Color(0xFF143324) else Color(0xFF1C6342),
                            highlightColor = Color.White.copy(alpha = 0.25f),
                            shadowColor = Color.Black.copy(alpha = 0.5f),
                            elevation = 6.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Wird Emblem",
                        modifier = Modifier.size(86.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // App title
            Text(
                text = "Wird",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "وِرْد · A Daily Qur'an Companion",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = gold,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "“The deeds most beloved to Allah are those that are consistent, even if they are few.”",
                fontSize = 13.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                lineHeight = 20.sp,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(28.dp))

            // Status timer pill
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF10261C) else Color(0xFFECE3D2),
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RhythmVectorIcon(tint = emerald, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Loading your quiet sanctuary...",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = emerald,
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 1: WHY WIRD? (THE PROBLEM & DEFINITION)
// ============================================================================
@Composable
private fun Step1WhyWird(
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF1B2E24) else Color(0xFFF3ECE0),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "WHY WIRD?",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = gold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "The Qur'an shouldn't feel like a chore you fail at.",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                lineHeight = 28.sp,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Most Quran apps expect you to recite a rigid 1 Juz a day, reset your streak if you miss a single busy evening, and leave you feeling guilty.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(16.dp))

            // Card 1: What is a Wird?
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clayCard(
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (isDark) Color(0xFF11261D) else Color(0xFFEDE4D4),
                                elevation = 1.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuranVectorIcon(tint = gold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "What is a Wird (ورْد)?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "In classical Islamic tradition, your Wird is simply your dedicated daily portion of Qur'an and remembrance. It can be 2 pages, 1 page, or even 3 verses — a quiet, lifelong habit.",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.5.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Card 2: Presence over Pressure
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clayCard(
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (isDark) Color(0xFF11261D) else Color(0xFFEDE4D4),
                                elevation = 1.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        HeartVectorIcon(tint = Color(0xFF8ED676))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Presence over Pressure",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Consistency wins over sporadic bursts. Wird builds a calm habit that fits into your actual daily life without guilt alarms or shame.",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.5.sp,
                )
            }
        }

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("How Wird Adapts to You", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ============================================================================
// STEP 2: ADAPTS TO YOUR LEARNING STYLE (Ghanaian & African Madrasa Emphasis)
// ============================================================================
@Composable
private fun Step2LearningStyle(
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    val emerald = Color(0xFF8ED676)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF1B2E24) else Color(0xFFF3ECE0),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "ADAPTS TO YOU",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = gold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Wird Adapts to Your Learning Style",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                lineHeight = 27.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Everyone has their own way of reciting and memorizing. Wird fits how you learn — and you can switch anytime:",
                fontSize = 12.5.sp,
                color = colors.textSecondary,
                lineHeight = 17.5.sp,
            )

            Spacer(Modifier.height(16.dp))

            // Card 1: Direction (Ghanaian & African madrasa tradition)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF182A1D) else Color(0xFFFDFBF7),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clayCard(
                                    shape = RoundedCornerShape(10.dp),
                                    backgroundColor = if (isDark) Color(0xFF2B2512) else Color(0xFFF7EED6),
                                    elevation = 1.dp,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            CompassVectorIcon(tint = gold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Direction: Reverse or Forward",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = gold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = RoundedCornerShape(999.dp),
                                backgroundColor = gold.copy(alpha = 0.18f),
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "Switch Anytime",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = gold,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "In Africa, and especially in Ghanaian madrasas and makaranta, we usually start memorizing and reciting from the back — beginning with Juz 'Amma (Surah An-Nas) and working upwards towards Al-Baqarah. Or you can read forward from the beginning. You can switch directions whenever you want.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Card 2: Method (Mushaf vs By Heart)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clayCard(
                                    shape = RoundedCornerShape(10.dp),
                                    backgroundColor = if (isDark) Color(0xFF11261D) else Color(0xFFEDE4D4),
                                    elevation = 1.dp,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BookVectorIcon(tint = emerald)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Method: In Qur'an or By Heart",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = RoundedCornerShape(999.dp),
                                backgroundColor = emerald.copy(alpha = 0.18f),
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "Switch Anytime",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = emerald,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Whether you prefer reading by looking directly at the authentic 15-line pages, or reciting by heart (Hifdh) walking to campus or on a trotro/bus without holding a book. Switch between them anytime with one tap.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.sp,
                )
            }
        }

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Next: Core Features Explained", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ============================================================================
// STEP 3: 4 CORE FEATURES EXPLAINED PLAINLY
// ============================================================================
@Composable
private fun Step3CoreFeatures(
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    val emerald = Color(0xFF8ED676)
    val blue = Color(0xFF60A5FA)
    val pink = Color(0xFFF472B6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF1B2E24) else Color(0xFFF3ECE0),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "4 CORE FEATURES",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = gold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Where Things Live in Wird",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                lineHeight = 27.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Plainly designed so your daily reading habit stays completely organized:",
                fontSize = 12.5.sp,
                color = colors.textSecondary,
                lineHeight = 17.5.sp,
            )

            Spacer(Modifier.height(14.dp))

            // Feature 1: Your Daily Wird
            FeatureCard(
                title = "1. Your Daily Wird (Portion)",
                description = "Your committed daily portion (even 1 page or 3 verses). Advances each day to keep you steady without pressure.",
                icon = { QuranVectorIcon(tint = emerald) },
                accentColor = emerald,
                isDark = isDark,
            )

            Spacer(Modifier.height(10.dp))

            // Feature 2: Sūrahs Section (Free reading, separated)
            FeatureCard(
                title = "2. The Sūrahs Section (Free Reading)",
                description = "Separate from your daily portion! When you want to recite Surah Al-Kahf on Friday, Al-Mulk at night, or browse, read freely here without moving your daily wird bookmark.",
                icon = { BookVectorIcon(tint = gold) },
                accentColor = gold,
                isDark = isDark,
            )

            Spacer(Modifier.height(10.dp))

            // Feature 3: Offline Voice Auditor (Whisper AI)
            FeatureCard(
                title = "3. Offline Voice Auditor (Whisper AI)",
                description = "A smart voice checker right on your phone. Recite out loud by heart — it listens and checks your verses word-for-word. 100% offline with zero data bundles used.",
                icon = { MicVectorIcon(tint = blue) },
                accentColor = blue,
                isDark = isDark,
            )

            Spacer(Modifier.height(10.dp))

            // Feature 4: AI Chat Companion
            FeatureCard(
                title = "4. The AI Chat Companion",
                description = "Chat with your quiet Quran companion anytime. Ask for English translations or Tafsir (explanations), ask 'What is my wird today?', or say 'I'm travelling this week' to pause without streak guilt.",
                icon = { ChatVectorIcon(tint = pink) },
                accentColor = pink,
                isDark = isDark,
            )
        }

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Ready to Personalize", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ============================================================================
// STEP 4: LET'S SET UP YOUR WIRD (Gateway)
// ============================================================================
@Composable
private fun Step4Gateway(
    onPersonalize: () -> Unit,
    onQuickStart: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clayCard(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF163325) else Color(0xFF1F6B47),
                        elevation = 4.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ShieldVectorIcon(tint = Color(0xFF8ED676), modifier = Modifier.size(34.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Let's Set Up Your Wird",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Takes under 45 seconds. No account, no email, and zero tracking.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(24.dp))

            // Reassurance Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF14241B) else Color(0xFFFFFFFF),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clayCard(
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (isDark) Color(0xFF102018) else Color(0xFFEDE4D4),
                                elevation = 1.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CheckVectorIcon(tint = Color(0xFF8ED676))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "100% Offline Guarantee",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Everything stays strictly on your device. Never consumes data or mobile money bundles. No cloud sync, no tracking.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.sp,
                )
            }
        }

        Column(
            modifier = Modifier.padding(bottom = 16.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onPersonalize,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Personalize Step-by-Step ›", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }

            Button(
                onClick = onQuickStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF162B21) else Color(0xFFECE4D6),
                    contentColor = colors.textPrimary,
                ),
            ) {
                Text("⚡ Quick Start with Defaults", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            }
        }
    }
}

// ============================================================================
// STEP 5: WHAT SHOULD WE CALL YOU? (Greeting Name, Skippable)
// ============================================================================
@Composable
private fun Step5Name(
    initialName: String?,
    onNext: (String?) -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var typed by remember { mutableStateOf(initialName ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            HeroHeader(
                icon = { UserVectorIcon(tint = Color(0xFFC9A24B)) },
                title = "What should we call you?",
                subtitle = "Only to greet you in your daily reflection. No account is required and your name stays strictly on this phone.",
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.take(24) },
                label = { Text("Your name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                colors = wirdFieldColors(),
            )

            Spacer(Modifier.height(16.dp))

            // Sacred Rule 1 reassurance card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color(0xFFF1EDE4),
                        elevation = 1.dp,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LockVectorIcon(tint = Color(0xFFC9A24B))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Sacred Rule 1: 100% offline & private. Never synced anywhere.",
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                )
            }
        }

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = { onNext(typed.trim().takeIf { it.isNotEmpty() }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Continue to Method & Direction ›", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ============================================================================
// STEP 6: READING METHOD & DIRECTION (With Switch Anytime Notes)
// ============================================================================
@Composable
private fun Step6MethodAndDirection(
    selectedMode: ReadingMode,
    onSelectMode: (ReadingMode) -> Unit,
    selectedDirection: ReadingDirection,
    onSelectDirection: (ReadingDirection) -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            HeroHeader(
                icon = { CompassVectorIcon(tint = gold) },
                title = "How do you recite?",
                subtitle = "Choose your starting preferences (you can switch anytime with one tap):",
            )

            Spacer(Modifier.height(18.dp))

            // Section 1: Reading Method
            Text(
                text = "1. READING METHOD",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = gold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))

            SelectionCard(
                title = "From the mushaf",
                description = "Reading with printed text open in front of you (Madani 15-line script).",
                tag = "TILĀWAH · SWITCH ANYTIME",
                selected = selectedMode == ReadingMode.READING,
                onClick = { onSelectMode(ReadingMode.READING) },
            )

            Spacer(Modifier.height(10.dp))

            SelectionCard(
                title = "From memory (Ḥifẓ)",
                description = "Reciting by heart without holding a book; pages for self-check & Murāja'ah.",
                tag = "HIFDH · SWITCH ANYTIME",
                selected = selectedMode == ReadingMode.MEMORISING,
                onClick = { onSelectMode(ReadingMode.MEMORISING) },
            )

            Spacer(Modifier.height(18.dp))

            // Section 2: Reading Direction
            Text(
                text = "2. READING DIRECTION",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = gold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))

            SelectionCard(
                title = "Towards Al-Fatihah (Reverse · Madrasa)",
                description = "An-Nas ➔ Al-Fatihah. The standard West African & Ghanaian madrasa progression.",
                tag = "MADRASA · SWITCH ANYTIME",
                selected = selectedDirection == ReadingDirection.TOWARDS_FATIHAH,
                onClick = { onSelectDirection(ReadingDirection.TOWARDS_FATIHAH) },
            )

            Spacer(Modifier.height(10.dp))

            SelectionCard(
                title = "Towards An-Nas (Forward)",
                description = "Al-Fatihah ➔ An-Nas. Standard front-to-back recitation towards full Khatmah.",
                tag = "STANDARD · SWITCH ANYTIME",
                selected = selectedDirection == ReadingDirection.TOWARDS_NAS,
                onClick = { onSelectDirection(ReadingDirection.TOWARDS_NAS) },
            )
        }

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Continue to Starting Position ›", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ============================================================================
// STEP 7: STARTING POSITION (Searchable Sūrahs + African Madrasa Presets)
// ============================================================================
@Composable
private fun Step7ReadingPosition(
    chosenSurah: Surah?,
    startPage: Int,
    onSurahPicked: (Surah, Int) -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            icon = { QuranVectorIcon(tint = gold) },
            title = "Starting Position",
            subtitle = "Choose where your recitation journey begins:",
        )

        Spacer(Modifier.height(14.dp))

        // Quick Presets
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetPositionCard(
                title = "Juz 'Amma (Surah An-Naba, Page 582)",
                subtitle = "The classic African & Ghanaian madrasa starting point",
                tag = "RECOMMENDED",
                selected = startPage == 582,
                onClick = {
                    SurahIndex.byNumber(78)?.let { onSurahPicked(it, 582) }
                },
                isDark = isDark,
            )

            PresetPositionCard(
                title = "From the Beginning (Page 1)",
                subtitle = "Surah Al-Fatihah & Al-Baqarah (Full Khatmah)",
                tag = "STANDARD",
                selected = startPage == 1,
                onClick = {
                    SurahIndex.byNumber(1)?.let { onSurahPicked(it, 1) }
                },
                isDark = isDark,
            )

            PresetPositionCard(
                title = "Surah Ya-Sin (Page 442)",
                subtitle = "The heart of the Holy Qur'an · Juz 23",
                tag = "POPULAR",
                selected = startPage == 442,
                onClick = {
                    SurahIndex.byNumber(36)?.let { onSurahPicked(it, 442) }
                },
                isDark = isDark,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Search Input for Any Sūrah
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clayCard(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = if (isDark) Color(0xFF13221A) else Color(0xFFFFFFFF),
                    elevation = 1.dp,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Or search any Sūrah (e.g. Al-Kahf or 18)...",
                        color = colors.textSecondary.copy(alpha = 0.6f),
                        fontSize = 12.5.sp,
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { searchQuery = "" },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Surah List
        Box(modifier = Modifier.weight(1f)) {
            SurahList(
                onPick = { surah -> onSurahPicked(surah, surah.firstPage) },
                searchQuery = searchQuery,
                contentPadding = PaddingValues(bottom = 12.dp),
            )
        }

        // Action Button
        Column(modifier = Modifier.padding(bottom = 16.dp, top = 6.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Confirm Position: ${chosenSurah?.name ?: "Page $startPage"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
// ============================================================================
// STEP 8: STARTING AYAH & DAILY TARGET (Open in Quran + Custom Verses)
// ============================================================================
@Composable
private fun Step8AyahAndTarget(
    surah: Surah,
    startAyah: Int,
    startAyahText: String,
    onAyahTextChange: (String) -> Unit,
    dailyUnits: Int,
    onDailyUnitsChange: (Int) -> Unit,
    isCustomTarget: Boolean,
    customVerses: String,
    onCustomVersesChange: (String) -> Unit,
    onOpenMushaf: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            HeroHeader(
                icon = { TargetVectorIcon(tint = gold) },
                title = "Starting Ayah & Daily Target",
                subtitle = "${surah.name} (${surah.verses} verses) · Choose your starting point and daily goal.",
            )

            Spacer(Modifier.height(18.dp))

            // Option 1: Open in Qur'an full mushaf picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = if (isDark) Color(0xFF172D22) else Color(0xFFFFFFFF),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                        elevation = 2.dp,
                    )
                    .clickable(onClick = onOpenMushaf)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clayCard(
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (isDark) Color(0xFF102018) else Color(0xFFEDE4D4),
                                elevation = 1.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuranVectorIcon(tint = gold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Open in Qur'an",
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "View Mushaf page and tap your ayah",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                        )
                    }
                }
                Text(
                    text = "Open →",
                    color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Divider or
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.weight(1f).height(0.5.dp).background(colors.ornament.copy(alpha = 0.3f)))
                Text(
                    text = "  OR TYPE AYAH NUMBER  ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp,
                )
                Box(modifier = Modifier.weight(1f).height(0.5.dp).background(colors.ornament.copy(alpha = 0.3f)))
            }

            Spacer(Modifier.height(10.dp))

            // Option 2: Type Ayah number field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (isDark) Color(0xFF13221A) else Color(0xFFFFFFFF),
                        elevation = 1.dp,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Ayah number in ${surah.name}:",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "Verse $startAyah of ${surah.verses}",
                        color = gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .clayCard(
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = if (isDark) Color(0xFF0F1A14) else Color(0xFFF3ECE0),
                            elevation = 1.dp,
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = startAyahText,
                        onValueChange = { onAyahTextChange(it.filter(Char::isDigit).take(3)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        ),
                        singleLine = true,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Section: Daily Reading Target
            Text(
                text = "DAILY READING TARGET",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = gold,
                letterSpacing = 0.8.sp,
            )

            Spacer(Modifier.height(8.dp))

            // Presets grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TargetPresetPill(
                    label = "Half a page",
                    selected = !isCustomTarget && dailyUnits == 1,
                    onClick = { onDailyUnitsChange(1) },
                    modifier = Modifier.weight(1f),
                )
                TargetPresetPill(
                    label = "1 page",
                    selected = !isCustomTarget && dailyUnits == 2,
                    onClick = { onDailyUnitsChange(2) },
                    modifier = Modifier.weight(1f),
                )
                TargetPresetPill(
                    label = "2 pages",
                    selected = !isCustomTarget && dailyUnits == 4,
                    onClick = { onDailyUnitsChange(4) },
                    modifier = Modifier.weight(1f),
                )
                TargetPresetPill(
                    label = "Custom",
                    selected = isCustomTarget,
                    onClick = { onCustomVersesChange(customVerses) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Custom Verses input row
            if (isCustomTarget) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clayCard(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (isDark) Color(0xFF14241B) else Color(0xFFF5EFE3),
                            elevation = 1.dp,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "How many verses per day?",
                        fontSize = 12.5.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .clayCard(
                                    shape = RoundedCornerShape(6.dp),
                                    backgroundColor = if (isDark) Color(0xFF0F1A14) else Color(0xFFFFFFFF),
                                    elevation = 1.dp,
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = customVerses,
                                onValueChange = onCustomVersesChange,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    color = gold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                ),
                                singleLine = true,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "verses",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Save Setup & View Blessing ›", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ============================================================================
// STEP 9: BLESSING DU'A & LAUNCH INTO TOOLKIT TOUR
// ============================================================================
@Composable
private fun Step9Blessing(
    readerName: String?,
    startPage: Int,
    chosenSurah: Surah?,
    startAyah: Int,
    dailyUnits: Int,
    readingMode: ReadingMode,
    readingDirection: ReadingDirection,
    onLaunch: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    val emerald = Color(0xFF8ED676)
    val nameGreeting = readerName?.trim()?.takeIf { it.isNotEmpty() } ?: "dear reader"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(16.dp))

            // Heart / Du'a Icon Bubble
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clayCard(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF163325) else Color(0xFF1F6B47),
                        elevation = 4.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                HeartVectorIcon(tint = emerald, modifier = Modifier.size(34.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Bismillah, $nameGreeting",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )

            Spacer(Modifier.height(6.dp))

            // Arabic Du'a
            Text(
                text = "اللَّهُمَّ اجْعَلِ القُرْآنَ رَبِيعَ قُلُوبِنَا",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = gold,
                letterSpacing = 0.5.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "“May Allah make the Qur'an the spring of our hearts, the light of our chests, and a steadfast companion in this life.”",
                fontSize = 12.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                lineHeight = 18.sp,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Configured Profile Summary Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF14241B) else Color(0xFFFFFFFF),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
            ) {
                Text(
                    text = "YOUR PROFILE IS CONFIGURED",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = emerald,
                    letterSpacing = 0.8.sp,
                )

                Spacer(Modifier.height(12.dp))

                ProfileSummaryRow(
                    label = "Starting Position",
                    value = if (startPage == 582) "Juz 'Amma (Page 582)" else "${chosenSurah?.name ?: "Al-Fatihah"} (Page $startPage, Ayah $startAyah)",
                )
                Spacer(Modifier.height(8.dp))
                ProfileSummaryRow(
                    label = "Reading Direction",
                    value = if (readingDirection == ReadingDirection.TOWARDS_FATIHAH) "Towards Al-Fatihah (Madrasa)" else "Towards An-Nas (Forward)",
                )
                Spacer(Modifier.height(8.dp))
                ProfileSummaryRow(
                    label = "Daily Reading Goal",
                    value = when (dailyUnits) {
                        1 -> "Half a Page Daily (~2 mins)"
                        2 -> "1 Page Daily (~3 to 4 mins)"
                        4 -> "2 Pages Daily (~7 mins)"
                        else -> "${(dailyUnits / 2.0)} Pages Daily"
                    },
                )
                Spacer(Modifier.height(8.dp))
                ProfileSummaryRow(
                    label = "Reading Method",
                    value = if (readingMode == ReadingMode.READING) "From the Mushaf" else "From Memory (Ḥifẓ)",
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color(0xFF0F1C15) else Color(0xFFF3EFE6),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Qur'an pages stream instantly and can be fully downloaded for offline reading anytime in Settings.",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)) {
            Button(
                onClick = onLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                CompassVectorIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enter Wird & Explore Toolkit ›", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
        }
    }
}

@Composable
private fun ProfileSummaryRow(label: String, value: String) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary,
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}


// ============================================================================
// REUSABLE COMPONENTS & VECTOR DRAWABLES (Sacred Rule 6)
// ============================================================================

@Composable
private fun HeroHeader(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    val colors = LocalWirdColors.current
    Column {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clayCard(
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = Color(0xFF1B2F25),
                    elevation = 2.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
            letterSpacing = (-0.3).sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 12.5.sp,
            color = colors.textSecondary,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun SelectionCard(
    title: String,
    description: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (selected) {
                    if (isDark) Color(0xFF1A3326) else Color(0xFFE5EFE8)
                } else {
                    if (isDark) Color(0xFF14241B) else Color(0xFFFFFFFF)
                },
                highlightColor = Color.White.copy(alpha = if (selected) 0.18f else 0.08f),
                shadowColor = if (selected) Color(0xFF245847).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f),
                elevation = if (selected) 3.dp else 1.dp,
                strokeWidth = if (selected) 1.5.dp else 0.5.dp,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Radio circle indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) (if (isDark) Color(0xFF93DB7A) else Color(0xFF245847)) else colors.textSecondary.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                            shape = CircleShape,
                        ),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = colors.textSecondary,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (selected) gold.copy(alpha = 0.15f) else colors.ornament.copy(alpha = 0.1f),
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) gold else colors.textSecondary,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

@Composable
private fun TargetPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Box(
        modifier = modifier
            .clayCard(
                shape = RoundedCornerShape(10.dp),
                backgroundColor = if (selected) {
                    if (isDark) Color(0xFF245847) else Color(0xFF2D6B52)
                } else {
                    if (isDark) Color(0xFF13221A) else Color(0xFFEDE6D8)
                },
                elevation = if (selected) 2.dp else 1.dp,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ---- Vector Canvas Drawables (Strictly Google Material / Lucide style) ------

/** Vector User Icon (Lucide User). */
@Composable
private fun UserVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        // Head circle
        drawCircle(color = tint, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
        // Torso arc
        val torso = Path().apply {
            moveTo(w * 0.15f, h * 0.88f)
            cubicTo(w * 0.18f, h * 0.65f, w * 0.82f, h * 0.65f, w * 0.85f, h * 0.88f)
        }
        drawPath(torso, color = tint, style = stroke)
    }
}

/** Vector Book Icon (Lucide Book-Open). */
@Composable
private fun BookVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        listOf(-1f, 1f).forEach { side ->
            val outer = Offset(w * (0.5f + side * 0.42f), h * 0.22f)
            val inner = Offset(w * 0.5f, h * 0.32f)
            drawLine(tint, outer, inner, stroke.width, stroke.cap)
            drawLine(tint, outer, Offset(outer.x, h * 0.80f), stroke.width, stroke.cap)
            drawLine(tint, Offset(outer.x, h * 0.80f), Offset(w * 0.5f, h * 0.86f), stroke.width, stroke.cap)
        }
        drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.86f), stroke.width, stroke.cap)
    }
}

/** Vector Compass/Navigation Icon (Lucide Navigation). */
@Composable
private fun CompassVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.48f)
            lineTo(w * 0.88f, h * 0.12f)
            lineTo(w * 0.52f, h * 0.88f)
            lineTo(w * 0.44f, h * 0.56f)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

/** Vector Quran Book with Ribbon Icon. */
@Composable
private fun QuranVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        // Book cover rect
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.70f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = stroke,
        )
        // Central bookmark ribbon
        drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.55f), stroke.width, stroke.cap)
    }
}

/** Vector Target Bullseye Icon. */
@Composable
private fun TargetVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = tint, radius = size.minDimension * 0.42f, style = stroke)
        drawCircle(color = tint, radius = size.minDimension * 0.20f, style = stroke)
        drawCircle(color = tint, radius = size.minDimension * 0.08f)
    }
}

/** Vector Lock Icon. */
@Composable
private fun LockVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        // Shackle
        val shackle = Path().apply {
            moveTo(w * 0.30f, h * 0.45f)
            cubicTo(w * 0.30f, h * 0.18f, w * 0.70f, h * 0.18f, w * 0.70f, h * 0.45f)
        }
        drawPath(shackle, color = tint, style = stroke)
        // Body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = stroke,
        )
    }
}

/** Vector Download into Tray Icon (Lucide Download). */
@Composable
private fun DownloadVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        // Downward arrow
        drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.62f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.28f, h * 0.42f), Offset(w * 0.5f, h * 0.62f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.72f, h * 0.42f), Offset(w * 0.5f, h * 0.62f), stroke.width, stroke.cap)
        // Tray
        val tray = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            lineTo(w * 0.18f, h * 0.84f)
            lineTo(w * 0.82f, h * 0.84f)
            lineTo(w * 0.82f, h * 0.68f)
        }
        drawPath(tray, color = tint, style = stroke)
    }
}

/** Vector Rhythm / Habit Calendar Icon. */
@Composable
private fun RhythmVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        // Calendar outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.70f, h * 0.65f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = stroke,
        )
        // Top binder rings
        drawLine(tint, Offset(w * 0.32f, h * 0.12f), Offset(w * 0.32f, h * 0.26f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.68f, h * 0.12f), Offset(w * 0.68f, h * 0.26f), stroke.width, stroke.cap)
        // Inner header divider
        drawLine(tint, Offset(w * 0.15f, h * 0.42f), Offset(w * 0.85f, h * 0.42f), stroke.width, stroke.cap)
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    accentColor: Color,
    isDark: Boolean,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (isDark) Color(0xFF14251C) else Color(0xFFFFFFFF),
                elevation = 2.dp,
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clayCard(
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = if (isDark) Color(0xFF0F1E16) else Color(0xFFEDE4D4),
                    elevation = 1.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 11.8.sp,
                color = colors.textSecondary,
                lineHeight = 16.5.sp,
            )
        }
    }
}

@Composable
private fun PresetPositionCard(
    title: String,
    subtitle: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
) {
    val colors = LocalWirdColors.current
    val gold = Color(0xFFC9A24B)
    val emerald = Color(0xFF8ED676)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = if (selected) {
                    if (isDark) Color(0xFF193627) else Color(0xFFE5EFE8)
                } else {
                    if (isDark) Color(0xFF14241B) else Color(0xFFFFFFFF)
                },
                elevation = if (selected) 3.dp else 1.dp,
                strokeWidth = if (selected) 1.5.dp else 0.5.dp,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clayPill(
                            shape = RoundedCornerShape(999.dp),
                            backgroundColor = if (selected) gold.copy(alpha = 0.2f) else colors.ornament.copy(alpha = 0.12f),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = tag,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) gold else colors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textSecondary,
                lineHeight = 15.sp,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(emerald, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF081F14),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Vector Heart Icon (Lucide Heart). */
@Composable
private fun HeartVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val heart = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.55f, w * 0.05f, h * 0.25f, w * 0.28f, h * 0.18f)
            cubicTo(w * 0.40f, h * 0.14f, w * 0.48f, h * 0.25f, w * 0.5f, h * 0.32f)
            cubicTo(w * 0.52f, h * 0.25f, w * 0.60f, h * 0.14f, w * 0.72f, h * 0.18f)
            cubicTo(w * 0.95f, h * 0.25f, w * 0.85f, h * 0.55f, w * 0.5f, h * 0.82f)
            close()
        }
        drawPath(heart, color = tint, style = stroke)
    }
}

/** Vector Chat Bubble Icon (Lucide Message-Square). */
@Composable
private fun ChatVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val bubble = Path().apply {
            moveTo(w * 0.18f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.68f)
            lineTo(w * 0.45f, h * 0.68f)
            lineTo(w * 0.28f, h * 0.84f)
            lineTo(w * 0.28f, h * 0.68f)
            lineTo(w * 0.18f, h * 0.68f)
            close()
        }
        drawPath(bubble, color = tint, style = stroke)
    }
}

/** Vector Shield Icon (Lucide Shield). */
@Composable
private fun ShieldVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.85f, h * 0.26f)
            lineTo(w * 0.85f, h * 0.58f)
            cubicTo(w * 0.85f, h * 0.78f, w * 0.5f, h * 0.90f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.5f, h * 0.90f, w * 0.15f, h * 0.78f, w * 0.15f, h * 0.58f)
            lineTo(w * 0.15f, h * 0.26f)
            close()
        }
        drawPath(shield, color = tint, style = stroke)
    }
}

/** Vector Check Icon (Lucide Check). */
@Composable
private fun CheckVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val check = Path().apply {
            moveTo(w * 0.20f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.75f)
            lineTo(w * 0.82f, h * 0.28f)
        }
        drawPath(check, color = tint, style = stroke)
    }
}


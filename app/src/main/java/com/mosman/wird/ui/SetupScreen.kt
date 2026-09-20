package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
 * First-run Onboarding flow redesigned into a cohesive 5-step journey:
 * - Step 1: What should I call you? (Greeting name, skippable)
 * - Step 2: Reading method ("From the mushaf" vs "From memory", matching Settings)
 * - Step 3: Reading direction ("Towards An-Nas" vs "Towards Al-Fatihah", matching Settings)
 * - Step 4: Reading position (Searchable Sūrah selection)
 * - Step 5: Starting Ayah & Daily Target (Open in Qur'an picker, Ayah number input, Custom daily verses)
 *
 * Implements Sacred Rule 6: Google Material / Lucide style vector drawables exclusively. Zero raw emojis.
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
    var readingDirection by remember { mutableStateOf(ReadingDirection.TOWARDS_NAS) }
    var chosenSurah by remember { mutableStateOf<Surah?>(SurahIndex.byNumber(1)) }
    var startAyah by remember { mutableIntStateOf(1) }
    var startAyahText by remember { mutableStateOf("1") }
    var startPage by remember { mutableIntStateOf(1) }
    var dailyUnits by remember { mutableIntStateOf(2) } // 1 page (2 units)
    var isCustomTarget by remember { mutableStateOf(false) }
    var customVersesText by remember { mutableStateOf("10") }

    // Full-screen Mushaf Page Picker modal
    var viewingMushafPage by remember { mutableStateOf(false) }

    // Handle back button across steps
    BackHandler(enabled = viewingMushafPage || currentStep > 0) {
        if (viewingMushafPage) {
            viewingMushafPage = false
        } else if (currentStep > 0) {
            currentStep--
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
            Step0Welcome(
                onStart = { currentStep = 1 },
            )
        } else {
            Spacer(Modifier.height(10.dp))

            // Top Stepper Navigation Row
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
                        text = "STEP $currentStep OF 6",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFC9A24B),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentStep == 1) {
                        TextButton(onClick = { currentStep = 2 }) {
                            Text(
                                text = "Skip",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
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

            Spacer(Modifier.height(8.dp))

            // 6-Capsule Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (stepIndex in 1..6) {
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
                    1 -> Step1Name(
                        initialName = readerName,
                        onNext = {
                            readerName = it
                            currentStep = 2
                        },
                    )
                    2 -> Step2ReadingMethod(
                        selectedMode = readingMode,
                        onSelectMode = { readingMode = it },
                        onNext = { currentStep = 3 },
                    )
                    3 -> Step3ReadingDirection(
                        selectedDirection = readingDirection,
                        onSelectDirection = { readingDirection = it },
                        onNext = { currentStep = 4 },
                    )
                    4 -> Step4ReadingPosition(
                        onSurahPicked = { surah ->
                            chosenSurah = surah
                            startPage = surah.firstPage
                            startAyah = 1
                            startAyahText = "1"
                            currentStep = 5
                        },
                    )
                    5 -> Step5AyahAndTarget(
                        surah = chosenSurah ?: SurahIndex.byNumber(1)!!,
                        startAyah = startAyah,
                        startAyahText = startAyahText,
                        onAyahTextChange = { text ->
                            startAyahText = text
                            val a = text.toIntOrNull()
                            val max = chosenSurah?.verses ?: 7
                            if (a != null && a in 1..max) {
                                startAyah = a
                                coroutineScope.launch {
                                    repo.pageOfVerse(chosenSurah?.number ?: 1, a)?.let { p ->
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
                            currentStep = 6
                        },
                    )
                    6 -> Step6DownloadPages(
                        onDone = {
                            val versePair = chosenSurah?.number?.let { s -> s to startAyah }
                            onDone(startPage, dailyUnits, versePair, readerName, readingMode, readingDirection)
                        },
                        onBack = { currentStep = 5 },
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 0: WELCOME & WHAT IS A WIRD?
// ============================================================================
@Composable
private fun Step0Welcome(
    onStart: () -> Unit,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(16.dp))

            // App Logo Badge
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clayCard(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF132D20) else Color(0xFF1E6A46),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.4f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF123B26).copy(alpha = 0.3f),
                        elevation = 4.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Wird Logo",
                    modifier = Modifier.size(76.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // App Name & Subtitle
            Text(
                text = "Wird",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "وِرْد · Daily Qur'an Companion",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = gold,
            )

            Spacer(Modifier.height(24.dp))

            // "WHAT IS A WIRD?" Definition Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                        elevation = 2.dp,
                    )
                    .padding(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clayPill(
                            shape = RoundedCornerShape(999.dp),
                            backgroundColor = if (isDark) Color(0xFF1B2E24) else Color(0xFFF3ECE0),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "WHAT IS A WIRD?",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = gold,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "“A Wird is the dedicated daily portion of the Holy Qur'an that you commit to reading every day — a quiet, lifelong habit.”",
                    fontSize = 13.5.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 20.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(18.dp))

            // 3 Core Pillars: Why Wird?
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                        elevation = 2.dp,
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                WelcomeFeatureRow(
                    icon = { RhythmVectorIcon(tint = gold) },
                    title = "Consistent Daily Pace",
                    description = "Small, manageable portions tailored to your speed and schedule.",
                    isDark = isDark,
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.ornament.copy(alpha = 0.25f)))
                WelcomeFeatureRow(
                    icon = { LockVectorIcon(tint = gold) },
                    title = "100% Offline & Private",
                    description = "No accounts, no ads, no tracking. Stays entirely on your device.",
                    isDark = isDark,
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.ornament.copy(alpha = 0.25f)))
                WelcomeFeatureRow(
                    icon = { QuranVectorIcon(tint = gold) },
                    title = "Authentic Mushaf",
                    description = "Original Madinah pages, tap-to-ayah selection, and verse-by-verse audio.",
                    isDark = isDark,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Get Started Button
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                    contentColor = Color.White,
                ),
            ) {
                Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

@Composable
private fun WelcomeFeatureRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    isDark: Boolean,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clayCard(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = if (isDark) Color(0xFF102018) else Color(0xFFEDE4D4),
                    elevation = 1.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = colors.textSecondary,
                lineHeight = 15.sp,
            )
        }
    }
}

// ============================================================================
// STEP 1: WHAT SHOULD I CALL YOU?
// ============================================================================
@Composable
private fun Step1Name(
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
                title = "What should I call you?",
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

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
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
                Text("Continue", fontWeight = FontWeight.Bold)
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
// STEP 2: READING METHOD (Matches Settings: Reading method)
// ============================================================================
@Composable
private fun Step2ReadingMethod(
    selectedMode: ReadingMode,
    onSelectMode: (ReadingMode) -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            HeroHeader(
                icon = { BookVectorIcon(tint = Color(0xFFC9A24B)) },
                title = "Reading method",
                subtitle = "Choose how you approach your daily recitation.",
            )

            Spacer(Modifier.height(20.dp))

            // Option 1: From the mushaf
            SelectionCard(
                title = "From the mushaf",
                description = "Reading with printed text open in front of you.",
                tag = "TILĀWAH & KHATM",
                selected = selectedMode == ReadingMode.READING,
                onClick = { onSelectMode(ReadingMode.READING) },
            )

            Spacer(Modifier.height(12.dp))

            // Option 2: From memory
            SelectionCard(
                title = "From memory",
                description = "Reciting by heart; page is for self-check and Murāja'ah.",
                tag = "HIFDH REVISION",
                selected = selectedMode == ReadingMode.MEMORISING,
                onClick = { onSelectMode(ReadingMode.MEMORISING) },
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Matches Settings → Reading Preferences. Can be changed anytime.",
                fontSize = 11.5.sp,
                color = colors.textSecondary.copy(alpha = 0.8f),
            )
        }

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
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
                Text("Next: Reading direction", fontWeight = FontWeight.Bold)
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
// STEP 3: READING DIRECTION (Matches Settings: Reading direction)
// ============================================================================
@Composable
private fun Step3ReadingDirection(
    selectedDirection: ReadingDirection,
    onSelectDirection: (ReadingDirection) -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            HeroHeader(
                icon = { CompassVectorIcon(tint = Color(0xFFC9A24B)) },
                title = "Reading direction",
                subtitle = "When you finish a portion, which direction does tomorrow's portion come from?",
            )

            Spacer(Modifier.height(20.dp))

            // Option 1: Towards An-Nas (Downwards)
            SelectionCard(
                title = "Towards An-Nas (Downwards)",
                description = "Front to back. Finish Ya-Sin and the next portion is As-Saffat.",
                tag = "STANDARD READING",
                selected = selectedDirection == ReadingDirection.TOWARDS_NAS,
                onClick = { onSelectDirection(ReadingDirection.TOWARDS_NAS) },
            )

            Spacer(Modifier.height(12.dp))

            // Option 2: Towards Al-Fatihah (Upwards)
            SelectionCard(
                title = "Towards Al-Fatihah (Upwards)",
                description = "Back to front. Finish Ya-Sin and the next portion is Fatir.",
                tag = "STANDARD REVISION",
                selected = selectedDirection == ReadingDirection.TOWARDS_FATIHAH,
                onClick = { onSelectDirection(ReadingDirection.TOWARDS_FATIHAH) },
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Matches Settings → Reading Preferences. Keeps your revision in order.",
                fontSize = 11.5.sp,
                color = colors.textSecondary.copy(alpha = 0.8f),
            )
        }

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
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
                Text("Next: Reading position", fontWeight = FontWeight.Bold)
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
// STEP 4: READING POSITION (Searchable Sūrah Selection)
// ============================================================================
@Composable
private fun Step4ReadingPosition(
    onSurahPicked: (Surah) -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroHeader(
            icon = { QuranVectorIcon(tint = Color(0xFFC9A24B)) },
            title = "Reading position",
            subtitle = "Select any Sūrah to start from.",
        )

        Spacer(Modifier.height(14.dp))

        // Search Input
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
                        text = "Search by name or number (e.g. Ya-Sin or 36)...",
                        color = colors.textSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
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

        Spacer(Modifier.height(10.dp))

        // Surah List
        Box(modifier = Modifier.weight(1f)) {
            SurahList(
                onPick = onSurahPicked,
                searchQuery = searchQuery,
                contentPadding = PaddingValues(bottom = 24.dp),
            )
        }
    }
}

// ============================================================================
// STEP 5: STARTING AYAH & DAILY TARGET (Open in Quran + Custom Verses)
// ============================================================================
@Composable
private fun Step5AyahAndTarget(
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

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
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
                Text("Next: Qur'an Pages", fontWeight = FontWeight.Bold)
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
// STEP 6: DOWNLOAD QUR'AN PAGES (All 604 Pages · Background Enabled)
// ============================================================================
@Composable
private fun Step6DownloadPages(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    val liveProgress by MushafDownloadService.mushafProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            HeroHeader(
                icon = { DownloadVectorIcon(tint = gold) },
                title = "Qur'an Pages Download",
                subtitle = "Download all 604 pages once to read completely offline anywhere.",
            )

            Spacer(Modifier.height(20.dp))

            if (liveProgress != null) {
                val progress = liveProgress!!
                val done = progress.done
                val total = progress.total
                val fraction = if (total > 0) done.toFloat() / total.toFloat() else 0f
                val mbDone = String.format(java.util.Locale.US, "%.1f", progress.bytesDownloaded.toFloat() / (1024 * 1024))
                val mbTotal = String.format(java.util.Locale.US, "%.1f", progress.totalBytes.toFloat() / (1024 * 1024))

                // Active Download Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clayCard(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                            elevation = 2.dp,
                        )
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Downloading Mushaf...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                        )
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = RoundedCornerShape(999.dp),
                                    backgroundColor = if (isDark) Color(0xFF102018) else Color(0xFFE8F3EE),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "${(fraction * 100).toInt()}%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = gold,
                        trackColor = if (isDark) Color(0xFF0F1A14) else Color(0xFFDDD7C8),
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "$done of $total pages",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = "$mbDone / $mbTotal MB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                    }

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
                            text = "Downloads smoothly in the background. You can enter the app now — reading is fully functional.",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp,
                        )
                    }
                }
            } else {
                // Specs & Benefit Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clayCard(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = if (isDark) Color(0xFF15261D) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                            elevation = 2.dp,
                        )
                        .padding(20.dp),
                ) {
                    Text(
                        text = "PACKAGE SPECIFICATIONS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = gold,
                    )
                    Spacer(Modifier.height(12.dp))

                    DownloadSpecRow(
                        label = "Total Pages",
                        value = "All 604 Pages (Full Qur'an)",
                    )
                    Spacer(Modifier.height(10.dp))
                    DownloadSpecRow(
                        label = "Download Size",
                        value = "~86.5 MB",
                    )
                    Spacer(Modifier.height(10.dp))
                    DownloadSpecRow(
                        label = "Offline Status",
                        value = "100% Offline forever",
                    )
                    Spacer(Modifier.height(10.dp))
                    DownloadSpecRow(
                        label = "Background",
                        value = "Runs while you read",
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
                            text = "Recommended on Wi-Fi. If you skip or cancel, pages will stream on-demand or can be downloaded in Settings anytime.",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action Buttons Column
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (liveProgress != null) {
                // While downloading: "Enter App & Start Reading" + "Cancel Download"
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Scale.minTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Enter App & Start Reading", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { MushafDownloadService.stop(context) },
                    modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
                ) {
                    Text(
                        text = "Cancel Download",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                // Idle: "Download & Start Reading" + "Skip for now"
                Button(
                    onClick = {
                        MushafDownloadService.start(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Scale.minTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Download & Start Reading", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDone,
                    modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
                ) {
                    Text(
                        text = "Skip for now (stream on-demand)",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadSpecRow(label: String, value: String) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = colors.textSecondary,
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}

/** Full Mushaf Page Picker allowing user to view the page and tap any ayah. */
@Composable
private fun FullMushafPagePicker(
    initialPage: Int,
    onAyahPicked: (surahNumber: Int, ayahNumber: Int, page: Int) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalWirdColors.current
    var currentPage by remember { mutableIntStateOf(initialPage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clayPill(shape = CircleShape, backgroundColor = colors.surface)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tap your starting ayah",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Page $currentPage · Swipe left/right to browse",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                    )
                }
            }

            TextButton(onClick = onClose) {
                Text("Cancel", color = colors.textSecondary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            MushafPager(
                initialPage = initialPage,
                onPageChanged = { currentPage = it },
                lit = { it.lines.toSet() },
                onWordTap = { verseKey ->
                    val s = verseKey.substringBefore(':').toIntOrNull()
                    val a = verseKey.substringAfter(':').toIntOrNull()
                    if (s != null && a != null) {
                        onAyahPicked(s, a, currentPage)
                    }
                },
            )
        }
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


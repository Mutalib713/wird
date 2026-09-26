package com.mosman.wird.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.audio.RecitationModel
import com.mosman.wird.data.DataOnDevice
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.AwayPeriod
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.PrivacyPledge
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.dayLabel
import com.mosman.wird.domain.label
import com.mosman.wird.domain.listLabel
import com.mosman.wird.nudge.Armed
import com.mosman.wird.nudge.NudgeDiagnostic
import com.mosman.wird.nudge.OemAdvice
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.SetStatusBarAppearance
import com.mosman.wird.ui.theme.arabicNumerals
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.TrackScheduleMode
import java.time.DayOfWeek
import java.time.LocalDate

/** Sub-screens within the Settings flow. */
enum class SettingsSubScreen {
    MAIN,
    POSITION_PICKER,
    AUDIO_MANAGER,
}

/** Which popup modal dialog is open. Null means none. */
private enum class SettingsDialog {
    DOWNLOAD_AMOUNT,
    DAILY_TARGET,
    READING_METHOD,
    READING_DIRECTION,
    WEEKLY_SCHEDULE,
    THEME,
    TRANSLATIONS,
    AUDIO_QUALITY,
    REMINDER,
    RECITER_PICKER,
    RECITATION_CHECKER_MODEL,
    BATTERY_OPT,
    REMINDER_DIAGNOSTIC,
    PRIVACY_PLEDGE,
    ABOUT_WIRD,
    PAGE_PREVIEW,
    SCHEDULE_MODE,
    LIFE_SPACE_MANAGER,
    EDIT_TRACK,
    MANAGE_TRACKS,
}

data class DialogOption<T>(
    val value: T,
    val title: String,
    val description: String,
)

/**
 * Settings screen.
 *
 * Designed in tactile claymorphism with:
 * - Direct Sūrah reading position selector matching SurahsTab styling.
 * - Dedicated Audio Manager subscreen for imam reciter selection and 114 Sūrah voice downloads.
 * - Separate Recitation checker model row with popup dialog for the on-device AI speech listener.
 * - Battery optimization row with explanation dialog and one-tap jump to app settings.
 * - Grouped clay cards with rounded 24dp surfaces, subtle highlights, and soft shadows.
 * - Tactile clay switches for instant on/off toggles.
 * - Modal dialog popups for multi-option settings.
 */
@Composable
fun SettingsScreen(
    theme: ThemeMode,
    plan: ReadingPlan,
    positionLabel: String,
    schedule: NudgeSchedule,
    armed: Armed?,
    away: AwayPeriod?,
    notificationsOn: Boolean = true,
    onFixNotifications: () -> Unit = {},
    model: RecitationModel? = null,
    fetchingModel: Pair<Long, Long>? = null,
    onGetModel: ((RecitationModel) -> Unit)? = null,
    downloadedModels: List<RecitationModel> = emptyList(),
    onUseModel: (RecitationModel) -> Unit = {},
    cachedPages: Pair<Int, Long> = 0 to 0L,
    downloading: Pair<Int, Int>? = null,
    onDownloadAll: () -> Unit = {},
    audioQuality: AudioQuality,
    readingMode: ReadingMode,
    direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
    onDirection: (ReadingDirection) -> Unit = {},
    onDevice: DataOnDevice?,
    onExport: () -> Unit,
    onDeleteRecordings: () -> Unit,
    exportNote: String?,
    onTheme: (ThemeMode) -> Unit,
    onPlan: (ReadingPlan) -> Unit,
    onSchedule: (NudgeSchedule) -> Unit,
    onResume: () -> Unit,
    onAudioQuality: (AudioQuality) -> Unit,
    onReadingMode: (ReadingMode) -> Unit,
    onUseLocation: () -> Unit,
    onChangePosition: () -> Unit = {},
    onPositionChanged: (Pair<Int, Int>?, Int) -> Unit = { _, _ -> },
    onLifeSpacesChanged: () -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { WirdStore(context) }
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)
    val liveMushafProgress by com.mosman.wird.mushaf.MushafDownloadService.mushafProgress.collectAsState()

    // Current sub-screen
    var subScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    var currentPositionLabel by remember(positionLabel) { mutableStateOf(positionLabel) }

    // Life Spaces state
    var lifeSpaces by remember { mutableStateOf(store.getLifeSpaces()) }
    var activeSpace by remember { mutableStateOf(store.activeSpace()) }
    var trackScheduleMode by remember { mutableStateOf(store.trackScheduleMode) }
    var editingTrack by remember { mutableStateOf<ReadingTrack?>(null) }
    var editingSpaceId by remember { mutableStateOf<String?>(null) }

    // Store-backed state
    var lockOrientation by remember { mutableStateOf(store.lockOrientation) }
    var landscapeOrientation by remember { mutableStateOf(store.landscapeOrientation) }
    var keepAwake by remember { mutableStateOf(store.keepScreenAwake) }
    var surahTranslated by remember { mutableStateOf(store.surahTranslatedName) }
    var ayahBeforeTrans by remember { mutableStateOf(store.ayahBeforeTranslation) }
    var customAyahTextSizeEnabled by remember { mutableStateOf(store.customAyahTextSizeEnabled) }
    var ayahTextSize by remember { mutableIntStateOf(store.ayahTextSize) }
    var streamingAudio by remember { mutableStateOf(store.streamingAudio) }
    var downloadAmount by remember { mutableStateOf(store.downloadAmount) }
    var selectedTrans by remember { mutableStateOf(store.selectedTranslation) }
    var selectedReciter by remember { mutableStateOf(store.selectedReciter) }
    var nightTextBrightness by remember { mutableIntStateOf(store.nightTextBrightness) }
    var nightBgBrightness by remember { mutableIntStateOf(store.nightBgBrightness) }

    // Sūrah search & page picker state in Position Picker
    var positionSearchActive by remember { mutableStateOf(false) }
    var positionQuery by remember { mutableStateOf("") }
    val positionFocusRequester = remember { FocusRequester() }
    var pickingPageForSurah by remember { mutableStateOf<Int?>(null) }

    // Audio downloads tracking
    var downloadedSurahs by remember { mutableStateOf(setOf(1, 36, 67, 112, 113, 114)) }

    // Active popup dialog state
    var activeDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    LaunchedEffect(positionSearchActive) {
        if (positionSearchActive) {
            positionFocusRequester.requestFocus()
        }
    }

    // Intercept back button when inside a modal dialog, sub-screen, or page picker
    BackHandler(enabled = activeDialog != null || subScreen != SettingsSubScreen.MAIN || pickingPageForSurah != null) {
        if (activeDialog != null) {
            activeDialog = null
        } else if (pickingPageForSurah != null) {
            pickingPageForSurah = null
        } else if (subScreen == SettingsSubScreen.POSITION_PICKER && positionSearchActive) {
            positionSearchActive = false
            positionQuery = ""
        } else {
            subScreen = SettingsSubScreen.MAIN
        }
    }

    SetStatusBarAppearance(isLightBackground = !isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(groundColor)
            .statusBarsPadding(),
    ) {
        when (subScreen) {
            // ================================================================
            // 1. MAIN SETTINGS SCREEN
            // ================================================================
            SettingsSubScreen.MAIN -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar with tactile clay back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clayCard(
                                    shape = CircleShape,
                                    backgroundColor = if (isDark) Color(0xFF16251E) else Color.White,
                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                                    elevation = 3.dp,
                                )
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF1E3F32),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Text(
                            text = "Settings",
                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        )
                    }

                    // Scrollable Settings Sections
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {

                        // 1. Display Settings
                        ClaySection(title = "Display Settings") {
                            ClaySettingRow(
                                title = "Lock screen orientation",
                                subtitle = if (lockOrientation) "Screen orientation is locked" else "Screen rotates freely with device orientation",
                                trailing = {
                                    ClaySwitch(
                                        checked = lockOrientation,
                                        onCheckedChange = {
                                            lockOrientation = it
                                            store.lockOrientation = it
                                        },
                                    )
                                },
                            )

                            ClaySettingRow(
                                title = "Landscape orientation",
                                subtitle = if (!lockOrientation) "Unlock device rotation above to choose orientation" else if (landscapeOrientation) "Locked to Landscape mode" else "Locked to Portrait mode",
                                enabled = lockOrientation,
                                trailing = {
                                    ClaySwitch(
                                        checked = landscapeOrientation,
                                        enabled = lockOrientation,
                                        onCheckedChange = {
                                            landscapeOrientation = it
                                            store.landscapeOrientation = it
                                        },
                                    )
                                },
                            )

                            ClaySettingRow(
                                title = "Surah translated name",
                                subtitle = "Show English translation beside surah name",
                                trailing = {
                                    ClaySwitch(
                                        checked = surahTranslated,
                                        onCheckedChange = {
                                            surahTranslated = it
                                            store.surahTranslatedName = it
                                        },
                                    )
                                },
                            )

                            ClaySettingRow(
                                title = "Keep screen awake",
                                subtitle = "Prevent screen timeout while reading or reciting",
                                trailing = {
                                    ClaySwitch(
                                        checked = keepAwake,
                                        onCheckedChange = {
                                            keepAwake = it
                                            store.keepScreenAwake = it
                                        },
                                    )
                                },
                            )

                            ClaySettingRow(
                                title = "Theme",
                                subtitle = themeLabel(theme),
                                onClick = { activeDialog = SettingsDialog.THEME },
                            )

                            val nightOn = theme == ThemeMode.DARK
                            ClaySettingRow(
                                title = "Night mode",
                                subtitle = "Use dark background and light fonts",
                                trailing = {
                                    ClaySwitch(
                                        checked = nightOn,
                                        onCheckedChange = { on ->
                                            onTheme(if (on) ThemeMode.DARK else ThemeMode.LIGHT)
                                        },
                                    )
                                },
                                showDivider = nightOn,
                            )

                            AnimatedVisibility(visible = nightOn) {
                                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                    SliderSubBox {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "Text brightness",
                                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF2D5A46),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "$nightTextBrightness / 255",
                                                color = Color(0xFF2D6B52),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Slider(
                                            value = nightTextBrightness.toFloat(),
                                            onValueChange = {
                                                nightTextBrightness = it.toInt()
                                                store.nightTextBrightness = nightTextBrightness
                                            },
                                            valueRange = 50f..255f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF2D6B52),
                                                inactiveTrackColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFE2DDD0),
                                            ),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(nightBgBrightness, nightBgBrightness, nightBgBrightness))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = "Preview text contrast",
                                                color = Color(nightTextBrightness, nightTextBrightness, nightTextBrightness),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "Background brightness",
                                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF2D5A46),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = if (nightBgBrightness == 0) "Deep Black" else "$nightBgBrightness / 255",
                                                color = Color(0xFF2D6B52),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Slider(
                                            value = nightBgBrightness.toFloat(),
                                            onValueChange = {
                                                nightBgBrightness = it.toInt()
                                                store.nightBgBrightness = nightBgBrightness
                                            },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF2D6B52),
                                                inactiveTrackColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFE2DDD0),
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Reading Modes & Tracks
                        ClaySection(title = "Reading Modes & Tracks") {
                            ClaySettingRow(
                                title = "Schedule mode",
                                subtitle = if (trackScheduleMode == TrackScheduleMode.AUTOMATIC) {
                                    "Automatic · Switches tracks by day of week"
                                } else {
                                    "Manual · You choose the active track"
                                },
                                onClick = { activeDialog = SettingsDialog.SCHEDULE_MODE },
                            )

                            ClaySettingRow(
                                title = "Active reading mode",
                                subtitle = "${activeSpace.name} ${if (activeSpace.isFrozen) "(Paused & Protected)" else "(Active)"} · Tap to switch or pause",
                                onClick = { activeDialog = SettingsDialog.LIFE_SPACE_MANAGER },
                            )

                            val tracksSummary = if (activeSpace.tracks.isEmpty()) {
                                "No tracks yet · Tap to add your first track"
                            } else {
                                "${activeSpace.tracks.size} track${if (activeSpace.tracks.size != 1) "s" else ""} (${activeSpace.tracks.joinToString(", ") { it.name }}) · Manage & edit"
                            }
                            ClaySettingRow(
                                title = "Recitation tracks",
                                subtitle = tracksSummary,
                                onClick = { activeDialog = SettingsDialog.MANAGE_TRACKS },
                                showDivider = false,
                            )
                        }

                        // 3. Reading Preferences
                        ClaySection(title = "Reading Preferences") {
                            ClaySettingRow(
                                title = "Reading position",
                                subtitle = currentPositionLabel,
                                onClick = { subScreen = SettingsSubScreen.POSITION_PICKER },
                            )

                            ClaySettingRow(
                                title = "Daily reading target",
                                subtitle = amountLabel(plan.defaultUnits),
                                onClick = { activeDialog = SettingsDialog.DAILY_TARGET },
                            )

                            ClaySettingRow(
                                title = "Reading method",
                                subtitle = modeLabel(readingMode),
                                onClick = { activeDialog = SettingsDialog.READING_METHOD },
                            )

                            ClaySettingRow(
                                title = "Reading direction",
                                subtitle = directionLabel(direction),
                                onClick = { activeDialog = SettingsDialog.READING_DIRECTION },
                            )

                            ClaySettingRow(
                                title = "Weekly reading schedule",
                                subtitle = weeklyScheduleLabel(plan),
                                onClick = { activeDialog = SettingsDialog.WEEKLY_SCHEDULE },
                            )

                            ClaySettingRow(
                                title = "Adjust ayah font size",
                                subtitle = if (customAyahTextSizeEnabled) "Custom ($ayahTextSize sp)" else "Default (fits full 15 lines on screen)",
                                trailing = {
                                    ClaySwitch(
                                        checked = customAyahTextSizeEnabled,
                                        onCheckedChange = {
                                            customAyahTextSizeEnabled = it
                                            store.customAyahTextSizeEnabled = it
                                        },
                                    )
                                },
                                showDivider = !customAyahTextSizeEnabled,
                            )

                            if (customAyahTextSizeEnabled) {
                                SliderSubBox {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Ayah font size",
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF2D5A46),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            if (ayahTextSize != 24) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isDark) Color(0xFF1E382B) else Color(0xFFD6EDE0))
                                                        .clickable {
                                                            ayahTextSize = 24
                                                            store.ayahTextSize = 24
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                                ) {
                                                    Text(
                                                        text = "Reset to default",
                                                        color = if (isDark) Color(0xFF86E39D) else Color(0xFF1E5B42),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (ayahTextSize == 24) "$ayahTextSize sp (default)" else "$ayahTextSize sp",
                                                color = Color(0xFF2D6B52),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    Slider(
                                        value = ayahTextSize.toFloat(),
                                        onValueChange = {
                                            ayahTextSize = it.toInt()
                                            store.ayahTextSize = ayahTextSize
                                        },
                                        valueRange = 18f..32f,
                                        steps = 13,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF2D6B52),
                                            inactiveTrackColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFE2DDD0),
                                        ),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (ayahTextSize <= 24) "18–24 sp fits full 15-line page on one screen without scrolling (like Quran for Android)." else "Larger Arabic text (scroll to read full page).",
                                        color = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    // Live Arabic preview box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF16271F) else Color(0xFFEAE5D8))
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                            fontSize = ayahTextSize.sp,
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0xFF1A3326) else Color(0xFFDCEDE3))
                                            .clickable { activeDialog = SettingsDialog.PAGE_PREVIEW }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Preview Sample 15-Line Page",
                                                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Translation Preferences
                        ClaySection(title = "Translation Preferences") {
                            ClaySettingRow(
                                title = "Translations",
                                subtitle = "$selectedTrans · Download & manage",
                                onClick = { activeDialog = SettingsDialog.TRANSLATIONS },
                            )

                            ClaySettingRow(
                                title = "Ayah before translation",
                                subtitle = "Show ayah in Arabic above the translation",
                                trailing = {
                                    ClaySwitch(
                                        checked = ayahBeforeTrans,
                                        onCheckedChange = {
                                            ayahBeforeTrans = it
                                            store.ayahBeforeTranslation = it
                                        },
                                    )
                                },
                                showDivider = false,
                            )
                        }

                        // 4. Download Options
                        ClaySection(title = "Download Options") {
                            ClaySettingRow(
                                title = "Streaming",
                                subtitle = "Stream audio instead of downloading",
                                trailing = {
                                    ClaySwitch(
                                        checked = streamingAudio,
                                        onCheckedChange = {
                                            streamingAudio = it
                                            store.streamingAudio = it
                                        },
                                    )
                                },
                            )

                            ClaySettingRow(
                                title = "Download amount",
                                subtitle = "$downloadAmount · Preferred download portion",
                                onClick = { activeDialog = SettingsDialog.DOWNLOAD_AMOUNT },
                            )

                            ClaySettingRow(
                                title = "Audio quality",
                                subtitle = "${audioQuality.label} · Abu Bakr al-Shatri",
                                onClick = { activeDialog = SettingsDialog.AUDIO_QUALITY },
                            )

                            // Audio Manager: reciter voice downloads
                            ClaySettingRow(
                                title = "Audio Manager",
                                subtitle = "$selectedReciter · Download voices & reciters",
                                onClick = { subScreen = SettingsSubScreen.AUDIO_MANAGER },
                            )

                            // Recitation checker model (separate AI speech listener setting)
                            val modelStatusLabel = when {
                                fetchingModel != null -> {
                                    val (done, total) = fetchingModel
                                    if (total > 0) "Downloading, ${done * 100 / total}%" else "Downloading…"
                                }
                                model != null -> "${model.label} in use"
                                downloadedModels.isNotEmpty() -> "${downloadedModels.first().label} ready"
                                else -> "Whisper AI · On-device listener"
                            }
                            ClaySettingRow(
                                title = "Recitation checker model",
                                subtitle = "$modelStatusLabel · Speech recognition AI",
                                onClick = { activeDialog = SettingsDialog.RECITATION_CHECKER_MODEL },
                            )

                            // Mushaf pages
                            val (pages, bytes) = cachedPages
                            val whole = Mushaf.PAGES
                            val live = liveMushafProgress
                            val pagesSubtitle = if (downloading != null || live != null) {
                                val done = live?.done ?: downloading?.first ?: 0
                                val total = live?.total ?: downloading?.second ?: whole
                                val mbDone = String.format(java.util.Locale.US, "%.1f", (live?.bytesDownloaded ?: 0L).toFloat() / (1024 * 1024))
                                val mbTotal = String.format(java.util.Locale.US, "%.1f", (live?.totalBytes ?: com.mosman.wird.mushaf.MushafDownloadService.ESTIMATED_TOTAL_BYTES).toFloat() / (1024 * 1024))
                                "Downloading $done of $total pages · $mbDone / $mbTotal MB"
                            } else if (pages >= whole) {
                                "All 604 pages ready · 100% offline (${megabytes(bytes)})"
                            } else {
                                "$pages of $whole pages · ${megabytes(bytes)}"
                            }
                            ClaySettingRow(
                                title = "Mushaf pages",
                                subtitle = pagesSubtitle,
                                onClick = {
                                    if (downloading != null || live != null) {
                                        com.mosman.wird.mushaf.MushafDownloadService.stop(context)
                                    } else if (pages < whole) {
                                        onDownloadAll()
                                    }
                                },
                                showDivider = false,
                            )
                        }

                        // 5. Reminders
                        ClaySection(title = "Reminders") {
                            ClaySettingRow(
                                title = "Notification schedule",
                                subtitle = schedule.label(),
                                onClick = { activeDialog = SettingsDialog.REMINDER },
                            )

                            if (away != null) {
                                ClaySettingRow(
                                    title = "Paused while you're away",
                                    subtitle = "Back ${dayLabel(away.returnsOn, LocalDate.now())} · tap to end",
                                    onClick = onResume,
                                )
                            }

                            val advice = remember { OemAdvice.forThisPhone() }
                            val diagnosticReport = remember(activeDialog) { NudgeDiagnostic.evaluate(context, store) }

                            ClaySettingRow(
                                title = "Battery & background survival",
                                subtitle = "${advice.vendor} (${advice.systemSkin ?: "Android"}) · Step-by-step setup",
                                onClick = { activeDialog = SettingsDialog.BATTERY_OPT },
                            )

                            ClaySettingRow(
                                title = "Did your last reminder ring?",
                                subtitle = diagnosticReport.headline,
                                trailing = {
                                    val (badgeBg, badgeFg, badgeText) = when (diagnosticReport.overallStatus) {
                                        NudgeDiagnostic.Status.OK -> Triple(
                                            if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                            if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                            "OK",
                                        )
                                        NudgeDiagnostic.Status.WARNING -> Triple(
                                            if (isDark) Color(0xFF382E18) else Color(0xFFFBF0D8),
                                            if (isDark) Color(0xFFFFD166) else Color(0xFF8F6300),
                                            "CHECK",
                                        )
                                        NudgeDiagnostic.Status.ERROR -> Triple(
                                            if (isDark) Color(0xFF3A1C1C) else Color(0xFFFCE8E8),
                                            if (isDark) Color(0xFFFF8B8B) else Color(0xFFB52A2A),
                                            "ALERT",
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clayPill(backgroundColor = badgeBg, elevation = 1.dp)
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeFg,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                },
                                onClick = { activeDialog = SettingsDialog.REMINDER_DIAGNOSTIC },
                                showDivider = false,
                            )
                        }

                        // 6. Advanced & Data
                        ClaySection(title = "Advanced & Data") {
                            ClaySettingRow(
                                title = "Export everything",
                                subtitle = exportNote ?: dataLabel(onDevice),
                                onClick = onExport,
                            )

                            if ((onDevice?.recordings ?: 0) > 0) {
                                ClaySettingRow(
                                    title = "Clear audio recordings",
                                    subtitle = "Free up ${megabytes(onDevice?.recordingBytes ?: 0)} · Keep text records",
                                    onClick = onDeleteRecordings,
                                )
                            }

                            ClaySettingRow(
                                title = PrivacyPledge.TITLE,
                                subtitle = PrivacyPledge.SUBTITLE,
                                onClick = { activeDialog = SettingsDialog.PRIVACY_PLEDGE },
                            )

                            ClaySettingRow(
                                title = "About Wird",
                                subtitle = "v1.0 · 100% offline & private · Zero guilt",
                                onClick = { activeDialog = SettingsDialog.ABOUT_WIRD },
                                showDivider = false,
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }

            // ================================================================
            // 2. DIRECT SŪRAH READING POSITION PICKER
            // ================================================================
            SettingsSubScreen.POSITION_PICKER -> {
                if (pickingPageForSurah != null) {
                    FullMushafPagePicker(
                        initialPage = pickingPageForSurah!!,
                        onAyahPicked = { surahNum, ayahNum, page ->
                            val newVerse = surahNum to ayahNum
                            store.positionPage = page
                            store.startVerse = newVerse
                            currentPositionLabel = positionLabelFor(newVerse, page)
                            onPositionChanged(newVerse, page)
                            pickingPageForSurah = null
                            subScreen = SettingsSubScreen.MAIN
                            positionSearchActive = false
                            positionQuery = ""
                        },
                        onClose = {
                            pickingPageForSurah = null
                        },
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!positionSearchActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clayCard(
                                                shape = CircleShape,
                                                backgroundColor = if (isDark) Color(0xFF16251E) else Color.White,
                                                highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                                                shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                                                elevation = 3.dp,
                                            )
                                            .clickable { subScreen = SettingsSubScreen.MAIN },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to settings",
                                            tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF1E3F32),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = "Reading Position",
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.5).sp,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Select any Sūrah to pick starting page & ayah",
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }

                                // Tactile Clay Search Button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clayCard(
                                            shape = CircleShape,
                                            backgroundColor = if (isDark) Color(0xFF16251E) else Color.White,
                                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                                            elevation = 3.dp,
                                        )
                                        .clickable { positionSearchActive = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search Sūrah",
                                        tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF1E3F32),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        } else {
                            // Active Search Top Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clayCard(
                                            shape = RoundedCornerShape(999.dp),
                                            backgroundColor = if (isDark) Color(0xFF13201A) else Color.White,
                                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.2f),
                                            elevation = 3.dp,
                                        )
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF50A773),
                                            modifier = Modifier.size(19.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (positionQuery.isEmpty()) {
                                                Text(
                                                    text = "Search by name or number...",
                                                    color = if (isDark) Color(0xFF6E8276) else Color(0xFF8C7D6B),
                                                    fontSize = 13.5.sp,
                                                )
                                            }
                                            BasicTextField(
                                                value = positionQuery,
                                                onValueChange = { positionQuery = it },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontSize = 14.sp,
                                                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                                cursorBrush = SolidColor(Color(0xFF50A773)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(positionFocusRequester),
                                            )
                                        }
                                        if (positionQuery.isNotEmpty()) {
                                            Text(
                                                text = "Clear",
                                                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF50A773),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier
                                                    .clickable { positionQuery = "" }
                                                    .padding(start = 6.dp, end = 2.dp),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clayCard(
                                            shape = CircleShape,
                                            backgroundColor = if (isDark) Color(0xFF16251E) else Color.White,
                                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                                            elevation = 3.dp,
                                        )
                                        .clickable {
                                            positionSearchActive = false
                                            positionQuery = ""
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close search",
                                        tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            if (positionQuery.isNotBlank()) {
                                val matchCount = remember(positionQuery) { searchSurahs(positionQuery).size }
                                Text(
                                    text = if (matchCount == 1) "1 MATCH FOUND" else "$matchCount MATCHES FOUND",
                                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        SurahList(
                            onPick = { surah ->
                                pickingPageForSurah = surah.firstPage
                            },
                            onOpenPage = { page ->
                                pickingPageForSurah = page
                            },
                            showTranslatedName = surahTranslated,
                            searchQuery = if (positionSearchActive) positionQuery else "",
                            contentPadding = PaddingValues(bottom = 32.dp),
                        )
                    }
                }
            }

            // ================================================================
            // 3. AUDIO MANAGER SUB-SCREEN
            // ================================================================
            SettingsSubScreen.AUDIO_MANAGER -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clayCard(
                                    shape = CircleShape,
                                    backgroundColor = if (isDark) Color(0xFF16251E) else Color.White,
                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                                    elevation = 3.dp,
                                )
                                .clickable { subScreen = SettingsSubScreen.MAIN },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to settings",
                                tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF1E3F32),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Audio Manager",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Reciter voices & offline audio",
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Content list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
                    ) {
                        // Card 0: Download Amount Selector Card
                        item {
                            ClaySection(title = "Download Amount") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf("Page", "Surah", "Juz").forEach { amt ->
                                        val isSelected = downloadAmount == amt
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clayPill(
                                                    backgroundColor = if (isSelected) Color(0xFF2D6B52) else (if (isDark) Color(0xFF1B2E24) else Color(0xFFEDE8DD)),
                                                    elevation = if (isSelected) 3.dp else 1.dp,
                                                )
                                                .clickable {
                                                    downloadAmount = amt
                                                    store.downloadAmount = amt
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = amt,
                                                color = if (isSelected) Color.White else (if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Card 1: Active Reciter Card
                        item {
                            ClaySection(title = "Selected Reciter") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = selectedReciter,
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "Murattal · Hafs an Asim",
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = Color(0xFF2D6B52),
                                                elevation = 3.dp,
                                            )
                                            .clickable { activeDialog = SettingsDialog.RECITER_PICKER }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "CHANGE",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }
                            }
                        }

                        // Card 2: Batch Download Card
                        item {
                            ClaySection(title = "Full Qur'an Offline") {
                                val allDownloaded = downloadedSurahs.size >= 114
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = if (allDownloaded) "All 114 Sūrahs Downloaded" else "Download All 114 Sūrahs",
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = if (allDownloaded) "Ready for complete offline recitation (~650 MB)" else "~650 MB · Batch download for $selectedReciter",
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (allDownloaded) (if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6)) else Color(0xFF2D6B52),
                                                elevation = 3.dp,
                                            )
                                            .clickable {
                                                downloadedSurahs = if (allDownloaded) emptySet() else SurahIndex.all.map { it.number }.toSet()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (allDownloaded) "CLEAR" else "DOWNLOAD ALL",
                                            color = if (allDownloaded) (if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52)) else Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "INDIVIDUAL SŪRAHS (${downloadedSurahs.size}/114 READY)",
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                            )
                        }

                        // 114 Sūrah rows with download status
                        items(SurahIndex.all, key = { it.number }) { surah ->
                            val isDownloaded = surah.number in downloadedSurahs
                            val bg = if (isDark) Color(0xFF13201A) else Color.White
                            val highlight = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f)
                            val shadow = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF8C7D6B).copy(alpha = 0.22f)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(18.dp),
                                        backgroundColor = bg,
                                        highlightColor = highlight,
                                        shadowColor = shadow,
                                        elevation = 3.dp,
                                        strokeWidth = 1.dp,
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Squircle medallion with Arabic numerals
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clayCard(
                                                shape = RoundedCornerShape(11.dp),
                                                backgroundColor = if (isDark) Color(0xFF1A2B24) else Color(0xFFEDF5F0),
                                                highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.85f),
                                                shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFFA0B9AA).copy(alpha = 0.35f),
                                                elevation = 2.dp,
                                                strokeWidth = 1.dp,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = arabicNumerals(surah.number),
                                            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF174233),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = surah.listLabel(includeMeaning = surahTranslated),
                                            color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${surah.verses} verses · ~${approxAudioSize(surah.verses)}",
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                                            fontSize = 12.sp,
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Download button or checkmark
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (isDownloaded) {
                                                    if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6)
                                                } else {
                                                    if (isDark) Color(0xFF1A2A20) else Color(0xFFEDE8DD)
                                                },
                                                elevation = 2.dp,
                                            )
                                            .clickable {
                                                downloadedSurahs = if (isDownloaded) downloadedSurahs - surah.number else downloadedSurahs + surah.number
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isDownloaded) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Downloaded",
                                                    tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                                    modifier = Modifier.size(15.dp),
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = if (isDownloaded) "READY" else "GET",
                                                color = if (isDownloaded) {
                                                    if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52)
                                                } else {
                                                    if (isDark) Color(0xFFB0C4B8) else Color(0xFF556C60)
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
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

        // ====================================================================
        // Tactile Clay Dialog Popups
        // ====================================================================

        when (activeDialog) {
            SettingsDialog.DOWNLOAD_AMOUNT -> {
                ClayOptionDialog(
                    title = "Download amount",
                    options = listOf(
                        DialogOption("Page", "Page", "Download audio for current page only"),
                        DialogOption("Surah", "Surah", "Download audio for entire chapter"),
                        DialogOption("Juz", "Juz", "Download audio for full 20-page section"),
                    ),
                    selected = downloadAmount,
                    onSelect = {
                        downloadAmount = it
                        store.downloadAmount = it
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.DAILY_TARGET -> {
                DailyTargetDialog(
                    currentUnits = plan.defaultUnits,
                    onSelect = { units ->
                        onPlan(plan.copy(defaultUnits = units))
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.READING_METHOD -> {
                ClayOptionDialog(
                    title = "Reading method",
                    options = listOf(
                        DialogOption(ReadingMode.READING, "From the mushaf", "Reading with printed text open"),
                        DialogOption(ReadingMode.MEMORISING, "From memory", "Reciting from memory (Hifdh revision)"),
                    ),
                    selected = readingMode,
                    onSelect = onReadingMode,
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.READING_DIRECTION -> {
                ClayOptionDialog(
                    title = "Reading direction",
                    options = listOf(
                        DialogOption(
                            ReadingDirection.TOWARDS_NAS,
                            "Towards An-Nas (Downwards)",
                            "Front to back. When finished Sūrah 112 (Al-Ikhlas), tomorrow moves down to 113 (Al-Falaq).",
                        ),
                        DialogOption(
                            ReadingDirection.TOWARDS_FATIHAH,
                            "Towards Al-Fatihah (Upwards)",
                            "Back to front. When finished Sūrah 112 (Al-Ikhlas), tomorrow moves up to 111 (Al-Masadd).",
                        ),
                    ),
                    selected = direction,
                    onSelect = onDirection,
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.WEEKLY_SCHEDULE -> {
                WeeklyScheduleDialog(
                    plan = plan,
                    onPlan = onPlan,
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.THEME -> {
                ClayOptionDialog(
                    title = "Theme",
                    options = listOf(
                        DialogOption(ThemeMode.LIGHT, "Warm Cream", "Sanctuary light daytime palette"),
                        DialogOption(ThemeMode.DARK, "Night Dark", "Deep dark green nighttime palette"),
                        DialogOption(ThemeMode.SYSTEM, "Match Phone", "Follow system dark mode toggle"),
                    ),
                    selected = theme,
                    onSelect = onTheme,
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.TRANSLATIONS -> {
                ClayOptionDialog(
                    title = "Translations",
                    options = listOf(
                        DialogOption("Saheeh International", "Saheeh International", "Standard contemporary English"),
                        DialogOption("The Clear Quran", "The Clear Quran", "Dr. Mustafa Khattab · Fluent thematic modern English"),
                        DialogOption("Yusuf Ali", "Yusuf Ali", "Classic English translation"),
                    ),
                    selected = selectedTrans,
                    onSelect = {
                        selectedTrans = it
                        store.selectedTranslation = it
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.AUDIO_QUALITY -> {
                ClayOptionDialog(
                    title = "Audio quality",
                    options = listOf(
                        DialogOption(AudioQuality.LIGHT, "Standard (64 kbps)", "1.1 MB / page · Recommended for mobile data"),
                        DialogOption(AudioQuality.BETTER, "High (128 kbps)", "2.3 MB / page · Best sound quality"),
                    ),
                    selected = audioQuality,
                    onSelect = onAudioQuality,
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.REMINDER -> {
                val currentRemIndex = when (schedule) {
                    is NudgeSchedule.AfterPrayer -> when (schedule.prayer) {
                        Prayer.MAGHRIB -> 0
                        Prayer.ISHA -> 1
                        Prayer.FAJR -> 2
                        else -> 0
                    }
                    is NudgeSchedule.AtClockTime -> if (schedule.time.hour == 20) 3 else 4
                    is NudgeSchedule.Off -> 5
                }
                val activeModeName = store.activeSpace().name
                ClayOptionDialog(
                    title = "Reminder schedule ($activeModeName)",
                    options = listOf(
                        DialogOption(0, "After Maghrib", "15 minutes after sunset"),
                        DialogOption(1, "After 'Isha", "Quiet night reading before sleep"),
                        DialogOption(2, "After Fajr", "Start of the morning"),
                        DialogOption(3, "Fixed time: 8:00 PM", "Every day at 8:00 PM"),
                        DialogOption(4, "Fixed time: 9:00 PM", "Every day at 9:00 PM"),
                        DialogOption(5, "Off", "No reminders"),
                    ),
                    selected = currentRemIndex,
                    onSelect = { idx ->
                        val newSchedule = when (idx) {
                            0 -> NudgeSchedule.AfterPrayer(Prayer.MAGHRIB, 15)
                            1 -> NudgeSchedule.AfterPrayer(Prayer.ISHA, 15)
                            2 -> NudgeSchedule.AfterPrayer(Prayer.FAJR, 15)
                            3 -> NudgeSchedule.AtClockTime(java.time.LocalTime.of(20, 0))
                            4 -> NudgeSchedule.AtClockTime(java.time.LocalTime.of(21, 0))
                            else -> NudgeSchedule.Off
                        }
                        store.updateActiveSpaceReminder(newSchedule)
                        onSchedule(newSchedule)
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.RECITER_PICKER -> {
                ClayOptionDialog(
                    title = "Select Reciter",
                    options = listOf(
                        DialogOption("Abu Bakr al-Shatri", "Abu Bakr al-Shatri", "Murattal · Hafs an Asim (Default)"),
                        DialogOption("Mishary Rashid Alafasy", "Mishary Rashid Alafasy", "Murattal · Melodic & Clear"),
                        DialogOption("Mahmoud Khalil Al-Husary", "Mahmoud Khalil Al-Husary", "Murattal · Classical Master of Tajweed"),
                        DialogOption("Abdul Basit Abdul Samad", "Abdul Basit Abdul Samad", "Murattal · Celebrated Egyptian Reciter"),
                    ),
                    selected = selectedReciter,
                    onSelect = {
                        selectedReciter = it
                        store.selectedReciter = it
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.RECITATION_CHECKER_MODEL -> {
                ClayOptionDialog(
                    title = "Recitation checker model",
                    options = listOf(
                        DialogOption(
                            RecitationModel.TINY,
                            "Whisper Compact (42 MB)",
                            "Faster on-device model, uses less RAM. Good for quick checks.",
                        ),
                        DialogOption(
                            RecitationModel.BASE,
                            "Whisper Accurate (78 MB)",
                            "Higher precision Arabic phoneme recognition. Recommended.",
                        ),
                    ),
                    selected = model ?: RecitationModel.TINY,
                    onSelect = { chosen ->
                        if (chosen in downloadedModels) {
                            onUseModel(chosen)
                        } else if (onGetModel != null) {
                            onGetModel(chosen)
                        }
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.BATTERY_OPT -> {
                val advice = remember { OemAdvice.forThisPhone() }
                Dialog(
                    onDismissRequest = { activeDialog = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { activeDialog = null }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(28.dp),
                                    backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                                    shadowColor = Color.Black.copy(alpha = 0.35f),
                                    elevation = 16.dp,
                                )
                                .clickable(enabled = false) {}
                                .padding(22.dp),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Background Survival",
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${advice.vendor} · ${advice.systemSkin ?: "Android"}",
                                            color = Color(0xFF2D6B52),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (advice.known) {
                                                    if (isDark) Color(0xFF382E18) else Color(0xFFFBF0D8)
                                                } else {
                                                    if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6)
                                                },
                                                elevation = 1.dp,
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = if (advice.known) "STRICT OEM" else "STANDARD",
                                            color = if (advice.known) {
                                                if (isDark) Color(0xFFFFD166) else Color(0xFF8F6300)
                                            } else {
                                                if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = if (advice.known) {
                                        "Phones from ${advice.vendor} aggressively freeze background apps to save battery. Follow these steps so your daily reminder and prayer-time alarms ring on time:"
                                    } else {
                                        "Your phone usually allows background alarms. If your reminder ever misses, set Wird's battery usage to 'Unrestricted':"
                                    },
                                    color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF4A6054),
                                    fontSize = 13.sp,
                                    lineHeight = 18.5.sp,
                                )

                                Spacer(Modifier.height(14.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    advice.steps.forEachIndexed { index, step ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clayCard(
                                                    shape = RoundedCornerShape(14.dp),
                                                    backgroundColor = if (isDark) Color(0xFF192C23) else Color(0xFFF3EFE6),
                                                    elevation = 1.dp,
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clayPill(backgroundColor = Color(0xFF2D6B52), elevation = 1.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = step,
                                                color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF1E3F32),
                                                fontSize = 12.5.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                                elevation = 2.dp,
                                            )
                                            .clickable {
                                                activeDialog = null
                                                context.startActivity(OemAdvice.createAppDetailsIntent(context))
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "APP INFO",
                                            color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF556C60),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .clayPill(
                                                backgroundColor = Color(0xFF2D6B52),
                                                elevation = 3.dp,
                                            )
                                            .clickable {
                                                activeDialog = null
                                                context.startActivity(OemAdvice.createBatterySettingsIntent(context))
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "OPEN SETTINGS",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SettingsDialog.REMINDER_DIAGNOSTIC -> {
                val report = remember { NudgeDiagnostic.evaluate(context, store) }
                Dialog(
                    onDismissRequest = { activeDialog = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { activeDialog = null }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(28.dp),
                                    backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                                    shadowColor = Color.Black.copy(alpha = 0.35f),
                                    elevation = 16.dp,
                                )
                                .clickable(enabled = false) {}
                                .padding(22.dp),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Reminder Diagnostics",
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = report.headline,
                                            color = when (report.overallStatus) {
                                                NudgeDiagnostic.Status.OK -> if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52)
                                                NudgeDiagnostic.Status.WARNING -> if (isDark) Color(0xFFFFD166) else Color(0xFF8F6300)
                                                NudgeDiagnostic.Status.ERROR -> if (isDark) Color(0xFFFF8B8B) else Color(0xFFB52A2A)
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = report.explanation,
                                    color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF4A6054),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )

                                Spacer(Modifier.height(14.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    report.checks.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clayCard(
                                                    shape = RoundedCornerShape(14.dp),
                                                    backgroundColor = if (isDark) Color(0xFF192C23) else Color(0xFFF3EFE6),
                                                    elevation = 1.dp,
                                                )
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val dotColor = when (item.status) {
                                                NudgeDiagnostic.Status.OK -> Color(0xFF2D6B52)
                                                NudgeDiagnostic.Status.WARNING -> Color(0xFFD48B00)
                                                NudgeDiagnostic.Status.ERROR -> Color(0xFFC0392B)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor),
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    text = item.detail,
                                                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                                    fontSize = 11.5.sp,
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                                elevation = 2.dp,
                                            )
                                            .clickable { activeDialog = null }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "CLOSE",
                                            color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF556C60),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    if (report.overallStatus != NudgeDiagnostic.Status.OK) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .clayPill(
                                                    backgroundColor = Color(0xFF2D6B52),
                                                    elevation = 3.dp,
                                                )
                                                .clickable {
                                                    activeDialog = SettingsDialog.BATTERY_OPT
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "FIX SETTINGS",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SettingsDialog.PRIVACY_PLEDGE -> {
                Dialog(
                    onDismissRequest = { activeDialog = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { activeDialog = null }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(28.dp),
                                    backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                                    shadowColor = Color.Black.copy(alpha = 0.35f),
                                    elevation = 16.dp,
                                )
                                .clickable(enabled = false) {}
                                .padding(22.dp),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = PrivacyPledge.TITLE,
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = PrivacyPledge.PROMISE,
                                            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                                elevation = 1.dp,
                                            )
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "100% ON-DEVICE",
                                            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    PrivacyPledge.ITEMS.forEach { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clayCard(
                                                    shape = RoundedCornerShape(14.dp),
                                                    backgroundColor = if (isDark) Color(0xFF192C23) else Color(0xFFF3EFE6),
                                                    elevation = 1.dp,
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                        ) {
                                            Text(
                                                text = item.title,
                                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = item.description,
                                                color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF4A6054),
                                                fontSize = 11.5.sp,
                                                lineHeight = 16.5.sp,
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clayPill(
                                            backgroundColor = Color(0xFF2D6B52),
                                            elevation = 3.dp,
                                        )
                                        .clickable { activeDialog = null }
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "I UNDERSTAND",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsDialog.ABOUT_WIRD -> {
                AboutWirdDialog(
                    onOpenPrivacyPledge = { activeDialog = SettingsDialog.PRIVACY_PLEDGE },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.PAGE_PREVIEW -> {
                SamplePagePreviewDialog(
                    initialSize = ayahTextSize,
                    onApplySize = { size ->
                        ayahTextSize = size
                        store.ayahTextSize = size
                    },
                    onClose = { activeDialog = null },
                )
            }

            SettingsDialog.SCHEDULE_MODE -> {
                ClayOptionDialog(
                    title = "Track schedule mode",
                    options = listOf(
                        DialogOption(
                            TrackScheduleMode.AUTOMATIC,
                            "Automatic (Recommended)",
                            "Activates tracks automatically based on the day of the week (e.g. Mon–Thu for weekday madrasa, Sat–Sun for weekend madrasa).",
                        ),
                        DialogOption(
                            TrackScheduleMode.MANUAL,
                            "Manual",
                            "You select which track is active by tapping its tab on the home screen.",
                        ),
                    ),
                    selected = trackScheduleMode,
                    onSelect = { mode ->
                        trackScheduleMode = mode
                        store.trackScheduleMode = mode
                        activeSpace = store.activeSpace()
                        onLifeSpacesChanged()
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.LIFE_SPACE_MANAGER -> {
                LifeSpaceManagerDialog(
                    spaces = lifeSpaces,
                    activeSpaceId = activeSpace.id,
                    onSetActiveSpace = { spaceId ->
                        store.setActiveSpace(spaceId)
                        lifeSpaces = store.getLifeSpaces()
                        activeSpace = store.activeSpace()
                        onLifeSpacesChanged()
                    },
                    onToggleFreezeSpace = { spaceId, freeze ->
                        store.setSpaceFrozen(spaceId, freeze)
                        lifeSpaces = store.getLifeSpaces()
                        activeSpace = store.activeSpace()
                        onLifeSpacesChanged()
                    },
                    onAddSpace = { name, goal ->
                        store.addLifeSpace(name, goal)
                        lifeSpaces = store.getLifeSpaces()
                        activeSpace = store.activeSpace()
                        onLifeSpacesChanged()
                    },
                    onDeleteSpace = { spaceId ->
                        store.deleteLifeSpace(spaceId)
                        lifeSpaces = store.getLifeSpaces()
                        activeSpace = store.activeSpace()
                        onLifeSpacesChanged()
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.MANAGE_TRACKS -> {
                ManageTracksDialog(
                    space = activeSpace,
                    onEditTrack = { track ->
                        editingSpaceId = activeSpace.id
                        editingTrack = track
                        activeDialog = SettingsDialog.EDIT_TRACK
                    },
                    onAddNewTrack = {
                        editingSpaceId = activeSpace.id
                        editingTrack = ReadingTrack(
                            id = "track_${System.currentTimeMillis()}",
                            name = "New Reading Track",
                            type = TrackType.HIFZ,
                            activeDays = DayOfWeek.entries.toSet(),
                            positionUnit = 0,
                            direction = ReadingDirection.TOWARDS_NAS,
                            dailyUnits = 2,
                        )
                        activeDialog = SettingsDialog.EDIT_TRACK
                    },
                    onDeleteTrack = { trackId ->
                        store.deleteTrackFromSpace(activeSpace.id, trackId)
                        lifeSpaces = store.getLifeSpaces()
                        activeSpace = store.activeSpace()
                        onLifeSpacesChanged()
                    },
                    onDismiss = { activeDialog = null },
                )
            }

            SettingsDialog.EDIT_TRACK -> {
                val trackToEdit = editingTrack
                val spaceId = editingSpaceId ?: activeSpace.id
                if (trackToEdit != null) {
                    val currentSpace = lifeSpaces.firstOrNull { it.id == spaceId } ?: activeSpace
                    val isExisting = currentSpace.tracks.any { it.id == trackToEdit.id }
                    EditTrackDialog(
                        track = trackToEdit,
                        canDelete = isExisting && currentSpace.tracks.size > 1,
                        onSaveTrack = { updated ->
                            if (isExisting) {
                                store.updateTrack(updated)
                            } else {
                                store.addTrackToSpace(spaceId, updated)
                            }
                            lifeSpaces = store.getLifeSpaces()
                            activeSpace = store.activeSpace()
                            onLifeSpacesChanged()
                        },
                        onDeleteTrack = {
                            store.deleteTrackFromSpace(spaceId, trackToEdit.id)
                            lifeSpaces = store.getLifeSpaces()
                            activeSpace = store.activeSpace()
                            onLifeSpacesChanged()
                        },
                        onDismiss = {
                            editingTrack = null
                            editingSpaceId = null
                            activeDialog = null
                        },
                    )
                }
            }

            null -> Unit
        }
    }
}

// ============================================================================
// Tactile Claymorphism UI Components
// ============================================================================

@Composable
fun ClaySection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clayCard(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                    highlightColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.95f),
                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                    elevation = 4.dp,
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun ClaySettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = title,
                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.5.dp))
                Text(
                    text = subtitle,
                    color = if (isDark) Color(0xFF9CAFA4) else Color(0xFF6A7C73),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF557766) else Color(0xFF9CAFA4),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    if (showDivider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isDark) Color(0xFF1B2E24) else Color(0xFFF1ECE1)),
        )
    }
}

@Composable
fun ClaySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        label = "switchThumb",
    )
    val trackBg = if (!enabled) {
        Color(0xFF8C7D6B).copy(alpha = 0.3f)
    } else if (checked) {
        Color(0xFF2D6B52)
    } else {
        Color(0xFFD8D2C4)
    }

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackBg)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                .size(22.dp)
                .clayCard(
                    shape = CircleShape,
                    backgroundColor = Color.White,
                    highlightColor = Color.White.copy(alpha = 0.95f),
                    shadowColor = Color.Black.copy(alpha = 0.35f),
                    elevation = 2.dp,
                    strokeWidth = 0.5.dp,
                ),
        )
    }
}

@Composable
fun SliderSubBox(
    content: @Composable () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF0F1A15) else Color(0xFFF8F5EE))
            .border(1.dp, if (isDark) Color(0xFF1B2E24) else Color(0xFFEBE4D5), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun <T> ClayOptionDialog(
    title: String,
    options: List<DialogOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var currentChoice by remember(selected) { mutableStateOf(selected) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column {
                    Text(
                        text = title,
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(bottom = 16.dp, start = 2.dp),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { opt ->
                            val isSelected = opt.value == currentChoice
                            val itemBg = if (isSelected) {
                                if (isDark) Color(0xFF1E382B) else Color(0xFFE8F4EC)
                            } else {
                                if (isDark) Color(0xFF0F1A15) else Color(0xFFF8F5EE)
                            }
                            val borderColor = if (isSelected) {
                                Color(0xFF2D6B52)
                            } else {
                                if (isDark) Color(0xFF1B2E24) else Color(0xFFEBE4D5)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(itemBg)
                                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable {
                                        currentChoice = opt.value
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Radio circle
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) Color(0xFF2D6B52) else Color(0xFF9CAFA4),
                                            shape = CircleShape,
                                        )
                                        .background(if (isDark) Color(0xFF14221B) else Color.White, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF2D6B52), CircleShape),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = opt.title,
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (opt.description.isNotEmpty()) {
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = opt.description,
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        // Cancel button
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                    elevation = 2.dp,
                                )
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "CANCEL",
                                color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF556C60),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // Select button
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    backgroundColor = Color(0xFF2D6B52),
                                    elevation = 3.dp,
                                )
                                .clickable {
                                    onSelect(currentChoice)
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "SELECT",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Value formatting helpers
// ============================================================================

fun approxAudioSize(verses: Int): String {
    val mb = (verses * 0.17).coerceAtLeast(0.8)
    return String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
}

private fun amountLabel(units: Int): String = when {
    units == 1 -> "Half a page"
    units == 2 -> "One page"
    units == 4 -> "Two pages"
    units % 2 == 0 -> "${units / 2} pages"
    else -> "${units / 2}½ pages"
}

private enum class DailyTargetStage {
    SELECTION,
    CONFIRMATION,
}

@Composable
private fun DailyTargetDialog(
    currentUnits: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    var stage by remember { mutableStateOf(DailyTargetStage.SELECTION) }
    val isStandardPreset = currentUnits in listOf(1, 2, 4)
    var isCustom by remember { mutableStateOf(!isStandardPreset) }
    var selectedUnits by remember { mutableIntStateOf(currentUnits) }
    var pendingUnits by remember { mutableIntStateOf(currentUnits) }
    var customMode by remember { mutableStateOf("pages") } // "pages" or "verses"
    var customInputText by remember {
        mutableStateOf(
            if (!isStandardPreset) {
                if (currentUnits % 2 == 0) "${currentUnits / 2}" else "$currentUnits"
            } else "3"
        )
    }

    BackHandler(enabled = stage == DailyTargetStage.CONFIRMATION) {
        stage = DailyTargetStage.SELECTION
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                when (stage) {
                    DailyTargetStage.SELECTION -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Daily reading portion",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                            Text(
                                text = "Choose how much you read each day, or set a custom amount.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            // Presets list
                            val presets = listOf(
                                1 to ("Half a page" to "Light daily portion"),
                                2 to ("1 page" to "Recommended daily portion"),
                                4 to ("2 pages" to "Faster completion pace"),
                            )

                            presets.forEach { (units, textPair) ->
                                val (label, desc) = textPair
                                val isSelected = !isCustom && selectedUnits == units
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clayCard(
                                            shape = RoundedCornerShape(14.dp),
                                            backgroundColor = if (isSelected) {
                                                if (isDark) Color(0xFF1D3528) else Color(0xFFE8F1EC)
                                            } else {
                                                if (isDark) Color(0xFF18221D) else Color(0xFFF5F1E8)
                                            },
                                            elevation = if (isSelected) 2.dp else 1.dp,
                                            strokeWidth = if (isSelected) 1.5.dp else 1.dp,
                                        )
                                        .clickable {
                                            isCustom = false
                                            selectedUnits = units
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(
                                            text = label,
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = desc,
                                            color = colors.textSecondary,
                                            fontSize = 11.5.sp,
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = gold,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }

                            // Custom Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clayCard(
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = if (isCustom) {
                                            if (isDark) Color(0xFF1D3528) else Color(0xFFE8F1EC)
                                        } else {
                                            if (isDark) Color(0xFF18221D) else Color(0xFFF5F1E8)
                                        },
                                        elevation = if (isCustom) 2.dp else 1.dp,
                                        strokeWidth = if (isCustom) 1.5.dp else 1.dp,
                                    )
                                    .clickable {
                                        isCustom = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Custom",
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                        fontSize = 14.sp,
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Set your own number of pages or verses",
                                        color = colors.textSecondary,
                                        fontSize = 11.5.sp,
                                    )
                                }
                                if (isCustom) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = gold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            // Expanded Custom Input Area
                            if (isCustom) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clayCard(
                                            shape = RoundedCornerShape(14.dp),
                                            backgroundColor = if (isDark) Color(0xFF18221D) else Color(0xFFF5EFE3),
                                            elevation = 1.dp,
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clayPill(
                                                    backgroundColor = if (customMode == "pages") colors.accent else Color.Transparent,
                                                    elevation = if (customMode == "pages") 2.dp else 0.dp,
                                                )
                                                .clickable { customMode = "pages"; customInputText = "3" }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                text = "Pages",
                                                color = if (customMode == "pages") Color.White else colors.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clayPill(
                                                    backgroundColor = if (customMode == "verses") colors.accent else Color.Transparent,
                                                    elevation = if (customMode == "verses") 2.dp else 0.dp,
                                                )
                                                .clickable { customMode = "verses"; customInputText = "15" }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                text = "Verses",
                                                color = if (customMode == "verses") Color.White else colors.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(60.dp)
                                                .clayCard(
                                                    shape = RoundedCornerShape(8.dp),
                                                    backgroundColor = if (isDark) Color(0xFF0F1A14) else Color(0xFFFFFFFF),
                                                    elevation = 1.dp,
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            BasicTextField(
                                                value = customInputText,
                                                onValueChange = { customInputText = it.filter { ch -> ch.isDigit() }.take(3) },
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                                ),
                                                textStyle = TextStyle(
                                                    color = gold,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                ),
                                                singleLine = true,
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = customMode,
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                            elevation = 2.dp,
                                        )
                                        .clickable(onClick = onDismiss)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = colors.textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clayPill(
                                            backgroundColor = colors.accent,
                                            elevation = 3.dp,
                                        )
                                        .clickable {
                                            val calculatedUnits = if (!isCustom) {
                                                selectedUnits
                                            } else {
                                                val num = customInputText.toIntOrNull() ?: 1
                                                if (customMode == "pages") {
                                                    (num * 2).coerceIn(1, 40)
                                                } else {
                                                    when {
                                                        num <= 5 -> 1
                                                        num <= 10 -> 2
                                                        num <= 20 -> 4
                                                        else -> ((num / 10) * 2).coerceIn(1, 40)
                                                    }
                                                }
                                            }
                                            pendingUnits = calculatedUnits
                                            stage = DailyTargetStage.CONFIRMATION
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = "Continue",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DailyTargetStage.CONFIRMATION -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Confirm daily portion",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                            Text(
                                text = "Review your new recitation commitment before updating your habit plan.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            // Current vs New Comparison Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(18.dp),
                                        backgroundColor = if (isDark) Color(0xFF18221D) else Color(0xFFF5F1E8),
                                        elevation = 1.dp,
                                    )
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CURRENT",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = colors.textSecondary,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = amountLabel(currentUnits),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Changes to",
                                    tint = gold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(20.dp),
                                )

                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    Text(
                                        text = "NEW PORTION",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = amountLabel(pendingUnits),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gold,
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Pace & Habit Projection Card
                            val pagesPerDay = pendingUnits / 2.0
                            val estDays = kotlin.math.max(1, (Mushaf.PAGES / pagesPerDay).toInt())
                            val estMonths = kotlin.math.max(1, kotlin.math.round(estDays / 30.4).toInt())

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFEAF2ED),
                                        elevation = 1.dp,
                                    )
                                    .padding(14.dp),
                            ) {
                                Text(
                                    text = "HABIT PROJECTION",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (pendingUnits == currentUnits) {
                                        "You selected your active daily portion. Your recitation schedule and pace will continue without changes."
                                    } else if (estDays <= 365) {
                                        "At this pace, completing one full Khatmah (all 604 pages) takes approximately $estDays days (~$estMonths months)."
                                    } else {
                                        val years = String.format(java.util.Locale.US, "%.1f", estDays / 365.25)
                                        "At this pace, completing one full Khatmah (all 604 pages) takes approximately $years years (~$estDays days)."
                                    },
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Streak, bookmarks, and completed records stay intact.",
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary,
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            // Bottom Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                            elevation = 2.dp,
                                        )
                                        .clickable { stage = DailyTargetStage.SELECTION }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "Back",
                                            color = colors.textSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .clayPill(
                                            backgroundColor = colors.accent,
                                            elevation = 3.dp,
                                        )
                                        .clickable {
                                            onSelect(pendingUnits)
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "Confirm Portion",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
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
}

@Composable
private fun WeeklyScheduleDialog(
    plan: ReadingPlan,
    onPlan: (ReadingPlan) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val initialActive = remember(plan) {
        val active = DayOfWeek.entries.filter { plan.unitsOn(it) == plan.defaultUnits }.toSet()
        if (active.isEmpty()) DayOfWeek.entries.toSet() else active
    }
    var activeDays by remember { mutableStateOf(initialActive) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column {
                    Text(
                        text = "Weekly reading schedule",
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Select the days you read your full target (${amountLabel(plan.defaultUnits).lowercase()}). Unselected days receive a lighter half-page target so your habit stays steady without feeling overwhelmed.",
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    )

                    Spacer(Modifier.height(18.dp))

                    // 7 Days chips in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val days = listOf(
                            DayOfWeek.MONDAY to "M",
                            DayOfWeek.TUESDAY to "T",
                            DayOfWeek.WEDNESDAY to "W",
                            DayOfWeek.THURSDAY to "T",
                            DayOfWeek.FRIDAY to "F",
                            DayOfWeek.SATURDAY to "S",
                            DayOfWeek.SUNDAY to "S",
                        )
                        days.forEach { (day, letter) ->
                            val isSelected = day in activeDays
                            val chipBg = if (isSelected) {
                                Color(0xFF2D6B52)
                            } else {
                                if (isDark) Color(0xFF1B2E24) else Color(0xFFEDE8DD)
                            }
                            val chipFg = if (isSelected) {
                                Color.White
                            } else {
                                if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73)
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(chipBg)
                                    .clickable {
                                        activeDays = if (isSelected) {
                                            if (activeDays.size > 1) activeDays - day else activeDays
                                        } else {
                                            activeDays + day
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = letter,
                                    color = chipFg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val allDays = DayOfWeek.entries.toSet()
                        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

                        PresetPill(
                            title = "All 7 Days",
                            selected = activeDays == allDays,
                            isDark = isDark,
                            onClick = { activeDays = allDays },
                            modifier = Modifier.weight(1f),
                        )
                        PresetPill(
                            title = "Weekdays",
                            selected = activeDays == weekdays,
                            isDark = isDark,
                            onClick = { activeDays = weekdays },
                            modifier = Modifier.weight(1f),
                        )
                        PresetPill(
                            title = "Weekends",
                            selected = activeDays == weekends,
                            isDark = isDark,
                            onClick = { activeDays = weekends },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF192C23) else Color(0xFFF3EFE6))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        val lightDaysCount = 7 - activeDays.size
                        val summaryText = if (lightDaysCount == 0) {
                            "Full target (${amountLabel(plan.defaultUnits).lowercase()}) every single day."
                        } else {
                            "${activeDays.size} full days (${amountLabel(plan.defaultUnits).lowercase()}) · $lightDaysCount lighter days (half page)."
                        }
                        Text(
                            text = summaryText,
                            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Cancel & Save buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CANCEL",
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    backgroundColor = Color(0xFF2D6B52),
                                    elevation = 3.dp,
                                )
                                .clickable {
                                    val newWeekdayUnits = if (activeDays.size == 7) {
                                        emptyMap()
                                    } else {
                                        (DayOfWeek.entries - activeDays).associateWith { 1 }
                                    }
                                    onPlan(plan.copy(weekdayUnits = newWeekdayUnits))
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "SAVE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetPill(
    title: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clayPill(
                backgroundColor = if (selected) Color(0xFF2D6B52) else (if (isDark) Color(0xFF1B2E24) else Color(0xFFEDE8DD)),
                elevation = if (selected) 2.dp else 1.dp,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else (if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73)),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AboutWirdDialog(
    onOpenPrivacyPledge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wird",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Version 1.0 · Daily Quran Companion",
                                color = Color(0xFF2D6B52),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                    elevation = 1.dp,
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "OFFLINE",
                                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Wird is built for quiet, daily consistency with the Noble Qur'an. No streaks that shame you, no ads, no analytics, and no accounts. Your progress stays 100% on your device.",
                        color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF4A6054),
                        fontSize = 13.sp,
                        lineHeight = 18.5.sp,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Features:",
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        "Madinah 15-line Mushaf with authentic QCF typography",
                        "Custom daily targets and flexible weekly schedules",
                        "Offline audio recitation with multiple renowned reciters",
                        "On-device Whisper AI speech verification",
                        "Zero telemetry, zero ads, zero accounts",
                    ).forEach { feat ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "• ",
                                color = Color(0xFF2D6B52),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = feat,
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF556C60),
                                fontSize = 12.5.sp,
                                lineHeight = 16.5.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPill(
                                    backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                    elevation = 2.dp,
                                )
                                .clickable {
                                    onOpenPrivacyPledge()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "PRIVACY PLEDGE",
                                color = if (isDark) Color(0xFF93DB7A) else Color(0xFF2D6B52),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPill(
                                    backgroundColor = Color(0xFF2D6B52),
                                    elevation = 3.dp,
                                )
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "CLOSE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun weeklyScheduleLabel(plan: ReadingPlan): String {
    val activeDays = DayOfWeek.entries.filter { plan.unitsOn(it) == plan.defaultUnits }
    if (activeDays.size == 7 || plan.weekdayUnits.isEmpty()) {
        return "Every day (${amountLabel(plan.defaultUnits).lowercase()})"
    }
    if (activeDays.isEmpty()) {
        return "Half a page every day"
    }
    val dayNames = activeDays.map { day ->
        when (day) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }
    val daysStr = when {
        activeDays.size == 5 && activeDays.containsAll(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) -> "Weekdays"
        activeDays.size == 2 && activeDays.containsAll(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) -> "Weekends"
        else -> dayNames.joinToString(", ")
    }
    return "$daysStr (${amountLabel(plan.defaultUnits).lowercase()}) · Light on others"
}

private fun themeLabel(theme: ThemeMode): String = when (theme) {
    ThemeMode.LIGHT -> "Warm Cream"
    ThemeMode.DARK -> "Night Dark"
    ThemeMode.SYSTEM -> "Match phone"
}

private fun modeLabel(mode: ReadingMode): String = when (mode) {
    ReadingMode.READING -> "From the mushaf"
    ReadingMode.MEMORISING -> "From memory"
}

private fun directionLabel(direction: ReadingDirection): String = when (direction) {
    ReadingDirection.TOWARDS_FATIHAH -> "Towards Al-Fatihah (Upwards)"
    ReadingDirection.TOWARDS_NAS -> "Towards An-Nas (Downwards)"
}

private fun megabytes(bytes: Long): String =
    if (bytes < 1024L * 1024L) "${bytes / 1024} KB"
    else String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1024.0 / 1024.0)

private fun dataLabel(d: DataOnDevice?): String {
    if (d == null) return "Checking…"
    if (d.days == 0 && d.recordings == 0) return "Nothing recorded yet"
    val days = if (d.days == 1) "1 day" else "${d.days} days"
    if (d.recordings == 0) return days
    val mb = d.recordingBytes / 1024.0 / 1024.0
    val size = if (mb < 1) "${d.recordingBytes / 1024} KB" else String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    val recs = if (d.recordings == 1) "1 recording" else "${d.recordings} recordings"
    return "$days · $recs · $size"
}

fun positionLabelFor(startVerse: Pair<Int, Int>?, page: Int): String {
    val surah = startVerse?.let { SurahIndex.byNumber(it.first) }
        ?: SurahIndex.on(page).firstOrNull()
    val name = surah?.name ?: "Page $page"
    return when {
        startVerse != null && surah != null -> "$name ${startVerse.second}, page $page"
        else -> "$name, page $page"
    }
}

fun clampPage(page: Int): Int = page.coerceIn(1, Mushaf.PAGES)

package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * First run, in three steps.
 *
 * Asks the question a person can answer. Nobody knows they are on page 453; most people
 * do not know the ayah number either. But everybody recognises the place when they see
 * it — so the mushaf step shows the page and you tap where you are.
 *
 * The ayah number box stays alongside, at Mutalib's request: typing is faster on the days
 * you do happen to know it.
 *
 * **The name comes first, and it is skippable** — PROFILE.md § 5j. It is first because the
 * greeting is the first thing Home renders, so asking later would mean one launch that
 * greets you as nobody. It is skippable because Sacred Rule 1 means this app has no
 * accounts and never should feel like it does; the Skip is as prominent as the answer.
 */
@Composable
fun SetupScreen(
    onDone: (
        page: Int,
        unitsPerDay: Int,
        startVerse: Pair<Int, Int>?,
        name: String?,
        mode: ReadingMode,
    ) -> Unit,
) {
    var name by remember { mutableStateOf<String?>(null) }
    var askedName by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf<ReadingMode?>(null) }
    var chosen by remember { mutableStateOf<Surah?>(null) }
    val surah = chosen
    val picked = mode

    when {
        !askedName -> YourName(
            onContinue = {
                name = it
                askedName = true
            },
        )

        picked == null -> HowYouRead { mode = it }

        surah == null -> ChooseSurah { chosen = it }

        else -> FindYourPlace(
            surah = surah,
            onBack = { chosen = null },
            onDone = { page, units, verse -> onDone(page, units, verse, name, picked) },
        )
    }
}

/**
 * "How do you read?"
 *
 * **PROFILE.md § 5l.** Mutalib's ask, in his words: *"we ask if u are memorizing the quran or
 * u are reading, cos some people memorize and some to look in the mushaf to read."*
 *
 * Two cards rather than a toggle, because a toggle needs a label that names one side as the
 * default and this question has no default worth implying. Each card says what the app will
 * do differently, so the choice is answerable without having used the app yet.
 *
 * ⚠ **It is honest about being small today.** The line underneath says the setting can be
 * changed later, which is true and is the right thing to say when the larger consequence —
 * checking a recitation against memory rather than against the page, PLAN task 14 — has not
 * been built. Overselling it here would be a promise the app cannot keep.
 */
@Composable
private fun HowYouRead(onPick: (ReadingMode) -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space6, vertical = Scale.space4),
    ) {
        Spacer(Modifier.height(Scale.space8))
        Text(
            "How do you read?",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.display),
        )
        Spacer(Modifier.height(Scale.space2))
        Text(
            text = "So Wird uses the right words for what you're doing.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )

        Spacer(Modifier.height(Scale.space6))
        ModeCard(
            title = "From the mushaf",
            detail = "You read the page in front of you.",
            onClick = { onPick(ReadingMode.READING) },
        )
        Spacer(Modifier.height(Scale.space3))
        ModeCard(
            title = "From memory",
            detail = "You're memorising, and the page is there to check yourself.",
            onClick = { onPick(ReadingMode.MEMORISING) },
        )

        Spacer(Modifier.height(Scale.space4))
        Text(
            text = "You can change this later in More.",
            color = colors.textOutsidePortion,
            style = TextStyle(fontSize = Scale.caption),
        )
    }
}

@Composable
private fun ModeCard(title: String, detail: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Scale.radius))
            .border(1.dp, colors.ornament.copy(alpha = 0.45f), RoundedCornerShape(Scale.radius))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(Scale.space4),
    ) {
        Text(title, color = colors.textPrimary, style = TextStyle(fontSize = Scale.title))
        Spacer(Modifier.height(Scale.space1))
        Text(detail, color = colors.textSecondary, style = TextStyle(fontSize = Scale.caption))
    }
}

/**
 * "What should I call you?"
 *
 * One field, one reassurance, and two ways out that both work. The reassurance is not
 * decoration — a text box asking for your name is the shape of a signup form, and this app
 * has no accounts at all, so the sentence has to say so before someone assumes otherwise.
 *
 * Skip is a full-width control rather than a grey word in a corner, because a Skip nobody
 * can find is not a choice.
 */
@Composable
private fun YourName(onContinue: (String?) -> Unit) {
    val colors = LocalWirdColors.current
    var typed by remember { mutableStateOf("") }
    val name = typed.trim().takeIf { it.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space6, vertical = Scale.space4),
    ) {
        Spacer(Modifier.height(Scale.space8))
        Text(
            "What should I call you?",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.display),
        )
        Spacer(Modifier.height(Scale.space2))
        Text(
            // True, and the reason the field is safe to show at all. Sacred Rule 1.
            text = "Only to greet you. It stays on this phone.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )

        Spacer(Modifier.height(Scale.space6))
        OutlinedTextField(
            value = typed,
            // A name, not a form field: no validation, no minimum, nothing rejected. The
            // cap is there so a pasted paragraph cannot break the greeting's layout.
            onValueChange = { typed = it.take(24) },
            label = { Text("Your name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
            colors = wirdFieldColors(),
        )

        Spacer(Modifier.height(Scale.space6))
        Button(
            onClick = { onContinue(name) },
            enabled = name != null,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.surface,
            ),
        ) {
            Text("Continue")
        }

        Spacer(Modifier.height(Scale.space2))
        TextButton(
            onClick = { onContinue(null) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
        ) {
            Text("Skip", color = colors.textSecondary, style = TextStyle(fontSize = Scale.body))
        }
    }
}

@Composable
private fun ChooseSurah(onPick: (Surah) -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space6, vertical = Scale.space4),
    ) {
        Text("Where are you?", color = colors.textPrimary, style = TextStyle(fontSize = Scale.display))
        Spacer(Modifier.height(Scale.space2))
        Text(
            text = "The surah you're reading now.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )
        Spacer(Modifier.height(Scale.space3))
        SurahList(onPick = onPick)
    }
}

@Composable
private fun FindYourPlace(
    surah: Surah,
    onBack: () -> Unit,
    onDone: (page: Int, unitsPerDay: Int, startVerse: Pair<Int, Int>?) -> Unit,
) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }

    var currentPage by remember { mutableIntStateOf(surah.firstPage) }
    var picked by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pickedPage by remember { mutableIntStateOf(surah.firstPage) }
    var ayahText by remember { mutableStateOf("") }
    var units by remember { mutableIntStateOf(Mushaf.UNITS_PER_PAGE) }
    var ayahCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(surah) { ayahCount = repo.ayahCount(surah.number) }

    // Typing a number is the other route to the same answer.
    val typed = ayahText.toIntOrNull()
    val typedOutOfRange = typed != null && ayahCount != null && typed !in 1..ayahCount!!
    LaunchedEffect(typed) {
        if (typed != null && !typedOutOfRange) {
            repo.pageOfVerse(surah.number, typed)?.let { page ->
                picked = surah.number to typed
                pickedPage = page
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Scale.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(surah.name, color = colors.textPrimary, style = TextStyle(fontSize = Scale.title))
                Text(
                    text = "Tap the ayah you're on",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            TextButton(onClick = onBack, modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget)) {
                Text("Change surah", color = colors.textSecondary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            MushafPager(
                initialPage = pickedPage,
                onPageChanged = { currentPage = it },
                // Nothing is dimmed here: you are looking for a place, not reading a
                // portion, and half a greyed-out page would just be harder to search.
                lit = { it.lines.toSet() },
                onWordTap = { verseKey ->
                    val s = verseKey.substringBefore(':').toIntOrNull()
                    val a = verseKey.substringAfter(':').toIntOrNull()
                    if (s != null && a != null) {
                        picked = s to a
                        pickedPage = currentPage
                        ayahText = if (s == surah.number) a.toString() else ""
                    }
                },
            )
        }

        Column(modifier = Modifier.padding(horizontal = Scale.space4, vertical = Scale.space3)) {
            val p = picked
            Text(
                text = when {
                    typedOutOfRange -> "${surah.name} has $ayahCount ayahs."
                    p != null -> "Starting at ${SurahIndex.byNumber(p.first)?.name ?: ""} ${p.second}, page $pickedPage."
                    else -> "Swipe to find your place, then tap the ayah."
                },
                color = if (typedOutOfRange) colors.textPrimary else colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )

            Spacer(Modifier.height(Scale.space2))
            OutlinedTextField(
                value = ayahText,
                onValueChange = { ayahText = it.filter(Char::isDigit).take(3) },
                label = { Text("Or type the ayah number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = typedOutOfRange,
                modifier = Modifier.fillMaxWidth(),
                colors = wirdFieldColors(),
            )

            Spacer(Modifier.height(Scale.space3))
            Text("How much a day?", color = colors.textPrimary, style = TextStyle(fontSize = Scale.body))
            Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
                AmountChoice("Half a page", 1, units) { units = it }
                AmountChoice("One page", 2, units) { units = it }
                AmountChoice("Two pages", 4, units) { units = it }
            }

            Spacer(Modifier.height(Scale.space3))
            Button(
                onClick = { p?.let { onDone(pickedPage, units, it) } },
                enabled = p != null,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.surface,
                ),
            ) {
                Text(
                    if (p == null) {
                        "Pick where you are"
                    } else {
                        "Start here"
                    }
                )
            }
        }
    }
}

/**
 * Selection is shown by a filled ground, not by colour alone.
 *
 * The first version dimmed the unselected labels to slate — 3.72:1 on paper, under the
 * 4.5:1 floor for text this size, so the options you had not picked were the hard ones to
 * read. Colouring the selected one differently does not work either: this palette is a
 * value ramp, so ink and deep teal sit 1.78:1 apart and read as the same colour.
 *
 * So both labels stay legible — deep teal at 9.37:1 unselected, ink on sage at 9.90:1
 * selected — and the *ground* carries the state. Which is the better pattern regardless:
 * colour should never be the only signal.
 */
@Composable
private fun AmountChoice(label: String, value: Int, current: Int, onPick: (Int) -> Unit) {
    val colors = LocalWirdColors.current
    val isOn = value == current
    TextButton(
        onClick = { onPick(value) },
        modifier = Modifier
            .defaultMinSize(minHeight = Scale.minTarget)
            .clip(RoundedCornerShape(Scale.radius))
            .background(if (isOn) colors.done else Color.Transparent)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            color = if (isOn) colors.onSurfaceRaised else colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}

package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors

/**
 * About Wird screen in the new look (PROFILE § 5bj, PLAN task 28).
 * Matching prototype/index.html § About and reference images.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(14.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WirdIcons.Back,
                    contentDescription = "Back",
                    tint = colors.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "About Wird",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
            )
        }

        Spacer(Modifier.height(18.dp))

        // Card 1: Version & Mission
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Version 0.1",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Wird helps you keep a daily portion of the Qur'an. Your days, tracks and recordings stay on this phone.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.ink2,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Card 2: Qur'an Text & Translations
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Qur'an text and translations",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "From Quran.com, bundled in the app so they open offline.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "English: Saheeh International. Hausa: Abubakar Mahmoud Gumi. Mushaf pages download from Quran.com when you first open them.",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = colors.ink2.copy(alpha = 0.85f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Card 3: Header Photos
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Header photos",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "The Ghana National Mosque, Accra.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Day photo: Amuzujoe. Sunset photo: Warmglow. Both under CC BY-SA 4.0, cropped and resized for the app.",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = colors.ink2.copy(alpha = 0.85f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Brand footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = WirdIcons.Leaf,
                contentDescription = null,
                tint = colors.action,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Wird",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
            )
            Text(
                text = "Your Qur'an. Your Journey.",
                fontSize = 12.sp,
                color = colors.ink2,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors

/**
 * Main Settings screen in the new look (PROFILE § 5bj, PLAN task 28).
 * Matches Screen 9 ("Settings" / "More") in the design brief and prototype/index.html.
 */
@Composable
fun NewLookSettingsMain(
    onBack: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = "Version 0.1",
    displayContent: @Composable (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.field)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(14.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
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
                text = "Settings",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Your Preferences",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.ink,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )

        // 5 Grouped Preference Rows
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                // 1. Reading Preferences
                NewLookSettingRow(
                    icon = WirdIcons.Sheet,
                    title = "Reading Preferences",
                    subtitle = "Font, translation, text size",
                    onClick = onOpenReading,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))

                // 2. Notifications
                NewLookSettingRow(
                    icon = WirdIcons.Bell,
                    title = "Notifications",
                    subtitle = "Reminders and alerts",
                    onClick = onOpenNotifications,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))

                // 3. Audio & Downloads
                NewLookSettingRow(
                    icon = WirdIcons.Download,
                    title = "Audio & Downloads",
                    subtitle = "Reciters, downloads, storage",
                    onClick = onOpenAudio,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))

                // 4. Data & Privacy
                NewLookSettingRow(
                    icon = WirdIcons.Shield,
                    title = "Data & Privacy",
                    subtitle = "Export, backup, privacy",
                    onClick = onOpenPrivacy,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))

                // 5. About Wird
                NewLookSettingRow(
                    icon = WirdIcons.Info,
                    title = "About Wird",
                    subtitle = "App version, sources",
                    onClick = onOpenAbout,
                )
            }
        }

        if (displayContent != null) {
            Spacer(Modifier.height(20.dp))
            displayContent()
        }

        Spacer(Modifier.height(36.dp))

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
                text = versionName,
                fontSize = 12.sp,
                color = colors.ink2,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun NewLookSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(colors.disc, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.action,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.ink2,
            )
        }
        Icon(
            imageVector = WirdIcons.ChevronRight,
            contentDescription = "Open",
            tint = colors.ink2,
            modifier = Modifier.size(18.dp),
        )
    }
}

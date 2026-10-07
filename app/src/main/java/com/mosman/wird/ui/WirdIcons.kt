package com.mosman.wird.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Clean vector iconography for Wird's new look.
 *
 * Drawn on a 24x24 grid with Lucide-style 1.8dp strokes and rounded caps/joins.
 * Vector icons are strictly used for buttons, dialogs, empty states and indicators.
 * Raw emojis are never used in UI controls (Sacred Rule 6).
 */
object WirdIcons {
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val ChevronRight = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val ChevronDown = Icons.Default.KeyboardArrowDown
    val Check = Icons.Default.Check
    val Close = Icons.Default.Close
    /** Search icon (authentic Lucide search vector). */
    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19f, 11f)
                curveTo(19f, 15.42f, 15.42f, 19f, 11f, 19f)
                curveTo(6.58f, 19f, 3f, 15.42f, 3f, 11f)
                curveTo(3f, 6.58f, 6.58f, 3f, 11f, 3f)
                curveTo(15.42f, 3f, 19f, 6.58f, 19f, 11f)
                close()
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()
    }

    /** Leaf icon for the Wird brand mark and greeting. */
    val Leaf: ImageVector by lazy {
        ImageVector.Builder(
            name = "Leaf",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Central leaf blade
                moveTo(12f, 3f)
                curveTo(14.6f, 5.3f, 15.6f, 8.2f, 15f, 11.6f)
                curveTo(14.6f, 13.8f, 13.6f, 15.5f, 12f, 16.8f)
                curveTo(10.4f, 15.5f, 9.4f, 13.8f, 9f, 11.6f)
                curveTo(8.4f, 8.2f, 9.4f, 5.3f, 12f, 3f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.72f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Left wing
                moveTo(4f, 9.5f)
                curveTo(7.1f, 9.7f, 9.3f, 11.1f, 10.6f, 13.8f)
                curveTo(11.2f, 15.2f, 11.4f, 16.6f, 11.2f, 18f)
                curveTo(9.3f, 17.9f, 7.7f, 17.2f, 6.4f, 15.9f)
                curveTo(4.9f, 14.4f, 4.1f, 12.3f, 4f, 9.5f)
                close()
                // Right wing
                moveTo(20f, 9.5f)
                curveTo(19.9f, 12.3f, 19.1f, 14.4f, 17.6f, 15.9f)
                curveTo(16.3f, 17.2f, 14.7f, 17.9f, 12.8f, 18f)
                curveTo(12.6f, 16.6f, 12.8f, 15.2f, 13.4f, 13.8f)
                curveTo(14.7f, 11.1f, 16.9f, 9.7f, 20f, 9.5f)
                close()
            }
        }.build()
    }

    /** Home tab icon. */
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 10.5f)
                lineTo(12f, 4f)
                lineTo(20f, 10.5f)
                verticalLineTo(20f)
                horizontalLineTo(15f)
                verticalLineTo(14.5f)
                horizontalLineTo(9f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
            }
        }.build()
    }

    /** Open book icon for Qur'an tab. */
    val Quran: ImageVector by lazy {
        ImageVector.Builder(
            name = "Quran",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Left page
                moveTo(3f, 5.5f)
                curveTo(6f, 4.2f, 9f, 4.2f, 12f, 5.5f)
                verticalLineTo(18.5f)
                curveTo(9f, 17.2f, 6f, 17.2f, 3f, 18.5f)
                close()
                // Right page
                moveTo(21f, 5.5f)
                curveTo(18f, 4.2f, 15f, 4.2f, 12f, 5.5f)
                verticalLineTo(18.5f)
                curveTo(15f, 17.2f, 18f, 17.2f, 21f, 18.5f)
                close()
            }
        }.build()
    }

    /** Chat bubble icon for Companion tab. */
    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 5f)
                horizontalLineTo(20f)
                verticalLineTo(16f)
                horizontalLineTo(9f)
                lineTo(4f, 20f)
                close()
                moveTo(8.5f, 10.5f)
                horizontalLineTo(15.5f)
            }
        }.build()
    }

    /** Settings gear icon (authentic Lucide settings cog). */
    val Gear: ImageVector by lazy {
        ImageVector.Builder(
            name = "Gear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12.22f, 2f)
                lineTo(11.78f, 2f)
                curveTo(10.68f, 2f, 9.78f, 2.9f, 9.78f, 4f)
                verticalLineTo(4.18f)
                curveTo(9.78f, 4.88f, 9.38f, 5.52f, 8.78f, 5.91f)
                lineTo(8.35f, 6.16f)
                curveTo(7.75f, 6.55f, 6.95f, 6.55f, 6.35f, 6.16f)
                lineTo(6.2f, 6.08f)
                curveTo(5.24f, 5.53f, 4.02f, 5.86f, 3.47f, 6.81f)
                lineTo(3.25f, 7.19f)
                curveTo(2.7f, 8.15f, 3.03f, 9.37f, 3.98f, 9.92f)
                lineTo(4.13f, 10.02f)
                curveTo(4.73f, 10.41f, 5.13f, 11.05f, 5.13f, 11.75f)
                verticalLineTo(12.26f)
                curveTo(5.13f, 12.96f, 4.73f, 13.6f, 4.13f, 13.99f)
                lineTo(3.98f, 14.08f)
                curveTo(3.03f, 14.63f, 2.7f, 15.85f, 3.25f, 16.81f)
                lineTo(3.47f, 17.19f)
                curveTo(4.02f, 18.14f, 5.24f, 18.47f, 6.2f, 17.92f)
                lineTo(6.35f, 17.84f)
                curveTo(6.95f, 17.45f, 7.75f, 17.45f, 8.35f, 17.84f)
                lineTo(8.78f, 18.09f)
                curveTo(9.38f, 18.48f, 9.78f, 19.12f, 9.78f, 19.82f)
                verticalLineTo(20f)
                curveTo(9.78f, 21.1f, 10.68f, 22f, 11.78f, 22f)
                lineTo(12.22f, 22f)
                curveTo(13.32f, 22f, 14.22f, 21.1f, 14.22f, 20f)
                verticalLineTo(19.82f)
                curveTo(14.22f, 19.12f, 14.62f, 18.48f, 15.22f, 18.09f)
                lineTo(15.65f, 17.84f)
                curveTo(16.25f, 17.45f, 17.05f, 17.45f, 17.65f, 17.84f)
                lineTo(17.8f, 17.92f)
                curveTo(18.76f, 18.47f, 19.98f, 18.14f, 20.53f, 17.19f)
                lineTo(20.75f, 16.8f)
                curveTo(21.3f, 15.85f, 20.97f, 14.63f, 20.02f, 14.08f)
                lineTo(19.87f, 14f)
                curveTo(19.27f, 13.61f, 18.87f, 12.97f, 18.87f, 12.27f)
                verticalLineTo(11.77f)
                curveTo(18.87f, 11.07f, 19.27f, 10.43f, 19.87f, 10.04f)
                lineTo(20.02f, 9.95f)
                curveTo(20.97f, 9.4f, 21.3f, 8.18f, 20.75f, 7.22f)
                lineTo(20.53f, 6.84f)
                curveTo(19.98f, 5.89f, 18.76f, 5.56f, 17.8f, 6.11f)
                lineTo(17.65f, 6.19f)
                curveTo(17.05f, 6.58f, 16.25f, 6.58f, 15.65f, 6.19f)
                lineTo(15.22f, 5.94f)
                curveTo(14.62f, 5.55f, 14.22f, 4.91f, 14.22f, 4.21f)
                verticalLineTo(4f)
                curveTo(14.22f, 2.9f, 13.32f, 2f, 12.22f, 2f)
                close()

                // Center circle
                moveTo(15f, 12f)
                curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
                curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
                curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
                curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
                close()
            }
        }.build()
    }

    /** Flame icon for streaks. */
    val Flame: ImageVector by lazy {
        ImageVector.Builder(
            name = "Flame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8.5f, 14.5f)
                curveTo(8.5f, 13.12f, 9.62f, 12f, 11f, 12f)
                curveTo(11f, 10.62f, 10.5f, 10f, 10f, 9f)
                curveTo(8.93f, 6.86f, 9.78f, 4.95f, 12f, 3f)
                curveTo(12.5f, 5.5f, 14f, 7.9f, 16f, 9.5f)
                curveTo(18f, 11.1f, 19f, 13f, 19f, 15f)
                curveTo(19f, 18.87f, 15.87f, 22f, 12f, 22f)
                curveTo(8.13f, 22f, 5f, 18.87f, 5f, 15f)
                curveTo(5f, 13.85f, 5.43f, 12.71f, 6f, 12f)
                curveTo(7.1f, 13.5f, 8.5f, 14.5f, 8.5f, 14.5f)
                close()
            }
        }.build()
    }

    /** Compass icon for Your Journey. */
    val Compass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                close()
                moveTo(16.24f, 7.76f)
                lineTo(14.12f, 14.12f)
                lineTo(7.76f, 16.24f)
                lineTo(9.88f, 9.88f)
                close()
            }
        }.build()
    }

    /** Calendar icon for total days. */
    val Calendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Calendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 4f)
                horizontalLineTo(19f)
                curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
                verticalLineTo(20f)
                curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                verticalLineTo(6f)
                curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
                close()
                moveTo(16f, 2f)
                verticalLineTo(6f)
                moveTo(8f, 2f)
                verticalLineTo(6f)
                moveTo(3f, 10f)
                horizontalLineTo(21f)
            }
        }.build()
    }

    /** Award / badge icon for recite rate. */
    val Award: ImageVector by lazy {
        ImageVector.Builder(
            name = "Award",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 8f)
                curveTo(18f, 11.31f, 15.31f, 14f, 12f, 14f)
                curveTo(8.69f, 14f, 6f, 11.31f, 6f, 8f)
                curveTo(6f, 4.69f, 8.69f, 2f, 12f, 2f)
                curveTo(15.31f, 2f, 18f, 4.69f, 18f, 8f)
                close()
                moveTo(15.4f, 13.5f)
                lineTo(18f, 21f)
                lineTo(12f, 18.5f)
                lineTo(6f, 21f)
                lineTo(8.6f, 13.5f)
            }
        }.build()
    }

    /** Microphone icon for Recite & Review. */
    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                curveTo(10.34f, 3f, 9f, 4.34f, 9f, 6f)
                verticalLineTo(11f)
                curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
                curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
                verticalLineTo(6f)
                curveTo(15f, 4.34f, 13.66f, 3f, 12f, 3f)
                close()
                moveTo(5.5f, 11f)
                curveTo(5.5f, 14.59f, 8.41f, 17.5f, 12f, 17.5f)
                curveTo(15.59f, 17.5f, 18.5f, 14.59f, 18.5f, 11f)
                moveTo(12f, 17.5f)
                verticalLineTo(21f)
            }
        }.build()
    }

    /** Bookmark ribbon icon. */
    val Bookmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 3.5f)
                horizontalLineTo(17f)
                verticalLineTo(20.5f)
                lineTo(12f, 17f)
                lineTo(7f, 20.5f)
                close()
            }
        }.build()
    }

    /** History clock icon. */
    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 12f)
                curveTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
                curveTo(16.42f, 20f, 20f, 16.42f, 20f, 12f)
                curveTo(20f, 7.58f, 16.42f, 4f, 12f, 4f)
                curveTo(9.5f, 4f, 7.3f, 5.2f, 5.9f, 7f)
                moveTo(4f, 4f)
                verticalLineTo(7.5f)
                horizontalLineTo(7.5f)
                moveTo(12f, 8f)
                verticalLineTo(12.3f)
                lineTo(15f, 14.1f)
            }
        }.build()
    }

    /** List icon for Sūrahs. */
    val List: ImageVector by lazy {
        ImageVector.Builder(
            name = "List",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 7f)
                horizontalLineTo(20f)
                moveTo(9f, 12f)
                horizontalLineTo(20f)
                moveTo(9f, 17f)
                horizontalLineTo(20f)
                moveTo(4f, 7f)
                horizontalLineTo(5f)
                moveTo(4f, 12f)
                horizontalLineTo(5f)
                moveTo(4f, 17f)
                horizontalLineTo(5f)
            }
        }.build()
    }

    /** Layers icon for Juz'. */
    val Layers: ImageVector by lazy {
        ImageVector.Builder(
            name = "Layers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4f)
                lineTo(20.5f, 8.5f)
                lineTo(12f, 13f)
                lineTo(3.5f, 8.5f)
                close()
                moveTo(3.5f, 12.5f)
                lineTo(12f, 17f)
                lineTo(20.5f, 12.5f)
            }
        }.build()
    }

    /** Download arrow icon. */
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4f)
                verticalLineTo(15f)
                moveTo(7f, 10.5f)
                lineTo(12f, 15.5f)
                lineTo(17f, 10.5f)
                moveTo(5f, 20f)
                horizontalLineTo(19f)
            }
        }.build()
    }

    /** Send paper airplane icon. */
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 12f)
                lineTo(20f, 4.5f)
                lineTo(15.5f, 20f)
                lineTo(12.3f, 13.7f)
                close()
                moveTo(12.3f, 13.7f)
                lineTo(20f, 4.5f)
            }
        }.build()
    }

    /** Cloud icon. */
    val Cloud: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cloud",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 18.5f)
                curveTo(4.5f, 18.5f, 2.5f, 16.5f, 2.5f, 14f)
                curveTo(2.5f, 11.8f, 4.1f, 9.9f, 6.4f, 9.5f)
                curveTo(7.1f, 6.3f, 9.9f, 4f, 13.3f, 4f)
                curveTo(17.4f, 4f, 20.7f, 7.3f, 20.7f, 11.4f)
                curveTo(21.5f, 12.1f, 22f, 13.2f, 22f, 14.5f)
                curveTo(22f, 16.7f, 20.2f, 18.5f, 18f, 18.5f)
                close()
            }
        }.build()
    }

    /** Plus icon for creating tracks. */
    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }.build()
    }

    /** Notification bell icon. */
    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 16f)
                verticalLineTo(11f)
                curveTo(6f, 7.7f, 8.7f, 5f, 12f, 5f)
                curveTo(15.3f, 5f, 18f, 7.7f, 18f, 11f)
                verticalLineTo(16f)
                lineTo(19.5f, 18f)
                horizontalLineTo(4.5f)
                close()
                moveTo(10f, 20.5f)
                curveTo(10f, 21.6f, 10.9f, 22.5f, 12f, 22.5f)
                curveTo(13.1f, 22.5f, 14f, 21.6f, 14f, 20.5f)
            }
        }.build()
    }

    /** Shield icon for privacy. */
    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                lineTo(19.5f, 6f)
                verticalLineTo(11.5f)
                curveTo(19.5f, 16f, 16.3f, 19.5f, 12f, 21f)
                curveTo(7.7f, 19.5f, 4.5f, 16f, 4.5f, 11.5f)
                verticalLineTo(6f)
                close()
            }
        }.build()
    }

    /** Info icon. */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20.5f)
                curveTo(16.7f, 20.5f, 20.5f, 16.7f, 20.5f, 12f)
                curveTo(20.5f, 7.3f, 16.7f, 3.5f, 12f, 3.5f)
                curveTo(7.3f, 3.5f, 3.5f, 7.3f, 3.5f, 12f)
                curveTo(3.5f, 16.7f, 7.3f, 20.5f, 12f, 20.5f)
                close()
                moveTo(12f, 11f)
                verticalLineTo(16.5f)
                moveTo(12f, 7.6f)
                verticalLineTo(7.8f)
            }
        }.build()
    }

    /** Repeat / Revision icon. */
    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(17f, 3.5f)
                lineTo(20f, 6.5f)
                lineTo(17f, 9.5f)
                moveTo(4f, 11.5f)
                verticalLineTo(10.5f)
                curveTo(4f, 8.3f, 5.8f, 6.5f, 8f, 6.5f)
                horizontalLineTo(20f)
                moveTo(7f, 20.5f)
                lineTo(4f, 17.5f)
                lineTo(7f, 14.5f)
                moveTo(20f, 12.5f)
                verticalLineTo(13.5f)
                curveTo(20f, 15.7f, 18.2f, 17.5f, 16f, 17.5f)
                horizontalLineTo(4f)
            }
        }.build()
    }

    val Sheet: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sheet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(8f)
                close()
                moveTo(14f, 2f)
                verticalLineTo(8f)
                horizontalLineTo(20f)
            }
        }.build()
    }
}

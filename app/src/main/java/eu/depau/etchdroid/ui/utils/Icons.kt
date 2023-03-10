package eu.depau.etchdroid.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val cache = mutableMapOf<Pair<Long, Long>, ImageVector>()

// Generated with https://github.com/DevSrSouza/svg-to-compose

fun getEtchDroidIcon(headColor: Long = 0xFF87d98a, usbColor: Long = 0xFFffffff): ImageVector {
    if (cache.containsKey(headColor to usbColor))
        return cache[headColor to usbColor]!!

    val icon = Builder(
        name = "EtchDroid",
        defaultWidth = 108.0.dp,
        defaultHeight = 108.0.dp,
        viewportWidth = 108.0f,
        viewportHeight = 108.0f
    ).apply {
        path(
            fill = SolidColor(Color(usbColor)),
            stroke = null,
            strokeLineWidth = 0.999998f,
            strokeLineCap = Butt,
            strokeLineJoin = Miter,
            strokeLineMiter = 4.0f,
            pathFillType = NonZero
        ) {
            moveToRelative(23.48f, 56.93f)
            verticalLineToRelative(43.55f)
            horizontalLineToRelative(30.52f)
            horizontalLineToRelative(30.53f)
            verticalLineToRelative(-43.55f)
            horizontalLineToRelative(-30.53f)
            close()
            moveTo(61.01f, 74.68f)
            horizontalLineToRelative(15.29f)
            lineTo(76.3f, 85.62f)
            horizontalLineToRelative(-15.29f)
            close()
            moveTo(31.75f, 74.68f)
            horizontalLineToRelative(15.29f)
            lineTo(47.04f, 85.63f)
            horizontalLineToRelative(-15.29f)
            close()
        }
        path(
            fill = SolidColor(Color(headColor)),
            stroke = null,
            strokeLineWidth = 1.08851f,
            strokeLineCap = Butt,
            strokeLineJoin = Miter,
            strokeLineMiter = 4.0f,
            pathFillType = NonZero
        ) {
            moveToRelative(74.49f, 21.71f)
            lineToRelative(6.97f, -12.07f)
            curveToRelative(0.39f, -0.68f, 0.16f, -1.54f, -0.51f, -1.93f)
            curveToRelative(-0.67f, -0.39f, -1.54f, -0.16f, -1.92f, 0.51f)
            lineToRelative(-7.06f, 12.23f)
            curveToRelative(-5.39f, -2.46f, -11.45f, -3.83f, -17.97f, -3.83f)
            curveToRelative(-6.52f, 0.0f, -12.58f, 1.37f, -17.97f, 3.83f)
            lineToRelative(-7.06f, -12.23f)
            curveToRelative(-0.39f, -0.68f, -1.25f, -0.91f, -1.93f, -0.51f)
            curveToRelative(-0.68f, 0.39f, -0.91f, 1.25f, -0.51f, 1.93f)
            lineToRelative(6.97f, 12.07f)
            curveToRelative(-12.01f, 6.51f, -20.15f, 18.67f, -21.5f, 32.91f)
            horizontalLineToRelative(83.99f)
            curveToRelative(-1.34f, -14.24f, -9.48f, -26.4f, -21.51f, -32.91f)
            close()
            moveTo(34.72f, 42.82f)
            curveToRelative(-1.95f, 0.0f, -3.52f, -1.58f, -3.52f, -3.52f)
            curveToRelative(0.0f, -1.95f, 1.58f, -3.52f, 3.52f, -3.52f)
            curveToRelative(1.95f, 0.0f, 3.52f, 1.58f, 3.52f, 3.52f)
            curveToRelative(0.01f, 1.94f, -1.57f, 3.52f, -3.52f, 3.52f)
            close()
            moveTo(73.28f, 42.82f)
            curveToRelative(-1.95f, 0.0f, -3.52f, -1.58f, -3.52f, -3.52f)
            curveToRelative(0.0f, -1.95f, 1.58f, -3.52f, 3.52f, -3.52f)
            curveToRelative(1.95f, 0.0f, 3.52f, 1.58f, 3.52f, 3.52f)
            curveToRelative(0.01f, 1.94f, -1.57f, 3.52f, -3.52f, 3.52f)
            close()
        }
    }.build()

    cache[headColor to usbColor] = icon
    return icon
}

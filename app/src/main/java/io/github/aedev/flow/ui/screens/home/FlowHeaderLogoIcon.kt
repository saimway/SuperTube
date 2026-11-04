package io.github.aedev.flow.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.PathParser

// SVG path data — a single unbreakable token, so the line-length rule cannot be satisfied by wrapping.
@Suppress("ktlint:standard:max-line-length")
private const val SUPERTUBE_LOGO_PATH =
    "M449,157 H551 V258 H449 Z M347,157 A202,202 0 0,0 145,359 H246 A101,101 0 0,1 347,258 Z M145,461 H347 A102,102 0 0,1 449,359 H550 A203,203 0 0,0 347,562 H145 Z"

@Suppress("ktlint:standard:max-line-length")
private const val FLOW_INCOGNITO_GLYPH_PATH =
    "M17.06 13C15.2 13 13.64 14.33 13.24 16.1C12.29 15.69 11.42 15.8 10.76 16.09C10.35 14.31 8.79 13 6.94 13C4.77 13 3 14.79 3 17C3 19.21 4.77 21 6.94 21C9 21 10.68 19.38 10.84 17.32C11.18 17.08 12.07 16.63 13.16 17.34C13.34 19.39 15 21 17.06 21C19.23 21 21 19.21 21 17C21 14.79 19.23 13 17.06 13M6.94 19.86C5.38 19.86 4.13 18.58 4.13 17S5.39 14.14 6.94 14.14C8.5 14.14 9.75 15.42 9.75 17S8.5 19.86 6.94 19.86M17.06 19.86C15.5 19.86 14.25 18.58 14.25 17S15.5 14.14 17.06 14.14C18.62 14.14 19.88 15.42 19.88 17S18.61 19.86 17.06 19.86M22 10.5H2V12H22V10.5M15.53 2.63C15.31 2.14 14.75 1.88 14.22 2.05L12 2.79L9.77 2.05L9.72 2.04C9.19 1.89 8.63 2.17 8.43 2.68L6 9H18L15.56 2.68L15.53 2.63Z"

/** The Home logo and the wordmark beside it: a tap does what tapping the Home tab again does, a long-press switches Deep Flow. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.homeLogoClicks(
    clickLabel: String,
    longClickLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(onClickLabel = clickLabel, onLongClickLabel = longClickLabel, onClick = onClick, onLongClick = onLongClick)

@Composable
internal fun FlowHeaderLogoIcon(
    isDeepFlowActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val logoPath =
        remember {
            PathParser().parsePathString(SUPERTUBE_LOGO_PATH).toPath()
        }
    val incognitoPath =
        remember {
            PathParser().parsePathString(FLOW_INCOGNITO_GLYPH_PATH).toPath()
        }

    val logoAlpha by animateFloatAsState(
        targetValue = if (isDeepFlowActive) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "deepFlowGlyphAlpha",
    )
    val incognitoAlpha by animateFloatAsState(
        targetValue = if (isDeepFlowActive) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "deepFlowIncognitoAlpha",
    )

    Canvas(modifier = modifier) {
        if (logoAlpha > 0f) {
            val sx = size.width / 720f
            val sy = size.height / 720f
            drawContext.canvas.save()
            drawContext.canvas.scale(sx, sy)
            drawPath(path = logoPath, color = primaryColor.copy(alpha = logoAlpha))
            drawContext.canvas.restore()
        }
        if (incognitoAlpha > 0f) {
            val sx = size.width / 24f
            val sy = size.height / 24f
            drawContext.canvas.save()
            drawContext.canvas.scale(sx, sy)
            drawPath(path = incognitoPath, color = primaryColor.copy(alpha = incognitoAlpha))
            drawContext.canvas.restore()
        }
    }
}

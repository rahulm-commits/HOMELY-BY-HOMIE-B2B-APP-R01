package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class RouteComboPattern(val displayName: String) {
  SOLID("Solid"),
  SPLIT_HALF("Split Half"),
  RING_CORE("Ring & Core"),
  DIAGONAL_STRIPE("Diagonal Duo"),
  TOP_BOTTOM_DUO("Top & Bottom"),
}

data class RouteColorSpec(
  val index: Int,
  val primary: Color,
  val secondary: Color?,
  val pattern: RouteComboPattern,
  val label: String,
) {
  val isCombination: Boolean get() = secondary != null && pattern != RouteComboPattern.SOLID
  val primaryHex: String get() = primary.toCssHex()
  val secondaryHex: String? get() = secondary?.toCssHex()
  val contrastInk: Color get() = if (primary.luminanceApprox() > 0.52f) DeepSlateInk else Color.White
}

private fun Color.toCssHex(): String {
  val rgb = toArgb() and 0xFFFFFF
  return String.format("#%06X", rgb)
}

private fun Color.luminanceApprox(): Float = (0.299f * red) + (0.587f * green) + (0.114f * blue)

object RouteColorPalette {
  private val baseColors = listOf(
    "Forest Green" to ForestSecondary,   // #1F4E43
    "Spiced Saffron" to SaffronPrimary,  // #D95300
    "Cobalt Blue" to CobaltInfo,         // #2457A6
    "Crimson Red" to CoralDanger,        // #C6362C
    "Royal Plum" to Color(0xFF7C3AED),   // #7C3AED
  )

  private val comboStyles = listOf(
    RouteComboPattern.SPLIT_HALF,
    RouteComboPattern.RING_CORE,
    RouteComboPattern.DIAGONAL_STRIPE,
    RouteComboPattern.TOP_BOTTOM_DUO,
  )

  // Contrasting ordered color pairs prioritized for immediate visual separation
  // (e.g., Crimson Red + Forest Green, Forest Green + Spiced Saffron, Crimson Red + Cobalt Blue)
  private val comboPairs: List<Pair<Int, Int>> = listOf(
    3 to 0, // Crimson Red + Forest Green
    0 to 1, // Forest Green + Spiced Saffron
    3 to 2, // Crimson Red + Cobalt Blue
    2 to 1, // Cobalt Blue + Spiced Saffron
    0 to 4, // Forest Green + Royal Plum
    1 to 4, // Spiced Saffron + Royal Plum
    2 to 4, // Cobalt Blue + Royal Plum
    3 to 4, // Crimson Red + Royal Plum
    0 to 2, // Forest Green + Cobalt Blue
    1 to 3, // Spiced Saffron + Crimson Red
    0 to 3, // Forest Green + Crimson Red
    1 to 0, // Spiced Saffron + Forest Green
    2 to 3, // Cobalt Blue + Crimson Red
    4 to 0, // Royal Plum + Forest Green
    4 to 1, // Royal Plum + Spiced Saffron
    4 to 2, // Royal Plum + Cobalt Blue
    4 to 3, // Royal Plum + Crimson Red
    1 to 2, // Spiced Saffron + Cobalt Blue
    2 to 0, // Cobalt Blue + Forest Green
    3 to 1, // Crimson Red + Spiced Saffron
  )

  val solidCount: Int get() = baseColors.size

  val allPresets: List<RouteColorSpec> by lazy {
    val singles = baseColors.mapIndexed { idx, (name, color) ->
      RouteColorSpec(
        index = idx,
        primary = color,
        secondary = null,
        pattern = RouteComboPattern.SOLID,
        label = name,
      )
    }
    val combos = mutableListOf<RouteColorSpec>()
    var comboIdx = 0
    for (style in comboStyles) {
      for ((a, b) in comboPairs) {
        val (nameA, colA) = baseColors[a]
        val (nameB, colB) = baseColors[b]
        combos += RouteColorSpec(
          index = baseColors.size + comboIdx,
          primary = colA,
          secondary = colB,
          pattern = style,
          label = "$nameA + $nameB (${style.displayName})",
        )
        comboIdx++
      }
    }
    singles + combos
  }

  fun specForIndex(index: Int): RouteColorSpec {
    if (index in allPresets.indices) return allPresets[index]
    val comboCount = (allPresets.size - baseColors.size).coerceAtLeast(1)
    val offset = ((index - baseColors.size) % comboCount + comboCount) % comboCount
    return allPresets[baseColors.size + offset]
  }

  /**
   * Cycles through the predefined solid palette first, then automatically assigns
   * high-contrast dual-color combinations across distinct patterns on overflow.
   */
  fun nextUnusedColorIndex(usedIndices: Collection<Int>): Int {
    val normalizedUsed = usedIndices.toSet()
    for (preset in allPresets) {
      if (preset.index !in normalizedUsed) return preset.index
    }
    val comboCount = (allPresets.size - baseColors.size).coerceAtLeast(1)
    return baseColors.size + (normalizedUsed.size % comboCount)
  }
}

@Composable
fun RouteColorDot(
  spec: RouteColorSpec,
  size: Dp = 14.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val radius = minOf(w, h) / 2f
    val center = Offset(w / 2f, h / 2f)
    val circlePath = Path().apply {
      addOval(Rect(0f, 0f, w, h))
    }

    val sec = spec.secondary
    if (sec == null || spec.pattern == RouteComboPattern.SOLID) {
      drawCircle(color = spec.primary, radius = radius, center = center)
      return@Canvas
    }

    when (spec.pattern) {
      RouteComboPattern.SOLID -> {
        drawCircle(color = spec.primary, radius = radius, center = center)
      }
      RouteComboPattern.SPLIT_HALF -> {
        clipPath(circlePath) {
          drawRect(color = spec.primary, topLeft = Offset.Zero, size = Size(w / 2f, h))
          drawRect(color = sec, topLeft = Offset(w / 2f, 0f), size = Size(w / 2f, h))
        }
      }
      RouteComboPattern.RING_CORE -> {
        drawCircle(color = spec.primary, radius = radius, center = center)
        drawCircle(color = Color.White, radius = radius * 0.56f, center = center)
        drawCircle(color = sec, radius = radius * 0.42f, center = center)
      }
      RouteComboPattern.DIAGONAL_STRIPE -> {
        clipPath(circlePath) {
          drawRect(color = spec.primary, topLeft = Offset.Zero, size = Size(w, h))
          val tri = Path().apply {
            moveTo(w, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(path = tri, color = sec)
        }
      }
      RouteComboPattern.TOP_BOTTOM_DUO -> {
        clipPath(circlePath) {
          drawRect(color = spec.primary, topLeft = Offset.Zero, size = Size(w, h / 2f))
          drawRect(color = sec, topLeft = Offset(0f, h / 2f), size = Size(w, h / 2f))
        }
      }
    }
    drawCircle(
      color = Color.Black.copy(alpha = 0.16f),
      radius = radius,
      center = center,
      style = Stroke(width = 1.dp.toPx()),
    )
  }
}

@Composable
fun RouteProgressBar(
  progress: Float,
  spec: RouteColorSpec,
  trackColor: Color,
  modifier: Modifier = Modifier,
) {
  val clamped = progress.coerceIn(0f, 1f)
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(h / 2f, h / 2f)
    drawRoundRect(color = trackColor, size = Size(w, h), cornerRadius = corner)
    val fillW = w * clamped
    if (fillW <= 0f) return@Canvas
    val fillPath = Path().apply {
      addRoundRect(RoundRect(0f, 0f, fillW, h, corner))
    }
    val sec = spec.secondary
    if (sec == null || spec.pattern == RouteComboPattern.SOLID) {
      drawPath(path = fillPath, color = spec.primary)
    } else {
      clipPath(fillPath) {
        when (spec.pattern) {
          RouteComboPattern.SPLIT_HALF, RouteComboPattern.TOP_BOTTOM_DUO -> {
            drawRect(color = spec.primary, topLeft = Offset.Zero, size = Size(fillW, h / 2f))
            drawRect(color = sec, topLeft = Offset(0f, h / 2f), size = Size(fillW, h / 2f))
          }
          RouteComboPattern.RING_CORE -> {
            drawRect(color = spec.primary, topLeft = Offset.Zero, size = Size(fillW, h))
            drawRect(color = sec, topLeft = Offset(0f, h * 0.3f), size = Size(fillW, h * 0.4f))
          }
          RouteComboPattern.DIAGONAL_STRIPE -> {
            val stripeW = 10.dp.toPx()
            var x = -h
            var usePrimary = true
            while (x < fillW + h) {
              drawRect(
                color = if (usePrimary) spec.primary else sec,
                topLeft = Offset(x, 0f),
                size = Size(stripeW, h),
              )
              x += stripeW
              usePrimary = !usePrimary
            }
          }
          RouteComboPattern.SOLID -> drawRect(color = spec.primary)
        }
      }
    }
  }
}

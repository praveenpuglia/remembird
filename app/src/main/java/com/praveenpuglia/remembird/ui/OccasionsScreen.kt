package com.praveenpuglia.remembird.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveenpuglia.remembird.Bucket
import com.praveenpuglia.remembird.Occasion
import com.praveenpuglia.remembird.R
import com.praveenpuglia.remembird.Season
import com.praveenpuglia.remembird.daysUntil
import com.praveenpuglia.remembird.monthEmoji
import com.praveenpuglia.remembird.nextDate
import com.praveenpuglia.remembird.seasonOf
import com.praveenpuglia.remembird.upcomingByMonth
import com.praveenpuglia.remembird.ui.theme.Handwriting
import com.praveenpuglia.remembird.ui.theme.Paper
import com.praveenpuglia.remembird.ui.theme.color
import com.praveenpuglia.remembird.ui.theme.tape
import com.praveenpuglia.remembird.yearsOn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaYearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccasionsScreen(
    hasPermission: Boolean,
    permanentlyDenied: Boolean,
    occasions: List<Occasion>?,
    today: LocalDate,
    onGrant: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.paper(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.paper(), // opaque paper, so the list doesn't show through as it scrolls under
                title = {
                    Row(Modifier.brushStroke()) {
                        // Stands on the wordmark's baseline, facing it; nudged below its toes (y=113 of 120) so it looks planted.
                        // Decorative: the name is already read out.
                        Image(painterResource(R.drawable.bird), null, Modifier.size(44.dp).alignBy { (it.measuredHeight * 106f / 120f).roundToInt() })
                        Text(
                            stringResource(R.string.app_name),
                            Modifier.alignByBaseline(),
                            fontFamily = Handwriting,
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when {
            !hasPermission -> PermissionPrompt(permanentlyDenied, onGrant, Modifier.padding(padding))
            occasions == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            else -> OccasionList(occasions, today, padding)
        }
    }
}

/** Kraft paper with a faint grid of fibre specks, drawn from one small repeating tile. */
private fun Modifier.paper() = drawWithCache {
    val step = 7.dp.toPx().roundToInt().coerceAtLeast(2)
    val tile = ImageBitmap(step, step)
    Canvas(tile).drawCircle(Offset(step / 2f, step / 2f), 1.dp.toPx(), Paint().apply { color = Paper.speck })
    val specks = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind {
        drawRect(Paper.kraft)
        drawRect(specks)
    }
}

/**
 * A dry-brush stroke of highlighter through the lower half: wavy edges, ragged ends, and a few gaps
 * where the bristles ran dry. Fixed shape, so it looks the same every time.
 */
private fun Modifier.brushStroke() = drawWithCache {
    val w = size.width
    fun x(f: Float) = w * f
    fun y(f: Float) = size.height * (0.47f + 0.4f * f) // f: 0 = top edge of the stroke, 1 = bottom edge
    val stroke = Path().apply {
        moveTo(x(0.02f), y(0.1f))
        cubicTo(x(0.3f), y(-0.12f), x(0.65f), y(0.08f), x(0.97f), y(-0.05f))
        lineTo(x(1f), y(0.2f))
        lineTo(x(0.965f), y(0.38f))
        lineTo(x(0.995f), y(0.55f))
        lineTo(x(0.955f), y(0.72f))
        lineTo(x(0.985f), y(0.95f))
        cubicTo(x(0.65f), y(1.1f), x(0.3f), y(0.92f), x(0.01f), y(1.05f))
        lineTo(x(0f), y(0.75f))
        lineTo(x(0.02f), y(0.5f))
        lineTo(x(0.005f), y(0.28f))
        close()
    }
    val bristle = 1.2.dp.toPx()
    // Short gaps where the brush ran dry, all near the end of the stroke: (from, to, height in the stroke).
    val dry = listOf(
        Triple(0.84f, 0.97f, 0.22f), Triple(0.9f, 1f, 0.4f), Triple(0.78f, 0.88f, 0.55f),
        Triple(0.88f, 0.99f, 0.68f), Triple(0.7f, 0.77f, 0.8f), Triple(0.93f, 1f, 0.86f),
    )
    onDrawBehind {
        drawPath(stroke, Paper.highlighter)
        for ((from, to, at) in dry) drawLine(Paper.kraft, Offset(x(from), y(at)), Offset(x(to), y(at)), bristle, StrokeCap.Round)
    }
}

/** Handwritten text with a highlighter brush stroke through its lower half. */
@Composable
private fun Highlighted(text: String, fontSize: Int, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.brushStroke(),
        fontFamily = Handwriting,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
    )
}

@Composable
private fun PermissionPrompt(permanentlyDenied: Boolean, onGrant: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.bird), null, Modifier.size(128.dp))
        Highlighted(stringResource(R.string.permission_title), fontSize = 34)
        Text(
            stringResource(if (permanentlyDenied) R.string.permission_denied_body else R.string.permission_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGrant) {
            Text(stringResource(if (permanentlyDenied) R.string.permission_open_settings else R.string.permission_grant))
        }
    }
}

@Composable
private fun OccasionList(occasions: List<Occasion>, today: LocalDate, padding: PaddingValues) {
    var filter by rememberSaveable { mutableStateOf<Bucket?>(null) }
    val shown = remember(occasions, today, filter) {
        occasions.filter { filter == null || it.bucket == filter }.upcomingByMonth(today)
    }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale) }

    LazyColumn(contentPadding = padding) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf(null) + Bucket.entries) { bucket ->
                    val selected = filter == bucket
                    FilterChip(
                        selected = selected,
                        onClick = { filter = bucket },
                        label = { Text(stringResource(bucket.titleRes()), fontWeight = FontWeight.Bold) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(containerColor = Paper.card, selectedContainerColor = Paper.highlighter),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Paper.ink,
                            selectedBorderColor = Paper.ink,
                            borderWidth = 1.5.dp,
                            selectedBorderWidth = 1.5.dp,
                        ),
                    )
                }
            }
        }
        if (shown.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.bird), null, Modifier.size(96.dp))
                    Text(
                        stringResource(R.string.empty),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        var previousSeason: Season? = null
        shown.forEach { (month, monthOccasions) ->
            val season = seasonOf(month.month.number)
            val first = previousSeason == null
            val seasonStarts = season != previousSeason
            previousSeason = season
            item(key = month.toString()) {
                MonthHeader(month, season, seasonStarts, showYear = month.year != today.year, first = first)
            }
            items(monthOccasions, key = { "${it.contactId}-${it.bucket}-${it.month}-${it.day}-${it.label}" }) { occasion ->
                OccasionRow(occasion, today, dateFormat, Modifier.timeline(season.color()))
            }
        }
    }
}

private val TIMELINE_GUTTER = 32.dp
private val TIMELINE_X = 18.dp

/** The season's stretch of the timeline: a dashed pen line down the left gutter. [fromTop] skips the first header's top. */
private fun Modifier.timelineLine(color: Color, fromTop: Float = 0f) = drawBehind {
    val x = TIMELINE_X.toPx()
    drawLine(
        color,
        Offset(x, size.height * fromTop),
        Offset(x, size.height),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
    )
}

private fun Modifier.timeline(color: Color) = timelineLine(color).padding(start = TIMELINE_GUTTER)

@Composable
private fun MonthHeader(month: YearMonth, season: Season, seasonStarts: Boolean, showYear: Boolean, first: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(month, showYear, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (showYear) "LLLLyyyy" else "LLLL")
        month.toJavaYearMonth().format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    Column(
        Modifier.fillMaxWidth().timelineLine(season.color(), fromTop = if (first) 0.4f else 0f).padding(top = 22.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // A strip of washi tape in the season's colour, stuck across the timeline.
        Row(
            Modifier
                .padding(start = 8.dp)
                .rotate(-2f)
                .drawWithCache {
                    // Square where it came off the roll, zigzag where the dispenser's teeth cut it.
                    val tooth = 4.dp.toPx()
                    val teeth = (size.height / (2 * tooth)).roundToInt().coerceAtLeast(2)
                    val step = size.height / teeth
                    val strip = Path().apply {
                        lineTo(size.width, 0f)
                        repeat(teeth) { i ->
                            lineTo(size.width - tooth, step * i + step / 2)
                            lineTo(size.width, step * (i + 1))
                        }
                        lineTo(0f, size.height)
                        close()
                    }
                    val tape = season.tape(12.dp.toPx())
                    onDrawBehind { drawPath(strip, tape, alpha = 0.92f) }
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                Modifier.semantics { heading() },
                fontFamily = Handwriting,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            Text(monthEmoji(month.month.number), Modifier.clearAndSetSemantics {}, fontSize = 18.sp)
        }
        if (seasonStarts) {
            Row(
                Modifier.padding(start = TIMELINE_GUTTER + 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(season.emoji, Modifier.clearAndSetSemantics {}, fontSize = 16.sp)
                Text(
                    stringResource(R.string.season_label, stringResource(season.nameRes()), stringResource(season.meaningRes())),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Season.nameRes() = when (this) {
    Season.SHISHIR -> R.string.season_shishir
    Season.VASANT -> R.string.season_vasant
    Season.GRISHMA -> R.string.season_grishma
    Season.VARSHA -> R.string.season_varsha
    Season.SHARAD -> R.string.season_sharad
    Season.HEMANT -> R.string.season_hemant
}

private fun Season.meaningRes() = when (this) {
    Season.SHISHIR -> R.string.season_shishir_meaning
    Season.VASANT -> R.string.season_vasant_meaning
    Season.GRISHMA -> R.string.season_grishma_meaning
    Season.VARSHA -> R.string.season_varsha_meaning
    Season.SHARAD -> R.string.season_sharad_meaning
    Season.HEMANT -> R.string.season_hemant_meaning
}

@Composable
private fun OccasionRow(occasion: Occasion, today: LocalDate, dateFormat: DateTimeFormatter, modifier: Modifier = Modifier) {
    val next = occasion.nextDate(today)
    val days = occasion.daysUntil(today)
    val emoji = occasion.bucket.emoji()
    // Polaroids lean a little, each contact always the same way.
    val tilt = (Math.floorMod(occasion.contactId, 3L) - 1) * 3.5f
    Box(modifier.clipToBounds()) {
        // The row's backdrop: its bucket emoji, oversized and faded like an old stamp.
        Text(
            emoji,
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
                .rotate(-18f)
                .alpha(0.24f)
                .clearAndSetSemantics {},
            fontSize = 64.sp,
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                PolaroidAvatar(occasion.name, occasion.photoUri, 44.dp, tilt)
                Text(emoji, Modifier.align(Alignment.BottomEnd).offset(x = 9.dp, y = 6.dp).clearAndSetSemantics {}, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    occasion.name,
                    fontFamily = Handwriting,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(occasion.describe(next), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                val date = next.toJavaLocalDate().format(dateFormat)
                if (days == 0) {
                    // Today gets circled in red pen.
                    Text(
                        date,
                        Modifier.rotate(-4f).drawBehind {
                            val pad = 5.dp.toPx()
                            drawOval(
                                Paper.pen,
                                topLeft = Offset(-pad, -pad / 2),
                                size = Size(size.width + pad * 2, size.height + pad),
                                style = Stroke(2.dp.toPx()),
                            )
                        },
                        color = Paper.pen,
                        fontFamily = Handwriting,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                    )
                } else {
                    Text(date, fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                }
                Text(
                    whenText(days),
                    // Clear of the red-pen circle around today's date.
                    Modifier.padding(top = if (days == 0) 6.dp else 0.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (days == 0) Paper.pen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Not translated: emoji read the same in every language. Screen readers skip them; the row text says the kind.
private fun Bucket.emoji() = when (this) {
    Bucket.BIRTHDAY -> "🎂"
    Bucket.ANNIVERSARY -> "💍"
    Bucket.OTHER -> "🎉"
}

/** "Birthday · Turns 30", "Anniversary · 5 years", or just the kind when the year is unknown. */
@Composable
private fun Occasion.describe(next: LocalDate): String {
    val kind = label ?: stringResource(
        when (bucket) {
            Bucket.BIRTHDAY -> R.string.kind_birthday
            Bucket.ANNIVERSARY -> R.string.kind_anniversary
            Bucket.OTHER -> R.string.kind_other
        }
    )
    val years = yearsOn(next)?.takeIf { it > 0 } ?: return kind
    val yearsText = if (bucket == Bucket.BIRTHDAY) stringResource(R.string.turns, years) else pluralStringResource(R.plurals.years, years, years)
    return stringResource(R.string.kind_with_years, kind, yearsText)
}

@Composable
private fun whenText(days: Int) = when (days) {
    0 -> stringResource(R.string.today)
    1 -> stringResource(R.string.tomorrow)
    else -> pluralStringResource(R.plurals.in_days, days, days)
}

private fun Bucket?.titleRes() = when (this) {
    null -> R.string.bucket_all
    Bucket.BIRTHDAY -> R.string.bucket_birthday
    Bucket.ANNIVERSARY -> R.string.bucket_anniversary
    Bucket.OTHER -> R.string.bucket_other
}

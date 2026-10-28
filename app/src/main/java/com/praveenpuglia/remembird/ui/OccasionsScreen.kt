package com.praveenpuglia.remembird.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
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
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccasionsScreen(
    hasPermission: Boolean,
    permanentlyDenied: Boolean,
    occasions: List<Occasion>?,
    today: LocalDate,
    onGrant: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Occasion) -> Unit,
) {
    Scaffold(
        modifier = Modifier.paper(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (hasPermission && occasions != null) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    containerColor = Paper.highlighter,
                    contentColor = Paper.ink,
                    icon = { Text("✍️", Modifier.clearAndSetSemantics {}, fontSize = 20.sp) },
                    text = { Text(stringResource(R.string.remember), fontWeight = FontWeight.Bold) },
                )
            }
        },
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
            else -> OccasionList(occasions, today, padding, onOpen)
        }
    }
}

/** Kraft paper with a faint grid of fibre specks, drawn from one small repeating tile. */
internal fun Modifier.paper() = drawWithCache {
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
internal fun Highlighted(text: String, fontSize: Int, modifier: Modifier = Modifier) {
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
        PolicyLinks()
    }
}

/** Privacy policy and terms, opened in the browser (Play wants the policy reachable from inside the app). */
@Composable
internal fun PolicyLinks(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { uriHandler.openUri(privacyUrl) }) { Text(stringResource(R.string.privacy_policy)) }
        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { uriHandler.openUri(termsUrl) }) { Text(stringResource(R.string.terms)) }
    }
}

@Composable
private fun OccasionList(occasions: List<Occasion>, today: LocalDate, padding: PaddingValues, onOpen: (Occasion) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<Bucket?>(null) }
    val shown = remember(occasions, today, filter) {
        occasions.filter { filter == null || it.bucket == filter }.upcomingByMonth(today)
    }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale) }

    // Room at the bottom so the Add a date button never covers the last row.
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp)) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf(null) + Bucket.entries) { bucket ->
                    PaperChip(stringResource(bucket.titleRes()), selected = filter == bucket) { filter = bucket }
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
            val inkFrom = (previousSeason ?: season).color()
            previousSeason = season
            item(key = month.toString()) {
                MonthHeader(month, season, seasonStarts, showYear = month.year != today.year, first = first, inkFrom = inkFrom)
            }
            items(monthOccasions, key = { "${it.contactId}-${it.bucket}-${it.month}-${it.day}-${it.label}" }) { occasion ->
                OccasionRow(occasion, today, dateFormat, Modifier.clickable { onOpen(occasion) }.timeline(season.color(), seed = occasion.hashCode()))
            }
        }
        item { Box(Modifier.fillMaxWidth().padding(top = 24.dp), Alignment.Center) { PolicyLinks() } }
    }
}

/** A filter chip as a card-stock pill with an ink outline; the chosen one is highlighted. */
@Composable
internal fun PaperChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
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

private val TIMELINE_GUTTER = 32.dp
private val TIMELINE_X = 18.dp

/**
 * The season's stretch of the timeline: a hand-drawn pen line down the left gutter, gone over twice like ink.
 * [seed] gives each item its own wobble; [fromTop] skips the first header's top. Where a new season starts,
 * the ink fades [from] the last season's colour into this one.
 */
private fun Modifier.timelineLine(color: Color, seed: Int, fromTop: Float = 0f, from: Color = color) = drawWithCache {
    val x = TIMELINE_X.toPx()
    val top = size.height * fromTop
    val wobble = 1.3.dp.toPx()
    val steps = ((size.height - top) / 4.dp.toPx()).roundToInt().coerceAtLeast(1)
    val random = Random(seed)
    // Whole half-waves are zero at both ends, so every item's line meets the next one's exactly on the timeline.
    fun pass() = Path().apply {
        val weights = listOf(2, 3, 5).map { it to random.nextFloat() * 2 - 1 }
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val dx = wobble * weights.sumOf { (halfWaves, weight) -> weight * sin(PI * halfWaves * t) }.toFloat() / 2
            if (i == 0) moveTo(x + dx, top) else lineTo(x + dx, top + t * (size.height - top))
        }
    }
    val ink = pass()
    val retrace = pass()
    val inkStroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val retraceStroke = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val brush = Brush.verticalGradient(listOf(from, color), startY = top, endY = size.height)
    onDrawBehind {
        drawPath(ink, brush, alpha = 0.9f, style = inkStroke)
        drawPath(retrace, brush, alpha = 0.45f, style = retraceStroke)
    }
}

private fun Modifier.timeline(color: Color, seed: Int) = timelineLine(color, seed).padding(start = TIMELINE_GUTTER)

@Composable
private fun MonthHeader(month: YearMonth, season: Season, seasonStarts: Boolean, showYear: Boolean, first: Boolean, inkFrom: Color) {
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(month, showYear, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (showYear) "LLLLyyyy" else "LLLL")
        month.toJavaYearMonth().format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    Column(
        Modifier.fillMaxWidth().timelineLine(season.color(), seed = month.hashCode(), fromTop = if (first) 0.4f else 0f, from = inkFrom).padding(top = 22.dp, bottom = 6.dp),
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
internal fun Bucket.emoji() = when (this) {
    Bucket.BIRTHDAY -> "🎂"
    Bucket.ANNIVERSARY -> "💍"
    Bucket.OTHER -> "🎉"
}

/** "Birthday · Turns 30", "Anniversary · 5 years", or just the kind when the year is unknown. */
@Composable
internal fun Occasion.describe(next: LocalDate): String {
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
internal fun whenText(days: Int) = when (days) {
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

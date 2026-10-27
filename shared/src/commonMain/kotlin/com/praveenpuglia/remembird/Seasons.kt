package com.praveenpuglia.remembird

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** The six ritus of the traditional Indian calendar. */
enum class Season(val emoji: String) {
    SHISHIR("❄️"), // winter
    VASANT("🌸"), // spring
    GRISHMA("☀️"), // summer
    VARSHA("🌧️"), // monsoon
    SHARAD("🌕"), // autumn, the season of clear skies and Sharad Purnima
    HEMANT("🌾"), // pre-winter, harvest
}

/**
 * The ritu a month falls in, two months each as commonly taught. The ritus really follow the lunar
 * months, so they start mid-month; whole Gregorian months keep the timeline readable.
 */
fun seasonOf(month: Int): Season = Season.entries[(month - 1) / 2]

/** A festive or seasonal emoji for each month: Sankranti kites, Holi colours, Diwali lamps… */
fun monthEmoji(month: Int): String = MONTH_EMOJI[month - 1]

private val MONTH_EMOJI = listOf("🪁", "🌼", "🎨", "🥭", "🍉", "☔", "🌈", "🪷", "🐘", "💃", "🪔", "🎄")

/** Upcoming occasions grouped by the month they next fall in, soonest month first. */
fun List<Occasion>.upcomingByMonth(today: LocalDate): Map<YearMonth, List<Occasion>> =
    upcoming(today).groupBy { it.nextDate(today).let { date -> YearMonth(date.year, date.month) } }

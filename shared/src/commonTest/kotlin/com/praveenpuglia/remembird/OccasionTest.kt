package com.praveenpuglia.remembird

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OccasionTest {
    private fun occasion(month: Int, day: Int, year: Int? = null, name: String = "A") =
        Occasion(1, name, Bucket.BIRTHDAY, null, month, day, year)

    private val today = LocalDate(2026, 10, 27)

    @Test fun todayCountsAsNext() = assertEquals(today, occasion(10, 27).nextDate(today))

    @Test fun laterThisYear() = assertEquals(LocalDate(2026, 12, 1), occasion(12, 1).nextDate(today))

    @Test fun passedRollsToNextYear() = assertEquals(LocalDate(2027, 1, 5), occasion(1, 5).nextDate(today))

    @Test fun leapDayInCommonYearFallsOnFeb28() =
        assertEquals(LocalDate(2027, 2, 28), occasion(2, 29, 2000).nextDate(today))

    @Test fun leapDayInLeapYearStays() =
        assertEquals(LocalDate(2028, 2, 29), occasion(2, 29).nextDate(LocalDate(2027, 3, 1)))

    @Test fun yearsOnUsesNextDate() = assertEquals(36, occasion(1, 5, 1991).yearsOn(occasion(1, 5).nextDate(today)))

    @Test fun yearsOnWithoutYear() = assertNull(occasion(1, 5).yearsOn(today))

    @Test fun upcomingSortsSoonestFirst() {
        val list = listOf(occasion(10, 26, name = "past"), occasion(10, 27, name = "today"), occasion(11, 1, name = "soon"))
        assertEquals(listOf("today", "soon", "past"), list.upcoming(today).map { it.name })
    }

    @Test fun customLabelsAreBucketed() {
        assertEquals(Bucket.ANNIVERSARY, bucketForLabel("Wedding Anniversary"))
        assertEquals(Bucket.BIRTHDAY, bucketForLabel("Bday (lunar)"))
        assertEquals(Bucket.OTHER, bucketForLabel("Graduation"))
        assertEquals(Bucket.OTHER, bucketForLabel(null))
    }
}

class SeasonsTest {
    private fun occasion(month: Int, day: Int, name: String) = Occasion(1, name, Bucket.BIRTHDAY, null, month, day, null)

    @Test fun groupsByMonthAcrossTheYearEnd() {
        val today = LocalDate(2026, 10, 27)
        val groups = listOf(occasion(1, 3, "jan"), occasion(11, 2, "nov"), occasion(10, 1, "oct"), occasion(11, 9, "nov2"))
            .upcomingByMonth(today)
        assertEquals(
            listOf(YearMonth(2026, 11) to listOf("nov", "nov2"), YearMonth(2027, 1) to listOf("jan"), YearMonth(2027, 10) to listOf("oct")),
            groups.map { (month, list) -> month to list.map { it.name } },
        )
    }

    @Test fun monthsMapToRitusAndEveryMonthHasAnEmoji() {
        assertEquals(Season.SHISHIR, seasonOf(1))
        assertEquals(Season.SHISHIR, seasonOf(2))
        assertEquals(Season.VASANT, seasonOf(3))
        assertEquals(Season.VARSHA, seasonOf(8))
        assertEquals(Season.HEMANT, seasonOf(12))
        assertEquals(12, (1..12).map(::monthEmoji).distinct().size)
    }
}

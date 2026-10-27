package com.praveenpuglia.remembird

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

enum class Bucket { BIRTHDAY, ANNIVERSARY, OTHER }

/** A yearly date saved on a contact. [year] is null when the contact stores only month and day. */
data class Occasion(
    val contactId: Long,
    val name: String,
    val bucket: Bucket,
    /** The contact's own label for the date, e.g. "Graduation". Null for the standard types. */
    val label: String?,
    val month: Int,
    val day: Int,
    val year: Int?,
    /** Where the platform can load the contact's thumbnail from (a content URI on Android). */
    val photoUri: String? = null,
)

/** The next date this occasion falls on, today included. Feb 29 is marked on Feb 28 in common years. */
fun Occasion.nextDate(today: LocalDate): LocalDate {
    fun on(year: Int) = LocalDate(year, month, if (month == 2 && day == 29 && !isLeap(year)) 28 else day)
    val thisYear = on(today.year)
    return if (thisYear >= today) thisYear else on(today.year + 1)
}

fun Occasion.daysUntil(today: LocalDate): Int = today.daysUntil(nextDate(today))

/** How many years [date] marks (the age being turned, years married), or null when the year is unknown. */
fun Occasion.yearsOn(date: LocalDate): Int? = year?.let { date.year - it }

/** Soonest first. */
fun List<Occasion>.upcoming(today: LocalDate): List<Occasion> = sortedBy { it.daysUntil(today) }

/** Which bucket a custom-labelled date belongs to, so "Wedding anniversary" lands with anniversaries. */
fun bucketForLabel(label: String?): Bucket {
    val l = label?.lowercase() ?: return Bucket.OTHER
    return when {
        "birthday" in l || "bday" in l -> Bucket.BIRTHDAY
        "anniversary" in l || "wedding" in l -> Bucket.ANNIVERSARY
        else -> Bucket.OTHER
    }
}

private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

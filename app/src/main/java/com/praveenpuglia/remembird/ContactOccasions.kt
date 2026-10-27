package com.praveenpuglia.remembird

import android.content.ContentResolver
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.Data
import kotlinx.datetime.LocalDate

/** Reads every dated event saved on the user's contacts. */
object ContactOccasions {
    private val PROJECTION = arrayOf(Data.CONTACT_ID, Data.DISPLAY_NAME_PRIMARY, Event.TYPE, Event.LABEL, Event.START_DATE, Data.PHOTO_THUMBNAIL_URI)

    fun load(resolver: ContentResolver): List<Occasion> {
        val occasions = mutableListOf<Occasion>()
        resolver.query(Data.CONTENT_URI, PROJECTION, "${Data.MIMETYPE} = ?", arrayOf(Event.CONTENT_ITEM_TYPE), null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                val date = c.getString(4)?.let(::parseEventDate) ?: continue
                val label = c.getString(3)?.takeIf { it.isNotBlank() }
                val bucket = when (c.getInt(2)) {
                    Event.TYPE_BIRTHDAY -> Bucket.BIRTHDAY
                    Event.TYPE_ANNIVERSARY -> Bucket.ANNIVERSARY
                    else -> bucketForLabel(label)
                }
                occasions += Occasion(c.getLong(0), name, bucket, label, date.month, date.day, date.year, c.getString(5))
            }
        }
        // A contact merged from several accounts (Google, WhatsApp, SIM) repeats its dates; keep one, preferring one with a year.
        return occasions.sortedBy { it.year == null }.distinctBy { listOf(it.contactId, it.bucket, it.month, it.day) }
    }
}

data class EventDate(val month: Int, val day: Int, val year: Int?)

private val FULL = Regex("""^(\d{4})-?(\d{2})-?(\d{2})""") // 1990-05-21, 19900521, 1990-05-21T00:00:00Z
private val NO_YEAR = Regex("""^--(\d{2})-?(\d{2})$""") // --05-21

/**
 * Parses an [Event.START_DATE]. The column is free text, so each sync adapter writes its own format.
 * shortcut: only ISO-style dates are understood; add formats as real contacts turn them up.
 */
fun parseEventDate(raw: String): EventDate? {
    val s = raw.trim()
    val (year, month, day) = FULL.find(s)?.destructured?.let { (y, m, d) -> Triple(y.toInt(), m.toInt(), d.toInt()) }
        ?: NO_YEAR.find(s)?.destructured?.let { (m, d) -> Triple(null, m.toInt(), d.toInt()) }
        ?: return null
    // Apple devices save year-less birthdays as 1604, so any year that old means "no year".
    val knownYear = year?.takeIf { it > 1700 }
    return try {
        LocalDate(knownYear ?: 2000, month, day) // 2000 is a leap year, so Feb 29 stays valid without a year
        EventDate(month, day, knownYear)
    } catch (_: IllegalArgumentException) {
        null
    }
}

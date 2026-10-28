package com.praveenpuglia.remembird

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import kotlinx.datetime.LocalDate
import java.util.Locale

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

    /** Everyone in the user's contacts, A to Z, for picking who to add a date to. */
    fun people(resolver: ContentResolver): List<Person> {
        val people = mutableListOf<Person>()
        val projection = arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY, Contacts.PHOTO_THUMBNAIL_URI)
        resolver.query(Contacts.CONTENT_URI, projection, "${Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL", null, "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC")?.use { c ->
            while (c.moveToNext()) people += Person(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
        }
        return people
    }

    /**
     * Opens the contacts app's editor for [person] with one date filled in; the user saves it there.
     * Remembird never writes contacts itself. The editor keeps only one date per request, and
     * Google Contacts accepts a missing year only on birthdays, so the add screen asks for one otherwise.
     */
    fun addDateIntent(person: Person, bucket: Bucket, label: String?, date: EventDate): Intent {
        val row = ContentValues().apply {
            put(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
            put(
                Event.TYPE,
                when (bucket) {
                    Bucket.BIRTHDAY -> Event.TYPE_BIRTHDAY
                    Bucket.ANNIVERSARY -> Event.TYPE_ANNIVERSARY
                    Bucket.OTHER -> if (label == null) Event.TYPE_OTHER else Event.TYPE_CUSTOM
                },
            )
            if (bucket == Bucket.OTHER && label != null) put(Event.LABEL, label)
            put(Event.START_DATE, date.toStartDate())
        }
        return Intent(Intent.ACTION_EDIT, Contacts.getLookupUri(person.id, person.lookupKey))
            .putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, arrayListOf(row))
            // Documented in the contacts-intents guide, but has no constant: return here after Save instead of staying in Contacts.
            .putExtra("finishActivityOnSaveCompleted", true)
    }

    /** Opens the contacts app's editor for a contact, where the user can change or remove any of their dates. */
    fun editIntent(resolver: ContentResolver, contactId: Long): Intent {
        val byId = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
        return Intent(Intent.ACTION_EDIT, Contacts.getLookupUri(resolver, byId) ?: byId).putExtra("finishActivityOnSaveCompleted", true)
    }
}

data class Person(val id: Long, val lookupKey: String, val name: String, val photoUri: String?)

data class EventDate(val month: Int, val day: Int, val year: Int?) {
    /** The [Event.START_DATE] form, which [parseEventDate] reads back: 1990-05-21, or --05-21 without a year. */
    fun toStartDate(): String =
        if (year != null) String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day) else String.format(Locale.ROOT, "--%02d-%02d", month, day)
}

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

package com.praveenpuglia.remembird

import android.Manifest
import android.content.ContentProviderOperation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * Not a real test: wipes the emulator's contacts and seeds people covering the cases Remembird handles.
 * Run it with scripts/seed-contacts.sh. It borrows the shell's contacts permissions, so the app itself
 * keeps READ_CONTACTS only.
 */
@RunWith(AndroidJUnit4::class)
class SeedContacts {

    private class Ev(val type: Int, val date: String, val label: String? = null)
    private class Person(val name: String, val photo: Boolean, vararg val events: Ev)

    private val today = LocalDate.now()
    private fun md(days: Long) = today.plusDays(days).let { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
    private fun birthday(date: String) = Ev(Event.TYPE_BIRTHDAY, date)
    private fun anniversary(date: String) = Ev(Event.TYPE_ANNIVERSARY, date)

    private val people = listOf(
        // Coming up soon
        Person("Asha Verma", true, birthday("1990-${md(0)}")),
        Person("Farhan Ali", false, birthday("1987-${md(0)}")),
        Person("Esha & Kunal", false, anniversary("--${md(0)}")),
        Person("Bilal Khan", false, birthday("--${md(1)}")),
        Person("Chitra & Arun Iyer", true, anniversary("2016-${md(3)}")),
        Person("Rohan Das", true, birthday("1991-${md(10)}")),
        Person("Sameer Kohli", false, birthday("--${md(20)}")),
        Person("Kavya Reddy", false, birthday("2003-${md(40)}")),
        Person("Anil Menon", false, birthday("1979-${md(60)}")),
        Person("Dev", false, birthday("1994-${md(-1)}")), // just passed, so it shows next year
        // One or more in every month
        Person("Gaurav Mehta", true, birthday("1988-01-14")),
        Person("Kiran & Neel", false, Ev(Event.TYPE_CUSTOM, "2010-02-14", "Wedding")),
        Person("Hema Leap", false, birthday("1996-02-29")),
        Person("Ishaan Bose", false, birthday("--03-21"), anniversary("2015-03-08")),
        Person("जयंती शर्मा", false, birthday("1975-04-02")),
        Person("Kabir", false, birthday("19900515")),
        Person("🎸 Arjun", false, birthday("1999-05-30")),
        Person("Lata Joshi", true, birthday("1604-06-10")), // Apple's "no year"
        Person("Meera Nair", false, Ev(Event.TYPE_CUSTOM, "2019-07-01", "Work anniversary")),
        Person("Nikhil Rao", false, birthday("1992-08-03T00:00:00Z"), Ev(Event.TYPE_CUSTOM, "2014-08-20", "Graduation")),
        Person("Olivia D'Souza", true, birthday("2001-09-30")),
        Person("Pooja Sharma", false, Ev(Event.TYPE_OTHER, "2020-10-31")),
        Person("Rahul & Sneha Gupta", false, anniversary("--11-25")),
        Person("Uma Krishnamurthy Subramanian Venkataraman", false, birthday("1950-11-15")),
        Person("Vikram Singh", false, birthday("--12-05"), birthday("1985-12-05")), // duplicate; the one with a year wins
        Person("Nani", false, birthday("${today.year - 100}-12-12")),
        Person("Tara Kapoor", false, birthday("${today.year - 1}-12-31")),
        // Should not appear
        Person("Yash Patel", false, birthday("someday")),
        Person("Zoya Mirza", false),
    )

    @Test
    fun seed() {
        assumeTrue("Seeding wipes contacts, so it only runs on an emulator", Build.HARDWARE == "ranchu")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        try {
            val resolver = instrumentation.targetContext.contentResolver
            val asSyncAdapter = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build()
            resolver.delete(asSyncAdapter, null, null)

            val ops = ArrayList<ContentProviderOperation>()
            people.forEachIndexed { i, person ->
                val raw = ops.size
                ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, null).withValue(RawContacts.ACCOUNT_NAME, null).build()
                fun row(mimeType: String) = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, raw).withValue(Data.MIMETYPE, mimeType)
                ops += row(StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.DISPLAY_NAME, person.name).build()
                if (person.photo) ops += row(Photo.CONTENT_ITEM_TYPE).withValue(Photo.PHOTO, photo(i)).build()
                person.events.forEach { ev ->
                    ops += row(Event.CONTENT_ITEM_TYPE).withValue(Event.TYPE, ev.type).withValue(Event.START_DATE, ev.date)
                        .withValue(Event.LABEL, ev.label).build()
                }
            }
            resolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } finally {
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }

    /** A stand-in photo: a head-and-shoulders silhouette on a gradient, different for each person. */
    private fun photo(seed: Int): ByteArray {
        val size = 256f
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val from = Color.HSVToColor(floatArrayOf(seed * 67f % 360, 0.45f, 0.85f))
        val to = Color.HSVToColor(floatArrayOf((seed * 67f + 140) % 360, 0.55f, 0.6f))
        canvas.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, size, size, from, to, Shader.TileMode.CLAMP) })
        val figure = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 255, 255, 255) }
        canvas.drawCircle(size / 2, size * 0.42f, size * 0.2f, figure)
        canvas.drawCircle(size / 2, size * 1.12f, size * 0.45f, figure)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }
}

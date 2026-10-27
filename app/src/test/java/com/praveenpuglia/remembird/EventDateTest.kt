package com.praveenpuglia.remembird

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventDateTest {
    @Test fun isoDate() = assertEquals(EventDate(5, 21, 1990), parseEventDate("1990-05-21"))

    @Test fun compactDate() = assertEquals(EventDate(5, 21, 1990), parseEventDate("19900521"))

    @Test fun dateWithTime() = assertEquals(EventDate(5, 21, 1990), parseEventDate("1990-05-21T00:00:00.000Z"))

    @Test fun noYear() = assertEquals(EventDate(5, 21, null), parseEventDate("--05-21"))

    @Test fun applePlaceholderYear() = assertEquals(EventDate(5, 21, null), parseEventDate("1604-05-21"))

    @Test fun leapDayWithoutYear() = assertEquals(EventDate(2, 29, null), parseEventDate("--02-29"))

    @Test fun invalidDay() = assertNull(parseEventDate("1990-02-30"))

    @Test fun unknownFormat() = assertNull(parseEventDate("May 21, 1990"))
}

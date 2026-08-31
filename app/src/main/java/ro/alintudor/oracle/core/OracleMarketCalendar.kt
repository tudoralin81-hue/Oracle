package ro.alintudor.oracle.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Permanent, rule-based NYSE full-closure calendar plus regular session hours. */
object OracleMarketCalendar {
    private val NEW_YORK = ZoneId.of("America/New_York")
    private val OPEN_TIME = LocalTime.of(9, 30)
    private val CLOSE_TIME = LocalTime.of(16, 0)
    data class Status(val open: Boolean, val label: String)

    fun isTradingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY && fullClosureName(date) == null

    fun fullClosureName(date: LocalDate): String? {
        observedFixedHolidays(date.year)[date]?.let { return it }
        if (date == nthWeekday(date.year, Month.JANUARY, DayOfWeek.MONDAY, 3)) return "Martin Luther King Jr. Day"
        if (date == nthWeekday(date.year, Month.FEBRUARY, DayOfWeek.MONDAY, 3)) return "Presidents' Day"
        if (date == easterSunday(date.year).minusDays(2)) return "Good Friday"
        if (date == lastWeekday(date.year, Month.MAY, DayOfWeek.MONDAY)) return "Memorial Day"
        if (date == nthWeekday(date.year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1)) return "Labor Day"
        if (date == nthWeekday(date.year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4)) return "Thanksgiving Day"
        return null
    }

    fun status(nowMillis: Long = System.currentTimeMillis()): Status {
        val now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), NEW_YORK)
        if (now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY) return Status(false, "BURSA ESTE ÎNCHISĂ — weekend")
        fullClosureName(now.toLocalDate())?.let { return Status(false, "BURSA ESTE ÎNCHISĂ — $it") }
        if (now.toLocalTime().isBefore(OPEN_TIME)) return Status(false, "BURSA ESTE ÎNCHISĂ — înainte de deschidere")
        if (!now.toLocalTime().isBefore(CLOSE_TIME)) return Status(false, "BURSA ESTE ÎNCHISĂ — după închidere")
        return Status(true, "BURSA ESTE DESCHISĂ")
    }

    private fun observedFixedHolidays(year: Int): Map<LocalDate, String> {
        val fixed = listOf(
            Month.JANUARY to 1 to "New Year's Day",
            Month.JUNE to 19 to "Juneteenth",
            Month.JULY to 4 to "Independence Day",
            Month.DECEMBER to 25 to "Christmas Day"
        )
        return buildMap {
            for ((pair, name) in fixed) {
                val actual = LocalDate.of(year, pair.first, pair.second)
                val observed = when (actual.dayOfWeek) {
                    DayOfWeek.SATURDAY -> actual.minusDays(1)
                    DayOfWeek.SUNDAY -> actual.plusDays(1)
                    else -> actual
                }
                put(observed, name)
            }
        }
    }

    private fun nthWeekday(year: Int, month: Month, day: DayOfWeek, n: Int): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, day))

    private fun lastWeekday(year: Int, month: Month, day: DayOfWeek): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(day))

    private fun easterSunday(year: Int): LocalDate {
        val a = year % 19; val b = year / 100; val c = year % 100; val d = b / 4; val e = b % 4
        val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31; val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}

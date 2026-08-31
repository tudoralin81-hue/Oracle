package ro.alintudor.oracle.core

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

object OracleGrowthLiveData {
    private val BUCHAREST = ZoneId.of("Europe/Bucharest")
    private val firstRender = AtomicBoolean(true)

    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> {
        if (firstRender.compareAndSet(true, false)) return emptyList()
        if (items.isEmpty()) return emptyList()
        if (items.any { it.referenceTimestamp <= 0L }) return emptyList()
        val expectedAnchor = currentGrowthAnchor(System.currentTimeMillis())
        if (items.any { it.referenceTimestamp != expectedAnchor }) return emptyList()
        return items
    }

    private fun currentGrowthAnchor(nowMillis: Long): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(BUCHAREST)
        var date = if (now.toLocalTime().isBefore(LocalTime.of(16, 0))) now.toLocalDate().minusDays(1) else now.toLocalDate()
        while (!OracleMarketCalendar.isTradingDay(date)) date = date.minusDays(1)
        return ZonedDateTime.of(date, LocalTime.of(16, 0), BUCHAREST).toInstant().toEpochMilli()
    }
}

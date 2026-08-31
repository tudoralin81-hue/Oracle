package ro.alintudor.oracle.core

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Growth is a historical 16:00 snapshot.
 *
 * Live market data must never mutate the persisted Growth state while the module
 * is open or while another module performs a local refresh. This adapter therefore
 * performs validation only; it does not enrich, recalculate, reseed, or repair the
 * visible recommendation cards.
 */
object OracleGrowthLiveData {
    private val BUCHAREST = ZoneId.of("Europe/Bucharest")

    /**
     * Only expose a cached Growth snapshot when it belongs to the latest valid
     * 16:00 trading-day anchor. This prevents the UI from briefly rendering a
     * previous trading day's recommendations while OracleLocalProcessor refreshes
     * the current snapshot.
     */
    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> {
        if (items.isEmpty()) return emptyList()
        // Invalidated bootstrap snapshots use referenceTimestamp=0. Do not expose
        // a transient seed to the UI while a snapshot is being generated.
        if (items.any { it.referenceTimestamp <= 0L }) return emptyList()

        val expectedAnchor = currentGrowthAnchor(System.currentTimeMillis())
        // A single persisted Growth page must be internally consistent: never mix
        // recommendations from different snapshot anchors.
        if (items.any { it.referenceTimestamp != expectedAnchor }) return emptyList()

        return items
    }

    private fun currentGrowthAnchor(nowMillis: Long): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(BUCHAREST)
        var date = if (now.toLocalTime().isBefore(LocalTime.of(16, 0))) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
        while (!OracleMarketCalendar.isTradingDay(date)) date = date.minusDays(1)
        return ZonedDateTime.of(date, LocalTime.of(16, 0), BUCHAREST).toInstant().toEpochMilli()
    }
}

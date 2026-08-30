package ro.alintudor.oracle.core

/**
 * Growth is a historical 16:00 snapshot.
 *
 * Live market data must never mutate the persisted Growth state while the module
 * is open or while another module performs a local refresh. This adapter therefore
 * performs validation only; it does not enrich, recalculate, reseed, or repair the
 * visible recommendation cards.
 */
object OracleGrowthLiveData {
    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> {
        if (items.isEmpty()) return emptyList()
        // Invalidated bootstrap snapshots use referenceTimestamp=0. Do not expose
        // a transient seed to the UI while a snapshot is being generated.
        if (items.any { it.referenceTimestamp <= 0L }) return emptyList()
        return items
    }
}

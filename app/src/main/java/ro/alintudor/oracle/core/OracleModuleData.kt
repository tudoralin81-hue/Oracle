package ro.alintudor.oracle.core

/** Single snapshot shared by all seven native modules. */
data class OracleModuleData(
    val positions: List<OraclePosition> = emptyList(),
    val alerts: List<OracleAlert> = emptyList(),
    val news: List<OracleNews> = emptyList(),
    val history: List<OracleHistoryPoint> = emptyList(),
    val actions: List<OracleAction> = emptyList(),
    val knowledge: List<OracleKnowledgeItem> = emptyList()
)

fun OracleRepository.snapshot(): OracleModuleData = OracleModuleData(
    positions = cachedPositions(),
    alerts = cachedAlerts(),
    news = cachedNews(),
    history = cachedHistory()
)

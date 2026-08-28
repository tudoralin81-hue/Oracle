package ro.alintudor.oracle

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ro.alintudor.oracle.core.OracleModuleData

/** Compatibility store backed by the same local data schema as OracleRepository. */
class OracleStore(context: Context) {
    private val repository = ro.alintudor.oracle.core.OracleRepository(context)

    fun load(): OracleModuleState = repository.snapshot()

    fun save(state: OracleModuleState) {
        repository.savePositions(state.positions)
        repository.saveAlerts(state.alerts)
        repository.saveNews(state.news)
        repository.saveHistory(state.history)
        repository.saveActions(state.actions)
        repository.saveKnowledge(state.knowledge)
    }
}

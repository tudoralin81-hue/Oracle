package ro.alintudor.oracle.core

import kotlin.math.abs

object OracleAlertEngine {
    fun generate(positions: List<OraclePosition>, actions: List<OracleAction>): List<OracleAlert> {
        val now = System.currentTimeMillis()
        val byTicker = actions.associateBy { it.ticker }
        return positions.flatMap { p ->
            val a = byTicker[p.ticker]
            buildList {
                if (p.pnlPercent <= -10.0) add(OracleAlert(p.ticker,"HIGH","Pierdere importantă","P/L sub -10%",now,true))
                if (p.weight >= 35.0) add(OracleAlert(p.ticker,"HIGH","Concentrare ridicată","Pondere peste 35%",now,true))
                if (a != null && abs(a.score) >= 70.0) add(OracleAlert(p.ticker,if(a.action=="SELL")"HIGH" else "MEDIUM","Semnal Oracle ${a.action}",a.reason,now,true))
            }
        }
    }
}

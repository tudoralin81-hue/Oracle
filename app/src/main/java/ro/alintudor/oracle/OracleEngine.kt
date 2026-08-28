package ro.alintudor.oracle

import kotlin.math.abs

/** Core calculations shared by native Oracle modules. */
object OracleEngine {
    fun position(ticker:String, shares:Double, avgCost:Double, price:Double): OraclePosition {
        val invested=shares*avgCost
        val value=shares*price
        val pnl=value-invested
        val pct=if(invested==0.0) 0.0 else pnl/invested*100.0
        return OraclePosition(ticker,shares,avgCost,price,pnl,pct)
    }
    fun portfolioValue(items:List<OraclePosition>) = items.sumOf { it.shares*it.currentPrice }
    fun portfolioPnl(items:List<OraclePosition>) = items.sumOf { it.pnl }
    fun riskScore(items:List<OraclePosition>):Double {
        if(items.isEmpty()) return 0.0
        val weights=items.map { it.shares*it.currentPrice }.let { v -> val total=v.sum(); if(total==0.0) v.map{0.0} else v.map{it/total} }
        return (weights.maxOrNull() ?: 0.0)*100.0
    }
    fun normalizedPct(value:Double, baseline:Double)=if(abs(baseline)<1e-9) 0.0 else value/baseline*100.0
}

package ro.alintudor.oracle.core

import kotlin.math.max
import kotlin.math.min

/**
 * Safe V6 enhancement layer over the Build 234 Oracle Growth engine.
 *
 * V5.9.7 remains the authoritative baseline. This layer only applies
 * deterministic, data-backed agreement/regime adjustments to the already
 * computed factors; it never fabricates missing fundamentals, sector data,
 * news or prices.
 */
object OracleGrowthV6Enhanced {
    private const val MAX_SCORE = 97

    fun run(seed: List<OracleGrowthRecommendation> = emptyList()): List<OracleGrowthRecommendation> {
        val base = OracleGrowthEngine.run(seed)
        if (base.isEmpty()) return base

        return base.map { r ->
            val f = r.factorValues
            val breakout = factor(f, 1)
            val trend = factor(f, 2)
            val momentum = factor(f, 3)
            val volume = factor(f, 4)
            val bollinger = factor(f, 7)
            val ichimoku = factor(f, 8)
            val riskReward = factor(f, 10)
            val adx = factor(f, 11)

            // Cross-factor agreement is more useful than rewarding one isolated spike.
            var adjustment = 0.0
            val bullishCount = listOf(trend, momentum, volume, ichimoku, adx)
                .count { it >= 60.0 }
            val bearishCount = listOf(trend, momentum, volume, ichimoku, adx)
                .count { it <= 40.0 }
            if (bullishCount >= 4) adjustment += 3.0
            if (bearishCount >= 4) adjustment -= 3.0
            if (breakout >= 85.0 && volume < 45.0) adjustment -= 2.0
            if (trend >= 70.0 && momentum < 42.0) adjustment -= 2.0
            if (riskReward < 40.0) adjustment -= 2.0
            if (adx >= 65.0 && trend >= 65.0) adjustment += 1.5
            if (bollinger < 25.0 && momentum < 45.0) adjustment -= 1.0

            val horizonBonus = when (r.horizon.uppercase()) {
                "SHORT" -> (momentum - 50.0) * 0.035 + (volume - 50.0) * 0.02
                "MEDIUM" -> (trend - 50.0) * 0.03 + (ichimoku - 50.0) * 0.025
                else -> (trend - 50.0) * 0.035 + (riskReward - 50.0) * 0.025
            }
            val enhancedScore = (r.score + adjustment + horizonBonus)
                .coerceIn(0.0, MAX_SCORE.toDouble()).toInt()

            val regimePenalty = when {
                trend < 35.0 && momentum < 40.0 -> 0.86
                trend > 70.0 && momentum > 60.0 && adx >= 55.0 -> 1.10
                else -> 1.0
            }
            val forecast = max(0.0, r.forecastPct * regimePenalty)
            val allocation = when {
                enhancedScore < 60 -> min(r.allocationMax, 2.0)
                enhancedScore < 70 -> min(r.allocationMax, 3.0)
                trend < 40.0 && momentum < 40.0 -> min(r.allocationMax, 3.0)
                else -> r.allocationMax
            }

            r.copy(
                score = enhancedScore,
                signal = rating(enhancedScore),
                allocationMax = allocation,
                forecastPct = forecast,
                factorScore = enhancedScore.toDouble(),
                source = "ORACLE_ENGINE_V6_ENHANCED"
            )
        }
    }

    private fun factor(values: List<Double>, index: Int): Double =
        values.getOrNull(index)?.takeIf { it.isFinite() } ?: 50.0

    private fun rating(score: Int): String = when {
        score >= 85 -> "STRONG BUY"
        score >= 75 -> "BUY"
        score >= 65 -> "HOLD"
        score >= 55 -> "WATCH"
        else -> "AVOID"
    }
}

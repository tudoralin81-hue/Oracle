from pathlib import Path
import re

ENGINE = Path("app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt")
SECTOR = Path("app/src/main/java/ro/alintudor/oracle/core/OracleSectorAllocation.kt")

engine = ENGINE.read_text(encoding="utf-8")
sector = SECTOR.read_text(encoding="utf-8")

# V5.9.7 authoritative weight profiles. The order MUST match the engine keys:
# News, Breakout, Trend, Momentum, Volume, Support/Resistance,
# Fundamentals, Bollinger, Ichimoku, Market/Sector, Risk/Reward, ADX.
weights_line = '    private val weights=mapOf("SHORT" to intArrayOf(21,18,18,12,16,12,3,4,4,2,2,1),"MEDIUM" to intArrayOf(12,12,12,16,12,9,9,5,5,6,5,4),"LONG" to intArrayOf(6,6,6,19,7,9,18,4,4,9,7,2))'
engine, n = re.subn(r'^    private val weights=.*$', weights_line, engine, count=1, flags=re.M)
if n != 1:
    raise SystemExit("Growth weights anchor not found")

# Sector is an allocation correction only. It must never alter the 12 score weights.
old_horizon = '''    /** Sector modifies the relative importance of the existing 12 factors; no 13th factor is added. */
    private fun horizonScore(c:Map<String,Double>,h:String,sector:String?):Int{
        val w=OracleSectorAllocation.correctedWeights(weights[h]!!,sector)
        val raw=(keys.indices.sumOf{(c[keys[it]]?:50.0)*(w[it]/100.0)}).toInt().coerceIn(0,100)
        return when{raw in 97..100->raw-3;raw in 92..96->raw-1;else->raw}
    }
'''
new_horizon = '''    /** V5.9.7: sector correction is applied only to allocation, never to score. */
    private fun horizonScore(c:Map<String,Double>,h:String,sector:String?):Int{
        val w=weights[h]!!
        val total=w.sum().toDouble()
        val raw=(keys.indices.sumOf{(c[keys[it]]?:50.0)*w[it].toDouble()}/total).toInt().coerceIn(0,100)
        return when{raw in 97..100->raw-3;raw in 92..96->raw-1;else->raw}
    }
'''
if old_horizon not in engine:
    raise SystemExit("Growth horizonScore anchor not found")
engine = engine.replace(old_horizon, new_horizon, 1)

# The displayed Growth weights are the authoritative V5.9.7 profile values.
engine = engine.replace(
    'val correctedWeights=OracleSectorAllocation.correctedWeights(weights[h]!!,sector)',
    'val correctedWeights=weights[h]!!.copyOf()',
    1,
)

ENGINE.write_text(engine, encoding="utf-8")

sector_text = '''package ro.alintudor.oracle.core

/**
 * V5.9.7 sector correction layer.
 * Sector is NOT a 13th scoring component. It changes allocation only.
 * Confirmed factors are the values recorded in the Oracle Growth reference document.
 */
object OracleSectorAllocation {
    private const val MIN_FACTOR = 0.50
    private const val MAX_FACTOR = 1.25
    private const val STEP = 0.025

    /** Allocation correction is separate from the 12-component Growth score. */
    fun factorFor(sector: String?): Double {
        val s = sector?.trim()?.lowercase() ?: return 1.0
        val raw = when {
            s.contains("biotech") || s.contains("biotechnology") -> 0.750
            s.contains("semiconductor") || s.contains("eda") -> 0.900
            s.contains("fintech") || s.contains("financial technology") -> 0.900
            s.contains("cybersecurity") || s.contains("cyber") -> 0.900
            s.contains("artificial intelligence") || s == "ai" || s.contains("ai /") || s.contains("/ ai") -> 0.850
            s.contains("healthcare defensive") || s.contains("defensive healthcare") -> 1.050
            s.contains("healthcare") || s.contains("health care") -> 1.050
            s.contains("industr") -> 1.000
            s.contains("utilities") -> 1.100
            else -> 1.000
        }
        return snapToStep(raw.coerceIn(MIN_FACTOR, MAX_FACTOR))
    }

    /** Allocation final = base allocation * sector factor, rounded to 0.025%. */
    fun apply(baseAllocation: Double, sector: String?): Double =
        snapToStep((baseAllocation * factorFor(sector)).coerceIn(0.0, 8.0))

    /** Retained for API compatibility: sector does not modify score weights in V5.9.7. */
    fun correctedWeights(base: IntArray, sector: String?): IntArray = base.copyOf()

    private fun snapToStep(value: Double): Double = kotlin.math.round(value / STEP) * STEP
}
'''
SECTOR.write_text(sector_text, encoding="utf-8")

# Hard validation against the reference profiles.
expected = {
    "SHORT": "21,18,18,12,16,12,3,4,4,2,2,1",
    "MEDIUM": "12,12,12,16,12,9,9,5,5,6,5,4",
    "LONG": "6,6,6,19,7,9,18,4,4,9,7,2",
}
for k, v in expected.items():
    if v not in engine:
        raise SystemExit(f"Missing authoritative {k} Growth weights")
for token in ["0.750", "0.900", "0.850", "1.000", "1.050", "1.100"]:
    if token not in sector_text:
        raise SystemExit(f"Missing sector factor {token}")
if "correctedWeights(weights[h]!!,sector)" in engine:
    raise SystemExit("Sector must not modify Growth score weights")

print("V5.9.7 Growth weights and allocation-only sector correction applied")

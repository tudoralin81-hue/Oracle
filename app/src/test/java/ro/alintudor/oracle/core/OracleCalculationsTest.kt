package ro.alintudor.oracle.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OracleCalculationsTest {
    @Test
    fun pnlAndMarketValueAreCalculatedFromSharesAndPrice() {
        assertEquals(250.0, OracleCalculations.pnl(100.0, 10.0, 12.5), 0.0001)
        assertEquals(1250.0, OracleCalculations.marketValue(100.0, 12.5), 0.0001)
    }

    @Test
    fun pnlPercentUsesAverageCostAsTheBase() {
        assertEquals(25.0, OracleCalculations.pnlPercent(10.0, 12.5), 0.0001)
        assertEquals(-20.0, OracleCalculations.pnlPercent(10.0, 8.0), 0.0001)
    }

    @Test
    fun weightsSumToOneHundredPercentForNonZeroPortfolio() {
        val result = OracleCalculations.weights(listOf(250.0, 750.0))
        assertEquals(25.0, result[0], 0.0001)
        assertEquals(75.0, result[1], 0.0001)
        assertEquals(100.0, result.sum(), 0.0001)
    }

    @Test
    fun zeroTotalProducesZeroWeightsInsteadOfNaN() {
        assertEquals(listOf(0.0, 0.0), OracleCalculations.weights(listOf(0.0, 0.0)))
    }
}

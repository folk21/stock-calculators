package com.stockcalculators.besttrading;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Negative test suite that focuses on situations where either:
 * <p>no profitable trade exists or the input data is invalid.
 */
public class BestTradingCalculatorNegativeTest {

    /**
     * <p>Prices strictly decrease over time, so any buy followed by any sell
     * produces a non-positive profit. The algorithm must conclude that no trade
     * should be executed.
     */
    @Test
    void shouldReturnNoTradeWhenPricesStrictlyDecreasing() {
        List<Double> lowPrices  = List.of(210.0,205.0,202.0,200.0,198.0,196.0,195.0,193.0,192.0,190.0);
        List<String> lowTimes   = List.of("09:30","09:45","10:00","09:55","10:10","09:50","10:20","09:40","10:05","09:35");
        List<Double> highPrices = List.of(208.0,203.0,200.0,199.0,197.0,195.0,194.0,192.0,191.0,189.0);
        List<String> highTimes  = List.of("15:00","14:30","16:00","15:40","15:20","16:10","15:50","15:15","15:00","16:00");

        LocalDate calculationDate = LocalDate.of(2025, 11, 10);

        BestTradingCalculator calculator = new BestTradingCalculator();
        BestTradingResult result = calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate);

        assertEquals(0, result.maxProfit());
        assertEquals(-1, result.buyDay());
        assertEquals(-1, result.sellDay());
        assertEquals(0, result.buyPrice());
        assertEquals(0, result.sellPrice());
        assertNull(result.buyTime());
        assertNull(result.sellTime());
        assertEquals(calculationDate, result.calculationDate());
    }

    /**
     * <p>All prices are exactly the same for each day, so the maximum achievable
     * profit is zero. Even though it is technically possible to trade, the task
     * specification treats this as "no profit" and the special no-trade result
     * must be returned.
     */
    @Test
    void shouldReturnNoTradeWhenAllPricesAreEqual() {
        List<Double> lowPrices  = List.of(50.0, 50.0, 50.0);
        List<String> lowTimes   = List.of("10:00","10:00","10:00");
        List<Double> highPrices = List.of(50.0, 50.0, 50.0);
        List<String> highTimes  = List.of("15:00","15:00","15:00");

        LocalDate calculationDate = LocalDate.of(2025, 11, 6);

        BestTradingCalculator calculator = new BestTradingCalculator();
        BestTradingResult result = calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate);

        assertEquals(0, result.maxProfit());
        assertEquals(-1, result.buyDay());
        assertEquals(-1, result.sellDay());
        assertEquals(0, result.buyPrice());
        assertEquals(0, result.sellPrice());
        assertNull(result.buyTime());
        assertNull(result.sellTime());
        assertEquals(calculationDate, result.calculationDate());
    }

    /**
     * <p>Only one trading day is available and on that day the intraday high
     * happens before the intraday low. Same-day trading is invalid and there
     * are no later days to sell on, so no trade is possible at all.
     */
    @Test
    void shouldReturnNoTradeWhenSingleDayHighBeforeLow() {
        List<Double> lowPrices  = List.of(100.0);
        List<String> lowTimes   = List.of("15:00"); // low happens late in the day
        List<Double> highPrices = List.of(120.0);
        List<String> highTimes  = List.of("10:00"); // high happens earlier, same-day trade is invalid

        LocalDate calculationDate = LocalDate.of(2025, 11, 6);

        BestTradingCalculator calculator = new BestTradingCalculator();
        BestTradingResult result = calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate);

        assertEquals(0, result.maxProfit());
        assertEquals(-1, result.buyDay());
        assertEquals(-1, result.sellDay());
        assertEquals(0, result.buyPrice());
        assertEquals(0, result.sellPrice());
        assertNull(result.buyTime());
        assertNull(result.sellTime());
        assertEquals(calculationDate, result.calculationDate());
    }

    /**
     * <p>The input lists intentionally have different sizes.
     * <p>The calculator must not try to guess the intended data and should instead
     * fail fast with an {@link IllegalArgumentException}.
     */
    @Test
    void shouldThrowWhenInputListsHaveDifferentSizes() {
        List<Double> lowPrices  = List.of(10.0, 11.0);
        List<String> lowTimes   = List.of("10:00");
        List<Double> highPrices = List.of(12.0, 13.0);
        List<String> highTimes  = List.of("15:00", "15:10");

        LocalDate calculationDate = LocalDate.of(2025, 11, 10);

        BestTradingCalculator calculator = new BestTradingCalculator();
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateBestTradingResult(
                        lowPrices, lowTimes, highPrices, highTimes, calculationDate));
    }

    /**
     * <p>One of the time strings is malformed and cannot be parsed using the
     * {@code HH:mm} pattern. The calculator should convert the parsing error into
     * an {@link IllegalArgumentException} with a clear message.
     */
    @Test
    void shouldThrowWhenTimeFormatIsInvalid() {
        List<Double> lowPrices  = List.of(10.0, 11.0);
        List<String> lowTimes   = List.of("10:00", "invalid");
        List<Double> highPrices = List.of(12.0, 13.0);
        List<String> highTimes  = List.of("15:00", "16:00");

        LocalDate calculationDate = LocalDate.of(2025, 11, 10);

        BestTradingCalculator calculator = new BestTradingCalculator();
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateBestTradingResult(
                        lowPrices, lowTimes, highPrices, highTimes, calculationDate));
    }
}

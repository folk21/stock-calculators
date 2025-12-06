package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stockcalculators.besttrading.model.BestTradingResult;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Negative test suite that focuses on situations where either:
 *
 * <p>no profitable trade exists or the input data is invalid.
 */
public class BestTradingCalculatorNegativeTest {

  @ParameterizedTest
  @MethodSource("noTradeScenarios")
  void shouldReturnNoTradeResult(
      List<Double> lowPrices,
      List<String> lowTimes,
      List<Double> highPrices,
      List<String> highTimes,
      LocalDate calculationDate) {
    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
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

  static Stream<Arguments> noTradeScenarios() {
    return Stream.of(
        // Prices strictly decreasing
        Arguments.of(
            List.of(210.0, 205.0, 202.0, 200.0, 198.0, 196.0, 195.0, 193.0, 192.0, 190.0),
            List.of("09:30", "09:45", "10:00", "09:55", "10:10", "09:50", "10:20", "09:40", "10:05", "09:35"),
            List.of(208.0, 203.0, 200.0, 199.0, 197.0, 195.0, 194.0, 192.0, 191.0, 189.0),
            List.of("15:00", "14:30", "16:00", "15:40", "15:20", "16:10", "15:50", "15:15", "15:00", "16:00"),
            LocalDate.of(2025, 11, 10)),
        // All prices equal
        Arguments.of(
            List.of(50.0, 50.0, 50.0),
            List.of("10:00", "10:00", "10:00"),
            List.of(50.0, 50.0, 50.0),
            List.of("15:00", "15:00", "15:00"),
            LocalDate.of(2025, 11, 6)),
        // Single day with high before low
        Arguments.of(
            List.of(100.0),
            List.of("15:00"),
            List.of(120.0),
            List.of("10:00"),
            LocalDate.of(2025, 11, 6))
    );
  }

  @ParameterizedTest
  @MethodSource("invalidInputScenarios")
  void shouldThrowIllegalArgumentException(
      List<Double> lowPrices,
      List<String> lowTimes,
      List<Double> highPrices,
      List<String> highTimes) {
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);
    BestTradingCalculator calculator = new BestTradingCalculator();
    
    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  static Stream<Arguments> invalidInputScenarios() {
    return Stream.of(
        // Different list sizes
        Arguments.of(
            List.of(10.0, 11.0),
            List.of("10:00"),
            List.of(12.0, 13.0),
            List.of("15:00", "15:10")),
        // Invalid time format
        Arguments.of(
            List.of(10.0, 11.0),
            List.of("10:00", "invalid"),
            List.of(12.0, 13.0),
            List.of("15:00", "16:00"))
    );
  }
}

package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Positive test suite that verifies typical profitable trading scenarios.
 *
 * <p>Each test focuses on a slightly different aspect of the algorithm:
 *
 * <p>cross-week trades, same-day spikes, ignoring invalid same-day combinations and correct
 * handling of different calculation dates.
 */
public class BestTradingCalculatorPositiveTest {

  @ParameterizedTest
  @MethodSource("calculationDateScenarios")
  void shouldFindBestTradeForDifferentCalculationDates(
      LocalDate calculationDate, Instant expectedBuyTime, Instant expectedSellTime) {
    List<Double> lowPrices = List.of(10.0, 12.0, 11.0, 13.0, 15.0, 16.0, 18.0, 19.0, 21.0, 22.0);
    List<String> lowTimes =
        List.of(
            "10:00", "09:45", "11:10", "09:50", "10:15", "09:55", "10:05", "09:40", "10:00",
            "09:35");
    List<Double> highPrices = List.of(14.0, 15.0, 16.0, 18.0, 20.0, 21.0, 23.0, 24.0, 26.0, 27.0);
    List<String> highTimes =
        List.of(
            "15:00", "16:00", "14:30", "15:40", "16:10", "15:20", "16:00", "15:10", "15:25",
            "16:05");

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    assertEquals(17, result.maxProfit());
    assertEquals(0, result.buyDay());
    assertEquals(9, result.sellDay());
    assertEquals(10, result.buyPrice());
    assertEquals(27, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());
    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  static Stream<Arguments> calculationDateScenarios() {
    return Stream.of(
        // Monday calculation date
        Arguments.of(
            LocalDate.of(2025, 11, 10),
            ZonedDateTime.of(LocalDate.of(2025, 10, 27), LocalTime.of(10, 0), ZoneOffset.UTC)
                .toInstant(),
            ZonedDateTime.of(LocalDate.of(2025, 11, 7), LocalTime.of(16, 5), ZoneOffset.UTC)
                .toInstant()),
        // Sunday calculation date
        Arguments.of(
            LocalDate.of(2025, 11, 9),
            ZonedDateTime.of(LocalDate.of(2025, 10, 27), LocalTime.of(10, 0), ZoneOffset.UTC)
                .toInstant(),
            ZonedDateTime.of(LocalDate.of(2025, 11, 7), LocalTime.of(16, 5), ZoneOffset.UTC)
                .toInstant()),
        // Tuesday calculation date
        Arguments.of(
            LocalDate.of(2025, 11, 11),
            ZonedDateTime.of(LocalDate.of(2025, 10, 28), LocalTime.of(10, 0), ZoneOffset.UTC)
                .toInstant(),
            ZonedDateTime.of(LocalDate.of(2025, 11, 10), LocalTime.of(16, 5), ZoneOffset.UTC)
                .toInstant()));
  }

  /**
   * Scenario with clear intraday spike where buying and selling on the same day produces the
   * highest possible profit.
   *
   * <p>Even though prices also move across days, the algorithm must prefer the better same-day
   * opportunity.
   */
  @Test
  void shouldUseSameDaySpikeWhenItIsBetterThanAnyCrossDayTrade() {
    List<Double> lowPrices = List.of(50.0, 52.0, 48.0, 47.0, 49.0, 46.0, 45.0, 44.0, 43.0, 42.0);
    List<String> lowTimes =
        List.of(
            "11:00", "10:20", "11:10", "10:40", "11:30", "10:15", "11:05", "10:50", "11:25",
            "10:45");
    List<Double> highPrices = List.of(55.0, 58.0, 52.0, 80.0, 60.0, 59.0, 57.0, 56.0, 54.0, 53.0);
    List<String> highTimes =
        List.of(
            "15:20", "15:45", "16:00", "15:10", "15:55", "15:30", "16:05", "15:40", "15:15",
            "16:20");

    LocalDate calculationDate =
        LocalDate.of(2025, 11, 10); // Monday after the last Friday (2025-11-07)

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    assertEquals(33, result.maxProfit());
    assertEquals(3, result.buyDay());
    assertEquals(3, result.sellDay());
    assertEquals(47, result.buyPrice());
    assertEquals(80, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 10, 30), LocalTime.of(10, 40), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 10, 30), LocalTime.of(15, 10), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  /**
   * Scenario where some intraday highs happen before intraday lows for the same day.
   *
   * <p>Such same-day combinations are invalid, but cross-day trading is still possible. The test
   * verifies that invalid same-day trades are ignored while valid cross-day trades are still
   * considered.
   */
  @Test
  void shouldIgnoreInvalidSameDayAndUseCrossDayTrade() {
    List<Double> lowPrices = List.of(100.0, 98.0, 99.0, 97.0, 95.0, 96.0, 94.0, 93.0, 92.0, 91.0);
    List<String> lowTimes =
        List.of(
            "14:00", "13:30", "12:50", "14:20", "13:45", "14:10", "13:55", "14:05", "13:50",
            "14:15");
    List<Double> highPrices =
        List.of(120.0, 110.0, 108.0, 115.0, 118.0, 125.0, 130.0, 135.0, 140.0, 150.0);
    List<String> highTimes =
        List.of(
            "09:30", "10:00", "09:45", "10:15", "10:40", "10:20", "09:50", "10:30", "09:55",
            "10:10");

    LocalDate calculationDate =
        LocalDate.of(2025, 11, 10); // Monday after the last Friday (2025-11-07)

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    assertEquals(58, result.maxProfit());
    assertEquals(8, result.buyDay());
    assertEquals(9, result.sellDay());
    assertEquals(92, result.buyPrice());
    assertEquals(150, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 6), LocalTime.of(13, 50), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 7), LocalTime.of(10, 10), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  /**
   * Scenario directly taken from the problem statement where a cross-week trade yields the best
   * profit by buying on the second day and selling on the ninth day.
   */
  @Test
  void shouldMatchProblemStatementExample() {
    List<Double> lowPrices =
        List.of(100.0, 98.0, 102.0, 104.0, 107.0, 110.0, 108.0, 111.0, 113.0, 112.0);
    List<String> lowTimes =
        List.of(
            "10:00", "11:00", "09:30", "10:15", "09:45", "11:20", "09:55", "10:10", "09:40",
            "10:25");
    List<Double> highPrices =
        List.of(103.0, 105.0, 106.0, 108.0, 112.0, 114.0, 116.0, 117.0, 119.0, 118.0);
    List<String> highTimes =
        List.of(
            "15:00", "16:00", "14:30", "15:45", "16:10", "15:30", "16:00", "15:00", "15:15",
            "16:30");

    LocalDate calculationDate =
        LocalDate.of(2025, 11, 10); // Monday after the last Friday (2025-11-07)

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    assertEquals(21, result.maxProfit());
    assertEquals(1, result.buyDay());
    assertEquals(8, result.sellDay());
    assertEquals(98, result.buyPrice());
    assertEquals(119, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 10, 28), LocalTime.of(11, 0), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 6), LocalTime.of(15, 15), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  /**
   * Scenario where the input lists are empty (no trading days at all).
   *
   * <p>The calculator must handle this gracefully and return the special no-trade result without
   * throwing any exceptions.
   */
  @Test
  void shouldReturnNoTradeForEmptyList() {
    List<Double> lowPrices = List.of();
    List<String> lowTimes = List.of();
    List<Double> highPrices = List.of();
    List<String> highTimes = List.of();

    LocalDate calculationDate = LocalDate.of(2025, 11, 10); // Monday

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    assertEquals(0, result.maxProfit());
    assertEquals(-1, result.buyDay());
    assertEquals(-1, result.sellDay());
    assertEquals(0, result.buyPrice());
    assertEquals(0, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());
  }

  /**
   * Simple monotonic increasing scenario over one trading week (Monday to Friday).
   *
   * <p>The lowest price is on Monday, and prices increase every day until Friday, which has the
   * highest price. The best trade must be: buy on Monday, sell on Friday.
   */
  @Test
  void shouldPickMondayToFridayInMonotonicIncreasingWeek() {
    // Prices strictly increasing across the 5 trading days
    List<Double> lowPrices = List.of(10.0, 11.0, 12.0, 13.0, 14.0);
    List<String> lowTimes = List.of("10:00", "10:00", "10:00", "10:00", "10:00");

    List<Double> highPrices = List.of(11.0, 12.0, 13.0, 14.0, 15.0);
    List<String> highTimes = List.of("15:00", "15:00", "15:00", "15:00", "15:00");

    // Monday; the 5 trading days are Mon–Fri of the previous week:
    // 0 -> 2025-11-03 (Mon)
    // 1 -> 2025-11-04 (Tue)
    // 2 -> 2025-11-05 (Wed)
    // 3 -> 2025-11-06 (Thu)
    // 4 -> 2025-11-07 (Fri)
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    // Best is: buy at 10 on Monday, sell at 15 on Friday
    assertEquals(5, result.maxProfit());
    assertEquals(0, result.buyDay());
    assertEquals(4, result.sellDay());
    assertEquals(10, result.buyPrice());
    assertEquals(15, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 3), LocalTime.of(10, 0), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 7), LocalTime.of(15, 0), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  /**
   * Scenario where the best trade is clearly buying on Monday for 1 and selling on Wednesday for 7,
   * even though there are other later days with positive (but smaller) profit.
   *
   * <p>Prices (low = high for simplicity):
   *
   * <ul>
   *   <li>Mon = 1
   *   <li>Tue = 2
   *   <li>Wed = 7
   *   <li>Thu = 3
   *   <li>Fri = 5
   * </ul>
   *
   * The best trade is: buy Monday (1), sell Wednesday (7) with profit 6.
   */
  @Test
  void shouldPickMondayToWednesdayWhenWednesdayHasHighestPrice() {
    // Using identical low/high so daily price is effectively a single "cost".
    List<Double> lowPrices = List.of(1.0, 2.0, 7.0, 3.0, 5.0);
    List<String> lowTimes = List.of("10:00", "10:00", "10:00", "10:00", "10:00");

    List<Double> highPrices = List.of(1.0, 2.0, 7.0, 3.0, 5.0);
    List<String> highTimes = List.of("15:00", "15:00", "15:00", "15:00", "15:00");

    // Same mapping of indices to dates as in the previous test.
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    BestTradingCalculator calculator = new BestTradingCalculator();
    BestTradingResult result =
        calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    // Best is: buy at 1 on Monday (index 0), sell at 7 on Wednesday (index 2)
    assertEquals(6, result.maxProfit());
    assertEquals(0, result.buyDay());
    assertEquals(2, result.sellDay());
    assertEquals(1, result.buyPrice());
    assertEquals(7, result.sellPrice());
    assertEquals(calculationDate, result.calculationDate());

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 3), LocalTime.of(10, 0), ZoneOffset.UTC)
            .toInstant(); // Monday
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 5), LocalTime.of(15, 0), ZoneOffset.UTC)
            .toInstant(); // Wednesday

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }
}

package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Positive test suite that verifies typical profitable trading scenarios.
 *
 * <p>Each test focuses on a slightly different aspect of the algorithm:
 *
 * <p>cross-week trades, same-day spikes, ignoring invalid same-day combinations and correct
 * handling of different calculation dates.
 */
public class BestTradingCalculatorPositiveTest {

  /**
   * Scenario based on a steadily rising market over two trading weeks.
   *
   * <p>The optimal strategy is to buy on the very first day and sell on the very last day. The test
   * verifies that cross-week profit is correctly detected when the calculation date is a Monday.
   */
  @Test
  void shouldFindBestTradeForMonotonicIncreaseAcrossTwoWeeksWhenCalculationDateIsMonday() {
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

    LocalDate calculationDate = LocalDate.of(2025, 11, 10); // Monday after last Friday (2025-11-07)

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

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 10, 27), LocalTime.of(10, 0), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 7), LocalTime.of(16, 5), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  /**
   * Scenario based on the same monotonically increasing market, but the calculation date is a
   * Sunday. The last trading day before Sunday is still Friday, so the calendar mapping and the
   * optimal trade remain exactly the same.
   */
  @Test
  void shouldFindBestTradeWhenCalculationDateIsSunday() {
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

    LocalDate calculationDate =
        LocalDate.of(2025, 11, 9); // Sunday, last trading day is still 2025-11-07 (Friday)

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

    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 10, 27), LocalTime.of(10, 0), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 7), LocalTime.of(16, 5), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
  }

  /**
   * Scenario based on the same monotonically increasing market, but the calculation date is a
   * Tuesday. In this case, the last trading day before Tuesday is Monday, so the calendar mapping
   * is shifted by one business day.
   */
  @Test
  void shouldFindBestTradeWhenCalculationDateIsTuesday() {
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

    LocalDate calculationDate = LocalDate.of(2025, 11, 11); // Tuesday

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

    // <p>With Tuesday as calculation date, the last trading day is Monday 2025-11-10.
    // <p>Walking backwards over business days yields:
    // <p>index 0 -> 2025-10-28 (Tuesday)
    // <p>index 9 -> 2025-11-10 (Monday)
    Instant expectedBuyTime =
        ZonedDateTime.of(LocalDate.of(2025, 10, 28), LocalTime.of(10, 0), ZoneOffset.UTC)
            .toInstant();
    Instant expectedSellTime =
        ZonedDateTime.of(LocalDate.of(2025, 11, 10), LocalTime.of(16, 5), ZoneOffset.UTC)
            .toInstant();

    assertEquals(expectedBuyTime, result.buyTime());
    assertEquals(expectedSellTime, result.sellTime());
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
}

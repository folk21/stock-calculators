package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests that verify how {@link BestTradingCalculator} behaves when the input data is logically
 * invalid or inconsistent (but not null).
 *
 * <p>This covers cases such as: - list size mismatches, - invalid time formats, - impossible time
 * values, - empty or malformed time strings, - null elements inside time lists.
 */
class BestTradingCalculatorInvalidDataTest {

  private final BestTradingCalculator calculator = new BestTradingCalculator();

  /**
   * When lowTimes has fewer elements than lowPrices, the calculator must fail fast with an
   * IllegalArgumentException.
   */
  @Test
  void differentListSizes_lowTimesShorter_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0, 11.0);
    List<String> lowTimes = List.of("10:00"); // size 1 instead of 2
    List<Double> highPrices = List.of(12.0, 13.0);
    List<String> highTimes = List.of("15:00", "15:10");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * When highPrices has fewer elements than lowPrices, the calculator must fail fast with an
   * IllegalArgumentException.
   */
  @Test
  void differentListSizes_highPricesShorter_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0, 11.0);
    List<String> lowTimes = List.of("10:00", "10:05");
    List<Double> highPrices = List.of(12.0); // size 1 instead of 2
    List<String> highTimes = List.of("15:00", "15:10");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * When highTimes has fewer elements than lowPrices, the calculator must fail fast with an
   * IllegalArgumentException.
   */
  @Test
  void differentListSizes_highTimesShorter_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0, 11.0);
    List<String> lowTimes = List.of("10:00", "10:05");
    List<Double> highPrices = List.of(12.0, 13.0);
    List<String> highTimes = List.of("15:00"); // size 1 instead of 2
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * Time values must match the HH:mm pattern. A value such as "9:30" (missing leading zero) is
   * considered invalid and must cause an IllegalArgumentException.
   */
  @Test
  void invalidTimeFormat_missingLeadingZero_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0);
    List<String> lowTimes = List.of("9:30"); // not HH:mm
    List<Double> highPrices = List.of(12.0);
    List<String> highTimes = List.of("15:00");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * Time values must represent a real time of day. A value such as "25:00" is impossible and must
   * cause an IllegalArgumentException.
   */
  @Test
  void impossibleTimeValue_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0);
    List<String> lowTimes = List.of("25:00"); // impossible hour
    List<Double> highPrices = List.of(12.0);
    List<String> highTimes = List.of("15:00");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * Arbitrary non-time strings such as "aa:bb" must also be rejected and lead to an
   * IllegalArgumentException.
   */
  @Test
  void nonParsableTimeString_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0);
    List<String> lowTimes = List.of("aa:bb"); // not a time at all
    List<Double> highPrices = List.of(12.0);
    List<String> highTimes = List.of("15:00");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * An empty string is not a valid time representation and must cause an IllegalArgumentException.
   */
  @Test
  void emptyTimeString_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0);
    List<String> lowTimes = List.of(""); // empty string is not a valid time
    List<Double> highPrices = List.of(12.0);
    List<String> highTimes = List.of("15:00");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * A blank string (only whitespace) is also invalid and must cause an IllegalArgumentException.
   */
  @Test
  void blankTimeString_shouldThrowIllegalArgumentException() {
    List<Double> lowPrices = List.of(10.0);
    List<String> lowTimes = List.of("   "); // blanks only
    List<Double> highPrices = List.of(12.0);
    List<String> highTimes = List.of("15:00");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  /**
   * A null element inside the time lists is logically invalid input data. Depending on the
   * implementation of parseTime this may result in either an IllegalArgumentException or a
   * NullPointerException, so we assert that some RuntimeException is thrown.
   */
  @Test
  void nullElementInsideTimeList_shouldThrowRuntimeException() {
    List<Double> lowPrices = List.of(10.0);
    // Arrays.asList allows null elements, unlike List.of
    List<String> lowTimes = Arrays.asList((String) null);
    List<Double> highPrices = List.of(12.0);
    List<String> highTimes = List.of("15:00");
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        RuntimeException.class,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }
}

package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests that verify how {@link BestTradingCalculator} behaves when the input data is logically
 * invalid or inconsistent (but not null).
 *
 * <p>This covers cases such as: - list size mismatches, - invalid time formats, - impossible time
 * values, - empty or malformed time strings, - null elements inside time lists.
 */
class BestTradingCalculatorInvalidDataTest {

  private final BestTradingCalculator calculator = new BestTradingCalculator();

  @ParameterizedTest
  @MethodSource("invalidDataProvider")
  void shouldThrowExceptionForInvalidData(
      List<Double> lowPrices,
      List<String> lowTimes,
      List<Double> highPrices,
      List<String> highTimes,
      Class<? extends RuntimeException> expectedException) {
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    assertThrows(
        expectedException,
        () ->
            calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }

  static Stream<Arguments> invalidDataProvider() {
    return Stream.of(
        // List size mismatches
        Arguments.of(
            List.of(10.0, 11.0),
            List.of("10:00"),
            List.of(12.0, 13.0),
            List.of("15:00", "15:10"),
            IllegalArgumentException.class),
        Arguments.of(
            List.of(10.0, 11.0),
            List.of("10:00", "10:05"),
            List.of(12.0),
            List.of("15:00", "15:10"),
            IllegalArgumentException.class),
        Arguments.of(
            List.of(10.0, 11.0),
            List.of("10:00", "10:05"),
            List.of(12.0, 13.0),
            List.of("15:00"),
            IllegalArgumentException.class),
        // Invalid time formats
        Arguments.of(
            List.of(10.0),
            List.of("9:30"),
            List.of(12.0),
            List.of("15:00"),
            IllegalArgumentException.class),
        Arguments.of(
            List.of(10.0),
            List.of("25:00"),
            List.of(12.0),
            List.of("15:00"),
            IllegalArgumentException.class),
        Arguments.of(
            List.of(10.0),
            List.of("aa:bb"),
            List.of(12.0),
            List.of("15:00"),
            IllegalArgumentException.class),
        Arguments.of(
            List.of(10.0),
            List.of(""),
            List.of(12.0),
            List.of("15:00"),
            IllegalArgumentException.class),
        Arguments.of(
            List.of(10.0),
            List.of("   "),
            List.of(12.0),
            List.of("15:00"),
            IllegalArgumentException.class),
        // Null element in time list
        Arguments.of(
            List.of(10.0),
            Arrays.asList((String) null),
            List.of(12.0),
            List.of("15:00"),
            RuntimeException.class));
  }
}

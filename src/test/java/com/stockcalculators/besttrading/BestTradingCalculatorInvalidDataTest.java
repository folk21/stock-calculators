package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BestTradingCalculatorInvalidDataTest {

  private final BestTradingCalculator calculator = new BestTradingCalculator();

  // ... остальные тесты ...

  /**
   * A null element inside the time lists is logically invalid input data.
   * Depending on the implementation of parseTime this may result in either
   * an IllegalArgumentException or a NullPointerException, so we assert
   * that some RuntimeException is thrown.
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
        () -> calculator.calculateBestTradingResult(
            lowPrices, lowTimes, highPrices, highTimes, calculationDate));
  }
}

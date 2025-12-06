package com.stockcalculators.besttrading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockcalculators.besttrading.model.BestTradingResult;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link BestTradingResult#toPrettyString()}. */
class BestTradingResultPrettyStringTest {

  /**
   * When there is no profitable trade (maxProfit <= 0 or invalid day indices),
   *
   * <p>the method must return a fixed multi-line message describing that situation.
   */
  @Test
  void shouldFormatNoTradeResult() {
    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    BestTradingResult result = BestTradingResult.noTrade(calculationDate);

    String pretty = result.toPrettyString();

    String expected = """
        No profitable trade was found.
        maxProfit = 0
        buyDay = -1, sellDay = -1
        buyPrice = 0, sellPrice = 0
        calculationDate = 2025-11-10""";

    assertEquals(expected, pretty);
  }

  /**
   * For a valid profitable trade, the method must produce a human readable multi-line
   *
   * <p>description that includes:
   *
   * <p>- first line with day indices and integer prices,
   *
   * <p>- second line with maxProfit,
   *
   * <p>- third and fourth lines with day labels, prices and timestamps,
   *
   * <p>- final line with the calculation date.
   */
  @Test
  void shouldFormatProfitableTradeResult() {
    // Example similar to the task description:
    // Best buy is Day 0 at 10 → sell Day 9 at 27
    int maxProfit = 17;
    int buyDay = 0;
    int sellDay = 9;
    int buyPrice = 10;
    int sellPrice = 27;

    // Timestamps are created as Instants; DateTimeUtils.formatInstant is expected
    // to render them as "yyyy-MM-dd'T'HH:mm" in UTC.
    Instant buyTime = Instant.parse("2025-10-27T10:00:00Z");
    Instant sellTime = Instant.parse("2025-11-07T16:05:00Z");

    LocalDate calculationDate = LocalDate.of(2025, 11, 10);

    BestTradingResult result =
        new BestTradingResult(
            maxProfit, buyDay, sellDay, buyPrice, buyTime, sellPrice, sellTime, calculationDate);

    String pretty = result.toPrettyString();

    String expected = """
        Best buy is Day 0 at 10 → sell Day 9 at 27
        maxProfit = 17
        buyDay = 0 (Monday),  buyPrice = 10.00 at 2025-10-27T10:00
        sellDay = 9 (Friday next week), sellPrice = 27.00 at 2025-11-07T16:05
        calculationDate = 2025-11-10""";

    // Full equality check ensures that formatting (line breaks, spaces, day labels)
    // matches the expected pretty-print contract exactly.
    assertEquals(expected, pretty);

    // Optionally, you can add a few more focused assertions if you want clearer
    // failure messages for specific pieces:
    assertTrue(pretty.contains("Best buy is Day 0 at 10 → sell Day 9 at 27"));
    assertTrue(pretty.contains("maxProfit = 17"));
    assertTrue(pretty.contains("buyDay = 0 (Monday),  buyPrice = 10.00 at 2025-10-27T10:00"));
    assertTrue(
        pretty.contains("sellDay = 9 (Friday next week), sellPrice = 27.00 at 2025-11-07T16:05"));
    assertTrue(pretty.contains("calculationDate = 2025-11-10"));
  }
}

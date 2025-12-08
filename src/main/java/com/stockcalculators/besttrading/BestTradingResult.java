package com.stockcalculators.besttrading;

import com.stockcalculators.util.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents the outcome of searching for the single best buy/sell transaction.
 *
 * <p>The record is immutable and holds both numeric indices (days and prices), precise timestamps
 * as {@link Instant} values and the logical calculation date.
 */
public record BestTradingResult(
    int maxProfit,
    int buyDay,
    int sellDay,
    int buyPrice,
    Instant buyTime,
    int sellPrice,
    Instant sellTime,
    LocalDate calculationDate) {

  /**
   * Creates a special instance that represents that no profitable trade exists.
   *
   * <p>Indices are set to {@code -1}, prices to {@code 0}, timestamps to {@code null}, while the
   * provided calculation date is preserved for traceability.
   *
   * @param calculationDate logical date for which the calculation was performed
   * @return result that can be used when the best achievable profit is not positive
   */
  public static BestTradingResult noTrade(LocalDate calculationDate) {
    return new BestTradingResult(0, -1, -1, 0, null, 0, null, calculationDate);
  }

  /**
   * Returns a human readable description of this result that matches the textual format from the
   * task description.
   *
   * <p>If no profitable trade exists, a short explanatory message is returned instead.
   *
   * @return formatted multi-line string describing the best trade
   */
  public String toPrettyString() {
    if (maxProfit <= 0 || buyDay < 0 || sellDay < 0) {
      return "No profitable trade was found.\n"
          + "maxProfit = 0\n"
          + "buyDay = -1, sellDay = -1\n"
          + "buyPrice = 0, sellPrice = 0\n"
          + "calculationDate = "
          + (calculationDate != null ? calculationDate : "n/a");
    }

    String buyDayLabel = DateTimeUtils.formatDayLabel(buyDay);
    String sellDayLabel = DateTimeUtils.formatDayLabel(sellDay);

    String buyDateTime = DateTimeUtils.formatInstant(buyTime);
    String sellDateTime = DateTimeUtils.formatInstant(sellTime);

    String firstLine =
        String.format(
            "Best buy is Day %d at %d \u2192 sell Day %d at %d",
            buyDay, buyPrice, sellDay, sellPrice);

    String secondLine = String.format("maxProfit = %d", maxProfit);

    String thirdLine =
        String.format(
            "buyDay = %d (%s),  buyPrice = %.2f at %s",
            buyDay, buyDayLabel, (double) buyPrice, buyDateTime);

    String fourthLine =
        String.format(
            "sellDay = %d (%s), sellPrice = %.2f at %s",
            sellDay, sellDayLabel, (double) sellPrice, sellDateTime);

    String fifthLine =
        String.format(
            "calculationDate = %s", calculationDate != null ? calculationDate.toString() : "n/a");

    return String.join("\n", firstLine, secondLine, thirdLine, fourthLine, fifthLine);
  }
}

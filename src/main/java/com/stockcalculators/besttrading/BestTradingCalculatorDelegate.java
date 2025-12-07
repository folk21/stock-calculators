package com.stockcalculators.besttrading;

import static com.stockcalculators.util.ListUtils.FIRST_INDEX;

import com.stockcalculators.besttrading.model.BestTradeState;
import com.stockcalculators.besttrading.model.TimeSeriesData;
import com.stockcalculators.util.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Internal helper for {@link BestTradingCalculator} that contains methods utilized by {@link
 * BestTradingCalculator#calculateBestTradingResult(List, List, List, List, LocalDate)}
 */
class BestTradingCalculatorDelegate {

  /** Minimum number of trading days required to consider a cross-day trade. */
  private static final int MIN_DAYS_FOR_CROSS_DAY_TRADE = 2;

  /** Validates the basic preconditions for the calculation. */
  static void validateInputs(
      final List<Double> lowPrices,
      final List<String> lowTimes,
      final List<Double> highPrices,
      final List<String> highTimes,
      final LocalDate calculationDate) {

    Objects.requireNonNull(lowPrices, "lowPrices must not be null");
    Objects.requireNonNull(lowTimes, "lowTimes must not be null");
    Objects.requireNonNull(highPrices, "highPrices must not be null");
    Objects.requireNonNull(highTimes, "highTimes must not be null");
    Objects.requireNonNull(calculationDate, "calculationDate must not be null");

    int numberOfTradingDays = lowPrices.size();
    if (lowTimes.size() != numberOfTradingDays
        || highPrices.size() != numberOfTradingDays
        || highTimes.size() != numberOfTradingDays) {
      throw new IllegalArgumentException("All input lists must have the same size");
    }
  }

  /** Prepares arrays of dates, parsed times and instants for each trading day. */
  static TimeSeriesData buildTimeSeriesData(
      final List<String> lowTimes,
      final List<String> highTimes,
      final LocalDate calculationDate,
      final int numberOfTradingDays) {

    // Find the last trading day before the calculation date (skipping weekends)
    LocalDate lastTradingDate = DateTimeUtils.findLastTradingDateBefore(calculationDate);

    // Build array of trading dates walking backwards from the last trading date
    LocalDate[] tradingDates =
        DateTimeUtils.buildTradingDates(lastTradingDate, numberOfTradingDays);

    // Initialize arrays to hold parsed times and UTC instants for each trading day
    LocalTime[] lowLocalTimes = new LocalTime[numberOfTradingDays];
    LocalTime[] highLocalTimes = new LocalTime[numberOfTradingDays];
    Instant[] lowInstants = new Instant[numberOfTradingDays];
    Instant[] highInstants = new Instant[numberOfTradingDays];

    for (int i = FIRST_INDEX; i < numberOfTradingDays; i++) {
      // Parse time strings (HH:mm format) into LocalTime objects
      LocalTime lowTime = DateTimeUtils.parseTime(lowTimes.get(i), "lowTimes", i);
      LocalTime highTime = DateTimeUtils.parseTime(highTimes.get(i), "highTimes", i);

      // Store parsed times in arrays
      lowLocalTimes[i] = lowTime;
      highLocalTimes[i] = highTime;

      // Combine trading date with time to create UTC ZonedDateTime
      ZonedDateTime lowZonedDateTime = DateTimeUtils.toUtcZonedDateTime(tradingDates[i], lowTime);
      ZonedDateTime highZonedDateTime = DateTimeUtils.toUtcZonedDateTime(tradingDates[i], highTime);

      // Convert to Instant for precise timestamp comparisons
      lowInstants[i] = lowZonedDateTime.toInstant();
      highInstants[i] = highZonedDateTime.toInstant();
    }

    // Return immutable record containing all time series data
    return new TimeSeriesData(
        tradingDates, lowLocalTimes, highLocalTimes, lowInstants, highInstants);
  }

  /**
   * Finds the best cross-day trade (buy on one day, sell on a strictly later day) using the classic
   * "minimum so far" approach.
   */
  static BestTradeState findBestCrossDayTrade(
      final List<Double> lowPrices,
      final List<Double> highPrices,
      final TimeSeriesData timeSeriesData) {

    int numberOfTradingDays = lowPrices.size();

    // Cross-day trades require at least 2 days (buy on one, sell on another)
    if (numberOfTradingDays < MIN_DAYS_FOR_CROSS_DAY_TRADE) {
      return BestTradeState.createEmpty();
    }

    // Initialize variables to track the best trade found so far
    BestTradeState bestTradeState = BestTradeState.createEmpty();

    // Track the minimum low price seen so far and which day it occurred
    int dayOfMinimumLowPrice = FIRST_INDEX;
    double minimumLowPriceSoFar = lowPrices.get(dayOfMinimumLowPrice);

    // Start from day 1 since we need to buy before we sell
    for (int sellDayIndex = 1; sellDayIndex < numberOfTradingDays; sellDayIndex++) {

      // Calculate profit if we sell today at the high price and bought at the minimum low so far
      double potentialProfit = highPrices.get(sellDayIndex) - minimumLowPriceSoFar;

      // Update best trade if this profit is better than what we've seen
      if (potentialProfit > bestTradeState.getBestProfit()) {
        bestTradeState.updateTrade(
            potentialProfit,
            dayOfMinimumLowPrice,
            sellDayIndex,
            minimumLowPriceSoFar,
            highPrices.get(sellDayIndex),
            timeSeriesData.lowInstants()[dayOfMinimumLowPrice],
            timeSeriesData.highInstants()[sellDayIndex]);
      }

      // Update minimum price tracking for future sell days
      double currentLowPrice = lowPrices.get(sellDayIndex);
      if (currentLowPrice < minimumLowPriceSoFar) {
        minimumLowPriceSoFar = currentLowPrice;
        dayOfMinimumLowPrice = sellDayIndex;
      }
    }

    // Return empty state if no profitable trade was found
    if (!bestTradeState.hasProfitableTrade()) {
      return BestTradeState.createEmpty();
    }

    // Return the best cross-day trade found
    return bestTradeState;
  }

  /**
   * Considers same-day trades (buy low, sell high within the same day) and updates the best trade
   * if a better profit is found, respecting the rule that the low time must be strictly earlier
   * than the high time.
   */
  static void updateWithSameDayTrades(
      final BestTradeState currentBestTrade,
      final List<Double> lowPrices,
      final List<Double> highPrices,
      final TimeSeriesData timeSeriesData) {

    int numberOfTradingDays = lowPrices.size();

    // Check each trading day for potential same-day trades
    for (int tradingDayIndex = FIRST_INDEX;
        tradingDayIndex < numberOfTradingDays;
        tradingDayIndex++) {

      // Get the low and high times for this trading day
      LocalTime lowTime = timeSeriesData.lowLocalTimes()[tradingDayIndex];
      LocalTime highTime = timeSeriesData.highLocalTimes()[tradingDayIndex];

      // Skip this day if high occurred before or at same time as low (invalid same-day trade)
      if (!lowTime.isBefore(highTime)) {
        continue;
      }

      // Calculate profit for buying at low and selling at high on the same day
      double potentialProfit = highPrices.get(tradingDayIndex) - lowPrices.get(tradingDayIndex);

      // Update the best trade if this same-day profit beats the current best
      if (potentialProfit > currentBestTrade.getBestProfit()) {
        currentBestTrade.updateTrade(
            potentialProfit,
            tradingDayIndex,
            tradingDayIndex,
            lowPrices.get(tradingDayIndex),
            highPrices.get(tradingDayIndex),
            timeSeriesData.lowInstants()[tradingDayIndex],
            timeSeriesData.highInstants()[tradingDayIndex]);
      }
    }
  }
}

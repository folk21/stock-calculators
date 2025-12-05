package com.stockcalculators.besttrading;

import com.stockcalculators.util.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

/**
 * Internal helper for {@link BestTradingCalculator} that performs the actual calculation of the
 * best single buy/sell transaction.
 *
 * <p>The class is intentionally package-private and should not be used from outside the {@code
 * com.stockcalculators.besttrading} package.
 */
@UtilityClass
class BestTraidingUtils {

  /**
   * Computes the best possible single buy/sell transaction.
   *
   * <p>The data represents the last {@code N} trading days (Monday to Friday only) before the given
   * calculation date.
   *
   * @param lowPrices intraday lowest prices, one per trading day
   * @param lowTimes intraday times of the lows, formatted as {@code HH:mm}
   * @param highPrices intraday highest prices, one per trading day
   * @param highTimes intraday times of the highs, formatted as {@code HH:mm}
   * @param calculationDate logical date of the calculation; the last input day corresponds to the
   *     last trading day strictly before this date
   * @return best trading result or {@link BestTradingResult#noTrade(LocalDate)} if no positive
   *     profit is possible
   */
  static BestTradingResult calculateBestTradingResultInternal(
      List<Double> lowPrices,
      List<String> lowTimes,
      List<Double> highPrices,
      List<String> highTimes,
      LocalDate calculationDate) {

    validateInputs(lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    int numberOfTradingDays = lowPrices.size();
    if (numberOfTradingDays == 0) {
      // No data at all means that no trade is possible.
      return BestTradingResult.noTrade(calculationDate);
    }

    TimeSeriesData timeSeriesData =
        buildTimeSeriesData(lowTimes, highTimes, calculationDate, numberOfTradingDays);

    BestTradeState bestTradeState = findBestCrossDayTrade(lowPrices, highPrices, timeSeriesData);

    bestTradeState = updateWithSameDayTrades(bestTradeState, lowPrices, highPrices, timeSeriesData);

    if (!bestTradeState.hasProfitableTrade()) {
      // We never found a strictly positive profit, so the "no trade" result is returned.
      return BestTradingResult.noTrade(calculationDate);
    }

    return bestTradeState.toResult(calculationDate);
  }

  /** Validates the basic preconditions for the calculation. */
  private static void validateInputs(
      List<Double> lowPrices,
      List<String> lowTimes,
      List<Double> highPrices,
      List<String> highTimes,
      LocalDate calculationDate) {

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
  private static TimeSeriesData buildTimeSeriesData(
      List<String> lowTimes,
      List<String> highTimes,
      LocalDate calculationDate,
      int numberOfTradingDays) {

    LocalDate lastTradingDate = DateTimeUtils.findLastTradingDateBefore(calculationDate);
    LocalDate[] tradingDates =
        DateTimeUtils.buildTradingDates(lastTradingDate, numberOfTradingDays);

    LocalTime[] lowLocalTimes = new LocalTime[numberOfTradingDays];
    LocalTime[] highLocalTimes = new LocalTime[numberOfTradingDays];
    Instant[] lowInstants = new Instant[numberOfTradingDays];
    Instant[] highInstants = new Instant[numberOfTradingDays];

    for (int tradingDayIndex = 0; tradingDayIndex < numberOfTradingDays; tradingDayIndex++) {
      LocalTime lowTime =
          DateTimeUtils.parseTime(lowTimes.get(tradingDayIndex), "lowTimes", tradingDayIndex);
      LocalTime highTime =
          DateTimeUtils.parseTime(highTimes.get(tradingDayIndex), "highTimes", tradingDayIndex);

      lowLocalTimes[tradingDayIndex] = lowTime;
      highLocalTimes[tradingDayIndex] = highTime;

      ZonedDateTime lowZonedDateTime =
          DateTimeUtils.toUtcZonedDateTime(tradingDates[tradingDayIndex], lowTime);
      ZonedDateTime highZonedDateTime =
          DateTimeUtils.toUtcZonedDateTime(tradingDates[tradingDayIndex], highTime);

      lowInstants[tradingDayIndex] = lowZonedDateTime.toInstant();
      highInstants[tradingDayIndex] = highZonedDateTime.toInstant();
    }

    return new TimeSeriesData(
        tradingDates, lowLocalTimes, highLocalTimes, lowInstants, highInstants);
  }

  /**
   * Finds the best cross-day trade (buy on one day, sell on a strictly later day) using the classic
   * "minimum so far" approach.
   */
  private static BestTradeState findBestCrossDayTrade(
      List<Double> lowPrices, List<Double> highPrices, TimeSeriesData timeSeriesData) {

    int numberOfTradingDays = lowPrices.size();
    if (numberOfTradingDays <= 1) {
      return BestTradeState.empty();
    }

    double bestProfit = 0.0;
    int bestBuyDay = -1;
    int bestSellDay = -1;
    double bestBuyPrice = 0.0;
    double bestSellPrice = 0.0;
    Instant bestBuyTime = null;
    Instant bestSellTime = null;

    double minimumLowPriceSoFar = lowPrices.get(0);
    int dayOfMinimumLowPrice = 0;

    for (int sellDayIndex = 1; sellDayIndex < numberOfTradingDays; sellDayIndex++) {
      double potentialProfit = highPrices.get(sellDayIndex) - minimumLowPriceSoFar;

      if (potentialProfit > bestProfit) {
        bestProfit = potentialProfit;
        bestBuyDay = dayOfMinimumLowPrice;
        bestSellDay = sellDayIndex;
        bestBuyPrice = minimumLowPriceSoFar;
        bestSellPrice = highPrices.get(sellDayIndex);
        bestBuyTime = timeSeriesData.lowInstants[dayOfMinimumLowPrice];
        bestSellTime = timeSeriesData.highInstants[sellDayIndex];
      }

      double currentLowPrice = lowPrices.get(sellDayIndex);
      if (currentLowPrice < minimumLowPriceSoFar) {
        minimumLowPriceSoFar = currentLowPrice;
        dayOfMinimumLowPrice = sellDayIndex;
      }
    }

    if (bestBuyDay < 0 || bestProfit <= 0.0) {
      return BestTradeState.empty();
    }

    return new BestTradeState(
        bestProfit,
        bestBuyDay,
        bestSellDay,
        bestBuyPrice,
        bestSellPrice,
        bestBuyTime,
        bestSellTime);
  }

  /**
   * Considers same-day trades (buy low, sell high within the same day) and updates the best trade
   * if a better profit is found, respecting the rule that the low time must be strictly earlier
   * than the high time.
   */
  private static BestTradeState updateWithSameDayTrades(
      BestTradeState currentBestTrade,
      List<Double> lowPrices,
      List<Double> highPrices,
      TimeSeriesData timeSeriesData) {

    int numberOfTradingDays = lowPrices.size();

    double bestProfit = currentBestTrade.bestProfit;
    int bestBuyDay = currentBestTrade.bestBuyDay;
    int bestSellDay = currentBestTrade.bestSellDay;
    double bestBuyPrice = currentBestTrade.bestBuyPrice;
    double bestSellPrice = currentBestTrade.bestSellPrice;
    Instant bestBuyTime = currentBestTrade.bestBuyTime;
    Instant bestSellTime = currentBestTrade.bestSellTime;

    for (int tradingDayIndex = 0; tradingDayIndex < numberOfTradingDays; tradingDayIndex++) {
      LocalTime lowTime = timeSeriesData.lowLocalTimes[tradingDayIndex];
      LocalTime highTime = timeSeriesData.highLocalTimes[tradingDayIndex];

      // If high happened before or at the same time as the low, same-day trade is not allowed.
      if (!lowTime.isBefore(highTime)) {
        continue;
      }

      double potentialProfit = highPrices.get(tradingDayIndex) - lowPrices.get(tradingDayIndex);
      if (potentialProfit > bestProfit) {
        bestProfit = potentialProfit;
        bestBuyDay = tradingDayIndex;
        bestSellDay = tradingDayIndex;
        bestBuyPrice = lowPrices.get(tradingDayIndex);
        bestSellPrice = highPrices.get(tradingDayIndex);
        bestBuyTime = timeSeriesData.lowInstants[tradingDayIndex];
        bestSellTime = timeSeriesData.highInstants[tradingDayIndex];
      }
    }

    return new BestTradeState(
        bestProfit,
        bestBuyDay,
        bestSellDay,
        bestBuyPrice,
        bestSellPrice,
        bestBuyTime,
        bestSellTime);
  }

  /** Small immutable holder for all date/time related arrays in the time series. */
  private static final class TimeSeriesData {

    private final LocalDate[] tradingDates;
    private final LocalTime[] lowLocalTimes;
    private final LocalTime[] highLocalTimes;
    private final Instant[] lowInstants;
    private final Instant[] highInstants;

    private TimeSeriesData(
        LocalDate[] tradingDates,
        LocalTime[] lowLocalTimes,
        LocalTime[] highLocalTimes,
        Instant[] lowInstants,
        Instant[] highInstants) {
      this.tradingDates = tradingDates;
      this.lowLocalTimes = lowLocalTimes;
      this.highLocalTimes = highLocalTimes;
      this.lowInstants = lowInstants;
      this.highInstants = highInstants;
    }
  }

  /** Internal state holder for the best trade found so far. */
  private static final class BestTradeState {

    private final double bestProfit;
    private final int bestBuyDay;
    private final int bestSellDay;
    private final double bestBuyPrice;
    private final double bestSellPrice;
    private final Instant bestBuyTime;
    private final Instant bestSellTime;

    private BestTradeState(
        double bestProfit,
        int bestBuyDay,
        int bestSellDay,
        double bestBuyPrice,
        double bestSellPrice,
        Instant bestBuyTime,
        Instant bestSellTime) {
      this.bestProfit = bestProfit;
      this.bestBuyDay = bestBuyDay;
      this.bestSellDay = bestSellDay;
      this.bestBuyPrice = bestBuyPrice;
      this.bestSellPrice = bestSellPrice;
      this.bestBuyTime = bestBuyTime;
      this.bestSellTime = bestSellTime;
    }

    static BestTradeState empty() {
      return new BestTradeState(0.0, -1, -1, 0.0, 0.0, null, null);
    }

    boolean hasProfitableTrade() {
      return bestBuyDay >= 0 && bestProfit > 0.0;
    }

    BestTradingResult toResult(LocalDate calculationDate) {
      int maxProfitInt = (int) Math.round(bestProfit);
      int buyPriceInt = (int) Math.round(bestBuyPrice);
      int sellPriceInt = (int) Math.round(bestSellPrice);

      return new BestTradingResult(
          maxProfitInt,
          bestBuyDay,
          bestSellDay,
          buyPriceInt,
          bestBuyTime,
          sellPriceInt,
          bestSellTime,
          calculationDate);
    }
  }
}

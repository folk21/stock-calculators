package com.stockcalculators.besttrading;

import static com.stockcalculators.besttrading.BestTradingCalculatorDelegate.buildTimeSeriesData;

import com.stockcalculators.besttrading.model.BestTradeState;
import com.stockcalculators.besttrading.model.BestTradingResult;
import com.stockcalculators.besttrading.model.TimeSeriesData;
import java.time.LocalDate;
import java.util.List;

/**
 * Facade class that exposes the main API for calculating the best single buy/sell trade over a
 * series of trading days.
 *
 * <p>The heavy lifting is delegated to {@link BestTradingCalculatorDelegate}, which keeps the
 * public class short and focused on its primary responsibility.
 *
 * <p>The documentation please read in the README.md: <a
 * href="https://github.com/folk21/stock-calculators">stock-calculators</a>
 */
public final class BestTradingCalculator {

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
  public BestTradingResult calculateBestTradingResult(
      final List<Double> lowPrices,
      final List<String> lowTimes,
      final List<Double> highPrices,
      final List<String> highTimes,
      final LocalDate calculationDate) {

    // 1. Validate that all inputs are present and consistent.
    BestTradingCalculatorDelegate.validateInputs(
        lowPrices, lowTimes, highPrices, highTimes, calculationDate);

    // 2. Determine how many trading days of data we have.
    int numberOfTradingDays = lowPrices.size();
    if (lowPrices.isEmpty()) {
      // 2.1.  No data at all means that no trade is possible.
      return BestTradingResult.noTrade(calculationDate);
    }

    // 3. Build a time series that pairs each trading day with its corresponding
    //  low/high times and resolves the actual calendar dates of those trading
    //  days relative to the calculationDate.
    TimeSeriesData timeSeriesData =
        buildTimeSeriesData(lowTimes, highTimes, calculationDate, numberOfTradingDays);

    // 4. First pass: search for the best cross-day trade.
    //    This step considers all trades where the buy happens on one trading day
    //    and the sell happens on a strictly later trading day.
    BestTradeState bestTradeState =
        BestTradingCalculatorDelegate.findBestCrossDayTrade(lowPrices, highPrices, timeSeriesData);

    // 5. Second pass: update the best trade with same-day opportunities.
    //    Here we check trades where the buy and sell both occur on the same day.
    //    For example, buying at the intraday low and selling at the intraday high
    //    of a single trading day.
    BestTradingCalculatorDelegate.updateWithSameDayTrades(
        bestTradeState, lowPrices, highPrices, timeSeriesData);

    // 6. If no positive profit was found
    if (!bestTradeState.hasProfitableTrade()) {
      // 6.1. After considering both cross-day and same-day trades, we still
      //    haven't found a strictly positive profit.
      return BestTradingResult.noTrade(calculationDate);
    }

    // 7. At this point, bestTradeState contains the optimal trade:
    //    - best buy day/time
    //    - best sell day/time
    //    - maximum achievable profit
    return bestTradeState.toResult(calculationDate);
  }
}

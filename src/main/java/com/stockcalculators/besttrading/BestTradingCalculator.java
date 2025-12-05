package com.stockcalculators.besttrading;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>Facade class that exposes the main API for calculating the best single
 * buy/sell trade over a series of trading days.
 * <p>The heavy lifting is delegated to {@link BestTraidingUtils}, which keeps
 * the public class short and focused on its primary responsibility.
 */
public final class BestTradingCalculator {

    /**
     * <p>Public no-argument constructor.
     * <p>The calculator itself is stateless; a new instance can be created
     * for each computation or reused multiple times.
     */
    public BestTradingCalculator() {
        // <p>No state to initialise.
    }

    /**
     * <p>Computes the best possible single buy/sell transaction.
     * <p>The data represents the last {@code N} trading days (Monday to Friday only)
     * before the given calculation date.
     *
     * @param lowPrices        intraday lowest prices, one per trading day
     * @param lowTimes         intraday times of the lows, formatted as {@code HH:mm}
     * @param highPrices       intraday highest prices, one per trading day
     * @param highTimes        intraday times of the highs, formatted as {@code HH:mm}
     * @param calculationDate  logical date of the calculation; the last input day corresponds
     *                         to the last trading day strictly before this date
     * @return best trading result or {@link BestTradingResult#noTrade(LocalDate)} if no positive profit is possible
     */
    public BestTradingResult calculateBestTradingResult(
            List<Double> lowPrices,
            List<String> lowTimes,
            List<Double> highPrices,
            List<String> highTimes,
            LocalDate calculationDate
    ) {
        return BestTraidingUtils.calculateBestTradingResultInternal(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate);
    }
}

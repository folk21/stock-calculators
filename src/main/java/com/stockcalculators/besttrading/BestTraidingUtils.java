package com.stockcalculators.besttrading;

import com.stockcalculator.util.DateTimeUtils;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>Internal helper for {@link BestTradingCalculator} that performs the actual
 * calculation of the best single buy/sell transaction.
 * <p>The class is intentionally package-private and should not be used from
 * outside the {@code com.stockcalculators.besttrading} package.
 */
@UtilityClass
class BestTraidingUtils {

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
    static BestTradingResult calculateBestTradingResultInternal(
            List<Double> lowPrices,
            List<String> lowTimes,
            List<Double> highPrices,
            List<String> highTimes,
            LocalDate calculationDate
    ) {

        Objects.requireNonNull(lowPrices, "lowPrices must not be null");
        Objects.requireNonNull(lowTimes, "lowTimes must not be null");
        Objects.requireNonNull(highPrices, "highPrices must not be null");
        Objects.requireNonNull(highTimes, "highTimes must not be null");
        Objects.requireNonNull(calculationDate, "calculationDate must not be null");

        int n = lowPrices.size();
        if (n == 0) {
            // <p>No data at all means that no trade is possible.
            return BestTradingResult.noTrade(calculationDate);
        }
        if (lowTimes.size() != n || highPrices.size() != n || highTimes.size() != n) {
            throw new IllegalArgumentException("All input lists must have the same size");
        }

        LocalDate lastTradingDate = DateTimeUtils.findLastTradingDateBefore(calculationDate);
        LocalDate[] dates = DateTimeUtils.buildTradingDates(lastTradingDate, n);
        LocalTime[] lowLocalTimes = new LocalTime[n];
        LocalTime[] highLocalTimes = new LocalTime[n];
        Instant[] lowInstants = new Instant[n];
        Instant[] highInstants = new Instant[n];

        for (int i = 0; i < n; i++) {
            LocalTime lowTime = DateTimeUtils.parseTime(lowTimes.get(i), "lowTimes", i);
            LocalTime highTime = DateTimeUtils.parseTime(highTimes.get(i), "highTimes", i);

            lowLocalTimes[i] = lowTime;
            highLocalTimes[i] = highTime;

            ZonedDateTime lowZdt = DateTimeUtils.toUtcZonedDateTime(dates[i], lowTime);
            ZonedDateTime highZdt = DateTimeUtils.toUtcZonedDateTime(dates[i], highTime);

            lowInstants[i] = lowZdt.toInstant();
            highInstants[i] = highZdt.toInstant();
        }

        // <p>Running maximum profit (in doubles before we cast to int) and its associated trade.
        double bestProfit = 0.0;
        int bestBuyDay = -1;
        int bestSellDay = -1;
        double bestBuyPrice = 0.0;
        double bestSellPrice = 0.0;
        Instant bestBuyTime = null;
        Instant bestSellTime = null;

        // <p>First, handle cross-day trades in a single pass using the classic
        // "minimum so far" technique.
        double minLowPrice = lowPrices.get(0);
        int minLowDay = 0;

        for (int sellDay = 1; sellDay < n; sellDay++) {
            double potentialProfit = highPrices.get(sellDay) - minLowPrice;

            if (potentialProfit > bestProfit) {
                bestProfit = potentialProfit;
                bestBuyDay = minLowDay;
                bestSellDay = sellDay;
                bestBuyPrice = minLowPrice;
                bestSellPrice = highPrices.get(sellDay);
                bestBuyTime = lowInstants[minLowDay];
                bestSellTime = highInstants[sellDay];
            }

            double currentLowPrice = lowPrices.get(sellDay);
            if (currentLowPrice < minLowPrice) {
                minLowPrice = currentLowPrice;
                minLowDay = sellDay;
            }
        }

        // <p>Next, consider same-day trades where low time is strictly earlier than high time.
        for (int day = 0; day < n; day++) {
            LocalTime lowTime = lowLocalTimes[day];
            LocalTime highTime = highLocalTimes[day];

            // <p>If high happened before or at the same time as the low, same-day trade is not allowed.
            if (!lowTime.isBefore(highTime)) {
                continue;
            }

            double potentialProfit = highPrices.get(day) - lowPrices.get(day);
            if (potentialProfit > bestProfit) {
                bestProfit = potentialProfit;
                bestBuyDay = day;
                bestSellDay = day;
                bestBuyPrice = lowPrices.get(day);
                bestSellPrice = highPrices.get(day);
                bestBuyTime = lowInstants[day];
                bestSellTime = highInstants[day];
            }
        }

        if (bestBuyDay < 0 || bestProfit <= 0.0) {
            // <p>We never found a strictly positive profit, so the "no trade" result is returned.
            return BestTradingResult.noTrade(calculationDate);
        }

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
                calculationDate
        );
    }
}

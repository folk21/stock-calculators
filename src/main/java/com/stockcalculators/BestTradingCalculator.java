package com.stockcalculators;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * <p>Provides a single static method that searches the best possible buy/sell pair
 * over the last N trading days.
 * <p>The algorithm runs in O(N) time and O(N) additional memory for computed dates.
 */
public final class BestTradingCalculator {

    /**
     * <p>Formatter used to read times in {@code HH:mm} format.
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private BestTradingCalculator() {
        // <p>Utility class; no instances should be created.
    }

    /**
     * <p>Computes the best possible single buy/sell transaction.
     * <p>The data represents the last {@code N} trading days (Monday to Friday only)
     * before the given calculation date.
     * <p>For each day index {@code i}:
     * <p>
     * <p>{@code lowPrices[i]} and {@code lowTimes[i]} describe the intraday minimum price and the time
     * when this minimum was reached.
     * <p>{@code highPrices[i]} and {@code highTimes[i]} describe the intraday maximum price and time.
     * <p>Buying and selling can happen on the same day only if the low time is strictly earlier than the high time.
     * For different days any buy on an earlier day and sell on a later day is allowed.
     *
     * @param lowPrices        intraday lowest prices, one per trading day
     * @param lowTimes         intraday times of the lows, formatted as {@code HH:mm}
     * @param highPrices       intraday highest prices, one per trading day
     * @param highTimes        intraday times of the highs, formatted as {@code HH:mm}
     * @param calculationDate  logical date of the calculation; the last input day corresponds
     *                         to the last trading day strictly before this date
     * @return best trading result or {@link BestTradingResult#noTrade(LocalDate)} if no positive profit is possible
     */
    public static BestTradingResult calculateBestTradingResult(
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

        LocalDate lastTradingDate = findLastTradingDateBefore(calculationDate);
        LocalDate[] dates = buildTradingDates(lastTradingDate, n);
        LocalTime[] lowLocalTimes = new LocalTime[n];
        LocalTime[] highLocalTimes = new LocalTime[n];
        Instant[] lowInstants = new Instant[n];
        Instant[] highInstants = new Instant[n];

        for (int i = 0; i < n; i++) {
            LocalTime lowTime = parseTime(lowTimes.get(i), "lowTimes", i);
            LocalTime highTime = parseTime(highTimes.get(i), "highTimes", i);

            lowLocalTimes[i] = lowTime;
            highLocalTimes[i] = highTime;

            LocalDate date = dates[i];
            lowInstants[i] = ZonedDateTime.of(date, lowTime, ZoneOffset.UTC).toInstant();
            highInstants[i] = ZonedDateTime.of(date, highTime, ZoneOffset.UTC).toInstant();
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

    /**
     * <p>Parses a {@code HH:mm} time value and converts parsing errors into
     * {@link IllegalArgumentException} with additional context.
     *
     * @param time      textual representation of the time
     * @param fieldName name of the field in the original input (for diagnostics)
     * @param index     day index (for diagnostics)
     * @return parsed {@link LocalTime}
     */
    private static LocalTime parseTime(String time, String fieldName, int index) {
        try {
            return LocalTime.parse(time, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    fieldName + "[" + index + "] has invalid time value: " + time,
                    e
            );
        }
    }

    /**
     * <p>Finds the last trading day strictly before the given calculation date.
     * <p>Trading days are considered to be Monday to Friday. If the calculation
     * date is Monday, the last trading day is the previous Friday.
     *
     * @param calculationDate logical date of the calculation
     * @return last trading day before the calculation date
     */
    private static LocalDate findLastTradingDateBefore(LocalDate calculationDate) {
        LocalDate candidate = calculationDate.minusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
                || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    /**
     * <p>Builds an array of calendar dates for every trading day in the series.
     * <p>The last element corresponds to the given last trading date; previous
     * elements are found by walking backward in the calendar and skipping
     * Saturdays and Sundays.
     *
     * @param lastTradingDate the calendar date that corresponds to index {@code N-1}
     * @param n               number of trading days in the time series
     * @return array of {@link LocalDate} instances, one per trading day
     */
    private static LocalDate[] buildTradingDates(LocalDate lastTradingDate, int n) {
        LocalDate[] dates = new LocalDate[n];
        dates[n - 1] = lastTradingDate;

        for (int i = n - 2; i >= 0; i--) {
            LocalDate candidate = dates[i + 1].minusDays(1);
            while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                candidate = candidate.minusDays(1);
            }
            dates[i] = candidate;
        }

        return dates;
    }
}

package com.stockcalculators;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * <p>Represents the outcome of searching for the single best buy/sell transaction.
 * <p>The record is immutable and holds both numeric indices (days and prices),
 * precise timestamps as {@link Instant} values and the logical calculation date.
 */
public record BestTradingResult(
        int maxProfit,
        int buyDay,
        int sellDay,
        int buyPrice,
        Instant buyTime,
        int sellPrice,
        Instant sellTime,
        LocalDate calculationDate
) {

    /**
     * <p>Formatter used to render timestamps in the human readable summary.
     * <p>The format matches {@code yyyy-MM-dd'T'HH:mm} from the problem statement.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /**
     * <p>Creates a special instance that represents that no profitable trade exists.
     * <p>Indices are set to {@code -1}, prices to {@code 0}, timestamps to {@code null},
     * while the provided calculation date is preserved for traceability.
     *
     * @param calculationDate logical date for which the calculation was performed
     * @return result that can be used when the best achievable profit is not positive
     */
    public static BestTradingResult noTrade(LocalDate calculationDate) {
        return new BestTradingResult(0, -1, -1, 0, null, 0, null, calculationDate);
    }

    /**
     * <p>Returns a human readable description of this result that matches
     * the textual format from the task description.
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
                    + "calculationDate = " + (calculationDate != null ? calculationDate : "n/a");
        }

        String buyDayLabel = formatDayLabel(buyDay);
        String sellDayLabel = formatDayLabel(sellDay);

        String buyDateTime = formatInstant(buyTime);
        String sellDateTime = formatInstant(sellTime);

        String firstLine = String.format(
                "Best buy is Day %d at %d \u2192 sell Day %d at %d",
                buyDay, buyPrice, sellDay, sellPrice
        );

        String secondLine = String.format("maxProfit = %d", maxProfit);

        String thirdLine = String.format(
                "buyDay = %d (%s),  buyPrice = %.2f at %s",
                buyDay,
                buyDayLabel,
                (double) buyPrice,
                buyDateTime
        );

        String fourthLine = String.format(
                "sellDay = %d (%s), sellPrice = %.2f at %s",
                sellDay,
                sellDayLabel,
                (double) sellPrice,
                sellDateTime
        );

        String fifthLine = String.format(
                "calculationDate = %s",
                calculationDate != null ? calculationDate.toString() : "n/a"
        );

        return String.join("\n", firstLine, secondLine, thirdLine, fourthLine, fifthLine);
    }

    /**
     * <p>Formats the given instant using UTC so that it matches the
     * string style in the task (no explicit time zone suffix).
     *
     * @param instant timestamp to convert, may be {@code null}
     * @return formatted date-time or {@code "n/a"} if the instant is {@code null}
     */
    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return "n/a";
        }
        LocalDateTime dt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return DATE_TIME_FORMATTER.format(dt);
    }

    /**
     * <p>Produces a descriptor like {@code "Monday"} or {@code "Wednesday next week"}
     * based purely on the day index.
     * <p>The mapping assumes that:
     * <p>Index 0 is Monday, index 1 is Tuesday, ..., index 4 is Friday,
     * index 5 is Monday next week and so on.
     *
     * @param dayIndex zero based trading day index
     * @return label to be shown in the human readable summary
     */
    private static String formatDayLabel(int dayIndex) {
        int dayOfWeekIndex = Math.floorMod(dayIndex, 5);

        String weekday;
        switch (dayOfWeekIndex) {
            case 0 -> weekday = "Monday";
            case 1 -> weekday = "Tuesday";
            case 2 -> weekday = "Wednesday";
            case 3 -> weekday = "Thursday";
            case 4 -> weekday = "Friday";
            default -> throw new IllegalArgumentException("Invalid day index: " + dayIndex);
        }

        int weekOffset = dayIndex / 5;
        if (weekOffset == 0) {
            return weekday;
        }
        if (weekOffset == 1) {
            return weekday + " next week";
        }
        return weekday + " (week +" + weekOffset + ")";
    }
}

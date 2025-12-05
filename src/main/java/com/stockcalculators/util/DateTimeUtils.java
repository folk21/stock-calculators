package com.stockcalculators.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import lombok.experimental.UtilityClass;

/**
 * Common date and time helper methods used by trading calculators.
 *
 * <p>The methods are intentionally kept stateless and are exposed via the {@link UtilityClass}
 * pattern (static methods only).
 */
@UtilityClass
public class DateTimeUtils {

  /** Formatter used to read intraday times in {@code HH:mm} format. */
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  /**
   * Formatter used to render timestamps in the human readable summary.
   *
   * <p>The format matches {@code yyyy-MM-dd'T'HH:mm} from the problem statement.
   */
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  /**
   * Parses a {@code HH:mm} time value and converts parsing errors into {@link
   * IllegalArgumentException} with additional context.
   *
   * @param time textual representation of the time
   * @param fieldName name of the field in the original input (for diagnostics)
   * @param index day index (for diagnostics)
   * @return parsed {@link LocalTime}
   */
  public static LocalTime parseTime(String time, String fieldName, int index) {
    try {
      return LocalTime.parse(time, TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          fieldName + "[" + index + "] has invalid time value: " + time, e);
    }
  }

  /**
   * Finds the last trading day strictly before the given calculation date.
   *
   * <p>Trading days are considered to be Monday to Friday.
   *
   * <p>Practical interpretation:
   *
   * <p>- If the calculation date is any weekday except Monday, the last trading day is simply the
   * previous calendar day.
   *
   * <p>- If the calculation date is Monday, the last trading day is the previous Friday.
   *
   * <p>- If the calculation date falls on a weekend (Saturday or Sunday), the last trading day is
   * also the previous Friday.
   *
   * @param calculationDate logical date of the calculation
   * @return last trading day before the calculation date
   */
  public static LocalDate findLastTradingDateBefore(LocalDate calculationDate) {
    LocalDate candidate = calculationDate.minusDays(1);
    while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
        || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }

  /**
   * Builds an array of calendar dates for every trading day in the series.
   *
   * <p>The last element corresponds to the given last trading date; previous elements are found by
   * walking backward in the calendar and skipping Saturdays and Sundays.
   *
   * @param lastTradingDate the calendar date that corresponds to index {@code N-1}
   * @param n number of trading days in the time series
   * @return array of {@link LocalDate} instances, one per trading day
   */
  public static LocalDate[] buildTradingDates(LocalDate lastTradingDate, int n) {
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

  /**
   * Formats the given instant using UTC so that it matches the string style in the task (no
   * explicit time zone suffix).
   *
   * @param instant timestamp to convert, may be {@code null}
   * @return formatted date-time or {@code "n/a"} if the instant is {@code null}
   */
  public static String formatInstant(Instant instant) {
    if (instant == null) {
      return "n/a";
    }
    LocalDateTime dt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    return DATE_TIME_FORMATTER.format(dt);
  }

  /**
   * Produces a descriptor like {@code "Monday"} or {@code "Wednesday next week"} based purely on
   * the day index.
   *
   * <p>The mapping assumes that:
   *
   * <p>Index 0 is Monday, index 1 is Tuesday, ..., index 4 is Friday, index 5 is Monday next week
   * and so on.
   *
   * @param dayIndex zero based trading day index
   * @return label to be shown in the human readable summary
   */
  public static String formatDayLabel(int dayIndex) {
    if (dayIndex < 0) {
      throw new IllegalArgumentException("dayIndex must be non-negative: " + dayIndex);
    }

    // 0 -> Monday, 1 -> Tuesday, ..., 4 -> Friday, 5 -> Monday next week, etc.
    int dayOfWeekIndex = Math.floorMod(dayIndex, 5);

    DayOfWeek dayOfWeek = DayOfWeek.MONDAY.plus(dayOfWeekIndex);
    // "MONDAY" -> "Monday" (locale-specific name)
    String weekday = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);

    int weekOffset = dayIndex / 5;
    if (weekOffset == 0) {
      return weekday;
    }
    if (weekOffset == 1) {
      return weekday + " next week";
    }
    return weekday + " (week +" + weekOffset + ")";
  }

  /**
   * Creates a {@link ZonedDateTime} using UTC from the provided date and time.
   *
   * <p>This helper is useful to keep all conversions to {@link Instant} consistent across the
   * project.
   *
   * @param date the calendar date
   * @param time the local intraday time
   * @return zoned date time in UTC
   */
  public static ZonedDateTime toUtcZonedDateTime(LocalDate date, LocalTime time) {
    return ZonedDateTime.of(date, time, ZoneOffset.UTC);
  }
}

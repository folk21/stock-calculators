# Stock Calculators – Best Single Buy/Sell Trade

This small Java project provides a utility to compute the **best single buy/sell transaction**
given daily low/high prices and times for the last _N_ trading days.

Trading days are assumed to be **Monday–Friday only** (no weekends).  
You are allowed to perform **at most one transaction**: buy once and sell once later.

---

## Core Idea

For each day `i` (0-based index) you know:

- `lowPrices[i]` – the lowest price during that day.
- `lowTimes[i]` – the time when the low occurred (format `HH:mm`).
- `highPrices[i]` – the highest price during that day.
- `highTimes[i]` – the time when the high occurred (format `HH:mm`).

Constraints:

- `0 <= lowPrices[i] <= highPrices[i]`.
- All timestamps belong to that specific calendar day.
- You may trade:
    - **Same day**: buy at the low and sell at the high **only if** `lowTime < highTime`.
    - **Different days**: buy on day `i` and sell on a strictly later trading day `j` (with `j > i`),
      so the sell happens later in the overall timeline by calendar date.

The goal is to find:

- `maxProfit = sellPrice - buyPrice` (maximum possible, but must be **strictly positive**).
- The corresponding `buyDay`, `sellDay`, `buyPrice`, `sellPrice`, and exact timestamps.

If no strictly positive profit is possible, we return a special **no-trade** result.

---

## Packages Overview

- `com.stockcalculators.besttrading`
    - `BestTradingCalculator` – public façade, instance-based API.
    - `BestTradingResult` – immutable record with the result.
    - `BestTraidingUtils` – internal helper with the core algorithm (package-private).
- `com.stockcalculator.util`
    - `DateTimeutils` – common date/time utilities shared by the trading components.

---

## Calendar Semantics

The main calculation method is an **instance method**:

```java
BestTradingResult BestTradingCalculator.calculateBestTradingResult(
        List<Double> lowPrices,
        List<String> lowTimes,
        List<Double> highPrices,
        List<String> highTimes,
        LocalDate calculationDate
)
```

Semantics:

- `calculationDate` is the **logical "today"**.
- The **last element** in the input lists (`index = N-1`) corresponds to the  
  **last trading day strictly before `calculationDate`**.
- Trading days are Monday–Friday only; Saturdays and Sundays are skipped.  
  The current (calculation) day is never part of the input series.

Examples:

- If `calculationDate` is a weekday **other than Monday** (e.g. Tuesday, Wednesday, Thursday, Friday),
  the last trading day is simply the previous calendar day (which is also a weekday).
- If `calculationDate` is **Monday**, the last trading day is the previous **Friday**  
  (the weekend is skipped).
- If `calculationDate` falls on a **weekend** (Saturday or Sunday), the last trading day
  is also the previous **Friday**.
- In all cases, the array contains the last `N` trading days **ending at that last trading day**,
  walking backwards and skipping Saturdays and Sundays.

Internally the code:

1. Uses `DateTimeutils.findLastTradingDateBefore(calculationDate)` to locate the last trading day.
2. Walks backwards in the calendar with `DateTimeutils.buildTradingDates` to build an array
   of dates for all `N` trading days, skipping Saturdays and Sundays.
3. Combines each date with the corresponding `lowTimes[i]` and `highTimes[i]` using
   `DateTimeutils.toUtcZonedDateTime(...)` to produce precise `Instant` values in UTC.

---

## Public API

### 1. `BestTradingCalculator` (facade)

Located in `com.stockcalculators.besttrading`:

```java
public final class BestTradingCalculator {

    public BestTradingCalculator() {
        // stateless
    }

    public BestTradingResult calculateBestTradingResult(
            List<Double> lowPrices,
            List<String> lowTimes,
            List<Double> highPrices,
            List<String> highTimes,
            LocalDate calculationDate
    ) {
        // delegates to BestTraidingUtils
    }
}
```

Validation rules (enforced by the internal helper):

- All list arguments must be non-null.
- All lists must have the same size.
- Time strings must match `HH:mm` format (e.g. `09:30`, `15:45`).
- If the list size is `0`, the method returns a **no-trade** result without error.

Algorithm (implemented in `BestTraidingUtils`):

1. **Cross-day trades (buyDay < sellDay)**  
   Uses the classic “minimum so far” technique:
    - Tracks the lowest `lowPrice` seen so far and its index.
    - For each day `sellDay` computes `highPrices[sellDay] - minLowPrice`.
    - Updates the best profit and trade tuple when a higher profit is found.

2. **Same-day trades (buyDay == sellDay)**  
   For each day:
    - Parses `lowTime` and `highTime` via `DateTimeutils.parseTime`.
    - If `lowTime.isBefore(highTime)`:
        - Computes `highPrice - lowPrice`.
        - Compares with the current best profit and updates if better.
    - If `highTime` is earlier or equal to `lowTime`, the same-day trade is **invalid** and ignored.

3. If, after all checks, there is **no strictly positive profit**, the method returns:
    - `maxProfit = 0`
    - `buyDay = -1`, `sellDay = -1`
    - `buyPrice = 0`, `sellPrice = 0`
    - `buyTime = null`, `sellTime = null`
    - `calculationDate` is still set to the original value for traceability.

---

### 2. `BestTradingResult` (record)

```java
public record BestTradingResult(
        int maxProfit,
        int buyDay,
        int sellDay,
        int buyPrice,
        Instant buyTime,
        int sellPrice,
        Instant sellTime,
        LocalDate calculationDate
)
```

Additional helper:

```java
public static BestTradingResult noTrade(LocalDate calculationDate)
```

Creates the special “no-trade” result.

#### Pretty printing

The record provides a convenience method:

```java
String BestTradingResult.toPrettyString()
```

Typical output:

```text
Best buy is Day 0 at 10 → sell Day 9 at 27
maxProfit = 17
buyDay = 0 (Monday),  buyPrice = 10.00 at 2025-10-27T10:00
sellDay = 9 (Friday next week), sellPrice = 27.00 at 2025-11-07T16:05
calculationDate = 2025-11-10
```

If no profitable trade exists:

```text
No profitable trade was found.
maxProfit = 0
buyDay = -1, sellDay = -1
buyPrice = 0, sellPrice = 0
calculationDate = 2025-11-10
```

Formatting is delegated to `DateTimeutils`:

- `formatInstant(Instant)` to format timestamps.
- `formatDayLabel(int)` to produce labels like `Monday`, `Friday next week`, etc.

---

### 3. `DateTimeutils` (utilities)

Located in `com.stockcalculator.util` and marked as a Lombok `@UtilityClass`:

- `parseTime(String, String, int)` – parses `HH:mm`, throws `IllegalArgumentException` on error.
- `findLastTradingDateBefore(LocalDate)` – finds the last Monday–Friday date before a given date.
- `buildTradingDates(LocalDate, int)` – builds an array of trading dates for the given series length.
- `formatInstant(Instant)` – renders an `Instant` in `yyyy-MM-dd'T'HH:mm` using UTC.
- `formatDayLabel(int)` – creates human readable day labels based on 0-based index.
- `toUtcZonedDateTime(LocalDate, LocalTime)` – produces a UTC `ZonedDateTime`.

---

## Usage Example

Minimal snippet (for illustration only):

```java
import com.stockcalculators.besttrading.BestTradingCalculator;
import com.stockcalculators.besttrading.BestTradingResult;

import java.time.LocalDate;
import java.util.List;

public class Demo {

    public static void main(String[] args) {
        List<Double> lowPrices  = List.of(10.0, 12.0, 11.0, 13.0, 15.0,
                                          16.0, 18.0, 19.0, 21.0, 22.0);
        List<String> lowTimes   = List.of("10:00","09:45","11:10","09:50","10:15",
                                          "09:55","10:05","09:40","10:00","09:35");
        List<Double> highPrices = List.of(14.0, 15.0, 16.0, 18.0, 20.0,
                                          21.0, 23.0, 24.0, 26.0, 27.0);
        List<String> highTimes  = List.of("15:00","16:00","14:30","15:40","16:10",
                                          "15:20","16:00","15:10","15:25","16:05");

        LocalDate calculationDate = LocalDate.of(2025, 11, 10); // Monday

        BestTradingCalculator calculator = new BestTradingCalculator();
        BestTradingResult result = calculator.calculateBestTradingResult(
                lowPrices, lowTimes, highPrices, highTimes, calculationDate);

        System.out.println(result.toPrettyString());
    }
}
```

---

## Tests

The project uses **JUnit 5**.

There are two test classes in `com.stockcalculators.besttrading`:

- `BestTradingCalculatorPositiveTest` – typical and edge-case **valid** inputs where a result
  should be successfully computed (including no-trade scenarios).
- `BestTradingCalculatorNegativeTest` – tests for invalid input data
  (mismatched list sizes, malformed time values, etc.).

Additional positive tests demonstrate how `calculationDate` affects the mapping:

- **Sunday calculation date** with 10 trading days.
- **Monday calculation date** with 10 trading days.
- **Tuesday calculation date** with 10 trading days.
- **Empty list** (0 trading days) with a Monday calculation date – processed without errors and
  returning a proper no-trade result.

---

## Building and Running

This is a Gradle project using Java 17 toolchain and Lombok.

To run the tests:

```bash
./gradlew test
```

Or on Windows:

```bash
gradlew test
```

You can import the project into any modern IDE (IntelliJ IDEA, Eclipse, VS Code with Java support)
as a Gradle project and run tests or create your own integration code on top of the
`BestTradingCalculator` / `BestTradingResult` API.

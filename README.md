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
- You can complete at most one transaction: buy once and sell once later.
- Buying and selling may happen on **different trading days** as long as the sell day
  is strictly later in the series (i.e. `sellDay > buyDay`), which matches the
  “sell once later” requirement.
- If buying and selling happen on the **same trading day**, the buy time must be
  strictly earlier than the sell time for that day (i.e. `lowTime < highTime`).

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

0. **Pre-processing**

    - For each trading day the code:
        - Parses `lowTime` and `highTime` using `DateTimeUtils.parseTime`.
        - Combines each date with its times via `DateTimeUtils.toUtcZonedDateTime(...)`
          to obtain `Instant` values for the low and the high.

1. **Cross-day trades (buyDay < sellDay)**  
   Uses the classic “minimum so far” technique:

    - Tracks the lowest `lowPrice` seen so far and the day index where it occurred.
    - For each potential selling day `sellDay` computes  
      `potentialProfit = highPrices[sellDay] - minimumLowPriceSoFar`.
    - If `potentialProfit` is greater than the current best profit, updates the
      best trade:
        - `buyDay` = day of the minimum low price so far,
        - `sellDay` = current `sellDay`,
        - buy/sell prices and timestamps are taken from the pre-computed series.

2. **Same-day trades (buyDay == sellDay)**

   For each trading day:

    - Takes the already parsed `lowTime` and `highTime`.
    - If `lowTime.isBefore(highTime)` (i.e. the low happens strictly before the high):
        - Computes `potentialProfit = highPrice - lowPrice` for that day.
        - If this `potentialProfit` is greater than the current best profit
          (possibly found during cross-day processing), updates the best trade so
          that both `buyDay` and `sellDay` equal this day.
    - If `highTime` is earlier than or equal to `lowTime`, the same-day trade is
      **invalid** according to the problem statement and is ignored.

3. **No profitable trade**

   If, after processing cross-day and same-day trades, there is **no strictly
   positive profit**, the method returns a special “no-trade” result:

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

## Behaviour and Test Coverage

This module is driven by tests that double as an executable specification.  
Below is a high-level overview of what the tests verify.

---

### 1. Trading Logic (Core Behaviour)

**Single buy/sell rule**

- Only one buy and one sell are allowed.
- Buy must happen **before** sell (either earlier day or earlier time on the same day).
- If no strictly positive profit is possible, the calculator returns a special *no-trade* result.

**Trading days and dates**

- Input lists represent the last `N` **trading days** (Monday–Friday only) strictly before a given `calculationDate`.
- The **last element** of each list corresponds to the **last trading day** before `calculationDate`.
- Calendar dates for each index are derived by walking backwards from `calculationDate`, skipping weekends.
- Tests cover mapping for different `calculationDate` values:
  - Monday, Sunday, and Tuesday calculation dates.
  - Correct mapping of indices to concrete calendar dates and timestamps.

**Best trade selection**

The positive test suite checks that the algorithm correctly finds the best trade in different situations:

- **Monotonic increasing week (Test 1)**  
  - 5 days (Mon–Fri); prices strictly increase every day.  
  - Requirement: **buy on Monday at the lowest price, sell on Friday at the highest price**.

- **“Mon–Wed is best” scenario (Test 2)**  
  - Example prices: Mon = 1, Tue = 2, Wed = 7, Thu = 3, Fri = 5.  
  - Requirement: best trade is **buy Monday (1) → sell Wednesday (7)**, profit = 6,  
    even though later days still offer smaller positive profits.

- **Cross-week profitable trade (problem statement example)**  
  - Best trade spans multiple days across two weeks (e.g., buy on day 1, sell on day 8).  
  - Requirement: indices, prices, timestamps, and calculated profit match the example.

- **Same-day spike vs. cross-day trade**  
  - There is a very strong intraday spike where buy and sell happen on the **same day**.  
  - Requirement: algorithm prefers this same-day trade when its profit is higher than any multi-day trade.

- **Ignoring invalid same-day trades**  
  - Some days have `highTime <= lowTime` (high before or at the same time as low).  
  - Requirement:
    - Such same-day trades are **ignored** as invalid.
    - Valid **cross-day** trades are still considered and the best one is chosen.

- **Empty input lists**  
  - No trading days at all.  
  - Requirement: returns a *no-trade* result (no exception).

---

### 2. No-Trade Scenarios (Non-Profitable Markets)

Tests explicitly cover cases where trading is possible in principle, but **no profitable trade** exists:

- **Strictly decreasing prices**  
  - Prices fall every day; any buy–sell pair yields non-positive profit.  
  - Requirement: returns *no-trade*.

- **All prices equal**  
  - No price movement across days.  
  - Requirement: returns *no-trade*.

- **Single day with invalid intraday order**  
  - Only one day, but `highTime` is before `lowTime`.  
  - Requirement: same-day trade is invalid and there is no other day, so result is *no-trade*.

In all of these, tests assert:

- `maxProfit = 0`
- `buyDay = -1`, `sellDay = -1`
- `buyPrice = 0`, `sellPrice = 0`
- `buyTime = null`, `sellTime = null`
- `calculationDate` is preserved.

---

### 3. Input Validation and Error Handling

The calculator performs strict validation of the input lists.  
Tests verify that invalid inputs are rejected with an exception rather than producing a silent or incorrect result.

**List size consistency**

- All four lists (`lowPrices`, `lowTimes`, `highPrices`, `highTimes`) must have the **same size**.
- Any mismatch (e.g., more prices than times) results in an `IllegalArgumentException`.

**Time format and content**

- Times must be non-blank strings in **`HH:mm`** format with valid hour and minute values.
- The following examples are explicitly tested and must cause an `IllegalArgumentException`:
  - `"9:30"` (missing leading zero),
  - `"25:00"` (hour out of range),
  - `"aa:bb"` (non-numeric),
  - `""` (empty string),
  - `"   "` (whitespace only).

**Null elements in time lists**

- `null` inside a time list is not allowed.  
- This scenario is tested and must result in a runtime exception.

---

### 4. Result Formatting (`toPrettyString()`)

Tests define the exact contract for `BestTradingResult.toPrettyString()`.

**No-trade formatting**

- For a *no-trade* result, the method returns a fixed multi-line message containing:
  - `maxProfit = 0`
  - `buyDay = -1, sellDay = -1`
  - `buyPrice = 0, sellPrice = 0`
  - `calculationDate = <date>`

**Profitable trade formatting**

- For a valid profitable trade, the string:
  - Starts with:  
    `Best buy is Day X at P_buy → sell Day Y at P_sell`
  - States `maxProfit` on the next line.
  - Includes:
    - Day indices and human-readable day labels (e.g. `Monday`, `Friday next week`),
    - Prices with two decimal places,
    - Buy and sell timestamps formatted as `yyyy-MM-dd'T'HH:mm` in UTC.
  - Ends with the `calculationDate`.

The formatting tests use full string equality, so any change in wording, spacing or line breaks 
is considered a breaking change to the pretty-print contract.

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

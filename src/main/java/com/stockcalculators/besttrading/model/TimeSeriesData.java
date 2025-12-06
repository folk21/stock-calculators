package com.stockcalculators.besttrading.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Small immutable holder for all date/time related arrays in the time series. */
public record TimeSeriesData(
    LocalDate[] tradingDates,
    LocalTime[] lowLocalTimes,
    LocalTime[] highLocalTimes,
    Instant[] lowInstants,
    Instant[] highInstants) {}

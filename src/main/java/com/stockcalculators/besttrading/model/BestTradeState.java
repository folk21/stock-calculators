package com.stockcalculators.besttrading.model;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

/** Internal state holder for the best trade found so far. */
@Getter
public final class BestTradeState {

  private double bestProfit;
  private int bestBuyDay;
  private int bestSellDay;
  private double bestBuyPrice;
  private double bestSellPrice;
  private Instant bestBuyTime;
  private Instant bestSellTime;

  public BestTradeState(
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

  public static BestTradeState empty() {
    return new BestTradeState(0.0, -1, -1, 0.0, 0.0, null, null);
  }

  public boolean hasProfitableTrade() {
    return bestBuyDay >= 0 && bestProfit > 0.0;
  }

  public void updateTrade(
      double profit,
      int buyDay,
      int sellDay,
      double buyPrice,
      double sellPrice,
      Instant buyTime,
      Instant sellTime) {
    this.bestProfit = profit;
    this.bestBuyDay = buyDay;
    this.bestSellDay = sellDay;
    this.bestBuyPrice = buyPrice;
    this.bestSellPrice = sellPrice;
    this.bestBuyTime = buyTime;
    this.bestSellTime = sellTime;
  }

  public BestTradingResult toResult(LocalDate calculationDate) {
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

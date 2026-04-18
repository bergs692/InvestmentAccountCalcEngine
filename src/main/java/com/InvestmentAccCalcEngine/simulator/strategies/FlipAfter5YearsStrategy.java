package com.InvestmentAccCalcEngine.simulator.strategies;

import com.InvestmentAccCalcEngine.simulator.SimulationContext;
import com.InvestmentAccCalcEngine.simulator.Strategy;

public class FlipAfter5YearsStrategy implements Strategy {
  public String getName() { return "Buy, Hold 5yr, Flip"; }

  public void execute(SimulationContext ctx) {
    // Buy in month 1-3 if you can afford it
    //if (ctx.getCurrentMonth() <= 3 && ctx.getBalance().compareTo(...) > 0) {
      //ctx.takeOutMortgage(...);
    //}
    // Sell everything after 60 months
    if (ctx.getCurrentMonth() == 60) {
      for (int i = ctx.getOwnedProperties().size() - 1; i >= 0; i--) {
        ctx.sellProperty(i);
      }
    }
  }
}

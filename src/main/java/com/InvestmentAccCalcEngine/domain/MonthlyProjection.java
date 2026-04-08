package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;
@Getter
@AllArgsConstructor
public class MonthlyProjection {
    private int month;
    private BigDecimal projectedBalance;
}
package com.InvestmentAccCalcEngine.domain;


import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {
    private Long id;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal balance;
}
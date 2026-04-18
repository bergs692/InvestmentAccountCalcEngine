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

    /** Deep copy constructor for simulation state isolation. */
    public BankAccount(BankAccount other) {
        this.id = other.id;
        this.accountNumber = other.accountNumber;
        this.accountHolder = other.accountHolder;
        this.balance = other.balance; // BigDecimal is immutable
    }
}

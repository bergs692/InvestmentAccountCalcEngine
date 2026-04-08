package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.BankAccount;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BankAccountService {

    private final List<BankAccount> accounts = new ArrayList<>();

    public void deposit(String accountNumber, BigDecimal amount) {
        BankAccount account = findByAccountNumber(accountNumber);
        account.setBalance(account.getBalance().add(amount));
    }

    public void withdraw(String accountNumber, BigDecimal amount) {
        BankAccount account = findByAccountNumber(accountNumber);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
    }

    public void charge(String accountNumber, BigDecimal amount) {
        withdraw(accountNumber, amount);
    }

    public BigDecimal getBalance(String accountNumber) {
        return findByAccountNumber(accountNumber).getBalance();
    }

    public void createAccount(String accountNumber, String accountHolder, BigDecimal initialBalance) {
        accounts.add(new BankAccount(null, accountNumber, accountHolder, initialBalance));
    }

    private BankAccount findByAccountNumber(String accountNumber) {
        return accounts.stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }
}
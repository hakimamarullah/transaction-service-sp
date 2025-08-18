package com.banking.transactions.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public interface IExchangeRateService {

    CompletableFuture<BigDecimal> getRateAsync(String fromCurrency, String toCurrency, LocalDate date);
}

package com.banking.transactions.service.impl;

import com.banking.transactions.service.IExchangeRateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Service
public class ExchangeRateService implements IExchangeRateService {
    @Override
    public CompletableFuture<BigDecimal> getRateAsync(String fromCurrency, String toCurrency, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            if (fromCurrency.equals(toCurrency)) {
                return BigDecimal.ONE;
            }
            // TODO: call real external API asynchronously (e.g. WebClient)
            // For demo we simulate a delay
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
            return BigDecimal.valueOf(1.1);
        });
    }
}

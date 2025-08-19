package com.banking.transactions.service.impl;

import com.banking.transactions.service.IExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService implements IExchangeRateService {

    @Async
    @Override
    public CompletableFuture<BigDecimal> getRateAsync(String fromCurrency, String toCurrency, LocalDate date) {

        if (fromCurrency.equals(toCurrency)) {
            return CompletableFuture.completedFuture(BigDecimal.ONE);
        }
        // TODO: call real external API asynchronously (e.g. WebClient)
        // For demo we simulate a delay
        try {
            log.info("Simulating Get Rate Async");
            Thread.sleep(500);
            log.info("Finished Get Rate Async");
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture(BigDecimal.valueOf(1.1));

    }
}

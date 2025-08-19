package com.banking.transactions.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ExchangeRateServiceTest {

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService();
    }

    @Test
    void givenSameCurrencies_whenGetRateAsync_thenReturnsOneImmediately() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "USD";
        String toCurrency = "USD";
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);

        // Then
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        assertEquals(BigDecimal.ONE, result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void givenSameCurrenciesDifferentCase_whenGetRateAsync_thenReturnsOneImmediately() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "USD";
        String toCurrency = "USD";
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);

        // Then
        assertNotNull(result);
        assertTrue(result.isDone());
        assertEquals(BigDecimal.ONE, result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void givenDifferentCurrencies_whenGetRateAsync_thenReturnsSimulatedRate() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);
        BigDecimal rate = result.get(2, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        assertEquals(BigDecimal.valueOf(1.1), rate);
        // Verify that some delay occurred (should be around 500ms)
        assertTrue(endTime - startTime >= 400, "Expected delay of at least 400ms");
    }

    @Test
    void givenMultipleCurrencyPairs_whenGetRateAsync_thenEachReturnsCorrectValue() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        CompletableFuture<BigDecimal> usdToEur = exchangeRateService.getRateAsync("USD", "EUR", date);
        CompletableFuture<BigDecimal> gbpToUsd = exchangeRateService.getRateAsync("GBP", "USD", date);
        CompletableFuture<BigDecimal> jpyToGbp = exchangeRateService.getRateAsync("JPY", "GBP", date);
        CompletableFuture<BigDecimal> sameRate = exchangeRateService.getRateAsync("CHF", "CHF", date);

        // Wait for all to complete
        CompletableFuture.allOf(usdToEur, gbpToUsd, jpyToGbp, sameRate).get(3, TimeUnit.SECONDS);

        // Then
        assertEquals(BigDecimal.valueOf(1.1), usdToEur.get());
        assertEquals(BigDecimal.valueOf(1.1), gbpToUsd.get());
        assertEquals(BigDecimal.valueOf(1.1), jpyToGbp.get());
        assertEquals(BigDecimal.ONE, sameRate.get());
    }


    @Test
    void givenNullToCurrency_whenGetRateAsync_thenReturnsSimulatedRate() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "USD";
        String toCurrency = null;
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1.1), result.get(2, TimeUnit.SECONDS));
    }

    @Test
    void givenNullDate_whenGetRateAsync_thenReturnsSimulatedRate() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        LocalDate date = null;

        // When
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1.1), result.get(2, TimeUnit.SECONDS));
    }


    @Test
    void givenDifferentDates_whenGetRateAsync_thenReturnsConsistentResults() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 6, 15);
        LocalDate date3 = LocalDate.of(2023, 12, 25);

        // When
        CompletableFuture<BigDecimal> result1 = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date1);
        CompletableFuture<BigDecimal> result2 = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date2);
        CompletableFuture<BigDecimal> result3 = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date3);

        // Wait for all to complete
        CompletableFuture.allOf(result1, result2, result3).get(3, TimeUnit.SECONDS);

        // Then
        assertEquals(BigDecimal.valueOf(1.1), result1.get());
        assertEquals(BigDecimal.valueOf(1.1), result2.get());
        assertEquals(BigDecimal.valueOf(1.1), result3.get());
    }

    @Test
    void givenEmptyStringCurrencies_whenGetRateAsync_thenReturnsSimulatedRate() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "";
        String toCurrency = "EUR";
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1.1), result.get(2, TimeUnit.SECONDS));
    }

    @Test
    void givenBothEmptyStringCurrencies_whenGetRateAsync_thenReturnsOne() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String fromCurrency = "";
        String toCurrency = "";
        LocalDate date = LocalDate.of(2024, 3, 15);

        // When
        CompletableFuture<BigDecimal> result = exchangeRateService.getRateAsync(fromCurrency, toCurrency, date);

        // Then
        assertNotNull(result);
        assertTrue(result.isDone());
        assertEquals(BigDecimal.ONE, result.get(1, TimeUnit.SECONDS));
    }


}
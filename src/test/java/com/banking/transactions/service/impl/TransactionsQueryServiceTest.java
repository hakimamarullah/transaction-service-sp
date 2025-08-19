package com.banking.transactions.service.impl;

import com.banking.transactions.dto.PageSummary;
import com.banking.transactions.dto.Transaction;
import com.banking.transactions.dto.TransactionDTO;
import com.banking.transactions.dto.TransactionPageResponse;
import com.banking.transactions.service.IExchangeRateService;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TransactionsQueryServiceTest {

    @Mock
    private StreamsBuilderFactoryBean streamsFactory;

    @Mock
    private IExchangeRateService exchangeRateService;

    @Mock
    private KafkaStreams kafkaStreams;

    @Mock
    private ReadOnlyKeyValueStore<String, Transaction> store;

    @Mock
    private KeyValueIterator<String, Transaction> iterator;

    @InjectMocks
    private TransactionsQueryService transactionsQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void givenValidCustomerAndDateAndPagination_whenGetTransactions_thenReturnsPagedTransactions() {
        // Given
        String customerId = "customer123";
        int year = 2024;
        int month = 3;
        int page = 0;
        int size = 10;
        String baseCurrency = "USD";

        List<KeyValue<String, Transaction>> mockTransactions = createMockTransactions(customerId, year, month);

        when(streamsFactory.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(store);
        when(store.all()).thenReturn(iterator);

        setupIteratorMock(mockTransactions);
        setupExchangeRateServiceMock();

        // When
        TransactionPageResponse result = transactionsQueryService.getTransactions(
                customerId, year, month, page, size, baseCurrency);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTransactions().size());
        assertEquals(3, result.getPageInfo().getTotalElements());
        assertEquals(1, result.getPageInfo().getTotalPages());
        assertEquals(0, result.getPageInfo().getPage());
        assertEquals(10, result.getPageInfo().getSize());
        assertTrue(result.getPageInfo().isFirst());
        assertTrue(result.getPageInfo().isLast());
        assertFalse(result.getPageInfo().isHasNext());
        assertFalse(result.getPageInfo().isHasPrevious());

        // Verify transactions are sorted by valueDate descending
        LocalDate previousDate = LocalDate.MAX;
        for (TransactionDTO dto : result.getTransactions()) {
            assertTrue(dto.getValueDate().isBefore(previousDate) || dto.getValueDate().isEqual(previousDate));
            previousDate = dto.getValueDate();
        }
    }

    @Test
    void givenMultiplePages_whenGetTransactionsSecondPage_thenReturnsCorrectPage() {
        // Given
        String customerId = "customer123";
        int year = 2024;
        int month = 3;
        int page = 1;
        int size = 2;
        String baseCurrency = "USD";

        List<KeyValue<String, Transaction>> mockTransactions = createMockTransactions(customerId, year, month);

        when(streamsFactory.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(store);
        when(store.all()).thenReturn(iterator);

        setupIteratorMock(mockTransactions);
        setupExchangeRateServiceMock();

        // When
        TransactionPageResponse result = transactionsQueryService.getTransactions(
                customerId, year, month, page, size, baseCurrency);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTransactions().size()); // Last page with 1 item
        assertEquals(3, result.getPageInfo().getTotalElements());
        assertEquals(2, result.getPageInfo().getTotalPages());
        assertEquals(1, result.getPageInfo().getPage());
        assertEquals(2, result.getPageInfo().getSize());
        assertFalse(result.getPageInfo().isFirst());
        assertTrue(result.getPageInfo().isLast());
        assertFalse(result.getPageInfo().isHasNext());
        assertTrue(result.getPageInfo().isHasPrevious());
    }

    @Test
    void givenNoTransactionsFound_whenGetTransactions_thenReturnsEmptyPage() {
        // Given
        String customerId = "customer123";
        int year = 2024;
        int month = 3;
        int page = 0;
        int size = 10;
        String baseCurrency = "USD";

        when(streamsFactory.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(store);
        when(store.all()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(false);

        // When
        TransactionPageResponse result = transactionsQueryService.getTransactions(
                customerId, year, month, page, size, baseCurrency);

        // Then
        assertNotNull(result);
        assertTrue(result.getTransactions().isEmpty());
        assertEquals(0, result.getPageInfo().getTotalElements());
        assertEquals(0, result.getPageInfo().getTotalPages());
        assertEquals(BigDecimal.ZERO, result.getSummary().getTotalCredits());
        assertEquals(BigDecimal.ZERO, result.getSummary().getTotalDebits());
        assertEquals(BigDecimal.ZERO, result.getSummary().getNetAmount());
        assertEquals(0, result.getSummary().getTransactionCount());
    }

    @Test
    void givenMixedTransactionTypes_whenGetTransactions_thenCalculatesCorrectSummary() {
        // Given
        String customerId = "customer123";
        int year = 2024;
        int month = 3;
        int page = 0;
        int size = 10;
        String baseCurrency = "USD";

        List<KeyValue<String, Transaction>> mockTransactions = createMixedTransactionTypes(customerId, year, month);

        when(streamsFactory.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(store);
        when(store.all()).thenReturn(iterator);

        setupIteratorMock(mockTransactions);
        setupExchangeRateServiceMock();

        // When
        TransactionPageResponse result = transactionsQueryService.getTransactions(
                customerId, year, month, page, size, baseCurrency);

        // Then
        assertNotNull(result);
        PageSummary summary = result.getSummary();
        assertEquals(new BigDecimal("300.00"), summary.getTotalCredits()); // 100 + 200
        assertEquals(new BigDecimal("150.00"), summary.getTotalDebits()); // 150
        assertEquals(new BigDecimal("150.00"), summary.getNetAmount()); // 300 - 150
        assertEquals(baseCurrency, summary.getBaseCurrency());
        assertEquals(3, summary.getTransactionCount());
    }

    @Test
    void givenNullKafkaStreams_whenGetTransactions_thenThrowsException() {
        // Given
        String customerId = "customer123";
        int year = 2024;
        int month = 3;
        int page = 0;
        int size = 10;
        String baseCurrency = "USD";

        when(streamsFactory.getKafkaStreams()).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () ->
                transactionsQueryService.getTransactions(customerId, year, month, page, size, baseCurrency));
    }

    @Test
    void givenDifferentBaseCurrency_whenGetTransactions_thenConvertsAmounts() {
        // Given
        String customerId = "customer123";
        int year = 2024;
        int month = 3;
        int page = 0;
        int size = 10;
        String baseCurrency = "EUR";

        List<KeyValue<String, Transaction>> mockTransactions = createMockTransactions(customerId, year, month);

        when(streamsFactory.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(store);
        when(store.all()).thenReturn(iterator);

        setupIteratorMock(mockTransactions);

        // Different exchange rate for EUR conversion
        when(exchangeRateService.getRateAsync(anyString(), eq("EUR"), any(LocalDate.class)))
                .thenReturn(CompletableFuture.completedFuture(new BigDecimal("0.85")));

        // When
        TransactionPageResponse result = transactionsQueryService.getTransactions(
                customerId, year, month, page, size, baseCurrency);

        // Then
        assertNotNull(result);
        assertEquals("EUR", result.getSummary().getBaseCurrency());
        result.getTransactions().forEach(dto -> {
            assertEquals("EUR", dto.getBaseCurrency());
            assertEquals(new BigDecimal("0.85"), dto.getExchangeRate());
        });
    }

    @Test
    void givenTransactionsFromDifferentCustomer_whenGetTransactions_thenFiltersCorrectly() {
        // Given
        String customerId = "customer123";
        String otherCustomerId = "customer456";
        int year = 2024;
        int month = 3;
        int page = 0;
        int size = 10;
        String baseCurrency = "USD";

        List<KeyValue<String, Transaction>> allTransactions = new ArrayList<>();
        allTransactions.addAll(createMockTransactions(customerId, year, month));
        allTransactions.addAll(createMockTransactions(otherCustomerId, year, month));

        when(streamsFactory.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(store);
        when(store.all()).thenReturn(iterator);

        setupIteratorMock(allTransactions);
        setupExchangeRateServiceMock();

        // When
        TransactionPageResponse result = transactionsQueryService.getTransactions(
                customerId, year, month, page, size, baseCurrency);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTransactions().size()); // Only customer123's transactions
        result.getTransactions().forEach(dto ->
                assertTrue(dto.getId().contains(customerId)));
    }

    private List<KeyValue<String, Transaction>> createMockTransactions(String customerId, int year, int month) {
        List<KeyValue<String, Transaction>> transactions = new ArrayList<>();

        String prefix = customerId + ":" + year + "-" + String.format("%02d", month) + ":";

        Transaction t1 = Transaction.builder()
                .id(customerId + "_tx1")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .accountIban("DE123456789")
                .valueDate(LocalDate.of(year, month, 1))
                .description("Payment 1")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        Transaction t2 = Transaction.builder()
                .id(customerId + "_tx2")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .accountIban("DE123456789")
                .valueDate(LocalDate.of(year, month, 15))
                .description("Payment 2")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        Transaction t3 = Transaction.builder()
                .id(customerId + "_tx3")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .accountIban("DE123456789")
                .valueDate(LocalDate.of(year, month, 30))
                .description("Payment 3")
                .type(Transaction.TransactionType.DEBIT)
                .build();

        transactions.add(new KeyValue<>(prefix + "tx1", t1));
        transactions.add(new KeyValue<>(prefix + "tx2", t2));
        transactions.add(new KeyValue<>(prefix + "tx3", t3));

        return transactions;
    }

    private List<KeyValue<String, Transaction>> createMixedTransactionTypes(String customerId, int year, int month) {
        List<KeyValue<String, Transaction>> transactions = new ArrayList<>();

        String prefix = customerId + ":" + year + "-" + String.format("%02d", month) + ":";

        Transaction credit1 = Transaction.builder()
                .id(customerId + "_credit1")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .accountIban("DE123456789")
                .valueDate(LocalDate.of(year, month, 1))
                .description("Credit 1")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        Transaction credit2 = Transaction.builder()
                .id(customerId + "_credit2")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .accountIban("DE123456789")
                .valueDate(LocalDate.of(year, month, 15))
                .description("Credit 2")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        Transaction debit1 = Transaction.builder()
                .id(customerId + "_debit1")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .accountIban("DE123456789")
                .valueDate(LocalDate.of(year, month, 30))
                .description("Debit 1")
                .type(Transaction.TransactionType.DEBIT)
                .build();

        transactions.add(new KeyValue<>(prefix + "credit1", credit1));
        transactions.add(new KeyValue<>(prefix + "credit2", credit2));
        transactions.add(new KeyValue<>(prefix + "debit1", debit1));

        return transactions;
    }

    private void setupIteratorMock(List<KeyValue<String, Transaction>> transactions) {
        when(iterator.hasNext())
                .thenReturn(!transactions.isEmpty())
                .thenReturn(transactions.size() > 1)
                .thenReturn(transactions.size() > 2)
                .thenReturn(false);

        if (!transactions.isEmpty()) {
            when(iterator.next())
                    .thenReturn(transactions.get(0))
                    .thenReturn(transactions.size() > 1 ? transactions.get(1) : null)
                    .thenReturn(transactions.size() > 2 ? transactions.get(2) : null);
        }
    }

    private void setupExchangeRateServiceMock() {
        when(exchangeRateService.getRateAsync(anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(CompletableFuture.completedFuture(BigDecimal.ONE));
    }
}
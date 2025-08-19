package com.banking.transactions.service.impl;


import com.banking.transactions.config.StoreConfig;
import com.banking.transactions.dto.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class StoreTransactionServiceTest {

    @Mock
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Transaction>> sendResultFuture;

    @InjectMocks
    private StoreTransactionService storeTransactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void givenValidTransaction_whenStoreTransaction_thenSendsToKafkaTopic() {
        // Given
        Transaction transaction = createTestTransaction();
        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(transaction);

        // Then
        verify(kafkaTemplate, times(1))
                .send(StoreConfig.TRANSACTION_TOPIC, transaction.getId(), transaction);
    }

    @Test
    void givenValidTransaction_whenStoreTransaction_thenUsesCorrectTopicName() {
        // Given
        Transaction transaction = createTestTransaction();
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Transaction> valueCaptor = ArgumentCaptor.forClass(Transaction.class);

        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(transaction);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertEquals(StoreConfig.TRANSACTION_TOPIC, topicCaptor.getValue());
        assertEquals(transaction.getId(), keyCaptor.getValue());
        assertEquals(transaction, valueCaptor.getValue());
    }

    @Test
    void givenTransactionWithSpecificId_whenStoreTransaction_thenUsesTransactionIdAsKey() {
        // Given
        String expectedTransactionId = "tx_12345_customer_abc";
        Transaction transaction = Transaction.builder()
                .id(expectedTransactionId)
                .amount(new BigDecimal("500.75"))
                .currency("EUR")
                .accountIban("DE89370400440532013000")
                .valueDate(LocalDate.of(2024, 6, 15))
                .description("Test payment")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(transaction);

        // Then
        verify(kafkaTemplate).send(eq(StoreConfig.TRANSACTION_TOPIC), keyCaptor.capture(), eq(transaction));
        assertEquals(expectedTransactionId, keyCaptor.getValue());
    }

    @Test
    void givenCreditTransaction_whenStoreTransaction_thenSendsCorrectTransactionData() {
        // Given
        Transaction creditTransaction = Transaction.builder()
                .id("credit_tx_001")
                .amount(new BigDecimal("1250.00"))
                .currency("USD")
                .accountIban("US12345678901234567890")
                .valueDate(LocalDate.of(2024, 3, 20))
                .description("Salary payment")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(creditTransaction);

        // Then
        verify(kafkaTemplate).send(eq(StoreConfig.TRANSACTION_TOPIC), eq("credit_tx_001"), transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals("credit_tx_001", capturedTransaction.getId());
        assertEquals(new BigDecimal("1250.00"), capturedTransaction.getAmount());
        assertEquals("USD", capturedTransaction.getCurrency());
        assertEquals("US12345678901234567890", capturedTransaction.getAccountIban());
        assertEquals(LocalDate.of(2024, 3, 20), capturedTransaction.getValueDate());
        assertEquals("Salary payment", capturedTransaction.getDescription());
        assertEquals(Transaction.TransactionType.CREDIT, capturedTransaction.getType());
    }

    @Test
    void givenDebitTransaction_whenStoreTransaction_thenSendsCorrectTransactionData() {
        // Given
        Transaction debitTransaction = Transaction.builder()
                .id("debit_tx_002")
                .amount(new BigDecimal("75.50"))
                .currency("GBP")
                .accountIban("GB82WEST12345698765432")
                .valueDate(LocalDate.of(2024, 3, 21))
                .description("Grocery shopping")
                .type(Transaction.TransactionType.DEBIT)
                .build();

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(debitTransaction);

        // Then
        verify(kafkaTemplate).send(eq(StoreConfig.TRANSACTION_TOPIC), eq("debit_tx_002"), transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals("debit_tx_002", capturedTransaction.getId());
        assertEquals(new BigDecimal("75.50"), capturedTransaction.getAmount());
        assertEquals("GBP", capturedTransaction.getCurrency());
        assertEquals("GB82WEST12345698765432", capturedTransaction.getAccountIban());
        assertEquals(LocalDate.of(2024, 3, 21), capturedTransaction.getValueDate());
        assertEquals("Grocery shopping", capturedTransaction.getDescription());
        assertEquals(Transaction.TransactionType.DEBIT, capturedTransaction.getType());
    }

    @Test
    void givenMultipleTransactions_whenStoreTransactionCalledMultipleTimes_thenEachTransactionSentSeparately() {
        // Given
        Transaction transaction1 = createTestTransaction("tx_001", new BigDecimal("100.00"));
        Transaction transaction2 = createTestTransaction("tx_002", new BigDecimal("200.00"));
        Transaction transaction3 = createTestTransaction("tx_003", new BigDecimal("300.00"));

        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(transaction1);
        storeTransactionService.storeTransaction(transaction2);
        storeTransactionService.storeTransaction(transaction3);

        // Then
        verify(kafkaTemplate, times(3)).send(eq(StoreConfig.TRANSACTION_TOPIC), anyString(), any(Transaction.class));
        verify(kafkaTemplate).send(StoreConfig.TRANSACTION_TOPIC, "tx_001", transaction1);
        verify(kafkaTemplate).send(StoreConfig.TRANSACTION_TOPIC, "tx_002", transaction2);
        verify(kafkaTemplate).send(StoreConfig.TRANSACTION_TOPIC, "tx_003", transaction3);
    }

    @Test
    void givenNullTransactionId_whenStoreTransaction_thenStillCallsKafkaTemplate() {
        // Given
        Transaction transactionWithNullId = Transaction.builder()
                .id(null)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .accountIban("US12345678901234567890")
                .valueDate(LocalDate.of(2024, 3, 15))
                .description("Test transaction")
                .type(Transaction.TransactionType.CREDIT)
                .build();

        when(kafkaTemplate.send(anyString(), any(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        storeTransactionService.storeTransaction(transactionWithNullId);

        // Then
        verify(kafkaTemplate, times(1))
                .send(StoreConfig.TRANSACTION_TOPIC, null, transactionWithNullId);
    }

    @Test
    void givenKafkaTemplateThrowsException_whenStoreTransaction_thenExceptionPropagates() {
        // Given
        Transaction transaction = createTestTransaction();
        RuntimeException kafkaException = new RuntimeException("Kafka connection failed");

        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenThrow(kafkaException);

        // When & Then
        RuntimeException thrownException = assertThrows(RuntimeException.class, () ->
                storeTransactionService.storeTransaction(transaction));

        assertEquals("Kafka connection failed", thrownException.getMessage());
        verify(kafkaTemplate, times(1))
                .send(StoreConfig.TRANSACTION_TOPIC, transaction.getId(), transaction);
    }

    @Test
    void givenValidTransaction_whenStoreTransaction_thenNoReturnValue() {
        // Given
        Transaction transaction = createTestTransaction();
        when(kafkaTemplate.send(anyString(), anyString(), any(Transaction.class)))
                .thenReturn(sendResultFuture);

        // When
        assertDoesNotThrow(() -> storeTransactionService.storeTransaction(transaction));

        // Then
        verify(kafkaTemplate, times(1))
                .send(StoreConfig.TRANSACTION_TOPIC, transaction.getId(), transaction);
    }

    private Transaction createTestTransaction() {
        return createTestTransaction("test_tx_123", new BigDecimal("250.75"));
    }

    private Transaction createTestTransaction(String id, BigDecimal amount) {
        return Transaction.builder()
                .id(id)
                .amount(amount)
                .currency("USD")
                .accountIban("DE89370400440532013000")
                .valueDate(LocalDate.of(2024, 3, 15))
                .description("Test transaction")
                .type(Transaction.TransactionType.CREDIT)
                .build();
    }
}
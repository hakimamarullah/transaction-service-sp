package com.banking.transactions.service;

import com.banking.transactions.dto.TransactionPageResponse;

public interface ITransactionsQueryService {

    TransactionPageResponse getTransactions(String customerId,
                                                   int year, int month,
                                                   int page, int size,
                                                   String baseCurrency);
}

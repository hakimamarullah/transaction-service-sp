package com.banking.transactions.service;

import com.banking.transactions.dto.Transaction;

public interface IStoreTransactionService {

    void storeTransaction(Transaction transaction);
}

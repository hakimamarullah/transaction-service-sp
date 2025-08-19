package com.banking.transactions.service.impl;

import com.banking.transactions.config.StoreConfig;
import com.banking.transactions.dto.Transaction;
import com.banking.transactions.service.IStoreTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@RegisterReflectionForBinding({
        Transaction.class
})
public class StoreTransactionService implements IStoreTransactionService {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;


    @Override
    public void storeTransaction(Transaction transaction) {
        kafkaTemplate.send(StoreConfig.TRANSACTION_TOPIC, transaction.getId(), transaction);
    }
}

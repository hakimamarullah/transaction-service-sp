package com.banking.transactions.service.impl;

import com.banking.transactions.config.TopologyConfig;
import com.banking.transactions.dto.Transaction;
import com.banking.transactions.service.IStoreTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreTransactionService implements IStoreTransactionService {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;


    @Override
    public void storeTransaction(Transaction transaction) {
        kafkaTemplate.send(TopologyConfig.TRANSACTION_TOPIC, transaction.getId(), transaction);
    }
}

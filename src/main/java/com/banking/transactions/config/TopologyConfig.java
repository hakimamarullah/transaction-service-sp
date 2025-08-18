package com.banking.transactions.config;

import com.banking.transactions.dto.Transaction;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
public class TopologyConfig {
    public static final String TRANSACTION_TOPIC = "transactions";
    public static final String STORE_NAME = "transactions-store";

    @Autowired
    public void buildTransactionsTable(StreamsBuilder builder) {
        var serde = new JsonSerde<>(Transaction.class);

        builder.stream(TRANSACTION_TOPIC,
                        Consumed.with(Serdes.String(), serde))
                .selectKey((k, v) -> {
                    // rekey by customerId:year-month:transactionId
                    String month = v.getValueDate().getYear() + "-" +
                            String.format("%02d", v.getValueDate().getMonthValue());
                    return v.getCustomerId() + ":" + month + ":" + v.getId();
                })
                .toTable(
                        Materialized.<String, Transaction>as(
                                        Stores.persistentKeyValueStore(STORE_NAME))
                                .withKeySerde(Serdes.String())
                                .withValueSerde(serde)
                );

    }
}
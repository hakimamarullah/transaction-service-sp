package com.banking.transactions.service.impl;

import com.banking.transactions.config.TopologyConfig;
import com.banking.transactions.dto.PageInfo;
import com.banking.transactions.dto.PageSummary;
import com.banking.transactions.dto.Transaction;
import com.banking.transactions.dto.TransactionDTO;
import com.banking.transactions.dto.TransactionPageResponse;
import com.banking.transactions.service.IExchangeRateService;
import com.banking.transactions.service.ITransactionsQueryService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TransactionsQueryService implements ITransactionsQueryService {

    private final StreamsBuilderFactoryBean streamsFactory;
    private final IExchangeRateService exchangeRateService;

    @Override
    public TransactionPageResponse getTransactions(String customerId,
                                                   int year, int month,
                                                   int page, int size,
                                                   String baseCurrency) {


        var streams = Optional.ofNullable(streamsFactory.getKafkaStreams()).orElseThrow();
        ReadOnlyKeyValueStore<String, Transaction> store =
                streams
                        .store(StoreQueryParameters.fromNameAndType(
                                TopologyConfig.STORE_NAME,
                                QueryableStoreTypes.keyValueStore()
                        ));


        String prefix = customerId + ":" + year + "-" + String.format("%02d", month) + ":";

        List<Transaction> all = new ArrayList<>();
        try (var iter = store.all()) {
            while (iter.hasNext()) {
                var kv = iter.next();
                if (kv.key.startsWith(prefix)) {
                    all.add(kv.value);
                }
            }
        }

        int totalElements = all.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<Transaction> pageContent = all.stream()
                .sorted(Comparator.comparing(Transaction::getValueDate).reversed())
                .skip((long) page * size)
                .limit(size)
                .toList();

        // --- async enrichment ---
        List<CompletableFuture<TransactionDTO>> futures = pageContent.stream()
                .map(t -> exchangeRateService.getRateAsync(t.getCurrency(), baseCurrency, t.getValueDate())
                        .thenApply(rate -> {
                            BigDecimal converted = t.getAmount().multiply(rate);
                            return TransactionDTO.builder()
                                    .id(t.getId())
                                    .originalAmount(t.getAmount())
                                    .originalCurrency(t.getCurrency())
                                    .convertedAmount(converted)
                                    .baseCurrency(baseCurrency)
                                    .exchangeRate(rate)
                                    .accountIban(t.getAccountIban())
                                    .valueDate(t.getValueDate())
                                    .description(t.getDescription())
                                    .type(t.getType())
                                    .build();
                        }))
                .toList();

        // Wait for all futures
        List<TransactionDTO> dtos = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Summary
        BigDecimal totalCredits = dtos.stream()
                .filter(dto -> dto.getType() == Transaction.TransactionType.CREDIT)
                .map(TransactionDTO::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = dtos.stream()
                .filter(dto -> dto.getType() == Transaction.TransactionType.DEBIT)
                .map(TransactionDTO::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PageInfo pageInfo = PageInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page == totalPages - 1)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();

        PageSummary summary = PageSummary.builder()
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .netAmount(totalCredits.subtract(totalDebits))
                .baseCurrency(baseCurrency)
                .transactionCount(dtos.size())
                .build();
        return TransactionPageResponse.builder()
                .transactions(dtos)
                .pageInfo(pageInfo)
                .summary(summary)
                .build();


    }
}

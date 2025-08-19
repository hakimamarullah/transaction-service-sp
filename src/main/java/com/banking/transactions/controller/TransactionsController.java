package com.banking.transactions.controller;


import com.banking.transactions.annotations.LogRequestResponse;
import com.banking.transactions.dto.Transaction;
import com.banking.transactions.dto.TransactionPageResponse;
import com.banking.transactions.service.IStoreTransactionService;
import com.banking.transactions.service.ITransactionsQueryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerJWT")
@LogRequestResponse
public class TransactionsController {

    private final ITransactionsQueryService queryService;

    private final IStoreTransactionService storeTransactionService;

    @GetMapping
    public ResponseEntity<TransactionPageResponse> getTransactions(
            JwtAuthenticationToken jwt,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "IDR") String baseCurrency) {

        TransactionPageResponse response =
                queryService.getTransactions(jwt.getToken().getClaimAsString("user_id"), year, month, page, size, baseCurrency);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<String> storeTransaction(@RequestBody @Valid Transaction transaction) {
        storeTransactionService.storeTransaction(transaction);
        return ResponseEntity.ok("Transaction stored successfully");
    }
}

package com.banking.transactions.dto;

import com.banking.transactions.annotations.Censor;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction DTO with exchange rate information")
public class TransactionDTO {

    @Schema(description = "Unique transaction identifier")
    private String id;

    @Schema(description = "Original transaction amount")
    private BigDecimal originalAmount;

    @Schema(description = "Original currency")
    private String originalCurrency;

    @Schema(description = "Amount converted to base currency at current exchange rate")
    private BigDecimal convertedAmount;

    @Schema(description = "Base currency for conversion", example = "EUR")
    private String baseCurrency;

    @Schema(description = "Exchange rate used for conversion")
    private BigDecimal exchangeRate;

    @Schema(description = "Account IBAN")
    @Censor
    private String accountIban;

    @Schema(description = "Transaction value date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate valueDate;

    @Schema(description = "Transaction description")
    private String description;

    @Schema(description = "Transaction type")
    private Transaction.TransactionType type;
}

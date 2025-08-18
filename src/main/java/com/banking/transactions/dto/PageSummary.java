package com.banking.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Page summary with totals")
public class PageSummary {

    @Schema(description = "Total credit amount in base currency for current page")
    private BigDecimal totalCredits;

    @Schema(description = "Total debit amount in base currency for current page")
    private BigDecimal totalDebits;

    @Schema(description = "Net amount (credits - debits) for current page")
    private BigDecimal netAmount;

    @Schema(description = "Base currency used for calculations")
    private String baseCurrency;

    @Schema(description = "Number of transactions in current page")
    private int transactionCount;
}

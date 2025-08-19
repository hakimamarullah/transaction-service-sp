package com.banking.transactions.dto;

import com.banking.transactions.annotations.Censor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Money account transaction")
public class Transaction {

    @Schema(description = "Unique transaction identifier", example = "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46")
    @NotBlank
    private String id;

    @Schema(description = "Transaction amount", example = "100.50")
    @NotNull
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "GBP")
    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    private String currency;

    @Schema(description = "Account IBAN", example = "CH93-0000-0000-0000-0000-0")
    @NotBlank
    @Censor
    private String accountIban;

    @Schema(description = "Transaction value date", example = "2020-10-01")
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate valueDate;

    @Schema(description = "Transaction description", example = "Online payment CHF")
    @NotBlank
    private String description;

    @Schema(description = "Customer identifier", example = "P-0123456789")
    @NotBlank
    @Censor
    private String customerId;

    @Schema(description = "Transaction type", example = "DEBIT")
    @NotNull
    private TransactionType type;

    public enum TransactionType {
        CREDIT, DEBIT;

        @JsonCreator
        public TransactionType forValue(String value) {
            return value != null ? valueOf(value.toUpperCase()) : null;
        }
    }
}
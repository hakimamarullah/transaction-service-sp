package com.banking.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated transaction response")
public class TransactionPageResponse {

    @Schema(description = "List of transactions")
    private List<TransactionDTO> transactions;

    @Schema(description = "Pagination information")
    private PageInfo pageInfo;

    @Schema(description = "Summary information for the current page")
    private PageSummary summary;
}

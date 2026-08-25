package br.com.kumabe.quotes.controllers.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record QuoteRequest(
    @NotBlank(message = "Customer ID is required")
    @Size(max = 50)
    String customerId,

    @NotNull(message = "Asset value is required")
    @Positive(message = "Asset value must be positive")
    @Max(1000000000)
    BigDecimal assetValue,

    @NotBlank(message = "Asset type is required")
    @Pattern(regexp = "^(AUTO|RESIDENCIAL|VIDA)$")
    String assetType
) {}
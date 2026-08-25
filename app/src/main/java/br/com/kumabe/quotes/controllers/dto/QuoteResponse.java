package br.com.kumabe.quotes.controllers.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record QuoteResponse(
    String quoteId,
    BigDecimal premiumValue,
    String status
) {}

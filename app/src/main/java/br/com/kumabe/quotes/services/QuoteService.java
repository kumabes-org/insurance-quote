package br.com.kumabe.quotes.services;

import br.com.kumabe.quotes.controllers.dto.QuoteRequest;
import br.com.kumabe.quotes.controllers.dto.QuoteResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class QuoteService {

    private static final BigDecimal BASE_RATE = new BigDecimal("0.05"); // 5%

    public QuoteResponse calculateQuote(QuoteRequest request) {
        // Simulação de processamento intensivo/IO
        BigDecimal premium = request.assetValue()
                .multiply(BASE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        return new QuoteResponse(
                UUID.randomUUID().toString(),
                premium,
                "CALCULATED"
        );
    }
}

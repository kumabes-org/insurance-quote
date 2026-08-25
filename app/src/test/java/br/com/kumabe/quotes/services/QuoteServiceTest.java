package br.com.kumabe.quotes.services;

import br.com.kumabe.quotes.controllers.dto.QuoteRequest;
import br.com.kumabe.quotes.controllers.dto.QuoteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class QuoteServiceTest {

    private final QuoteService quoteService = new QuoteService();

    @Test
    @DisplayName("Should calculate premium correctly with 5% rate")
    void shouldCalculatePremiumCorrectly() {
        // Arrange
        QuoteRequest request = new QuoteRequest("CUST-123", new BigDecimal("1000.00"), "AUTO");

        // Act
        QuoteResponse response = quoteService.calculateQuote(request);

        // Assert
        assertNotNull(response.quoteId());
        assertEquals(new BigDecimal("50.00"), response.premiumValue());
        assertEquals("CALCULATED", response.status());
    }
}
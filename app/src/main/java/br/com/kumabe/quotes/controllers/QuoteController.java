package br.com.kumabe.quotes.controllers;

import br.com.kumabe.quotes.controllers.dto.QuoteRequest;
import br.com.kumabe.quotes.controllers.dto.QuoteResponse;
import br.com.kumabe.quotes.services.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(quoteService.calculateQuote(request));
    }
}
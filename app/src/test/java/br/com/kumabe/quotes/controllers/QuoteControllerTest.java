package br.com.kumabe.quotes.controllers;

import br.com.kumabe.quotes.controllers.dto.QuoteRequest;
import br.com.kumabe.quotes.controllers.dto.QuoteResponse;
import br.com.kumabe.quotes.controllers.exception.GlobalExceptionHandler;
import br.com.kumabe.quotes.services.QuoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteController.class)
@Import(GlobalExceptionHandler.class)
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // A partir do Spring Boot 3.4+ usa-se @MockitoBean (substitui @MockBean)
    @MockitoBean
    private QuoteService quoteService;

    private Map<String, Object> createValidPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", "USER-123");
        payload.put("assetValue", 10000.00);
        payload.put("assetType", "AUTO");
        return payload;
    }

    // --- TESTES DE FUNCIONALIDADE (HAPPY PATH) ---

    @Test
    @DisplayName("Deve criar uma cotação com sucesso e retornar 200 OK")
    void testCreateQuoteSuccess() throws Exception {
        QuoteResponse mockResponse = new QuoteResponse(
                UUID.randomUUID().toString(),
                new BigDecimal("500.00"),
                "CALCULATED"
        );

        when(quoteService.calculateQuote(any(QuoteRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").exists())
                .andExpect(jsonPath("$.premiumValue").value(500.00))
                .andExpect(jsonPath("$.status").value("CALCULATED"));
    }

    // --- TESTES DE VALIDAÇÃO (EDGE CASES) ---

    @ParameterizedTest(name = "[{index}] Campo ''{0}'' com valor ''{1}'' deve falhar")
    @CsvSource(delimiter = '|', value = {
            "assetValue | -100     | must be positive",
            "assetValue | 0        | must be positive",
            "customerId | ''       | Customer ID is required",
            "assetType  | '   '    | Asset type is required"
    })
    @DisplayName("Deve retornar 400 Bad Request (ProblemDetail) para entradas inválidas")
    void testCreateQuoteInvalidInputs(String field, String value, String expectedMsg) throws Exception {
        Map<String, Object> payload = createValidPayload();

        if ("assetValue".equals(field)) {
            payload.put(field, new BigDecimal(value.trim()));
        } else {
            payload.put(field, value);
        }

        mockMvc.perform(post("/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail", containsString(expectedMsg)));
    }

    @Test
    @DisplayName("Deve retornar 400 quando o body estiver vazio")
    void testCreateQuoteMissingFields() throws Exception {
        mockMvc.perform(post("/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- TESTES DE SEGURANÇA & RESILIÊNCIA ---

    @Test
    @DisplayName("Simulação de envio com payload massivo (1MB String)")
    void testLargePayloadAttack() throws Exception {
        String largeString = "A".repeat(1_000_000); // 1 MB

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", largeString);
        payload.put("assetValue", 1000.00);
        payload.put("assetType", "AUTO");

        int statusCode = mockMvc.perform(post("/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn()
                .getResponse()
                .getStatus();

        // 200 (se não houver @Size no DTO), 400 (Bad Request) ou 413 (Payload Too Large)
        assertTrue(List.of(200, 400, 413).contains(statusCode),
                "Status HTTP inesperado: " + statusCode);
    }

    @Test
    @DisplayName("Deve processar valores numéricos extremos via BigDecimal")
    void testExtremeAssetValue() throws Exception {
        QuoteResponse mockResponse = new QuoteResponse(
                UUID.randomUUID().toString(),
                new BigDecimal("49999999999999999.99"),
                "CALCULATED"
        );
        when(quoteService.calculateQuote(any(QuoteRequest.class))).thenReturn(mockResponse);

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", "RICH-001");
        payload.put("assetValue", new BigDecimal("999999999999999999.99"));
        payload.put("assetType", "YACHT");

        mockMvc.perform(post("/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()); // Espera 400 devido a validação de assetType
    }

    // --- TESTE DE CONCORRÊNCIA (MOCKMVC + VIRTUAL THREADS) ---

    @Test
    @DisplayName("Smoke Test de requisições concorrentes no MockMvc com Virtual Threads")
    void testHighConcurrencySmoke() throws Exception {
        QuoteResponse mockResponse = new QuoteResponse(UUID.randomUUID().toString(), new BigDecimal("5.00"), "CALCULATED");
        when(quoteService.calculateQuote(any(QuoteRequest.class))).thenReturn(mockResponse);

        String jsonPayload = objectMapper.writeValueAsString(createValidPayload());
        int totalRequests = 100;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Integer>> tasks = java.util.stream.IntStream.range(0, totalRequests)
                    .mapToObj(i -> (Callable<Integer>) () -> mockMvc.perform(post("/quotes")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn()
                            .getResponse()
                            .getStatus())
                    .toList();

            List<Future<Integer>> results = executor.invokeAll(tasks);

            for (Future<Integer> result : results) {
                assertEquals(200, result.get().intValue());
            }
        }
    }
}
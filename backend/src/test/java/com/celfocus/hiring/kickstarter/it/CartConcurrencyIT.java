package com.celfocus.hiring.kickstarter.it;

import com.celfocus.hiring.kickstarter.api.AuthController;
import com.celfocus.hiring.kickstarter.api.dto.CartItemInput;
import com.celfocus.hiring.kickstarter.api.dto.CartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CartConcurrencyIT {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setup() {
        this.baseUrl = "http://localhost:" + port;
    }

    private String loginAndGetToken(String username, String password) {
        AuthController.LoginRequest req = new AuthController.LoginRequest(username, password);
        ResponseEntity<AuthController.AuthResponse> response = restTemplate.postForEntity(
                baseUrl + "/auth/login", req, AuthController.AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().token();
    }

    private HttpHeaders authHeaders(String token, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", username);
        return headers;
    }

    @Test
    void testConcurrentCartAdditionsAndStockDecrement() throws Exception {
        String sku = "SKUTEST2";
        int initialStock = 10;
        List<String> users = new ArrayList<>();
        users.add("user1");
        users.add("user2");

        // login users and collect tokens
        List<String> tokens = users.stream().map(user ->
                loginAndGetToken(user, "pass1")).collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(users.size());

        List<Callable<ResponseEntity<Void>>> tasks = new ArrayList<>();
        for (int rounds = 0; rounds < 7; rounds++) {
            for (int i = 0; i < users.size(); i++) {
                final int idx = 0;
                tasks.add(() -> {
                    CartItemInput input = new CartItemInput(sku);
                    HttpEntity<CartItemInput> entity =
                            new HttpEntity<>(input, authHeaders(tokens.get(idx), users.get(idx)));

                    return restTemplate.postForEntity(
                            baseUrl + "/api/v1/carts/items", entity, Void.class);
                });
            }
        }

        List<Future<ResponseEntity<Void>>> results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = results.stream()
                .map(f -> {
                    try {
                        return f.get().getStatusCode();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(code -> code.is2xxSuccessful())
                .count();

        long failedStatus = results.stream()
                .map(f -> {
                    try {
                        return f.get().getStatusCode();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(code -> code.is4xxClientError())
                .count();
        assertThat(successCount).isEqualTo(initialStock);

        String token = tokens.get(0);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token, users.get(0)));
        ResponseEntity<CartResponse> cartResponse = restTemplate.exchange(
                baseUrl + "/api/v1/carts", HttpMethod.GET, entity, CartResponse.class);

        assertThat(cartResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cartResponse.getBody().items())
                .anyMatch(it -> it.itemId().equals(sku));
    }


}

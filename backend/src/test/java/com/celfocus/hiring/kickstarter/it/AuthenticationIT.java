package com.celfocus.hiring.kickstarter.it;

import com.celfocus.hiring.kickstarter.api.AuthController;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationIT {
    @LocalServerPort
    private int port;

    private String AUTH_URL = "/auth/login";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    public void test_userCorrectCredentials_expect_success() {
        AuthController.LoginRequest req = new AuthController.LoginRequest("user1", "pass1");
        given().contentType(ContentType.JSON)
                .body(req)
                .when()
                .post(AUTH_URL)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", notNullValue());
    }

    @Test
    public void test_userWrongUsername_expect_Unauthorized() {
        AuthController.LoginRequest req = new AuthController.LoginRequest("user4", "pass1");
        given().contentType(ContentType.JSON)
                .body(req)
                .when()
                .post(AUTH_URL)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("token", nullValue());
    }

    @Test
    public void test_userWrongPassword_expect_Unauthorized() {
        AuthController.LoginRequest req = new AuthController.LoginRequest("user1", "wrongpass");
        given().contentType(ContentType.JSON)
                .body(req)
                .when()
                .post(AUTH_URL)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("token", nullValue());
    }
}

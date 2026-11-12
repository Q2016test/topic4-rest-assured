package ru.homework.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.preemptive;
import static org.hamcrest.Matchers.*;

@Tag("api")
abstract class ApiTestBase {
    protected final String baseUrl = System.getProperty("api.baseUrl", "http://localhost:8080");
    protected final String username = System.getProperty("api.username", "admin");
    protected final String password = System.getProperty("api.password", "secret123");
    private final List<Long> createdIds = new ArrayList<>();

    protected RequestSpecification anonymous() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setConfig(RestAssuredConfig.config()
                        .logConfig(LogConfig.logConfig().blacklistHeader("Authorization")
                                .enableLoggingOfRequestAndResponseIfValidationFails())
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", 5000)
                                .setParam("http.socket.timeout", 5000)))
                .build();
    }

    protected RequestSpecification authorized() {
        return new RequestSpecBuilder().addRequestSpecification(anonymous())
                .setAuth(preemptive().basic(username, password)).build();
    }

    protected Map<String, Object> product(String name, double price) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("price", price);
        return body;
    }

    protected String uniqueName() {
        return "api-test-" + UUID.randomUUID();
    }

    protected long createProduct(String name, double price) {
        Response response = given().spec(authorized()).body(product(name, price))
                .when().post("/goods/add");
        response.then().statusCode(200).body("message", equalTo("success"))
                .body("data.id", notNullValue());
        long id = response.jsonPath().getLong("data.id");
        createdIds.add(id);
        return id;
    }

    @AfterEach
    void removeCreatedProducts() {
        for (long id : createdIds) {
            given().spec(authorized()).when().delete("/goods/{id}", id)
                    .then().statusCode(anyOf(is(200), is(404)));
        }
        createdIds.clear();
    }
}

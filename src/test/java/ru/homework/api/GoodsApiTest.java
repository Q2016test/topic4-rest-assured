package ru.homework.api;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Tag("webinar")
class GoodsApiTest extends ApiTestBase {
    @Test
    void listEmpty200() {
        given().spec(anonymous()).when().get("/goods/list")
                .then().statusCode(200).body("goods", empty());
    }

    @ParameterizedTest
    @CsvSource({"0,2,2", "1,2,1", "2,2,0"})
    void listPagination200(int page, int size, int expectedCount) {
        for (int i = 0; i < 3; i++) {
            createProduct(uniqueName(), i + 1.5);
        }
        given().spec(anonymous()).queryParam("page", page).queryParam("size", size)
                .when().get("/goods/list")
                .then().statusCode(200).body("goods", hasSize(expectedCount));
    }

    @Test
    void add200() {
        String name = uniqueName();
        long id = createProduct(name, 0.0);
        assertThat(id).as("POST /goods/add generated id").isPositive();
        given().spec(authorized()).when().get("/goods/{id}", id)
                .then().statusCode(200).body("name", equalTo(name)).body("price", equalTo(0.0f));
    }

    @ParameterizedTest
    @MethodSource("invalidProducts")
    void addInvalid400(String body, String field) {
        given().spec(authorized()).body(body).when().post("/goods/add")
                .then().statusCode(400).body(field, not(isEmptyOrNullString()));
        given().spec(anonymous()).when().get("/goods/list")
                .then().statusCode(200).body("goods", empty());
    }

    @Test
    void addDuplicate400() {
        String name = uniqueName();
        createProduct(name, 10.0);
        given().spec(authorized()).body(product(name, 20.0)).when().post("/goods/add")
                .then().statusCode(400).body("data", nullValue())
                .body("message", containsString("already exists"));
    }

    @Test
    void get200() {
        String name = uniqueName();
        long id = createProduct(name, 42.5);
        given().spec(authorized()).when().get("/goods/{id}", id)
                .then().statusCode(200).body("id", equalTo((int) id))
                .body("name", equalTo(name)).body("price", equalTo(42.5f));
    }

    @Test
    void getMissing404() {
        long id = createProduct(uniqueName(), 15.0);
        given().spec(authorized()).when().delete("/goods/{id}", id).then().statusCode(200);
        given().spec(authorized()).when().get("/goods/{id}", id)
                .then().statusCode(404).body("data", nullValue())
                .body("message", containsString("not found"));
    }

    @Test
    void delete200() {
        long id = createProduct(uniqueName(), 15.0);
        given().spec(authorized()).when().delete("/goods/{id}", id)
                .then().statusCode(200).body(containsString("has been deleted successfully"));
        given().spec(authorized()).when().get("/goods/{id}", id).then().statusCode(404);
    }

    @Test
    void deleteMissing404() {
        given().spec(authorized()).when().delete("/goods/{id}", -1)
                .then().statusCode(404).body(containsString("not found"));
    }

    @Test
    void patch200() {
        long id = createProduct(uniqueName(), 10.0);
        String newName = uniqueName();
        given().spec(authorized()).body(product(newName, 35.25))
                .when().patch("/goods/{id}", id)
                .then().statusCode(200).body("id", equalTo((int) id))
                .body("name", equalTo(newName)).body("price", equalTo(35.25f));
        given().spec(authorized()).when().get("/goods/{id}", id)
                .then().statusCode(200).body("name", equalTo(newName)).body("price", equalTo(35.25f));
    }

    @ParameterizedTest
    @MethodSource("invalidProducts")
    void patchInvalid400(String body, String field) {
        String name = uniqueName();
        long id = createProduct(name, 10.0);
        given().spec(authorized()).body(body).when().patch("/goods/{id}", id)
                .then().statusCode(400).body(field, not(isEmptyOrNullString()));
        given().spec(authorized()).when().get("/goods/{id}", id)
                .then().statusCode(200).body("name", equalTo(name)).body("price", equalTo(10.0f));
    }

    @Test
    void patchDuplicate400() {
        long id = createProduct(uniqueName(), 10.0);
        String occupiedName = uniqueName();
        createProduct(occupiedName, 20.0);
        given().spec(authorized()).body(product(occupiedName, 30.0))
                .when().patch("/goods/{id}", id)
                .then().statusCode(400).body(containsString("already exists"));
    }

    @Test
    void patchMissing404() {
        given().spec(authorized()).body(product(uniqueName(), 15.0))
                .when().patch("/goods/{id}", -1)
                .then().statusCode(404).body(containsString("not found"));
    }

    @ParameterizedTest
    @CsvSource({"GET,/goods/1", "POST,/goods/add", "PATCH,/goods/1", "DELETE,/goods/1"})
    void authenticationRequired401(String method, String path) {
        RequestSpecification request = given().spec(anonymous());
        if (method.equals("POST") || method.equals("PATCH")) {
            request.body(product(uniqueName(), 10.0));
        }
        request.when().request(method, path).then().statusCode(401)
                .header("WWW-Authenticate", containsString("Basic"));
    }

    @Test
    void incorrectCredentials401() {
        given().spec(anonymous()).auth().preemptive().basic(username, password + "-incorrect")
                .body(product(uniqueName(), 10.0)).when().post("/goods/add")
                .then().statusCode(401);
    }

    static Stream<Arguments> invalidProducts() {
        return Stream.of(
                Arguments.of("{\"name\":\"\",\"price\":1}", "name"),
                Arguments.of("{\"name\":\"   \",\"price\":1}", "name"),
                Arguments.of("{\"price\":1}", "name"),
                Arguments.of("{\"name\":null,\"price\":1}", "name"),
                Arguments.of("{\"name\":\"" + java.util.UUID.randomUUID() + "\",\"price\":-1}", "price"));
    }
}

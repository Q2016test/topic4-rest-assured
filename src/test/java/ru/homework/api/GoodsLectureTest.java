package ru.homework.api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Tag("lecture")
class GoodsLectureTest extends ApiTestBase {
    @Test
    void emptyListWithGivenWhenThen() {
        given().baseUri(baseUrl)
                .when().get("/goods/list")
                .then().statusCode(200).body("goods", empty());
    }

    @Test
    void emptyListWithRequestSpecification() {
        RequestSpecification specification = anonymous();
        given().spec(specification)
                .when().get("/goods/list")
                .then().statusCode(200).body("goods", empty());
    }

    @Test
    void createdProductIsListedWithRestAssured() {
        String name = uniqueName();
        long id = createProduct(name, 12.5);
        given().spec(anonymous())
                .when().get("/goods/list")
                .then().statusCode(200)
                .body("goods", hasSize(1))
                .body("goods[0].name", equalTo(name))
                .body("goods[0].price", equalTo(12.5f))
                .body("goods[0].id", equalTo((int) id));
    }

    @Test
    void createdProductIsListedWithAssertJ() {
        String name = uniqueName();
        long id = createProduct(name, 23.75);
        Response response = given().spec(anonymous()).when().get("/goods/list");
        assertThat(response.statusCode()).as("GET /goods/list status").isEqualTo(200);
        List<Map<String, Object>> goods = response.jsonPath().getList("goods");
        assertThat(goods).as("GET /goods/list goods").hasSize(1);
        assertThat(goods.get(0)).containsEntry("name", name);
        assertThat(((Number) goods.get(0).get("id")).longValue()).isEqualTo(id);
        assertThat(((Number) goods.get(0).get("price")).doubleValue()).isEqualTo(23.75);
    }
}

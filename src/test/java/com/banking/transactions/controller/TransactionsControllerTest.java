package com.banking.transactions.controller;


import com.banking.transactions.config.TokenGenerator;
import com.banking.transactions.dto.Transaction;
import com.banking.transactions.dto.TransactionPageResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.banner-mode=off",
                "logging.level.org.testcontainers=INFO",
                "logging.level.com.github.dockerjava=INFO",
                "logging.level.org.apache.kafka=INFO"
        }
)
@Slf4j
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionsControllerTest {

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withExposedPorts(9092);

    @LocalServerPort
    private int port;

    private String validJwtToken;

    @Autowired
    private TokenGenerator tokenGenerator;


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.retries", () -> "3");
        registry.add("spring.kafka.producer.acks", () -> "1");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.streams.application-id", () -> "transaction-service-test");
        registry.add("spring.kafka.streams.auto-startup", () -> "true");
        registry.add("spring.kafka.streams.properties.default.key.serde", () -> "org.apache.kafka.common.serialization.Serdes$StringSerde");
        registry.add("spring.kafka.streams.properties.default.value.serde", () -> "org.springframework.kafka.support.serializer.JsonSerde");
        registry.add("spring.kafka.streams.properties.commit.interval.ms", () -> "1000");
        registry.add("spring.kafka.streams.properties.cache.max.bytes.buffering", () -> "0");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Wait for Kafka to be ready
        assertTrue(kafka.isRunning(), "Kafka container should be running");

        // Setup Kafka consumer for testing with a small delay to ensure Kafka is ready
        try {
            Thread.sleep(2000); // Give Kafka time to fully initialize
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group", true);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        consumerProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "10000");


        // Mock JWT token for testing
        validJwtToken = "Bearer " + tokenGenerator.generateToken();
    }


    @Test
    @Order(1)
    void givenValidTransaction_whenPostTransaction_thenTransactionStoredSuccessfully() {
        // Given
        Transaction transaction = createValidTransaction();

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction)
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(200)
                .body(equalTo("Transaction stored successfully"));

        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "EUR")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // assert
                    boolean found = res.getTransactions().stream().anyMatch(it -> it.getId().equals(transaction.getId()));
                    assertTrue(found);

                });

    }

    @Test
    @Order(2)
    void givenValidTransaction_whenPostTransaction_thenKafkaMessageSent() {
        // Given
        Transaction transaction = createValidTransaction();

        // When
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction)
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "EUR")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // Then - Verify Kafka message was sent
                    assertEquals(2, res.getPageInfo().getTotalElements());
                });

    }

    @Test
    @Order(3)
    void givenValidTransactionWithKafkaProcessing_whenPostTransaction_thenKafkaMessageContainsCorrectData() {
        // Given
        Transaction transaction = createValidTransaction();

        // When
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction)
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "EUR")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // assert
                    boolean found = res.getTransactions().stream().anyMatch(it -> it.getId().equals(transaction.getId()));
                    assertTrue(found);

                });
    }

    @Test
    @Order(4)
    void givenStoredTransactions_whenGetTransactionsWithAuth_thenReturnsPaginatedResponse() {
        // Given - Store a transaction first
        Transaction transaction = createValidTransaction();
        given().contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction)
                .when().post("/api/v1/transactions")
                .then().statusCode(200);

        // When & Then
        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "EUR")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // assert
                    boolean found = res.getTransactions().stream().anyMatch(it -> it.getId().equals(transaction.getId()));
                    assertTrue(found);
                    assertNotNull(res.getPageInfo());
                    assertNotNull(res.getSummary());

                });
    }

    @Test
    @Order(5)
    void givenInvalidTransaction_whenPostTransaction_thenValidationErrorAndNoKafkaMessage() {
        // Given
        Transaction invalidTransaction = new Transaction();


        // When & Then
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(invalidTransaction)
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(400);

        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "EUR")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // assert
                    boolean found = res.getTransactions().stream().anyMatch(it -> it.getId().equals(invalidTransaction.getId()));
                    assertFalse(found);
                    assertNotNull(res.getPageInfo());
                    assertNotNull(res.getSummary());

                });
    }

    @Test
    @Order(6)
    void givenValidRequestWithDifferentBaseCurrency_whenGetTransactions_thenProcessesRequest() {
        // Given - Store a transaction first
        Transaction transaction = createValidTransaction();
        given().contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction)
                .when().post("/api/v1/transactions");

        // When & Then
        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "USD")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // assert
                    boolean found = res.getTransactions().stream().anyMatch(it -> it.getId().equals(transaction.getId()));
                    assertTrue(found);
                    assertNotNull(res.getPageInfo());
                    assertNotNull(res.getSummary());

                });
    }

    @Test
    @Order(7)
    void givenMultipleTransactions_whenPostSequentially_thenAllProcessedAndKafkaMessagesReceived() {
        // Given
        Transaction transaction1 = createValidTransaction();
        Transaction transaction2 = createValidTransaction();

        // When - Post transactions sequentially
        given().contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction1)
                .when().post("/api/v1/transactions")
                .then().statusCode(200);

        given().contentType(ContentType.JSON)
                .header("Authorization", validJwtToken)
                .body(transaction2)
                .when().post("/api/v1/transactions")
                .then().statusCode(200);

        await().pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var res = given()
                            .header("Authorization", validJwtToken)
                            .queryParam("year", 2025)
                            .queryParam("month", 8)
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .queryParam("baseCurrency", "USD")
                            .when()
                            .get("/api/v1/transactions")
                            .then()
                            .statusCode(equalTo(200))
                            .extract()
                            .body().as(TransactionPageResponse.class);

                    // assert
                    boolean foundTransaction1 = res.getTransactions().stream().anyMatch(it -> it.getId().equals(transaction1.getId()));
                    boolean foundTransaction2 = res.getTransactions().stream().anyMatch(it -> it.getId().equals(transaction2.getId()));
                    assertTrue(foundTransaction1);
                    assertTrue(foundTransaction2);
                    assertNotNull(res.getPageInfo());
                    assertNotNull(res.getSummary());

                });
    }

    private Transaction createValidTransaction() {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setAccountIban("C92-0000-0000-0000-0000-0000-0000");
        transaction.setValueDate(LocalDate.of(2025, 8, 15));
        transaction.setCustomerId("P-0123456789");
        transaction.setAmount(new BigDecimal("150.75"));
        transaction.setCurrency("IDR");
        transaction.setType(Transaction.TransactionType.CREDIT);
        transaction.setDescription("Test transaction");
        return transaction;
    }
}
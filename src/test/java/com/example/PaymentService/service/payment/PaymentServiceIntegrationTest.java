package com.example.PaymentService.service.payment;

import com.example.PaymentService.dto.payment.PaymentResponse;
import com.example.PaymentService.entity.Payment;
import com.example.PaymentService.enums.Status;
import com.example.PaymentService.kafka.entity.Outbox;
import com.example.PaymentService.kafka.repository.OutboxRepository;
import com.example.PaymentService.repository.PaymentRepository;
import com.example.PaymentService.service.util.TestJwtGenerator;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtGenerator jwtServiceTest;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    private static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static WireMockContainer wiremock =
            new WireMockContainer("wiremock/wiremock:3.13.1")
                    .withMapping("randomNumber", """
                            {
                              "request": {
                                "method": "GET",
                                "url": "/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new"
                              },
                              "response": {
                                "status": 200,
                                "body": "10",
                                "headers": {
                                  "Content-Type": "text/plain"
                                }
                              }
                            }
                            """);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.2"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("external.payment-service.url", wiremock::getBaseUrl);
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String randomBase64Secret = Base64.getEncoder().encodeToString(randomBytes);
        registry.add("JWT_SECRET", () -> randomBase64Secret);
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    private PaymentResponse createPayment(Long userId) throws Exception {
        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post("/payments")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"orderId": 5, "userId": %d, "paymentAmount": 10}
                                        """.formatted(userId))
                )
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PaymentResponse.class
        );
    }

    @Test
    void createPayment_shouldSavePaymentWithSuccessStatus_whenRandomNumberIsEven() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/payments")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"orderId": 5, "userId": 1, "paymentAmount": 10}
                                    """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.orderId").value(5))
                .andExpect(jsonPath("$.paymentAmount").value(10))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        List<Payment> savedPayments = paymentRepository.findAll();
        assertEquals(1, savedPayments.size());

        Payment savedPayment = savedPayments.get(0);
        assertEquals(Status.SUCCESS, savedPayment.getStatus());
        assertEquals(5L, savedPayment.getOrderId());

        List<Outbox> outboxEvents = outboxRepository.findAll();
        assertEquals(1, outboxEvents.size());

        Outbox savedEvent = outboxEvents.get(0);
        assertEquals("CREATE_PAYMENT", savedEvent.getEventType());
        assertEquals("NEW", savedEvent.getStatus().name());

        Map<String, Object> consumerProps = new java.util.HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(List.of("CREATE_PAYMENT"));

            await()
                    .atMost(30, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                        assertFalse(records.isEmpty());

                        ConsumerRecord<String, String> record = records.iterator().next();
                        String payload = record.value();

                        assertNotNull(payload);
                        assertTrue(payload.contains("\"orderId\":5"));
                        assertTrue(payload.contains("\"userId\":1"));
                    });
        }
    }

    @Test
    void search_shouldUseProvidedUserId_whenUserIsAdmin() throws Exception {
        Long adminId = 1L;
        Long userId = 2L;

        PaymentResponse response = createPayment(userId);
        PaymentResponse response2 = createPayment(userId);

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/payments/search")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", adminId, "ADMIN"))
                                .param("userId", String.valueOf(userId))
                                .param("orderId", "5")
                                .param("status", "SUCCESS")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString());

        Set<String> ids = new HashSet<>();
        content.forEach(node -> ids.add(node.get("id").asText()));

        assertTrue(ids.contains(response.getId()));
        assertTrue(ids.contains(response2.getId()));
        assertEquals(2, ids.size());
    }

    @Test
    void search_shouldOverwriteUserIdWithAuthId_whenUserIsNotAdmin() throws Exception {
        Long maliciousUserId = 2L;
        Long targetUserId = 1L;

        PaymentResponse response = createPayment(maliciousUserId);
        PaymentResponse response2 = createPayment(maliciousUserId);

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/payments/search")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", maliciousUserId, "USER"))
                                .param("userId", String.valueOf(targetUserId))
                                .param("orderId", "5")
                                .param("status", "SUCCESS")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString());

        Set<String> ids = new HashSet<>();
        content.forEach(node -> ids.add(node.get("id").asText()));

        assertTrue(ids.contains(response.getId()));
        assertTrue(ids.contains(response2.getId()));
        assertEquals(2, ids.size());
    }

    @Test
    void search_shouldThrowIllegalStatusException_whenIncorrectStatusInput() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/payments/search")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "USER"))
                                .param("userId", "1")
                                .param("orderId", "5")
                                .param("status", "INCORRECT")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTotalPaymentsByUserId_shouldReturnTotal_whenUserIsAdmin() throws Exception {
        Long adminId = 1L;
        Long userId = 2L;

        String to = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        String from = Instant.now().minus(1, ChronoUnit.DAYS).toString();

        createPayment(userId);
        createPayment(userId);

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/payments/total")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", adminId, "ADMIN"))
                                .param("id", String.valueOf(userId))
                                .param("from", String.valueOf(from))
                                .param("to", String.valueOf(to))
                                .param("status", "SUCCESS")
                )
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal total = content.asDecimal();

        assertEquals(new BigDecimal(20), total);
    }

    @Test
    void getTotalPaymentsByUserId_shouldReturnTotalForOverwriteUser_whenUserIsUser() throws Exception {
        Long maliciousUserId = 2L;
        Long targetUserId = 1L;

        String to = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        String from = Instant.now().minus(1, ChronoUnit.DAYS).toString();

        createPayment(maliciousUserId);
        createPayment(targetUserId);
        createPayment(targetUserId);

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/payments/total")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", maliciousUserId, "USER"))
                                .param("id", String.valueOf(targetUserId))
                                .param("from", String.valueOf(from))
                                .param("to", String.valueOf(to))
                                .param("status", "SUCCESS")
                )
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal total = content.asDecimal();

        assertEquals(new BigDecimal(10), total);
    }

    @Test
    void getTotalPayments_shouldReturnTotalForAllPayments_whenUserIsAdmin() throws Exception {
        Long adminId = 1L;
        Long userId = 2L;

        createPayment(adminId);
        createPayment(userId);
        createPayment(userId);

        String to = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        String from = Instant.now().minus(1, ChronoUnit.DAYS).toString();

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/payments/total/all")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", adminId, "ADMIN"))
                                .param("from", String.valueOf(from))
                                .param("to", String.valueOf(to))
                )
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal total = content.asDecimal();

        assertEquals(new BigDecimal(30), total);
    }
}

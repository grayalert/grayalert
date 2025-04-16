package com.github.grayalert.integration;

import com.github.grayalert.persistence.DBManager;
import com.github.grayalert.persistence.LogExample;
import com.github.grayalert.core.Poller;
import com.github.grayalert.input.CSVLogParser;
import com.github.grayalert.dto.LogEntry;
import com.github.grayalert.http.GraylogClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraylogIntegrationTest {

    private static WireMockServer wireMockServer;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @LocalServerPort
    private int port;

    @Autowired
    private Poller poller;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DBManager dbManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        registry.add("graylog.instances.main.baseurl", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void setUp() throws IOException {
        String csvContent = new String(new ClassPathResource("graylog.csv").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        wireMockServer.stubFor(get(urlPathMatching("/api/search/universal/relative/export.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/csv")
                        .withBody(csvContent)));
        poller.triggerFetch();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        dbManager.clear();
    }

    @Test
    void testDeleteOldLogs() throws Exception {
        // First, trigger a fetch to populate the database
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/api/logs", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify we have 3 records initially
        List<LogExample> initialRecords = dbManager.load();
        assertEquals(3, initialRecords.size(), "Should have 3 records initially");

        // Call delete-old endpoint with 300 minutes (18000 seconds) threshold
        long referenceTime = Instant.parse("2024-03-20T15:02:00.000Z").toEpochMilli();
        String deleteUrl = String.format("http://localhost:" + port + "/api/logs/delete-old?ageInSeconds=18000&referenceTime=%d", referenceTime);
        response = restTemplate.postForEntity(deleteUrl, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = restTemplate.getForEntity("http://localhost:" + port + "/api/logs", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Parse the JSON response into a List of LogExamples
        List<LogExample> remainingRecords = objectMapper.readValue(response.getBody(), new TypeReference<List<LogExample>>() {});
        assertEquals(2, remainingRecords.size(), "Should have  records after deletion: " + remainingRecords);

        // Double check the database state
        List<LogExample> dbRecords = dbManager.load();
        assertEquals(2, dbRecords.size(), "Should have 1 record in database after deletion");
    }

    @Test
    void testHtmlResponse() throws Exception {
        // First run the integration test to populate the database
        testGraylogIntegration();

        // Then fetch the main page
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String html = response.getBody();
        assertTrue(html.contains("<span id=\"rowCount\">3"), "HTML contains wrong content: " + html);
    }

    @Test
    void testGraylogIntegration() throws Exception {
        // Trigger a fetch
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/api/logs", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify that we have 3 log examples in the database
        List<LogExample> logExamples = dbManager.load();
        assertEquals(3, logExamples.size());
    }
} 
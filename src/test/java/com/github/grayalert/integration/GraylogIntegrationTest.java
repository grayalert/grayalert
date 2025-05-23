package com.github.grayalert.integration;

import com.github.grayalert.output.LoggingAlarmNotifier;
import com.github.grayalert.persistence.DBManager;
import com.github.grayalert.persistence.LogExample;
import com.github.grayalert.core.Poller;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.net.URLDecoder;
import java.time.Clock;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import wiremock.org.eclipse.jetty.util.component.AbstractLifeCycle;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GraylogIntegrationTest {

    private static WireMockServer wireMockServer;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @LocalServerPort
    private int port;

    @Autowired
    private Poller poller;

    @Autowired
    Clock clock;

    @Autowired
    private TestRestTemplate restTemplate;

    @SpyBean
    private DBManager dbManager;

    @SpyBean
    LoggingAlarmNotifier alarmNotifier;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        registry.add("graylog.instances.main.baseurl", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void setUp() throws IOException {
        String csvContent1 = new String(new ClassPathResource("graylog1.csv").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String csvContent2 = new String(new ClassPathResource("graylog2.csv").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        wireMockServer.stubFor(get(urlPathMatching("/api/search/universal/relative/export.*"))
            .inScenario("Graylog CSVs")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/csv")
                .withBody(csvContent1))
            .willSetStateTo("SECOND_CALL"));

        wireMockServer.stubFor(get(urlPathMatching("/api/search/universal/relative/export.*"))
            .inScenario("Graylog CSVs")
            .whenScenarioStateIs("SECOND_CALL")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/csv")
                .withBody(csvContent2)));

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
        List<LogExample> initialRecords = dbManager.load(null);
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
        List<LogExample> dbRecords = dbManager.load(null);
        assertEquals(2, dbRecords.size(), "Should have 1 record in database after deletion");
    }

    @Test
    void testHtmlResponseAndAlarmMessagesContainURL() throws Exception {
        // First run the integration test to populate the database
        testGraylogIntegration();

        ArgumentCaptor<List> rowsSaveCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(dbManager, Mockito.atLeastOnce()).save(rowsSaveCaptor.capture());
        List<LogExample> htmlLogExamples = fetchRows(null);
        assertEquals(3, htmlLogExamples.size());
        assertEquals(3, rowsSaveCaptor.getValue().size());
        for (LogExample logExample : htmlLogExamples) {
            String url = logExample.getUrl();
            assertTrue(url.contains("rangetype=absolute"), "linkHtml should contain 'rangetype=absolute'");
        }
        List<String> decodedUrls = htmlLogExamples.stream().map(x -> URLDecoder.decode(x.getUrl()))
            .toList();
        assertEquals(1, decodedUrls.stream().filter(p-> p.contains("_id:3")).count());
        assertEquals(1, decodedUrls.stream().filter(p-> p.contains("trace_id:123")).count());
        assertEquals(1, decodedUrls.stream().filter(p-> p.contains("trace_id:456")).count());
        poller.triggerFetch();
        List<LogExample> updatedHtmlLogExamples = fetchRows(null);
        assertEquals(3, updatedHtmlLogExamples.size());
        for (LogExample logExample : updatedHtmlLogExamples) {
            assertNotNull(logExample.getLastSeen());
            assertNotSame("",logExample.getLastSeen());
        }

        ArgumentCaptor<String> alarmMessageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alarmNotifier).notifyMessage(alarmMessageCaptor.capture());
        String alarmMessage = alarmMessageCaptor.getValue();
        assertTrue(alarmMessage.contains("rangetype=absolute"), "Alarm message should contain 'rangetype=absolute': " + alarmMessage);

        OptionalLong maxTimestamp = updatedHtmlLogExamples.stream()
            .filter(p -> p.getLastTimestamp() != null).mapToLong(LogExample::getLastTimestamp).max();
        Long minTimestamp = poller.calculateMinTimestamp(updatedHtmlLogExamples);

        long maxAge = clock.millis() - maxTimestamp.getAsLong() + 60_000;
        System.out.println("Last timestamp: " + maxTimestamp);
        List<LogExample> lastRecord = fetchRows(maxAge);

        assertEquals(1, lastRecord.size());
    }

    private List<LogExample> fetchRows(Long maxAge) {
        // Then fetch the main page
        String u = "http://localhost:" + port + "/";
        if (maxAge != null) {
            u += "?maxAge=" + maxAge;
        }
        ResponseEntity<String> response = restTemplate.getForEntity(u, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String html = response.getBody();
        List<LogExample> htmlLogExamples = new java.util.ArrayList<>();
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
        org.jsoup.select.Elements rows = doc.select("#recordsTable tbody tr");
        for (org.jsoup.nodes.Element row : rows) {
            LogExample logExample = new LogExample();
            logExample.setAppName(row.selectFirst("td.appName").text());
            logExample.setId(row.attr("id"));
            logExample.setLoggerName(row.selectFirst("td.loggerName").text());
            logExample.setShortMessage(row.selectFirst("td.shortMessage").text());
            logExample.setLinkHtml(row.selectFirst("td.linkHtml a").text());
            org.jsoup.nodes.Element linkElement = row.selectFirst("td.linkHtml a");
            String url = linkElement != null ? linkElement.attr("href") : null;
            logExample.setUrl(url);
            logExample.setCount(Integer.parseInt(row.selectFirst("td.count").text()));
            String firstSeen = row.selectFirst("td.firstSeen").text();
            logExample.setFirstTimestamp(parseTime(firstSeen));
            String lastSeen = row.selectFirst("td.lastSeen").text();
            logExample.setLastTimestamp(parseTime(lastSeen));
            logExample.setFirstTraceId(row.selectFirst("td.firstTraceId").text());
            htmlLogExamples.add(logExample);
        }
        return htmlLogExamples;
    }

    private static Long parseTime(String firstSeen) {
        if (firstSeen == null || firstSeen.isEmpty()) {
            return null;
        }
        return Instant.parse(firstSeen.replace(" ", "T") + "Z").toEpochMilli();
    }

    @Test
    void testGraylogIntegration() throws Exception {
        // Trigger a fetch
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/api/logs", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify that we have 3 log examples in the database
        List<LogExample> logExamples = dbManager.load(null);
        assertEquals(3, logExamples.size());
    }
}

package com.ims.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@ConditionalOnProperty(name = "app.notifications.sms.enabled", havingValue = "true")
@Slf4j
public class TwilioSmsService implements SmsService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final RestTemplate restTemplate;

    public TwilioSmsService(
            @Value("${app.notifications.sms.twilio.account-sid}") String accountSid,
            @Value("${app.notifications.sms.twilio.auth-token}") String authToken,
            @Value("${app.notifications.sms.twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Async
    public void sendSms(String to, String message) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = Base64.getEncoder().encodeToString(
                    (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + auth);

            String body = "To=" + to + "&From=" + fromNumber + "&Body=" + message;
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("SMS sent to {} — Status: {}", to, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
        }
    }
}

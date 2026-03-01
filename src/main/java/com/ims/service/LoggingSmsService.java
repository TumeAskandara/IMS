package com.ims.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(TwilioSmsService.class)
@Slf4j
public class LoggingSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        log.info("[SMS-DEV] Would send SMS to {}: {}", to, message);
    }
}

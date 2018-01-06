package com.baswara.uaa.service;

import com.baswara.uaa.domain.User;
import io.github.jhipster.config.JHipsterProperties;
import org.apache.commons.lang3.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service for sending sms.
 * <p>
 * We use the @Async annotation to send sms asynchronously.
 */
@Service
public class SmsService {

    private final Logger log = LoggerFactory.getLogger(SmsService.class);

    private static final String USER = "user";

    private static final String PARAM = "param";

    private final SpringTemplateEngine templateEngine;

    @Autowired
    private RestTemplate restTemplate;

    public SmsService(SpringTemplateEngine templateEngine) {

        this.templateEngine = templateEngine;
    }

    @Async
    public void sendSms(String to, String content) {
        log.debug("Send sms to '{}' with content={}", to, content);
        try {
            final String captchaUri = "https://reguler.zenziva.net/apps/smsapi.php?userkey=89exry&passkey=loot1234&nohp=" + to + "&pesan=" + content;
            String result = restTemplate.getForObject(captchaUri, String.class);
            log.debug("result {}", result);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Sms could not be sent to '{}'", to, e);
            } else {
                log.warn("Sms could not be sent to '{}': {}", to, e.getMessage());
            }
        }
    }

    @Async
    public void sendSmsFromTemplate(User user, String to, String templateName, Map<String, String> param) {
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(PARAM, param);
        String content = templateEngine.process(templateName, context);
        sendSms(to, content);
    }

    @Async
    public void sendActivationSms(User user) {
        log.debug("Sending activation email to '{}'", user.getMobile());
        sendSmsFromTemplate(user, user.getMobile(), "activationSms", new HashMap<>());
    }

    @Async
    public void sendPasswordResetSms(User user) {
        log.debug("Sending password reset sms to '{}'", user.getMobile());
        sendSmsFromTemplate(user, user.getMobile(), "passwordResetSms", new HashMap<>());
    }

}

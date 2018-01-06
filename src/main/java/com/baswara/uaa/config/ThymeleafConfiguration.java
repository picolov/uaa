package com.baswara.uaa.config;

import org.apache.commons.lang3.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.StandardTemplateModeHandlers;

@Configuration
public class ThymeleafConfiguration {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(ThymeleafConfiguration.class);

    @Bean
    @Description("Thymeleaf template resolver serving HTML 5 emails")
    public ClassLoaderTemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver emailTemplateResolver = new ClassLoaderTemplateResolver();
        emailTemplateResolver.setPrefix("mails/");
        emailTemplateResolver.setSuffix(".html");
        emailTemplateResolver.setTemplateMode("HTML5");
        emailTemplateResolver.setCharacterEncoding(CharEncoding.UTF_8);
        emailTemplateResolver.setOrder(1);
        return emailTemplateResolver;
    }

    @Bean
    @Description("Thymeleaf template resolver serving HTML 5 sms")
    public ClassLoaderTemplateResolver smsTemplateResolver() {
        ClassLoaderTemplateResolver smsTemplateResolver = new ClassLoaderTemplateResolver();
        smsTemplateResolver.setPrefix("sms/");
        smsTemplateResolver.setSuffix(".html");
        smsTemplateResolver.setTemplateMode("HTML5");
        smsTemplateResolver.setCharacterEncoding(CharEncoding.UTF_8);
        smsTemplateResolver.setOrder(2);
        return smsTemplateResolver;
    }
}

package com.tribalbattle.tribal_battle_api;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

@Configuration(proxyBeanMethods = false)
public class TestMailConfiguration {

    @Bean
    JavaMailSender javaMailSender() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);

        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(invocation ->
                        new MimeMessage(Session.getInstance(new Properties()))
                );

        return mailSender;
    }
}

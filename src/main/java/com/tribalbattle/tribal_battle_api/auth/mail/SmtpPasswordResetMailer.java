package com.tribalbattle.tribal_battle_api.auth.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@localhost}")
    private String from;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void sendPasswordReset(
            String email,
            String displayName,
            String rawToken
    ) {
        String resetUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .queryParam("resetToken", rawToken)
                .fragment("account")
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Reset your Tribal Battle password");
        message.setText(
                "Hello " + displayName + ",\n\n" +
                "Use the link below to reset your Tribal Battle password:\n\n" +
                resetUrl + "\n\n" +
                "If you did not request this change, you can ignore this email."
        );

        mailSender.send(message);
    }
}

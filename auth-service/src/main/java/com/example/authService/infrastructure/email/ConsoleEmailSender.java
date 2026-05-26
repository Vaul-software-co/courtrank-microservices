package com.example.authService.infrastructure.email;

import com.example.authService.application.ports.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendEmailVerification(String to, String link, String lang) {
        logger.info("email verification to={} lang={} link={}", to, lang, link);
    }

    @Override
    public void sendPasswordOtp(String to, String otp, String lang) {
        logger.info("password otp to={} lang={} otp={}", to, lang, otp);
    }
}

package com.courtrank.authService.application.ports.email;

public interface EmailSender {
    void sendEmailVerification(String to, String link, String lang);
    void sendPasswordOtp(String to, String otp, String lang);
}

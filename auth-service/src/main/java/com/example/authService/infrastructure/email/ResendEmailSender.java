package com.example.authService.infrastructure.email;

import com.example.authService.application.ports.email.EmailSender;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(ResendEmailSender.class);

    private final Resend resend;
    private final String from;

    public ResendEmailSender(
            Resend resend,
            String from
    ) {
        this.resend = resend;
        this.from = from;
    }

    @Override
    public void sendEmailVerification(String to, String link, String lang) {
        EmailTemplate template = CourtRankEmailTemplates.verificationEmail(link, lang);
        this.send(to, template);
    }

    @Override
    public void sendPasswordOtp(String to, String otp, String lang) {
        EmailTemplate template = CourtRankEmailTemplates.passwordReset(otp, lang);
        this.send(to, template);
    }

    private void send(String to, EmailTemplate template) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(this.from)
                .to(to)
                .subject(template.subject())
                .html(template.html())
                .build();

        try {
            CreateEmailResponse response = this.resend.emails().send(params);
            logger.info("email sent to={} resendId={}", to, response.getId());
        } catch (ResendException exception) {
            throw new EmailDeliveryException("Failed to send email with Resend", exception);
        }
    }
}

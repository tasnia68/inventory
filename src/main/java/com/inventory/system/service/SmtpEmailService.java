package com.inventory.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SmtpEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@inventory-system.com}")
    private String fromEmail;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendInvitationEmail(String to, String invitationLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("You've been invited to the Inventory System");
            message.setText(
                    "Hello,\n\n" +
                            "You have been invited to join the Inventory Management System.\n\n" +
                            "Please click the link below to complete your registration:\n\n" +
                            invitationLink + "\n\n" +
                            "This invitation link will expire in 7 days.\n\n" +
                            "If you did not expect this invitation, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "The Inventory System Team");

            mailSender.send(message);
            logger.info("Invitation email sent successfully to: {}", to);

        } catch (Exception e) {
            logger.error("Failed to send invitation email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }
}

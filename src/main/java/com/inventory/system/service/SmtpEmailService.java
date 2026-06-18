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

    @Override
    public void sendStorefrontLoginEmail(String to, String otpCode, String magicLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your storefront sign-in code");
            message.setText(
                    "Hello,\n\n" +
                            "Use this one-time code to sign in to your storefront account:\n\n" +
                            otpCode + "\n\n" +
                            "Or click the sign-in link below:\n\n" +
                            magicLink + "\n\n" +
                            "This code and link expire in 15 minutes.\n\n" +
                            "If you did not request this email, you can ignore it.\n\n" +
                            "Best regards,\n" +
                            "The Storefront Team");

            mailSender.send(message);
            logger.info("Storefront login email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send storefront login email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send storefront login email", e);
        }
    }

    @Override
    public void sendOrderConfirmationEmail(String to, String customerName, String orderNumber, String orderTotal, String currency) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Order Confirmation — " + orderNumber);
            message.setText(
                    "Hello " + (customerName != null ? customerName : "") + ",\n\n" +
                            "Thank you for your order!\n\n" +
                            "Order Number: " + orderNumber + "\n" +
                            "Total: " + currency + " " + orderTotal + "\n\n" +
                            "Your order has been received and is pending review. We will notify you once it is confirmed.\n\n" +
                            "If you have any questions about your order, please contact our support team and reference your order number.\n\n" +
                            "Best regards,\n" +
                            "The Storefront Team");

            mailSender.send(message);
            logger.info("Order confirmation email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendTrackingEmail(String to, String customerName, String orderNumber, String trackingUrl, String courier) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your order " + orderNumber + " has shipped — track it");
            message.setText(
                    "Hello " + (customerName != null ? customerName : "") + ",\n\n" +
                            "Good news — your order " + orderNumber + " is on its way" +
                            (courier != null && !courier.isBlank() ? " with " + courier : "") + ".\n\n" +
                            "Track your delivery here:\n\n" +
                            trackingUrl + "\n\n" +
                            "Thank you for shopping with us.\n\n" +
                            "Best regards,\n" +
                            "The Storefront Team");

            mailSender.send(message);
            logger.info("Tracking email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send tracking email to {}: {}", to, e.getMessage());
        }
    }
}

package com.inventory.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendInvitationEmail(String to, String invitationLink) {
        logger.info("Sending invitation email to: {}", to);
        logger.info("Invitation Link: {}", invitationLink);
        logger.info("--------------------------------------------------");
    }

    @Override
    public void sendStorefrontLoginEmail(String to, String otpCode, String magicLink) {
        logger.info("Sending storefront login email to: {}", to);
        logger.info("Storefront OTP: {}", otpCode);
        logger.info("Storefront Magic Link: {}", magicLink);
        logger.info("--------------------------------------------------");
    }

    @Override
    public void sendOrderConfirmationEmail(String to, String customerName, String orderNumber, String orderTotal, String currency) {
        logger.info("Sending order confirmation email to: {}", to);
        logger.info("Customer: {}, Order: {}, Total: {} {}", customerName, orderNumber, currency, orderTotal);
        logger.info("--------------------------------------------------");
    }

    @Override
    public void sendTrackingEmail(String to, String customerName, String orderNumber, String trackingUrl, String courier) {
        logger.info("Sending tracking email to: {}", to);
        logger.info("Customer: {}, Order: {}, Courier: {}, Tracking: {}", customerName, orderNumber, courier, trackingUrl);
        logger.info("--------------------------------------------------");
    }
}

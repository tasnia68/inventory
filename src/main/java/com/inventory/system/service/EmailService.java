package com.inventory.system.service;

public interface EmailService {
    void sendInvitationEmail(String to, String invitationLink);
    void sendStorefrontLoginEmail(String to, String otpCode, String magicLink);
    void sendOrderConfirmationEmail(String to, String customerName, String orderNumber, String orderTotal, String currency);
    void sendTrackingEmail(String to, String customerName, String orderNumber, String trackingUrl, String courier);
}

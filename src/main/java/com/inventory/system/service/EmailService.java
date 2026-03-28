package com.inventory.system.service;

public interface EmailService {
    void sendInvitationEmail(String to, String invitationLink);
    void sendStorefrontLoginEmail(String to, String otpCode, String magicLink);
}

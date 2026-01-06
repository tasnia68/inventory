package com.inventory.system.service;

public interface EmailService {
    void sendInvitationEmail(String to, String invitationLink);
}

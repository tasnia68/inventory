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
}

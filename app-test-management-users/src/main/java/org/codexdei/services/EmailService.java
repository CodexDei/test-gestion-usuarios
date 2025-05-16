package org.codexdei.services;

public interface EmailService {

    void sendWelcomeEmail(String email, String name);
    void sendActivationEmail(String email, String name);
    void sendAccountDeletedEmail(String email, String name);
}

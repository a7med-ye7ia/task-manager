package com.taskmanagesys.email.service;

public interface MailService {
    void send(Message message);

    default boolean isRemoteDelivery() {
        return true;
    }

    record Message(String to, String subject, String html, String text) {}
}

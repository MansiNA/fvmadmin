package com.example.application.service;

import org.springframework.stereotype.Service;

@Service
public class MessageStatus {
    private Boolean Status = false;

    private String Messages;

    public Boolean getStatus() {
        return Status;
    }

    public void setStatus(Boolean status) {
        Status = status;
    }

    public String getMessages() {
        return Messages;
    }

    public void setMessages(String Messages) {
        this.Messages = Messages;
    }
}

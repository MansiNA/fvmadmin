package com.example.application.data.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MailboxShutdown {

    private String mailboxId;
    private String shutdownReason;

}

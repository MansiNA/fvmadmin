package com.example.application.data.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "fvmadmin_mailbox_shutdown")
public class MailboxShutdown {

    @Id
    @Column(name = "mailbox_id")
    private String mailboxId;

    @Column(name = "shutdown_reason")
    private String shutdownReason;

}

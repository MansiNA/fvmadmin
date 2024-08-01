package com.example.application.data.repository;

import com.example.application.data.entity.MailboxShutdown;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailboxShutdownRepository extends JpaRepository<MailboxShutdown, Long> {
}

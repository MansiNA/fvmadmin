package com.example.application.data.service;

import com.example.application.data.entity.MailboxShutdown;
import com.example.application.data.entity.Protokoll;
import com.example.application.data.repository.MailboxShutdownRepository;
import com.example.application.data.repository.ProtokollRepository;
import com.example.application.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ProtokollService {

    private final ProtokollRepository protokollRepository;
    private final MailboxShutdownRepository mailboxShutdownRepository;

    @Autowired
    public ProtokollService(ProtokollRepository protokollRepository, MailboxShutdownRepository mailboxShutdownRepository) {
        this.protokollRepository = protokollRepository;
        this.mailboxShutdownRepository = mailboxShutdownRepository;
    }

    public void logAction(String action, String reason) {
        Protokoll logEntry = new Protokoll();
        logEntry.setUsername(MainLayout.userName);
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        logEntry.setZeitpunkt(date);
        logEntry.setInfo(action);
        logEntry.setShutdownReason(reason);
        protokollRepository.save(logEntry);
    }

    public void saveMailboxShutdownState(String mailboxId, String shutdownReason) {
        MailboxShutdown mailboxShutdown = new MailboxShutdown();
        mailboxShutdown.setMailboxId(mailboxId);
        mailboxShutdown.setShutdownReason(shutdownReason);
        mailboxShutdownRepository.save(mailboxShutdown);
    }

    public List<MailboxShutdown> findAllMailboxShutdowns() {
        return mailboxShutdownRepository.findAll();
    }

    public void deleteShutdownTable() {
        mailboxShutdownRepository.deleteAll();
    }

    public Protokoll save(Protokoll protokoll) {
        return protokollRepository.save(protokoll);
    }

    public void deleteById(Long id) {
        protokollRepository.deleteById(id);
    }
}

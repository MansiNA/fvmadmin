package com.example.application.data.service;

import com.example.application.data.entity.MailboxShutdown;
import com.example.application.data.entity.Protokoll;
import com.example.application.data.repository.ProtokollRepository;
import com.example.application.utils.MailboxWatchdogJobExecutor;
import com.example.application.views.MainLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ProtokollService.class);

    @Autowired
    public ProtokollService(ProtokollRepository protokollRepository) {
        this.protokollRepository = protokollRepository;

    }

    public void logAction(String username, String verbindung, String info, String reason) {
        Protokoll logEntry = new Protokoll();
        logEntry.setUsername(username);
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        logEntry.setZeitpunkt(date);
        logEntry.setInfo(info);
        logEntry.setShutdownReason(reason);
        logEntry.setVerbindung(verbindung);
        protokollRepository.save(logEntry);
        logger.info("added in fvm_protokoll Username: {} Verbindung: {} Info: {} Reason: {} ", username, verbindung,info, reason);
    }

//    public void saveMailboxShutdownState(String mailboxId, String shutdownReason) {
//        MailboxShutdown mailboxShutdown = new MailboxShutdown();
//        mailboxShutdown.setMailboxId(mailboxId);
//        mailboxShutdown.setShutdownReason(shutdownReason);
//        mailboxShutdownRepository.save(mailboxShutdown);
//    }
//
//    public List<MailboxShutdown> findAllMailboxShutdowns() {
//        return mailboxShutdownRepository.findAll();
//    }
//
//    public void deleteShutdownTable() {
//        mailboxShutdownRepository.deleteAll();
//    }

    public Protokoll save(Protokoll protokoll) {
        return protokollRepository.save(protokoll);
    }
    public List<Protokoll> findAllLogsOrderedByZeitpunktDesc() {
        return protokollRepository.findAllByOrderByZeitpunktDesc();
    }
    public void deleteById(Long id) {
        protokollRepository.deleteById(id);
    }

    public List<Object[]> findInfoZeitpunktShutdownReasonByVerbindung(String verbindung) {
        return protokollRepository.findInfoZeitpunktShutdownReasonByVerbindung(verbindung);
    }
}

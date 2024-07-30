package com.example.application.data.service;

import com.example.application.data.entity.Protokoll;
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

    @Autowired
    public ProtokollService(ProtokollRepository protokollRepository) {
        this.protokollRepository = protokollRepository;
    }

    public void logAction(String action) {
        Protokoll logEntry = new Protokoll();
        logEntry.setUsername(MainLayout.userName);
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        logEntry.setZeitpunkt(date);
        logEntry.setInfo(action);

        protokollRepository.save(logEntry);
    }

    public Protokoll save(Protokoll protokoll) {
        return protokollRepository.save(protokoll);
    }

    public void deleteById(Long id) {
        protokollRepository.deleteById(id);
    }
}

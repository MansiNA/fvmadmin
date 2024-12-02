package com.example.application.data.repository;

import com.example.application.data.entity.Protokoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtokollRepository extends JpaRepository<Protokoll, Long> {
    List<Protokoll> findAllByOrderByZeitpunktDesc();

    @Query("SELECT p.info, p.zeitpunkt, p.shutdownReason FROM Protokoll p WHERE p.username = 'watchdog' AND p.verbindung = :verbindung ORDER BY p.zeitpunkt DESC")
    List<Object[]> findInfoZeitpunktShutdownReasonByVerbindung(String verbindung);
}

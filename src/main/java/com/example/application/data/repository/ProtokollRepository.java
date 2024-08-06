package com.example.application.data.repository;

import com.example.application.data.entity.Protokoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtokollRepository extends JpaRepository<Protokoll, Long> {
    List<Protokoll> findAllByOrderByZeitpunktDesc();
}

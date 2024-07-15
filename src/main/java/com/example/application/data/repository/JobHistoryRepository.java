package com.example.application.data.repository;

import com.example.application.data.entity.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, Long> {
    // Custom query methods (if needed) can be defined here
}

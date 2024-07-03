package com.example.application.data.repository;

import com.example.application.data.entity.JobManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDefinitionRepository extends JpaRepository<JobManager, Integer> {
    // Custom query methods can be defined here if needed
}

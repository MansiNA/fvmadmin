package com.example.application.data.repository;

import com.example.application.data.entity.JobDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDefinitionRepository extends JpaRepository<JobDefinition, Integer> {
    // Custom query methods can be defined here if needed
}

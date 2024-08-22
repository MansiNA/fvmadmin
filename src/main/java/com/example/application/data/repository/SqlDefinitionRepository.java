package com.example.application.data.repository;

import com.example.application.data.entity.SqlDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SqlDefinitionRepository extends JpaRepository<SqlDefinition, Integer> {
}

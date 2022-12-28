package com.example.application.data.repository;

import com.example.application.data.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfigurationRepository extends JpaRepository<Configuration, UUID> {
}

package com.example.application.data.repository;

import com.example.application.data.entity.ServerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerConfigurationRepository extends JpaRepository<ServerConfiguration, Long> {
    Optional<ServerConfiguration> findByHostNameAndUserName(String hostName, String userName);
}

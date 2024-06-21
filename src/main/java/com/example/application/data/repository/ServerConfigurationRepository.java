package com.example.application.data.repository;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ServerConfiguration;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServerConfigurationRepository extends JpaRepository<ServerConfiguration, Long> {

}

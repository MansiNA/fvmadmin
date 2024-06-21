package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ServerConfiguration;
import com.example.application.data.repository.ConfigurationRepository;
import com.example.application.data.repository.ServerConfigurationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServerConfigurationService {

    private ServerConfigurationRepository configurationRepository;

    public ServerConfigurationService(ServerConfigurationRepository configurationRepository) {

        this.configurationRepository = configurationRepository;
    };

    public List<ServerConfiguration> findAllConfigurations(){
        return configurationRepository.findAll();
    };

    public void saveConfiguration(ServerConfiguration config) {

        if (config == null) {
            System.err.println("Configuration is null!");
            return;
        }
        configurationRepository.save(config);
    }

    // Delete a configuration by ID
    public void deleteConfiguration(ServerConfiguration config) {
        if (config.getId() == null) {
            System.err.println("ID is null!");
            return;
        }
        configurationRepository.deleteById(config.getId());
    }
}
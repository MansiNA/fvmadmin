package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ServerConfiguration;
import com.example.application.data.repository.ConfigurationRepository;
import com.example.application.data.repository.ServerConfigurationRepository;
import com.vaadin.flow.component.notification.Notification;
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

    public void saveConfigurationold(ServerConfiguration config) {

        if (config == null) {
            System.err.println("Configuration is null!");
            return;
        }
        configurationRepository.save(config);
    }
    public String saveConfiguration(ServerConfiguration config) {
        if (config == null) {
            System.err.println("Configuration is null!");
            return "Configuration is null!";
        }

        // Check if a configuration with the same hostname and username already exists
        Optional<ServerConfiguration> existingConfig = configurationRepository.findByHostNameAndUserName(
                config.getHostName(), config.getUserName());

        if (existingConfig.isPresent()) {
            return "Configuration with the same hostname and username already exists!";
        }

        configurationRepository.save(config);
        return "Ok";
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
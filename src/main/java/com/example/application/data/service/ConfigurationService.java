package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ServerConfiguration;
import com.example.application.data.repository.ConfigurationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConfigurationService {

    private ConfigurationRepository configurationRepository;

    public ConfigurationService(ConfigurationRepository configurationRepository) {

        this.configurationRepository = configurationRepository;
    };

    public List<Configuration> findAllConfigurations(){
        return configurationRepository.findAll();
    };

    //   public List<Configuration> findMessageConfigurations(){
    //       return configurationRepository.findByName();
    //   };

    public List<Configuration> findMessageConfigurations(){
        return configurationRepository.findAll();
    };


    public void saveConfigurationOld(Configuration config) {

        if (config == null) {
            System.err.println("Configuration is null!");
            return;
        }
        configurationRepository.save(config);
    }

    @Transactional
    public void saveConfiguration(Configuration config) {
        if (config == null) {
            System.err.println("Configuration is null!");
            return;
        }
        //  String plainTextPassword = config.getPassword();

        if (config.getId() == null) {
            // New configuration, save it and let the database generate the ID
            config.setPassword(Configuration.encodePassword(config.getPassword()));
            configurationRepository.save(config);
            System.out.println("New Configuration saved with ID: " + config.getId());
        } else {
            Optional<Configuration> existingConfigOptional = configurationRepository.findById(config.getId());
            if (existingConfigOptional.isPresent()) {
                Configuration existingConfig = existingConfigOptional.get();
                System.out.println("password existing = " + existingConfig.getPassword());
                boolean passwordChanged = !config.getPassword().equals(existingConfig.getPassword());
                System.out.println("password new updated = " + config.getPassword());
                System.out.println("password changed = " + passwordChanged);

                if (passwordChanged) {
                    System.out.println("password changed!!!!");
                    String encodedPassword = Configuration.encodePassword(config.getPassword());
                    //  String encodedPassword = (config.getPassword());
                    configurationRepository.updateWithPassword(
                            config.getId(),
                            config.getName(),
                            config.getUserName(),
                            encodedPassword,
                            config.getDb_Url()
                    );
                } else {
                    System.out.println("password not changed!!!!");
                    configurationRepository.updateWithoutPassword(
                            config.getId(),
                            config.getName(),
                            config.getUserName(),
                            config.getDb_Url()
                    );
                }
            } else {
                // Handle case where configuration might not exist
                System.err.println("Configuration with name and ID " + config.getName() + "___"+ config.getId() + " not found!");
            }
        }
    }

    public void deleteConfiguration(Configuration config) {
        if (config.getId() == null) {
            System.err.println("ID is null!");
            return;
        }

        configurationRepository.deleteById(config.getId());
    }
}
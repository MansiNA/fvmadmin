package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.repository.ConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigurationService {

    private ConfigurationRepository configurationRepository;

    public ConfigurationService(ConfigurationRepository configurationRepository) {

        this.configurationRepository = configurationRepository;
    };

   public List<Configuration> findAllConfigurations(){
            return configurationRepository.findAll();
        };
    public void saveConfiguration(Configuration config){

    if(config == null){
        System.err.println("Configuration is null!");
        return;
    }
    configurationRepository.save(config);
}

}

package com.example.application.data.service;

import com.example.application.data.entity.Metadaten;
import com.example.application.data.repository.MetadatenRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetadatenService {

    private final MetadatenRepository metadatenRepository;


    public MetadatenService(MetadatenRepository metadatenRepository) {
        this.metadatenRepository = metadatenRepository;
    }

    public List<Metadaten> findAllContacts(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return metadatenRepository.findAll();
        } else {
            return metadatenRepository.search(stringFilter);
        }
    }

}

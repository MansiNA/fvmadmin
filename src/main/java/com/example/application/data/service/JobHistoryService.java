package com.example.application.data.service;

import com.example.application.data.entity.JobHistory;
import com.example.application.data.repository.JobHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobHistoryService {

    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    public List<JobHistory> getAllJobHistories() {
        return jobHistoryRepository.findAll();
    }

    public Optional<JobHistory> getJobHistoryById(Long id) {
        return jobHistoryRepository.findById(id);
    }

    public JobHistory createOrUpdateJobHistory(JobHistory jobHistory) {
        return jobHistoryRepository.save(jobHistory);
    }

    public void deleteJobHistory(Long id) {
        jobHistoryRepository.deleteById(id);
    }
}

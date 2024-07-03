package com.example.application.utils;

import com.example.application.data.entity.JobManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JobDefinitionUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serializeJobDefinition(JobManager jobManager) throws JsonProcessingException {
        return objectMapper.writeValueAsString(jobManager);
    }

    public static JobManager deserializeJobDefinition(String jobDefinitionString) throws JsonProcessingException {
        return objectMapper.readValue(jobDefinitionString, JobManager.class);
    }
}

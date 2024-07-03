package com.example.application.utils;

import com.example.application.data.entity.JobDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JobDefinitionUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serializeJobDefinition(JobDefinition jobDefinition) throws JsonProcessingException {
        return objectMapper.writeValueAsString(jobDefinition);
    }

    public static JobDefinition deserializeJobDefinition(String jobDefinitionString) throws JsonProcessingException {
        return objectMapper.readValue(jobDefinitionString, JobDefinition.class);
    }
}

package com.example.application.utils;

import com.example.application.data.entity.JobManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JobExecutor implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobDefinitionString = context.getMergedJobDataMap().getString("jobManager");
        JobManager jobManager;
        try {
            jobManager = JobDefinitionUtils.deserializeJobDefinition(jobDefinitionString);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }

        // Add your job execution logic here
        System.out.println("Executing job: " + jobManager.getName());

        try {
            switch (jobManager.getTyp()) {
                case "SQL":
                    executeSQLJob(jobManager);
                    break;
                case "Command":
                    executeCommandJob(jobManager);
                    break;
                default:
                    throw new JobExecutionException("Unsupported job type: " + jobManager.getTyp());
            }
        } catch (Exception e) {
            throw new JobExecutionException("Error executing job: " + jobManager.getName(), e);
        }
    }

    private void executeSQLJob(JobManager jobManager) throws Exception {
        // Dummy database connection
        String jdbcUrl = "jdbc:your_database_url";
        String username = "your_db_username";
        String password = "your_db_password";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(jobManager.getCommand())) {
            while (rs.next()) {
                // Process the result set
                System.out.println(rs.getString(1));
            }
        }
    }

    private void executeCommandJob(JobManager jobManager) throws Exception {
        String command = jobManager.getCommand();
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Command execution failed with exit code " + exitCode);
        }
    }
}
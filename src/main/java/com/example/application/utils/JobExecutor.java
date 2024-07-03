package com.example.application.utils;

import com.example.application.data.entity.JobManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.notification.Notification;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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

        executeJob(jobManager);
    }

    private void executeJob(JobManager jobManager) {
        System.out.println("Executing job: " + jobManager.getName());

        try {
            switch (jobManager.getTyp()) {
                case "SQL":
                    executeSQLJob(jobManager);
                    break;
                case "Command":
                    executeCommandJob(jobManager);
                    break;
                case "Shell":
                    executeShellJob(jobManager);
                    break;
                default:
                    throw new Exception("Unsupported job type: " + jobManager.getTyp());
            }
        } catch (Exception e) {
            Notification.show("Error executing job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void executeSQLJob(JobManager jobManager) throws Exception {
        String jdbcUrl = "jdbc:your_database_url";
        String username = "your_db_username";
        String password = "your_db_password";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(jobManager.getCommand())) {
            while (rs.next()) {
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

    private void executeShellJob(JobManager jobManager) throws Exception {
        String scriptPath = "D:\\file\\executer.cmd";
        String jobName = jobManager.getCommand();
        String runID = "777";

        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "\"" + scriptPath + "\"", jobName, runID);
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", "\"" + scriptPath + "\" " + jobName + " " + runID);
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Shell script execution failed with exit code " + exitCode + "\nOutput:\n" + output);
        }

        System.out.println("Shell script executed successfully:\n" + output);
    }
}

package com.example.application.utils;

import com.example.application.data.entity.JobHistory;
import com.example.application.data.entity.JobManager;
import com.example.application.data.service.JobHistoryService;
import com.example.application.service.MessageService;
import com.example.application.views.JobManagerView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class JobExecutor implements Job {

    @Value("${script.path}")
    private String scriptPath;

    @Value("${run.id}")
    private String runID;

    private JobHistoryService jobHistoryService;
    private static final ConcurrentHashMap<Integer, Process> runningProcesses = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ProcessBuilder> runningProcessBuilders = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private JobHistory jobHistory;
    private int exitCode;
    private long processID;
    private String startType;
    private StringBuilder output;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        scriptPath = context.getMergedJobDataMap().getString("scriptPath");
        runID = context.getMergedJobDataMap().getString("runID");
        startType = context.getMergedJobDataMap().getString("startType");
        jobHistoryService = SpringContextHolder.getBean(JobHistoryService.class);
        String jobDefinitionString = context.getMergedJobDataMap().getString("jobManager");
        JobManager jobManager;
        try {
            jobManager = JobDefinitionUtils.deserializeJobDefinition(jobDefinitionString);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }
        executeJob(jobManager);
    }

    private JobHistory createJobHistory(JobManager jobManager) throws IOException {
        JobHistory jobHistory = new JobHistory();
        jobHistory.setProcessId(processID);
        jobHistory.setJobName(jobManager.getName());
        jobHistory.setNamespace(jobManager.getNamespace());
        jobHistory.setParameter(jobManager.getParameter());
        jobHistory.setStartType(startType);
        jobHistory.setStartTime(new Date());
        jobHistory.setEndTime(null); // Will be updated after job completion
        jobHistory.setReturnValue(""); // Placeholder
        jobHistory.setExitCode(null); // Will be updated after job completion
        return jobHistory;
    }
    private String getMemoryUsage(long processID) {
        try {
            // Command to get the memory usage of the process by its PID
            String command = "tasklist /FI \"PID eq " + processID + "\" /FO CSV /NH";
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();  // Close the reader after reading

            if (line != null) {
                // The tasklist output is CSV formatted, so we need to split by comma and trim quotes
                String[] columns = line.split(",");
                if (columns.length >= 6) {
                    // Memory usage is typically in the 5th or 6th column depending on tasklist output
                    String memoryUsage = columns[4].replace("\"", "").trim(); // Adjust column index if necessary
                    return memoryUsage;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return "Memory usage not available";
    }

    private void updateJobHistory() {
        jobHistory.setEndTime(new Date());
        if (output != null && output.length() > 0) {
            jobHistory.setReturnValue(output.toString());
        } else {
            jobHistory.setReturnValue("No output or empty output.");
        }
        jobHistory.setMemoryUsage(getMemoryUsage( processID));
        jobHistory.setExitCode(exitCode);
        jobHistoryService.createOrUpdateJobHistory(jobHistory);
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
            updateJobHistory();
            JobManagerView.notifySubscribers("Job " + jobManager.getName() + " executed successfully,,"+jobManager.getId());
         //   MessageService.addMessage("Job " + jobManager.getName() + " executed successfully.");
        } catch (Exception e) {
            e.getMessage();
            updateJobHistory();
            System.out.println(e.getMessage());
            if(e.getMessage().contains("was stopped manually")) {
                System.out.println(e.getMessage());
            } else if (!stopFlags.get(jobManager.getId()).get()) {
                JobManagerView.notifySubscribers("Error while Job " + jobManager.getName() + " executed,,"+jobManager.getId());
            }
          //  MessageService.addMessage("Error while Job " + jobManager.getName() + " executed.");
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
        String jobName = jobManager.getName();
        String sPath = scriptPath + jobManager.getCommand();
        System.out.println("start executeShellJob");
        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "\"" + sPath + "\"", jobManager.getParameter());
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", "\"" + sPath + "\" " + jobManager.getParameter());
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        processID = process.pid();
        jobHistory = createJobHistory(jobManager);
        jobHistoryService.createOrUpdateJobHistory(jobHistory);

        System.out.println("middle executeShellJob");
        runningProcesses.put(jobManager.getId(), process);
        stopFlags.put(jobManager.getId(), new AtomicBoolean(false));

        // Capture the output
        output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                if (stopFlags.get(jobManager.getId()).get()) {
                    process.destroyForcibly(); // Forcefully terminate the process
                    throw new Exception("Job " + jobName + " was stopped manually.");
                }
                output.append(line).append("\n");
            }
            if (stopFlags.get(jobManager.getId()).get()) {
                System.out.println("Job " + jobName + " was stopped.");
                process.destroyForcibly();
                throw new Exception("Job " + jobName + " was stopped manually.");
            }

            exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new Exception("Shell script execution failed with exit code " + exitCode + "\nOutput:\n" + output);
            }
        } catch (IOException e) {
            throw new Exception("IOException occurred during shell script execution: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new Exception("Job " + jobName + " was interrupted.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing reader: " + e.getMessage());
                }
            }
        }
        System.out.println("end executeShellJob");
        System.out.println("Shell script executed successfully:\n" + output);
    }

    public static void stopProcessold(int jobId) {
        Process process = runningProcesses.get(jobId);
        if (process != null) {
            System.out.println("Stopping process for job id: " + jobId);
            process.destroy();

            try {
                if (process.isAlive()) {
                    process.waitFor(10, TimeUnit.SECONDS); // Wait for 10 seconds max
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Handle interruption if necessary
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly(); // Forceful destroy if not terminated
                }
                runningProcesses.remove(jobId);
             //   stopFlags.get(jobId).set(true);
            }
        }
        process.destroyForcibly();
        AtomicBoolean flag = stopFlags.get(jobId);

        if (flag != null) {
            flag.set(true);
            System.out.println("stopFlags....... " + stopFlags );
        } else {
            System.out.println("No stop flag found for job id: " + jobId);
        }
    }
    public static void stopProcess(int jobId) {
        Process process = runningProcesses.get(jobId);
        ProcessBuilder processBuilder = runningProcessBuilders.get(jobId);
        if (process != null) {
            System.out.println("Stopping process for job id: " + jobId);
            try {
                String killCommand="taskkill /PID " + process.pid() + " /T /F";
                System.out.println("killCommand: " + killCommand);
                Runtime.getRuntime().exec(killCommand);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                if (process.isAlive()) {
                    process.waitFor(10, TimeUnit.SECONDS); // Wait for 10 seconds max
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Handle interruption if necessary
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly(); // Forceful destroy if not terminated
                }
                runningProcesses.remove(jobId);
                //   stopFlags.get(jobId).set(true);
            }
        }
        AtomicBoolean flag = stopFlags.get(jobId);

        if (flag != null) {
            flag.set(true);
            System.out.println("stopFlags....... " + stopFlags );
        } else {
            System.out.println("No stop flag found for job id: " + jobId);
        }
    }
}

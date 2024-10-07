package com.example.application.utils;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.MonitorAlerting;
import com.example.application.data.entity.fvm_monitoring;
import com.example.application.service.CockpitService;
import com.example.application.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class BackgroundJobExecutor implements Job {

    private JdbcTemplate jdbcTemplate;
    private CockpitService cockpitService;
    private String startType;
    private Configuration configuration;
    public static boolean stopJob = false;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        startType = context.getMergedJobDataMap().getString("startType");

        cockpitService = SpringContextHolder.getBean(CockpitService.class);

        String jobDefinitionString = context.getMergedJobDataMap().getString("configuration");

        try {
            configuration = JobDefinitionUtils.deserializeJobConfDefinition(jobDefinitionString);
            executeJob(configuration);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }

    }

    private void executeJob(Configuration configuration) {
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);
        System.out.println(configuration.getName()+"______________________background job execution start");
        if (monitorAlerting == null || monitorAlerting.getCron() == null) {
            return; // Exit if no configuration or interval is set
        }


        // Fetch max parallel checks from MonitorAlerting
        int maxParallelChecks = monitorAlerting.getMaxParallelCheck();

        // Create an executor service with a fixed thread pool size
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelChecks);

        // Check all monitoring entries
        List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);
        jdbcTemplate = cockpitService.getJdbcTemplateWithDBConnetion(configuration);

        // Clean up old results based on retention time
        cleanUpOldResults(monitorAlerting.getRetentionTime());

        for (fvm_monitoring monitoring : monitorings) {
            if(monitoring.getIS_ACTIVE().equals("1")) {
                try {
                    if (jdbcTemplate != null) {
                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID()
                        );
                    }
                } catch (Exception e) {
                    e.getMessage();
                }
                executorService.submit(() -> {
                    try {
                        if (stopJob) {
                            return; // Exit if the job is stopped
                        }
                        if (jdbcTemplate != null) {
                            Timestamp lastCheck = jdbcTemplate.queryForObject(
                                    "SELECT MAX(Zeitpunkt) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                                    new Object[]{monitoring.getID()},
                                    Timestamp.class
                            );

                            long timeSinceLastCheck = (lastCheck != null) ?
                                    Duration.between(lastCheck.toLocalDateTime(), LocalDateTime.now()).toMinutes() :
                                    Long.MAX_VALUE;

                            System.out.println(monitoring.getID()+"  +++++++ "+timeSinceLastCheck+" +++++++++ "+monitoring.getCheck_Intervall());
                            if (timeSinceLastCheck >= monitoring.getCheck_Intervall()) {

                                String sqlQuery = monitoring.getSQL();
                                String result = jdbcTemplate.queryForObject(sqlQuery, String.class);


                                // Insert new result with IS_ACTIVE = 1 and store any message
                                jdbcTemplate.update("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?) ",
                                        monitoring.getID(),
                                        Timestamp.valueOf(LocalDateTime.now()),
                                        1,
                                        result,
                                        "Query executed successfully");

                                System.out.println(monitoring.getID() + " query executed: " + monitoring.getSQL());
                            }
                        }

                    } catch (Exception e) {
                        System.out.println("Error executing SQL for monitoring ID: " + monitoring.getID() + " - " + e.getMessage());

                        // Store the error message in DB_MESSAGE
                        jdbcTemplate.update("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                                monitoring.getID(),
                                Timestamp.valueOf(LocalDateTime.now()),
                                0,
                                null, // No result on error
                                e.getMessage()); // Store error message
                    }
                });
            }
        }

        executorService.shutdown();
        try {
            // Wait for all tasks to complete or timeout
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.out.println("Executor service interrupted: " + e.getMessage());
        }

    }

    private void cleanUpOldResults(int retentionDays) {
        try {
            // Use SYSDATE - retentionDays to subtract days
            int rowsDeleted = jdbcTemplate.update(
                    "DELETE FROM FVM_MONITOR_RESULT WHERE TRUNC(Zeitpunkt) < TRUNC(SYSDATE - ?)",
                    retentionDays);

            System.out.println("Number of rows deleted: " + rowsDeleted);

        } catch (Exception e) {
            System.out.println("Error deleting old results: " + e.getMessage());
        }
    }


}


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
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
        transactionTemplate = SpringContextHolder.getBean(TransactionTemplate.class); // Retrieve TransactionTemplate
        String jobDefinitionString = context.getMergedJobDataMap().getString("configuration");

        try {
            configuration = JobDefinitionUtils.deserializeJobConfDefinition(jobDefinitionString);
            executeJob(configuration);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }

    }

    // Global variables
    private static int currentThreads = 0;
    private static final Object threadLock = new Object(); // Lock for thread synchronization
    private static final Set<Integer> globalStatus = ConcurrentHashMap.newKeySet(); // Global status to track running IDs

    @Autowired
    private TransactionTemplate transactionTemplate; // Add this dependency

    private void executeJob(Configuration configuration) {
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);
        System.out.println(configuration.getName() + "______________________background job execution start");

        if (monitorAlerting == null || monitorAlerting.getCron() == null) {
            return; // Exit if no configuration or interval is set
        }

        int maxParallelChecks = monitorAlerting.getMaxParallelCheck();
        List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);
        jdbcTemplate = cockpitService.getJdbcTemplateWithDBConnetion(configuration);

        // Initialize executor service with a fixed thread pool based on maxParallelChecks
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelChecks);

        // Clean up old results based on retention time
        cleanUpOldResults(monitorAlerting.getRetentionTime());

        for (fvm_monitoring monitoring : monitorings) {
            if (monitoring.getIS_ACTIVE().equals("1") && monitoring.getPid() != 0) {

                try {
                    // step 1.
                    Timestamp lastCheck = jdbcTemplate.queryForObject(
                            "SELECT MAX(Zeitpunkt) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                            new Object[]{monitoring.getID()},
                            Timestamp.class
                    );

                    long timeSinceLastCheck = (lastCheck != null) ?
                            Duration.between(lastCheck.toLocalDateTime(), LocalDateTime.now()).toMinutes() :
                            Long.MAX_VALUE;
//                    System.out.println(monitoring.getID()+"............... "+timeSinceLastCheck +" >=" +monitoring.getCheck_Intervall()+" = "+(timeSinceLastCheck >= monitoring.getCheck_Intervall()));
//                    System.out.println("currentThreads:"+currentThreads+ " <= maxParallelChecks:"+maxParallelChecks +" = "+(currentThreads <= maxParallelChecks));
//                    System.out.println("!globalStatus.contains(monitoring.getID() = "+(!globalStatus.contains(monitoring.getID())));

                    synchronized (threadLock) {
                        if (timeSinceLastCheck >= monitoring.getCheck_Intervall()
                                && currentThreads <= maxParallelChecks
                                && !globalStatus.contains(monitoring.getID())) {

                            // step 2.
                            currentThreads++;
                            globalStatus.add(monitoring.getID());

                            executorService.submit(() -> {
                                try {
                                    if (stopJob) {
                                        return; // Exit if the job is stopped
                                    }
                                    String sqlQuery = monitoring.getSQL();
                                    String result = jdbcTemplate.queryForObject(sqlQuery, String.class);

                                    // step 3.
                                    transactionTemplate.execute(status -> {
                                        try {

                                            jdbcTemplate.update(
                                                    "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                                    monitoring.getID());

                                            jdbcTemplate.update(
                                                    "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                                                    monitoring.getID(),
                                                    Timestamp.valueOf(LocalDateTime.now()),
                                                    1, // Mark as active
                                                    result,
                                                    "Query executed successfully");

                                            System.out.println(monitoring.getID() + "----------------------query executed: " + monitoring.getSQL().toString());
                                        } catch (Exception ex) {
                                            status.setRollbackOnly();
                                            throw ex;
                                        }

                                        return null;
                                    });

                                } catch (Exception e) {
                                    System.out.println("Error executing SQL for monitoring ID: " + monitoring.getID() + " - " + e.getMessage());

                                    jdbcTemplate.update(
                                            "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                            monitoring.getID());
                                    // Store the error message in DB_MESSAGE in case of error
                                    jdbcTemplate.update("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                                            monitoring.getID(),
                                            Timestamp.valueOf(LocalDateTime.now()),
                                            1,
                                            null, // No result on error
                                            e.getMessage()); // Store error message
                                } finally {
                                    // step 4.
                                    synchronized (threadLock) {
                                        currentThreads--;
                                        globalStatus.remove(monitoring.getID());
                                    }
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Error executing monitoring ID: " + monitoring.getID() + " - " + e.getMessage());
                }
            }
        }

        // Shut down executor service after all tasks are submitted
        executorService.shutdown();
        try {
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

    private void executeJobOld(Configuration configuration) {
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

        try {
            if (jdbcTemplate != null) {
                jdbcTemplate.update(
                        "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1"
                );
            }
        } catch (Exception e) {
            e.getMessage();
        }

        for (fvm_monitoring monitoring : monitorings) {
            if(monitoring.getIS_ACTIVE().equals("1")) {

           //     executorService.submit(() -> {
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

                            System.out.println("ID ="+monitoring.getID()+" .....+++++++ "+timeSinceLastCheck+" +++++++++ "+monitoring.getCheck_Intervall());
                            if (timeSinceLastCheck >= monitoring.getCheck_Intervall()) {
                                System.out.println(monitoring.getID()+" ---------"+monitoring.getSQL());
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
                                1,
                                null, // No result on error
                                e.getMessage()); // Store error message
                    }
           //     });
            }
        }

//        executorService.shutdown();
//        try {
//            // Wait for all tasks to complete or timeout
//            executorService.awaitTermination(10, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            System.out.println("Executor service interrupted: " + e.getMessage());
//        }

    }

    private void cleanUpOldResultsOld(int retentionDays) {
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


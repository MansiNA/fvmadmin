package com.example.application.utils;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.MonitorAlerting;
import com.example.application.data.entity.fvm_monitoring;
import com.example.application.service.CockpitService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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

    //  private JdbcTemplate jdbcTemplate;
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
         //   JdbcTemplate jdbcTemplate = cockpitService.getJdbcTemplateWithDBConnetion(configuration);

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
        MonitorAlerting monitorAlerting = fetchEmailConfiguration(configuration);
        System.out.println(configuration.getName() + "______________________background job execution start........................................................................................");

        if (monitorAlerting == null || monitorAlerting.getCron() == null) {
            return; // Exit if no configuration or interval is set
        }

        //   int maxParallelChecks = monitorAlerting.getMaxParallelCheck();
        int maxParallelChecks = (monitorAlerting.getMaxParallelCheck() > 0) ? monitorAlerting.getMaxParallelCheck() : 1;
        int retentionTime = (monitorAlerting.getRetentionTime() > 0) ? monitorAlerting.getRetentionTime() : 1;

        List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);
        System.out.println(configuration.getUserName()+"......................monitorings list = "+monitorings.size());

        // Initialize executor service with a fixed thread pool based on maxParallelChecks
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelChecks);

        // Clean up old results based on retention time
        // cleanUpOldResults(retentionTime, configuration);

        if(monitorings.size() == 0) {
            System.out.println("global retention used");
            cleanUpOldResults(retentionTime, configuration, null);
        }

        for (fvm_monitoring monitoring : monitorings) {
            if (monitoring.getIS_ACTIVE().equals("1") && monitoring.getPid() != 0) {
                JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
                try {
                    cleanUpOldResults(monitoring.getRetentionTime(), configuration, monitoring.getID());
                    // step 1.
                    Timestamp lastCheck = jdbcTemplate.queryForObject(
                            "SELECT MAX(Zeitpunkt) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                            new Object[]{monitoring.getID()},
                            Timestamp.class
                    );

                    long timeSinceLastCheck = (lastCheck != null) ?
                            Duration.between(lastCheck.toLocalDateTime(), LocalDateTime.now()).toMinutes() :
                            Long.MAX_VALUE;

                    synchronized (threadLock) {
                        if (timeSinceLastCheck >= monitoring.getCheck_Intervall()
                                && currentThreads <= maxParallelChecks
                                && !globalStatus.contains(monitoring.getID())) {

                            // step 2.
                            currentThreads++;
                            globalStatus.add(monitoring.getID());

                            // step 3.
                            executorService.submit(() -> {
                                executeMonitoringTask(monitoring, jdbcTemplate);
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

    private void executeMonitoringTask(fvm_monitoring monitoring, JdbcTemplate jdbcTemplate) {
        try {
            if (stopJob) {
                return; // Exit if the job is stopped
            }

            transactionTemplate.execute(status -> {
                try {
                    String sqlQuery = monitoring.getSQL();
                    String result = jdbcTemplate.queryForObject(sqlQuery, String.class);
                    String username = jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName();
                    System.out.println(monitoring.getID() + "------"+ username +"----------------query executed: " + monitoring.getSQL().toString());

                    Integer activeRowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE IS_ACTIVE = 1 AND ID = ?",
                            Integer.class,
                            monitoring.getID());
                    if (activeRowCount != null && activeRowCount > 0) {
                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID());
                    }

                    jdbcTemplate.update(
                            "INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            1, // Mark as active
                            result,
                            "Query executed successfully");
                    username = jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName();
                    System.out.println(monitoring.getID() + "-----------"+username+"-----------query result store: " + monitoring.getSQL().toString());
                } catch (Exception ex) {
                    status.setRollbackOnly();

                    Integer activeRowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM FVM_MONITOR_RESULT WHERE IS_ACTIVE = 1 AND ID = ?",
                            Integer.class,
                            monitoring.getID());
                    if (activeRowCount != null && activeRowCount > 0) {
                        jdbcTemplate.update(
                                "UPDATE FVM_MONITOR_RESULT SET IS_ACTIVE = 0 WHERE IS_ACTIVE = 1 AND ID = ?",
                                monitoring.getID());
                    }
                    // Store the error message in DB_MESSAGE in case of error
                    jdbcTemplate.update("INSERT INTO FVM_MONITOR_RESULT (ID, Zeitpunkt, IS_ACTIVE, RESULT, DB_MESSAGE) VALUES (?, ?, ?, ?, ?)",
                            monitoring.getID(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            1,
                            null, // No result on error
                            ex.getMessage()); // Store error message

                    try {
                        throw ex;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                return null;
            });

        } catch (Exception e) {
            System.out.println("Error executing SQL for monitoring ID: " + monitoring.getID() + " - " + e.getMessage());

        } finally {
            // step 4.
            synchronized (threadLock) {
                currentThreads--;
                globalStatus.remove(monitoring.getID());
            }
            connectionClose(jdbcTemplate);
        }
    }

    private void cleanUpOldResults(int retentionDays, Configuration configuration, Integer id) {
        JdbcTemplate jdbcTemplate = null;
        try {
            int rowsDeleted = 0;
            jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            if(id != null) {
                rowsDeleted = jdbcTemplate.update(
                        "DELETE FROM FVM_MONITOR_RESULT WHERE ID = ? AND TRUNC(Zeitpunkt) < TRUNC(SYSDATE - ?)",
                        id, retentionDays);
           //     System.out.println(id +".....this id retention time..."+retentionDays);
            } else {
                 rowsDeleted = jdbcTemplate.update(
                        "DELETE FROM FVM_MONITOR_RESULT WHERE TRUNC(Zeitpunkt) < TRUNC(SYSDATE - ?)",
                        retentionDays);
            }

            System.out.println("Number of rows deleted: " + rowsDeleted);

        } catch (Exception e) {
            System.out.println("Error deleting old results: " + e.getMessage());
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
         //   System.out.println("connection closeeeeeeeee-------------"+configuration.getUserName());
        }
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabase(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
            return new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }
    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            System.out.println(configuration.getName()+",,,,,,,,,,,,,,,,,,,,,,,,,,,");


            // Query to get the existing configuration
            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CRON_EXPRESSION, LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, ISBACKJOBACTIVE FROM FVM_MONITOR_ALERTING";

            // Use jdbcTemplate to query and map results to MonitorAlerting object
            jdbcTemplate.query(sql, rs -> {
                // Set the values in the MonitorAlerting object from the result set
                monitorAlerting.setMailEmpfaenger(rs.getString("MAIL_EMPFAENGER"));
                monitorAlerting.setMailCCEmpfaenger(rs.getString("MAIL_CC_EMPFAENGER"));
                monitorAlerting.setMailBetreff(rs.getString("MAIL_BETREFF"));
                monitorAlerting.setMailText(rs.getString("MAIL_TEXT"));
                monitorAlerting.setCron(rs.getString("CRON_EXPRESSION"));
                monitorAlerting.setRetentionTime(rs.getInt("RETENTION_TIME"));
                monitorAlerting.setMaxParallelCheck(rs.getInt("MAX_PARALLEL_CHECKS"));
                // Converting SQL Timestamp to LocalDateTime
                Timestamp lastAlertTimeStamp = rs.getTimestamp("LAST_ALERT_TIME");
                if (lastAlertTimeStamp != null) {
                    monitorAlerting.setLastAlertTime(lastAlertTimeStamp.toLocalDateTime());
                }

                Timestamp lastAlertCheckTimeStamp = rs.getTimestamp("LAST_ALERT_CHECKTIME");
                if (lastAlertCheckTimeStamp != null) {
                    monitorAlerting.setLastALertCheckTime(lastAlertCheckTimeStamp.toLocalDateTime());
                }
                monitorAlerting.setIsActive(rs.getInt("IS_ACTIVE"));
                monitorAlerting.setIsBackJobActive(rs.getInt("ISBACKJOBACTIVE"));
            });

            return monitorAlerting;
        } catch (Exception e) {
            e.printStackTrace();
            //   Notification.show("Failed to load configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return null;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            dataSource = jdbcTemplate.getDataSource();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();

                    if (dataSource instanceof HikariDataSource) {
                        ((HikariDataSource) dataSource).close();
                    }

                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }
    }

}


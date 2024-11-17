package com.example.application.utils;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.MonitorAlerting;
import com.example.application.data.entity.fvm_monitoring;
import com.example.application.data.service.ConfigurationService;
import com.example.application.service.CockpitService;
import com.example.application.views.CockpitView;
import com.example.application.views.MainLayout;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class BackgroundJobExecutor implements Job {

    //  private JdbcTemplate jdbcTemplate;
    private CockpitService cockpitService;
    private ConfigurationService configurationService;
    private String startType;
    private Configuration configuration;
    public static boolean stopJob = false;
    private static final Logger logger = LoggerFactory.getLogger(BackgroundJobExecutor.class);
    private static int count = 0;
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        startType = context.getMergedJobDataMap().getString("startType");

        cockpitService = SpringContextHolder.getBean(CockpitService.class);
        configurationService = SpringContextHolder.getBean(ConfigurationService.class);
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
        logger.info("Starting executeJob() of.................."+configuration.getName());
        MonitorAlerting monitorAlerting = fetchEmailConfiguration(configuration);

        if (monitorAlerting == null || monitorAlerting.getCron() == null) {
            return; // Exit if no configuration or interval is set
        }

        //   int maxParallelChecks = monitorAlerting.getMaxParallelCheck();
        int maxParallelChecks = (monitorAlerting.getMaxParallelCheck() > 0) ? monitorAlerting.getMaxParallelCheck() : 1;
        int retentionTime = (monitorAlerting.getRetentionTime() > 0) ? monitorAlerting.getRetentionTime() : 1;

        List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);
        logger.info("Count QS-Checks: " + monitorings.size());

        // Initialize executor service with a fixed thread pool based on maxParallelChecks
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelChecks);

        // Clean up old results based on retention time
        // cleanUpOldResults(retentionTime, configuration);

        if(monitorings.size() == 0) {


            logger.info("No active QS-Checks found");
            return;
            //logger.info("Global retention time used");
            //cleanUpOldResults(retentionTime, configuration, null);
        }

        for (fvm_monitoring monitoring : monitorings) {
            if (monitoring.getIS_ACTIVE().equals("1") && monitoring.getPid() != 0) {
                logger.info("#########################");
                logger.info("Start CHECK-SQL ID=" + monitoring.getID());

                try {
                    cleanUpOldResults(monitoring.getRetentionTime(), configuration, monitoring.getID());

                    JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

                    // step 1.
                    Timestamp lastCheck = jdbcTemplate.queryForObject(
                            "SELECT MAX(Zeitpunkt) FROM FVM_MONITOR_RESULT WHERE ID = ?",
                            new Object[]{monitoring.getID()},
                            Timestamp.class
                    );

                    logger.info("Last Checktime of QS-ID: " + monitoring.getID() + ": " + lastCheck);

                    long timeSinceLastCheck = (lastCheck != null) ?
                            Duration.between(lastCheck.toLocalDateTime(), LocalDateTime.now()).toMinutes() :
                            Long.MAX_VALUE;

                    synchronized (threadLock) {
                        logger.info("TimeSinceLastCheck of QS-ID " + monitoring.getID() + " = " + timeSinceLastCheck + " Min. CheckInterval is " + monitoring.getCheck_Intervall() + " Min.");
                        logger.info("CurrentThreads " + currentThreads + " Max Threads: " + maxParallelChecks );

                        if (timeSinceLastCheck >= monitoring.getCheck_Intervall()
                                && currentThreads <= maxParallelChecks
                                && !globalStatus.contains(monitoring.getID())) {

                            // step 2.
                            currentThreads++;
                            globalStatus.add(monitoring.getID());
                            logger.info("Requirements met, add Thread. CurrentThreads now: " + currentThreads);

                            // step 3.
                            logger.info("Execute Query: \"" + monitoring.getSQL() + "\"");
                           // logger.info("Connection: " + jdbcTemplate.getDataSource().getConnection().getSchema().toString());
                            executorService.submit(() -> {
                                executeMonitoringTask(monitoring, jdbcTemplate);
                            });
                        }
                    }

                } catch (Exception e) {
                    logger.error("Executing executeJob() while error monitoring ID: " + monitoring.getID() + " - " + e.getMessage());
                //    System.out.println("Error executing monitoring ID: " + monitoring.getID() + " - " + e.getMessage());
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
        logger.info("Method executeMonitoringTask called");
        try {
            if (stopJob) {
                return; // Exit if the job is stopped
            }

            transactionTemplate.execute(status -> {
                try {
                    String sqlQuery = monitoring.getSQL();
                    String result = jdbcTemplate.queryForObject(sqlQuery, String.class);
        //            String username = jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName();
                    logger.info("Store result for ID " + monitoring.getID() + ": "+ result);
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
          //          username = jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName();
                    connectionClose(jdbcTemplate);

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
                    logger.info(monitoring.getID() + "----query error result store: " + monitoring.getSQL().toString());
            //        connectionClose(jdbcTemplate);
                }
                 finally {
                    // step 4.
                    synchronized (threadLock) {
                        currentThreads--;
                        globalStatus.remove(monitoring.getID());
                    }
              //      connectionClose(jdbcTemplate);
                }
                return null;

            });

        } catch (Exception e) {
          //  System.out.println("Error executing SQL for monitoring
            //  +ID: " + monitoring.getID() + " - " + e.getMessage());
            logger.error("Executing executeMonitoringTask: Error SQL for monitoring ID: " + monitoring.getID() + " - " + e.getMessage());
        } finally {
            // step 4.
            synchronized (threadLock) {
                currentThreads--;
                globalStatus.remove(monitoring.getID());
            }
            //connectionClose(jdbcTemplate);
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
            logger.info("Executing cleanUpOldResults: Number of rows deleted: " + rowsDeleted);
         //   System.out.println("Number of rows deleted: " + rowsDeleted);

        } catch (Exception e) {
            logger.error("Executing cleanUpOldResults: Error deleting old results: " + e.getMessage());
         //   System.out.println("Error deleting old results: " + e.getMessage());
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
         //   System.out.println("connection closeeeeeeeee-------------"+configuration.getUserName());
        }
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabaseold(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {
            logger.info(conf.getUserName()+": Connection open........");

            return new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabase(Configuration conf) {

        try {
            return new JdbcTemplate(configurationService.getActivePools().get(conf.getId()));
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabasenew(Configuration conf) {
        // Create HikariConfig object
        HikariConfig hikariConfig = new HikariConfig();

        // Set database connection parameters
        hikariConfig.setJdbcUrl(conf.getDb_Url());
        hikariConfig.setUsername(conf.getUserName());
        hikariConfig.setPassword(Configuration.decodePassword(conf.getPassword()));

        // Set maxLifetime to 2 minutes (120000 ms), after which connections are closed
        hikariConfig.setMaxLifetime(30000); // 2 minutes

        // Set idleTimeout to 1 minute (60000 ms) to close connections that are idle for over 1 minute
        hikariConfig.setIdleTimeout(30000); // 1 minute

        // Set the maximum pool size to control the number of open connections
        hikariConfig.setMaximumPoolSize(10); // Customize as needed
        hikariConfig.setPoolName("DB-pool");
        count = count + 1;

        // Optionally set minimumIdle to keep fewer connections open during low usage periods
        hikariConfig.setMinimumIdle(5); // Maintain at least 1 connection

        // Set connection timeout for how long to wait for an available connection
        hikariConfig.setConnectionTimeout(30000);
        // Create HikariDataSource from HikariConfig
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        return new JdbcTemplate(dataSource);
    }

    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        JdbcTemplate jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
        try {
            logger.info("Executing fetchEmailConfiguration");

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


        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Executing fetchEmailConfiguration: from"+ configuration.getUserName());
            //   Notification.show("Failed to load configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
            return monitorAlerting;
        }

    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
//        Connection connection = null;
//        DataSource dataSource = null;
//        Statement stmt = null;

//        try {
            
//            logger.info("try closing Connection!");

//            jdbcTemplate.getDataSource().getConnection().endRequest();
//            jdbcTemplate.getDataSource().getConnection().close();

//            Connection con=jdbcTemplate.getDataSource().getConnection();
            //stmt=jdbcTemplate.getDataSource().

  //          dataSource = jdbcTemplate.getDataSource();
  //          JdbcUtils.closeStatement(stmt);


//            Thread.sleep(2000);
//            logger.info("Connection isClosed: " + jdbcTemplate.getDataSource().getConnection().isClosed());
//            if (jdbcTemplate.getDataSource() instanceof HikariDataSource) {
//                ((HikariDataSource) jdbcTemplate.getDataSource()).close();
//            } else {
//                jdbcTemplate.setDataSource(null);
//            }
//            jdbcTemplate.setDataSource(null);
//            // logger.info(connection.getMetaData().getUserName()+": Connection close........");
//       //     connection = jdbcTemplate.getDataSource().getConnection();
//       //     dataSource = jdbcTemplate.getDataSource();
  //      } catch (Exception e) {
   //         e.printStackTrace();
//        } finally {
//            if (connection != null) {
//                try {
//                    connection.close();
//
//                   // System.out.println("connection closed..."+connection.isClosed() +".....");
//                        if (dataSource instanceof HikariDataSource) {
//                            ((HikariDataSource) dataSource).close();
//                        } else {
//                            dataSource.getConnection().close();
//                        }
//
//                } catch (SQLException e) {
//
//                    e.printStackTrace();
//                }
//            }
  //      }
    }

}


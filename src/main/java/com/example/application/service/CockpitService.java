package com.example.application.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.JobManager;
import com.example.application.data.entity.MonitorAlerting;
import com.example.application.data.entity.fvm_monitoring;
import com.vaadin.flow.component.notification.Notification;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CockpitService {

    private JdbcTemplate jdbcTemplate;
    List<fvm_monitoring> listOfMonitores;


    public CockpitService(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    private void connectWithDatabase(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
            this.jdbcTemplate = new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
           return this.jdbcTemplate = new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public void connectionClose() {
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

    public List<fvm_monitoring> getMonitoring(Configuration configuration) {
     //   List<fvm_monitoring> monitore = new ArrayList<>();

        //String sql = "SELECT ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT, ERROR_SCHWELLWERT FROM EKP.FVM_MONITORING";

//        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
//                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
//                ", Zeitpunkt, m.is_active, nvl(m.sql_detail,'select ''Detail-SQL nicht definiert'' from dual') as sql_detail FROM FVM_MONITORING m\n" +
//                "left outer join FVM_MONITOR_RESULT mr\n" +
//                "on m.id=mr.id\n" +
//                "and mr.is_active='1'";

        String sql = "SELECT m.ID,m.PID, m.Bereich, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
                ", Zeitpunkt, m.is_active, m.sql_detail as sql_detail FROM FVM_MONITORING m\n" +
                "left outer join FVM_MONITOR_RESULT mr\n" +
                "on m.id=mr.id\n" +
                "and mr.is_active='1'";


        System.out.println("Abfrage EKP.FVM_Monitoring (CockpitView.java): ");
        System.out.println(sql);

        connectWithDatabase(configuration);

        try {
            listOfMonitores = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(fvm_monitoring.class));

            System.out.println("FVM_Monitoring eingelesen");

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            System.out.println("Exception: " + e.getMessage());
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }

        return listOfMonitores;
    }
    private boolean tableExistsold(String tableName) {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM ALL_TABLES WHERE table_name = ?";
            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);

            return tableCount > 0;
        } catch (Exception e) {
            System.out.println("Exception while checking table existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean tableExists(String tableName, String databaseType) {
        try {
            String checkTableSql;
            // Switch based on the database type
            switch (databaseType.toLowerCase()) {
                case "oracle":
                    checkTableSql = "SELECT COUNT(*) FROM ALL_TABLES WHERE table_name = ?";
                    break;
                case "sqlserver":
                    checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported database type: " + databaseType);
            }

            // Execute the query to check if the table exists
            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName.toUpperCase());

            return tableCount > 0;
        } catch (Exception e) {
            System.out.println("Exception while checking table existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void createFvmMonitorAlertingTable(Configuration configuration) {

        String tableName = "FVM_MONITOR_ALERTING";
        try {
            connectWithDatabase(configuration);
            String dbType = "oracle";
            if(configuration.getName().contains("SQLServer")) {
                dbType = "sqlserver";
            }
            if (!tableExists(tableName, dbType)) {
                System.out.println("Creating table: " + tableName);

                String createTableSQL = "CREATE TABLE FVM_MONITOR_ALERTING ("
                        + "MAIL_EMPFAENGER VARCHAR(255), "
                        + "MAIL_CC_EMPFAENGER VARCHAR(255), "
                        + "MAIL_BETREFF VARCHAR(255), "
                        + "MAIL_TEXT VARCHAR(255), "
                        + "CHECK_INTERVALL INT, "
                        + "LAST_ALERT_TIME DATE, "
                        + "LAST_ALERT_CHECKTIME TIMESTAMP, "
                        + "IS_ACTIVE INT, "
                        + "RETENTION_TIME INT"
                        + ")";

                jdbcTemplate.execute(createTableSQL);

                System.out.println("Table created successfully.");
            } else {
                System.out.println("Table already exists: " + tableName);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose();
        }
    }

    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        try {

            connectWithDatabase(configuration);

            // Query to get the existing configuration
            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CRON_EXPRESSION, LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS FROM FVM_MONITOR_ALERTING";

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
            });

            return monitorAlerting;
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to load configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
        return null;
    }
    public boolean saveEmailConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        try {
            connectWithDatabase(configuration);

            // Check if there is any existing data in the table
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                // If record exists, update the configuration
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET " +
                        "MAIL_EMPFAENGER = ?, " +
                        "MAIL_CC_EMPFAENGER = ?, " +
                        "MAIL_BETREFF = ?, " +
                        "MAIL_TEXT = ?, " +
                        "IS_ACTIVE = ? ";

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                    //    monitorAlerting.getCron(),
                        monitorAlerting.getIsActive()
//                        monitorAlerting.getMaxParallelCheck(),
//                        monitorAlerting.getRetentionTime()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("No configuration was updated.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                // If no record exists, insert a new configuration
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING " +
                        "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CRON_EXPRESSION, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?,?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("Failed to insert new configuration.", 5000, Notification.Position.MIDDLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
        return false;
    }

    public boolean saveBackgoundJobConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        try {
            connectWithDatabase(configuration);

            // Check if there is any existing data in the table
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                // If record exists, update the configuration
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET " +
                        "CRON_EXPRESSION = ?, " +
                        "IS_ACTIVE = ?, " +
                        "RETENTION_TIME = ?, " +
                        "MAX_PARALLEL_CHECKS = ? ";

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck()
                        //    monitorAlerting.getCron(),

//                        monitorAlerting.getMaxParallelCheck(),
//                        monitorAlerting.getRetentionTime()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("No configuration was updated.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                // If no record exists, insert a new configuration
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING " +
                        "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CRON_EXPRESSION, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?,?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getCron(),
                        monitorAlerting.getIsActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("Failed to insert new configuration.", 5000, Notification.Position.MIDDLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
        return false;
    }

    public boolean updateIsActive(int isActive, Configuration configuration) {
        try {
            connectWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET IS_ACTIVE = ?";

            // Update the database with the new configuration
            int rowsAffected = jdbcTemplate.update(updateQuery, isActive);

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to update configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
        return false;
    }

    public void updateLastAlertTimeInDatabase(MonitorAlerting monitorAlerting, Configuration configuration) {
        try {
            connectWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());
            System.out.println("Updated last alert time in database.");
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while updating last alert in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
    }

    public void updateLastAlertCheckTimeInDatabase(MonitorAlerting monitorAlerting, Configuration configuration) {
        try {
            connectWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_CHECKTIME = ?";
            jdbcTemplate.update(updateQuery, LocalDateTime.now());

            System.out.println("Updated last alert check time in database for ID: " + monitorAlerting.getId());
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error while updating last alert check time in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
    }

    public void deleteLastAlertTimeInDatabase(Configuration configuration) {
        try {
            // Establish a connection to the database using the provided configuration
            connectWithDatabase(configuration);

            // SQL query to set LAST_ALERT_TIME to NULL, effectively deleting the timestamp
            String deleteQuery = "UPDATE FVM_MONITOR_ALERTING SET LAST_ALERT_TIME = NULL";

            // Execute the query to update the record for the given monitorAlerting ID
            jdbcTemplate.update(deleteQuery);

            // Log success message
            System.out.println("Deleted last alert time in the database.");
        } catch (Exception e) {
            // Log and show error message in case of an exception
            e.printStackTrace();
            Notification.show("Error while deleting last alert in DB: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
    }

    public List<fvm_monitoring> getRootMonitor() {
        System.out.println("-----------"+ listOfMonitores.size()+"-----------------------------");
        List<fvm_monitoring> rootProjects = listOfMonitores
                .stream()
                .filter(monitor -> monitor.getPid() == 0)
                .collect(Collectors.toList());

        // Log the names of root projects
        //    rootProjects.forEach(project -> System.out.println("Root Project: " + project.toString()));

        return rootProjects;
    }

    public List<fvm_monitoring> getChildMonitor(fvm_monitoring parent) {

        List<fvm_monitoring> childProjects = listOfMonitores
                .stream()
                .filter(monitor -> Objects.equals(monitor.getPid(), parent.getID()))
                .collect(Collectors.toList());

        // Log the names of child projects
        //    childProjects.forEach(project -> System.out.println("Child Project of " + parent.getName() + ": " + project.getName()));

        return childProjects;
    }

    public List<fvm_monitoring> getParentNodes() {
        return listOfMonitores
                .stream()
                .filter(monitor -> monitor.getPid() == 0)
                .collect(Collectors.toList());
    }

    public fvm_monitoring getParentByPid(Integer pid) {
        return listOfMonitores.stream()
                .filter(monitor -> monitor.getID().equals(pid))
                .findFirst()
                .orElse(null);
    }

}

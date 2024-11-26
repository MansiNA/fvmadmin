package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Mailbox;
import com.example.application.data.entity.MonitorAlerting;
import com.example.application.utils.MailboxWatchdogJobExecutor;
import com.vaadin.flow.component.notification.Notification;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class MailboxService {

    private JdbcTemplate jdbcTemplate;
    private ConfigurationService configurationService;
    //  List<fvm_monitoring> listOfMonitores;

    private static final Logger logger = LoggerFactory.getLogger(MailboxWatchdogJobExecutor.class);

    public MailboxService(JdbcTemplate jdbcTemplate, ConfigurationService configurationService){
        this.jdbcTemplate = jdbcTemplate;
        this.configurationService = configurationService;
    }

    public List<Mailbox> getMailboxes(Configuration configuration) {

        //String sql = "select name,court_id,quantifier, user_id,typ,konvertierungsdienste from EKP.MAILBOX_CONFIG";

        //String sql = "select name,court_id,quantifier, user_id,typ,konvertierungsdienste from EKP.MAILBOX_CONFIG";
        List<Mailbox> mailboxen;
        String sql="select Name,user_id,court_id,typ,konvertierungsdienste,max_message_count,DAYSTOEXPIRE,ROLEID,STATUS,in_egvp_wartend,quantifier,aktuell_in_eKP_verarbeitet,in_ekp_haengend,in_ekp_warteschlange,in_ekp_fehlerhospital from EKP.v_Postfach_Incoming_Status";

        System.out.println("Abfrage EKP.Mailbox_Config (MailboxConfigView.java)");

//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        Configuration conf;
//        conf = comboBox.getValue();
//
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {
            getJdbcTemplateWithDBConnetion(configuration);

            mailboxen = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Mailbox.class));



            System.out.println("MAILBOX_CONFIG eingelesen");

        } catch (Exception e) {
            //   System.out.println("Exception: " + e.getMessage());
            throw new RuntimeException("Error querying the database: " + e.getMessage(), e);
            // Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            connectionClose(jdbcTemplate);
        }

        return mailboxen;
    }

    public boolean saveMailboxJobConfiguration(MonitorAlerting monitorAlerting, Configuration configuration) {
        getJdbcTemplateWithDBConnetion(configuration);
        try {
            // connectWithDatabase(configuration);
            //  jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);

            // Check if there is any existing data in the table
            String checkQuery = "SELECT COUNT(*) FROM FVM_MONITOR_ALERTING";
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (count != null && count > 0) {
                // If record exists, update the configuration
                String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET " +
                        "MB_WATCHDOG_CRON_EXPRESSION = ?, " +
                        "ISMBWATCHDOGACTIVE = ? " ;

                int rowsAffected = jdbcTemplate.update(updateQuery,
                        monitorAlerting.getMbWatchdogCron(),
                        monitorAlerting.getIsMBWatchdogActive()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("No configuration was updated.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                // If no record exists, insert a new configuration
                String insertQuery = "INSERT INTO FVM_MONITOR_ALERTING " +
                        "(MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION ISBACKJOBACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, ISMBWATCHDOGACTIVE) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?,?,?,?)";

                int rowsAffected = jdbcTemplate.update(insertQuery,
                        monitorAlerting.getMailEmpfaenger(),
                        monitorAlerting.getMailCCEmpfaenger(),
                        monitorAlerting.getMailBetreff(),
                        monitorAlerting.getMailText(),
                        monitorAlerting.getBgCron(),
                        monitorAlerting.getMbWatchdogCron(),
                        monitorAlerting.getIsBackJobActive(),
                        monitorAlerting.getRetentionTime(),
                        monitorAlerting.getMaxParallelCheck(),
                        monitorAlerting.getIsMBWatchdogActive()
                );

                if (rowsAffected > 0) {
                    return true;
                } else {
                    Notification.show("Failed to insert new configuration.", 5000, Notification.Position.MIDDLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //   Notification.show("Failed to save configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public void createFvmMonitorAlertingTable(Configuration configuration) {

        String tableName = "FVM_MONITOR_ALERTING";
        getJdbcTemplateWithDBConnetion(configuration);
        try {
            //  connectWithDatabase(configuration);

            String dbType = "oracle";
            if(configuration.getName().contains("SQLServer")) {
                dbType = "sqlserver";
            }
            System.out.println("db______________________ "+configuration.getName());

//            // Form the SQL statement to drop the table, considering case sensitivity and special characters
//            String dropTableSQL = "DROP TABLE \"" + schema + "\".\"" + tableName.toUpperCase() + "\"";
//            jdbcTemplate.execute(dropTableSQL);

            if (!tableExists(tableName, dbType, jdbcTemplate)) {
                System.out.println("Creating table: " + tableName);

                String createTableSQL = "CREATE TABLE FVM_MONITOR_ALERTING ("
                        + "MAIL_EMPFAENGER VARCHAR(255), "
                        + "MAIL_CC_EMPFAENGER VARCHAR(255), "
                        + "MAIL_BETREFF VARCHAR(255), "
                        + "MAIL_TEXT VARCHAR(255), "
                        + "BG_JOB_CRON_EXPRESSION VARCHAR(255), "
                        + "LAST_ALERT_TIME DATE, "
                        + "LAST_ALERT_CHECKTIME TIMESTAMP, "
                        + "IS_ACTIVE INT, "
                        + "RETENTION_TIME INT,"
                        + "MAX_PARALLEL_CHECKS INT, "
                        + "ISBACKJOBACTIVE INT, "
                        + "ISMBWATCHDOGACTIVE INT, "
                        + "MB_WATCHDOG_CRON_EXPRESSION VARCHAR(255) "
                        + ")";

                jdbcTemplate.execute(createTableSQL);

                System.out.println("Table created successfully.");

                // Insert default row
                String insertRowSQL = "INSERT INTO FVM_MONITOR_ALERTING ("
                        + "MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, "
                        + "LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, "
                        + "ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE, MB_WATCHDOG_CRON_EXPRESSION) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";

                jdbcTemplate.update(insertRowSQL,
                        "m.quaschny@t-online.de", "", "In der EKP sind Probleme", "In der EKP sind Probleme",
                        "0 0/1 * * * ?", null, null, 0, 5, 1, 1, 0, "0 0/2 * * * ?");

                System.out.println("Default row inserted successfully.");
            } else {
                System.out.println("Table already exists: " + tableName);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    private boolean tableExists(String tableName, String databaseType, JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql;
            // Switch based on the database type
            switch (databaseType.toLowerCase()) {
                case "oracle":
                    checkTableSql = "SELECT COUNT(*) FROM user_tables WHERE UPPER(table_name) = UPPER(?)";
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
    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        getJdbcTemplateWithDBConnetion(configuration);
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        try {
            System.out.println(configuration.getName()+",,,,,,,,,,,,,,,,,,,,,,,,,,,");
            //   connectWithDatabase(configuration);
            //    jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            // Query to get the existing configuration
            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, BG_JOB_CRON_EXPRESSION, MB_WATCHDOG_CRON_EXPRESSION,LAST_ALERT_TIME, LAST_ALERT_CHECKTIME, IS_ACTIVE, RETENTION_TIME, MAX_PARALLEL_CHECKS, ISBACKJOBACTIVE, ISMBWATCHDOGACTIVE FROM FVM_MONITOR_ALERTING";

            // Use jdbcTemplate to query and map results to MonitorAlerting object
            jdbcTemplate.query(sql, rs -> {
                // Set the values in the MonitorAlerting object from the result set
                monitorAlerting.setMailEmpfaenger(rs.getString("MAIL_EMPFAENGER"));
                monitorAlerting.setMailCCEmpfaenger(rs.getString("MAIL_CC_EMPFAENGER"));
                monitorAlerting.setMailBetreff(rs.getString("MAIL_BETREFF"));
                monitorAlerting.setMailText(rs.getString("MAIL_TEXT"));
                monitorAlerting.setBgCron(rs.getString("BG_JOB_CRON_EXPRESSION"));
                monitorAlerting.setMbWatchdogCron(rs.getString("MB_WATCHDOG_CRON_EXPRESSION"));
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
                monitorAlerting.setIsMBWatchdogActive(rs.getInt("ISMBWATCHDOGACTIVE"));
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
    public boolean updateIsMBWatchdogJobActive(int isActive, Configuration configuration) {
        getJdbcTemplateWithDBConnetion(configuration);
        try {
            //   connectWithDatabase(configuration);
            //   jdbcTemplate = getNewJdbcTemplateWithDatabase(configuration);
            String updateQuery = "UPDATE FVM_MONITOR_ALERTING SET ISMBWATCHDOGACTIVE = ?";

            // Update the database with the new configuration
            int rowsAffected = jdbcTemplate.update(updateQuery, isActive);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure database connection is properly closed
            connectionClose(jdbcTemplate);
        }
        return false;
    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(com.example.application.data.entity.Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(com.example.application.data.entity.Configuration.decodePassword(conf.getPassword()));
        try {
            jdbcTemplate.setDataSource(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
        //    jdbcTemplate.getDataSource().getConnection().close();
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

    public String updateMessageBox(Mailbox mb, String i, Configuration configuration) {
//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        Configuration conf;
//        conf = comboBox.getValue();
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {

            //    jdbcTemplate.setDataSource(ds);
            getJdbcTemplateWithDBConnetion(configuration);
            logger.info("set Quantifier of " + mb.getNAME() + " to " + i);

            jdbcTemplate.execute("update EKP.MAILBOX_CONFIG set quantifier=" + i + " where user_id='" + mb.getUSER_ID() +"'");

            jdbcTemplate.execute("update EKP.MAILBOX_CONFIG_Folder set quantifier=" + i + " where user_id='" + mb.getUSER_ID() +"'");

            return "Ok";
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            return e.getMessage();
        } finally {
            connectionClose(jdbcTemplate);
        }

    }
}

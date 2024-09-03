package com.example.application.service;

import com.example.application.data.entity.Configuration;
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

@Service
public class CockpitService {

    private JdbcTemplate jdbcTemplate;

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
        List<fvm_monitoring> monitore = new ArrayList<>();

        //String sql = "SELECT ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT, ERROR_SCHWELLWERT FROM EKP.FVM_MONITORING";

        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
                ", Zeitpunkt, m.is_active, nvl(m.sql_detail,'select ''Detail-SQL nicht definiert'' from dual') as sql_detail FROM FVM_MONITORING m\n" +
                "left outer join FVM_MONITOR_RESULT mr\n" +
                "on m.id=mr.id\n" +
                "and mr.is_active='1'";


        System.out.println("Abfrage EKP.FVM_Monitoring (CockpitView.java): ");
        System.out.println(sql);

        connectWithDatabase(configuration);

        try {
            monitore = jdbcTemplate.query(
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

        return monitore;
    }

    public MonitorAlerting fetchEmailConfiguration(Configuration configuration) {
        MonitorAlerting monitorAlerting = new MonitorAlerting();
        try {

            connectWithDatabase(configuration);

            // Query to get the existing configuration
            String sql = "SELECT MAIL_EMPFAENGER, MAIL_CC_EMPFAENGER, MAIL_BETREFF, MAIL_TEXT, CHECK_INTERVALL, LAST_ALERT_TIME, LAST_ALERT_CHECKTIME FROM FVM_MONITOR_ALERTING";

            // Use jdbcTemplate to query and map results to MonitorAlerting object
            jdbcTemplate.query(sql, rs -> {
                // Set the values in the MonitorAlerting object from the result set
                monitorAlerting.setMailEmpfaenger(rs.getString("MAIL_EMPFAENGER"));
                monitorAlerting.setMailCCEmpfaenger(rs.getString("MAIL_CC_EMPFAENGER"));
                monitorAlerting.setMailBetreff(rs.getString("MAIL_BETREFF"));
                monitorAlerting.setMailText(rs.getString("MAIL_TEXT"));
                monitorAlerting.setIntervall(rs.getInt("CHECK_INTERVALL"));
                // Converting SQL Timestamp to LocalDateTime
                Timestamp lastAlertTimeStamp = rs.getTimestamp("LAST_ALERT_TIME");
                if (lastAlertTimeStamp != null) {
                    monitorAlerting.setLastAlertTime(lastAlertTimeStamp.toLocalDateTime());
                }

                Timestamp lastAlertCheckTimeStamp = rs.getTimestamp("LAST_ALERT_CHECKTIME");
                if (lastAlertCheckTimeStamp != null) {
                    monitorAlerting.setLastALertCheckTime(lastAlertCheckTimeStamp.toLocalDateTime());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to load configuration: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            // Ensure database connection is properly closed
            connectionClose();
        }
        return monitorAlerting;
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
            String updateQuery = "UPDATE ekp.FVM_MONITOR_ALERTING SET LAST_ALERT_CHECKTIME = ?";
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

}

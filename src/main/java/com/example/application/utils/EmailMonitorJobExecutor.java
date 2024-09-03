package com.example.application.utils;

import com.example.application.data.entity.*;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.data.service.JobHistoryService;
import com.example.application.service.CockpitService;
import com.example.application.service.EmailService;
import com.example.application.views.JobManagerView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.notification.Notification;
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

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class EmailMonitorJobExecutor implements Job {


    @Autowired
    private JdbcTemplate jdbcTemplate;
    private EmailService emailService;
    private CockpitService cockpitService;
    private String startType;
    private Configuration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        startType = context.getMergedJobDataMap().getString("startType");

        cockpitService = SpringContextHolder.getBean(CockpitService.class);
        emailService = SpringContextHolder.getBean(EmailService.class);


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

            if (monitorAlerting == null || monitorAlerting.getIntervall() == null) {
                return; // Exit if no configuration or interval is set
            }

            // Update the LAST_ALERT_CHECKTIME column with the current datetime
            cockpitService.updateLastAlertCheckTimeInDatabase(monitorAlerting, configuration);

            // Check the last alert time to ensure 60 minutes have passed
            LocalDateTime lastAlertTimeFromDB = cockpitService.fetchEmailConfiguration(configuration).getLastAlertTime();
            if (lastAlertTimeFromDB != null && lastAlertTimeFromDB.plusMinutes(60).isAfter(LocalDateTime.now())) {
                System.out.println("60 minutes have not passed since the last alert. Skipping alert.");
                return;
            }

            // Check all monitoring entries
            List<fvm_monitoring> monitorings = cockpitService.getMonitoring(configuration);

            for (fvm_monitoring monitoring : monitorings) {

                if (!monitoring.getIS_ACTIVE().equals("1")) {
                    System.out.println(monitoring.getTitel() + "------------skip-----------" + monitoring.getIS_ACTIVE());
                    continue; // Skip non-active entries
                }
                System.out.println(monitoring.getTitel() + "shouldSendAlert(monitoring) = " + shouldSendAlert(monitoring));
                if (shouldSendAlert(monitoring)) {
                 //   System.out.println("shouldSendEmail(monitorAlerting) = " + shouldSendEmail(monitorAlerting));
                  //  if (shouldSendEmail(monitorAlerting)) {
                    //    System.out.println("send email............. = " + shouldSendEmail(monitorAlerting));
                        sendAlertEmail(monitorAlerting, monitoring);
                        LocalDateTime lastAlertTime = LocalDateTime.now(); // Update last alert time
                        monitorAlerting.setLastAlertTime(lastAlertTime);
                        cockpitService.updateLastAlertTimeInDatabase(monitorAlerting, configuration); // Update the DB with the current time
                 //   }
                }
            }
    }

    private boolean shouldSendAlert(fvm_monitoring monitoring) {
        return monitoring.getAktueller_Wert() > monitoring.getError_Schwellwert();
    }

    private boolean shouldSendEmail(MonitorAlerting monitorAlerting) {
        // Calculate the next valid email sending time
        LocalDateTime nextValidTime = monitorAlerting.getLastAlertTime().plusMinutes(monitorAlerting.getIntervall());
        return LocalDateTime.now().isAfter(nextValidTime);
    }

    private void sendAlertEmail(MonitorAlerting config, fvm_monitoring monitoring) {
        try {
            emailService.sendAttachMessage(config.getMailEmpfaenger(), config.getMailCCEmpfaenger(), config.getMailBetreff(), config.getMailText());
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}


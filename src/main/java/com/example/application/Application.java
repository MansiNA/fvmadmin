package com.example.application;

import com.example.application.data.entity.JobManager;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.example.application.utils.SpringContextHolder;
import com.example.application.views.JobManagerView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@Push
@SpringBootApplication
@Theme(value = "fvmadmin")
@PWA(name = "FVM Admin Tool", shortName = "FVM Admin", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@ConfigurationPropertiesScan
public class Application implements AppShellConfigurator {

    @Value("${cron.autostart}")
    private boolean cronAutostart;
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (cronAutostart) {
            System.out.println("Cron jobs will be started automatically");
            startAllCronJobs();
        } else {
            System.out.println("Cron jobs will not be started automatically");
        }
    }

    private void startAllCronJobs() {
        JobDefinitionService jobDefinitionService = SpringContextHolder.getBean(JobDefinitionService.class);
        List<JobManager> jobManagerList = jobDefinitionService.findAll();

        JobManagerView.allCronButton.setText("Cron Stop");
        JobManagerView.notifySubscribers("Start running all cron jobs...");

        for (JobManager jobManager : jobManagerList) {
            try {
                String type = jobManager.getTyp();
                if (jobManager.getCron() != null && !type.equals("Node") && !type.equals("Jobchain")) {
                    scheduleJob(jobManager);
                }
            } catch (Exception e) {
                JobManagerView.allCronButton.setText("Cron Start");
                Notification.show("Error executing job: " + jobManager.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }
    }

    public void scheduleJob(JobManager jobManager) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("jobManager", JobDefinitionUtils.serializeJobDefinition(jobManager));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("startType", "cron");

        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
                .withIdentity("job-cron-" + jobManager.getId(), "group1")
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-cron-" + jobManager.getId(), "group1")
                .withSchedule(CronScheduleBuilder.cronSchedule(jobManager.getCron()))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}

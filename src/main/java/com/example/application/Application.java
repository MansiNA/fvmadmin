package com.example.application;

import com.example.application.data.entity.*;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.data.service.MailboxService;
import com.example.application.service.CockpitService;
import com.example.application.utils.*;
import com.example.application.views.CockpitView;
import com.example.application.views.JobManagerView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

//    @Value("${email.alerting}")
//    private String emailAlertingAutostart;

    @Autowired
    private JobDefinitionService jobDefinitionService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CockpitService cockpitService;

    @Autowired
    private MailboxService mailboxService;
    public static HashMap<Long, Integer> maxPoolsizeMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static List<MailboxShutdown> globalList=new ArrayList<>();

    public static List<Mailbox> mailboxen=new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        logger.info("main(): Application Starting");
    }
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        if (cronAutostart) {
            logger.info("onApplicationReady(): cronAutostart = "+cronAutostart);
            allCronJobStart();
        }

        initializePools();
        allMonitorCronStart();
        allBackGroundCronStart();
        allMailboxWatchdogCronStart();
//        if ("On".equals(emailAlertingAutostart)) {
//            System.out.println("---------------yyyyyyyyyyyyyyyy-------------------");
//            allMonitorCronStart();
//        }
    }

    private void initializePools() {
        logger.info("initializePools(): initialize confifuration hikari-pools");
        List<Configuration> configurations = configurationService.findMessageConfigurations();

        for (Configuration config : configurations) {
            int maximumPoolSize  = cockpitService.fetchMaxParallel(config);
            maximumPoolSize = ( maximumPoolSize > 0) ? maximumPoolSize : 1;
            maxPoolsizeMap.put(config.getId(), maximumPoolSize);
            managePoolForConfiguration(config);
        }
        System.out.println("Count Hikari Pools: " + configurationService.getActivePools().size());
        for (Map.Entry<Long, HikariDataSource> entry : configurationService.getActivePools().entrySet()) {
            HikariDataSource dataSource = entry.getValue();
            String poolName = dataSource.getPoolName();
            System.out.println("Pool ID: " + entry.getKey() + ", Pool Name: " + poolName);
        }
    }

    /**
     * Start or stop a HikariCP connection pool based on the 'Is_Monitoring' flag.
     */
    public void managePoolForConfiguration(Configuration config) {
        logger.info("managePoolForConfiguration(Configuration config) : manage pool of"+ config.getUserName());
        if (config.getIsMonitoring() == 1) {
          configurationService.startPool(config);
        } else {
            configurationService.stopPool(config.getId());
        }
    }

    private void allBackGroundCronStart() {
        logger.info("allBackGroundCronStart(): all background job schedule");
        List<Configuration> configList = configurationService.findMessageConfigurations();
        List<Configuration> monitoringConfigs = configList.stream()
                .filter(config -> config.getIsMonitoring() != null && config.getIsMonitoring() == 1)
                .collect(Collectors.toList());
        //  for(Configuration configuration)
        // Fetch email configurations
        for(Configuration configuration : monitoringConfigs) {
            try {
                scheduleBackgroundJob(configuration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void allMailboxWatchdogCronStart() {
        logger.info("allMailboxWatchdogCronStart(): all watchdog job schedule");
        List<Configuration> configList = configurationService.findMessageConfigurations();
        List<Configuration> monitoringConfigs = configList.stream()
                .filter(config -> config.getIsWatchdog() != null && config.getIsWatchdog() == 1)
                .collect(Collectors.toList());
        //  for(Configuration configuration)
        // Fetch email configurations


        for(Configuration configuration : monitoringConfigs) {
            try {
                MonitorAlerting monitorAlerting = mailboxService.fetchEmailConfiguration(configuration);
                if(monitorAlerting.getIsMBWatchdogActive() != null && monitorAlerting.getIsMBWatchdogActive() == 1) {
                    scheduleMBWatchdogJob(configuration);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void allMonitorCronStart() {
        logger.info("allBackGroundCronStart(): all Email Monitor job schedule");
        List<Configuration> configList = configurationService.findMessageConfigurations();
        List<Configuration> monitoringConfigs = configList.stream()
                .filter(config -> config.getIsMonitoring() != null && config.getIsMonitoring() == 1)
                .collect(Collectors.toList());
        //  for(Configuration configuration)
        // Fetch email configurations
        for(Configuration configuration : monitoringConfigs) {
            try {
                cockpitService.createFvmMonitorAlertingTable(configuration);
                cockpitService.deleteLastAlertTimeInDatabase(configuration);
                scheduleEmailMonitorJob(configuration);
            } catch (Exception e) {
                //        JobManagerView.allCronButton.setText("Cron Start");
                e.printStackTrace();
             //   Notification.show("Error executing job: " + configuration.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }
    }

    public void scheduleEmailMonitorJob(Configuration configuration) throws SchedulerException {
        logger.info("scheduleEmailMonitorJob(Configuration configuration): start emailAlert job schedule");
        // Fetch monitorAlerting configuration to get the interval
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);
        if(monitorAlerting.getIsActive() != null && monitorAlerting.getIsActive() == 1) {
            System.out.println("configuration......"+configuration.getName());
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            //  notifySubscribers(",,"+jobManager.getId());
            JobDataMap jobDataMap = new JobDataMap();
            try {
                jobDataMap.put("configuration", JobDefinitionUtils.serializeJobDefinition(configuration));
            } catch (JsonProcessingException e) {
                Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                return;
            }

            jobDataMap.put("startType", "cron");

            JobDetail jobDetail = JobBuilder.newJob(EmailMonitorJobExecutor.class)
                    .withIdentity("job-alert-cron-" + configuration.getId(), "Email_group")
                    .usingJobData(jobDataMap)
                    .build();


            if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
                System.out.println("No interval set for the configuration. Job will not be scheduled.");
                return;
            }

        //    int interval = monitorAlerting.getIntervall(); // assuming this returns the interval in minutes
            String cronExpression = monitorAlerting.getBgCron() ;

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-alert-cron-" + configuration.getId(), "Email_group")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .forJob(jobDetail)
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    public void scheduleBackgroundJob(Configuration configuration) throws SchedulerException {
        logger.info("scheduleBackgroundJob(Configuration configuration): start background job schedule");
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);
        cockpitService.updateIsBackJobActive(1, configuration);
//        if(monitorAlerting.getIsBackJobActive() != null && monitorAlerting.getIsBackJobActive() == 1) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDataMap jobDataMap = new JobDataMap();
            try {
                jobDataMap.put("configuration", JobDefinitionUtils.serializeJobDefinition(configuration));
            } catch (JsonProcessingException e) {
                Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                return;
            }

            jobDataMap.put("startType", "cron");

            JobDetail jobDetail = JobBuilder.newJob(BackgroundJobExecutor.class)
                    .withIdentity("job-background-cron-" + configuration.getId(), "Chek_group")
                    .usingJobData(jobDataMap)
                    .build();

            if (monitorAlerting == null || monitorAlerting.getBgCron() == null) {
                System.out.println("No interval set for the configuration. Job will not be scheduled.");
                return;
            }

            String cronExpression = monitorAlerting.getBgCron();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-background -cron-" + configuration.getId(), "Chek_group")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .forJob(jobDetail)
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
       // }
    }

    private String createCronExpression(int interval) {
        // Cron expression format for every N minutes
        return "0 0/" + interval + " * * * ?";
    }
    private void  allCronJobStart(){
        logger.info("allCronJobStart(): all jobmanager jobs schedule");
        System.out.println("all cron start");
        jobDefinitionService = SpringContextHolder.getBean(JobDefinitionService.class);

        //  List<JobManager> jobManagerList = jobDefinitionService.findAll();
        List<JobManager> filterJobsList = jobDefinitionService.getFilteredJobManagers();
        JobManagerView.notifySubscribers("start running all...");
        for (JobManager jobManager : filterJobsList) {
            try {
                if(jobManager.getAktiv() == 1) {
                    String type = jobManager.getTyp();
                    if (jobManager.getCron() != null && !type.equals("Node") ) {
                        scheduleJob(jobManager,  Constants.CRON);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                //        JobManagerView.allCronButton.setText("Cron Start");
            //    Notification.show("Error executing job: " + jobManager.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }
    }
    public int countJobChainChildren(int jobId) {
        Map<Integer, JobManager> jobManagerMap = jobDefinitionService.getJobManagerMap();
        JobManager jobManager = jobManagerMap.get(jobId);
        List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);
        int count = childJobs.size();
        for (JobManager child :childJobs) {
            count += countJobChainChildren(child.getId());
        }
        return count;
    }
    public void scheduleJob(JobManager jobManager, String startType) throws SchedulerException {
        logger.info(" scheduleJob(JobManager jobManager, String startType): jobmanager job schedule");
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        //  notifySubscribers(",,"+jobManager.getId());
        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("jobManager", JobDefinitionUtils.serializeJobDefinition(jobManager));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("startType", startType);

        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
                .withIdentity("job-cron-" + jobManager.getId(), "group1")
                .usingJobData(jobDataMap)
                .build();

        // Using the cron expression from the JobManager
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-cron-" + jobManager.getId(), "group1")
                .withSchedule(CronScheduleBuilder.cronSchedule(jobManager.getCron()))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
    public void scheduleMBWatchdogJob(Configuration configuration) throws SchedulerException {
        logger.info("scheduleMBWatchdogJob(Configuration configuration): start watchdog job schedule");
        MailboxWatchdogJobExecutor.stopJob = false;
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("configuration", JobDefinitionUtils.serializeJobDefinition(configuration));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("startType", "cron");
        // Fetch monitorAlerting configuration to get the interval
        MonitorAlerting monitorAlerting = mailboxService.fetchEmailConfiguration(configuration);

        jobDataMap.put("startType", "cron");
        jobDataMap.put("monitorAlerting", monitorAlerting);

        JobDetail jobDetail = JobBuilder.newJob(MailboxWatchdogJobExecutor.class)
                .withIdentity("job-mbWatchdog-cron-" + configuration.getId(), "mbWatchdog_group")
                .usingJobData(jobDataMap)
                .build();

        if (monitorAlerting == null || monitorAlerting.getMbWatchdogCron() == null) {
            System.out.println("No interval set for the configuration. Job will not be scheduled.");
            return;
        }

        String cronExpression = monitorAlerting.getMbWatchdogCron();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-mbWatchdog -cron-" + configuration.getId(), "mbWatchdog_group")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

}
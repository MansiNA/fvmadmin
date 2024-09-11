package com.example.application.service;

import com.example.application.data.entity.JobManager;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.utils.Constants;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.example.application.utils.LogPannel;
import com.example.application.views.JobManagerView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.function.SerializableConsumer;
import lombok.Getter;
import lombok.Setter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Getter
@Setter
@Service
public class JobSchedulerService {
    private Scheduler scheduler;
    private JobDefinitionService jobDefinitionService;
  //  private SubscriptionService subscriptionService;
    public boolean isContinueChildJob = false;
    public boolean isJobChainRunning = false;
    public int jobChainId;
    public int chainCount = 0;
 //   LogPannel logPannel;


    public JobSchedulerService( JobDefinitionService jobDefinitionService) {
     //   this.scheduler = scheduler;
        this.jobDefinitionService = jobDefinitionService;
       // this.subscriptionService = subscriptionService;
    }

    public void getDefaultScheduler() {
    //    logPannel = JobManagerView.logPannel;
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void scheduleJobWithoutCorn(JobManager jobManager) throws SchedulerException {
   //     logPannel.logMessage(Constants.INFO, "Starting scheduleJob manualy for " + jobManager.getName());
        getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("jobManager", JobDefinitionUtils.serializeJobDefinition(jobManager));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("startType", "manual");

        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
                .withIdentity("job-manual-" + jobManager.getId(), "group1")
                .usingJobData(jobDataMap)
                .build();

        // Create a trigger to run immediately
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-manual-" + jobManager.getId(), "group1")
                .startNow() // Trigger will start immediately
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
   //     logPannel.logMessage(Constants.INFO, "Ending scheduleJob manualy for " + jobManager.getName());
    }



    public boolean isJobRunning(int jobId) throws SchedulerException {
        JobKey jobKey = new JobKey("job-" + jobId, "group1");
        for (JobExecutionContext jobExecutionContext : scheduler.getCurrentlyExecutingJobs()) {
            if (jobExecutionContext.getJobDetail().getKey().equals(jobKey)) {
                return true;
            }
        }
        return false;
    }

    public void triggerChildJobs(int jobId) {
        Map<Integer, JobManager> jobManagerMap = jobDefinitionService.getJobManagerMap();
        JobManager jobManager = jobManagerMap.get(jobId);
    //    logPannel.logMessage(Constants.INFO, "Starting stopJob for " + jobManager.getName());
        if (jobManager != null) {
            if(isJobChainRunning && chainCount == 0) {
                isJobChainRunning = false;
                JobManagerView.notifySubscribers("Jobchain execution done,," + jobChainId);
            }
            // Execute child jobs for Jobchain type without checking the exit code
            if ("Jobchain".equals(jobManager.getTyp())) {
                List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);
                for (JobManager childJob : childJobs) {
                    try {
                        System.out.println("jobchain.........+++++++++"+childJob.getName());
                        scheduleJobWithoutCorn(childJob);
                        JobManagerView.notifySubscribers(",," + childJob.getId());
                    } catch (SchedulerException e) {
                        System.out.println("Error scheduling child job (Jobchain): " + childJob.getName() + " - " + e.getMessage());
                    }
                }
            }
            // Execute child jobs for other types only if exit code is 0
            else if (isContinueChildJob && jobManager.getExitCode() != null && jobManager.getExitCode() == 0) {
                List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);

                for (JobManager childJob : childJobs) {
                    try {
                        scheduleJobWithoutCorn(childJob);
                        JobManagerView.notifySubscribers(",," + childJob.getId());
                    } catch (SchedulerException e) {
                        System.out.println("Error scheduling child job: " + childJob.getName() + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    public boolean isAnyCronJobRunning() {
        try {
            Map<Integer, JobManager> jobManagerMap = jobDefinitionService.getJobManagerMap();
            List<JobManager> cronJobManagers = jobManagerMap.values().stream()
                    .filter(jobManager -> jobManager.getCron() != null) // Check that cron is not null
                    .collect(Collectors.toList());
            for (JobManager jobManager : cronJobManagers) {
                JobKey cronJobKey = new JobKey("job-cron-" + jobManager.getId(), "group1");
                if (scheduler.checkExists(cronJobKey)) {
                    return true;
                }
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            // Handle the exception appropriately
        }
        return false;
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

}

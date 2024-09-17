package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.CronInfo;
import com.example.application.data.entity.JobManager;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.utils.Constants;
import com.example.application.utils.LogPannel;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;

@PageTitle("Cron Infoview")
@Route(value = "cronInfo", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FVM"})
public class CronInfoView extends VerticalLayout {

    private JobDefinitionService jobDefinitionService;
    private ConfigurationService configurationService;
    private Grid<CronInfo> grid = new Grid<>();
    private Scheduler scheduler;
    private LogPannel logPannel;
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;


    public CronInfoView(JobDefinitionService jobDefinitionService, ConfigurationService configurationService) {

        this.jobDefinitionService = jobDefinitionService;
        this.configurationService = configurationService;

        logPannel = new LogPannel();
        logPannel.logMessage(Constants.INFO, "Starting CronInfoView");

        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        HorizontalLayout hl = new HorizontalLayout(new H2("Cron Infoview"));
        hl.setAlignItems(Alignment.BASELINE);
        add(hl);

        add(hl,createTreeGrid());
        logPannel.setVisible(false);
        add(logPannel);

        if (MainLayout.isAdmin) {

            UI.getCurrent().addShortcutListener(
                    () -> {
                        isLogsVisible = !isLogsVisible;
                        logPannel.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.CONTROL);
        }

        logPannel.logMessage(Constants.INFO, "Ending CronInfoView");
    }
    private Grid<CronInfo> createTreeGrid() {
        logPannel.logMessage(Constants.INFO, "Starting createTreeGrid");

        updateGrid();

        grid.addColumn(CronInfo::getJobName).setHeader("Job Name").setAutoWidth(true).setResizable(true);
        grid.addColumn(CronInfo::getJobGroup).setHeader("Job Group").setWidth("120px").setResizable(true);
        grid.addColumn(CronInfo::getNextFireTime).setHeader("Next FireTime").setAutoWidth(true).setResizable(true);

        grid.setThemeName("dense");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);


        logPannel.logMessage(Constants.INFO, "Ending createTreeGrid");
        return grid;
    }

    private void updateGridold() {
        logPannel.logMessage(Constants.INFO, "updateGrid() for Updating TreeGrid");
        List<JobManager> runningJobs = new ArrayList<>();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                System.out.println("--------------"+groupName);
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                    String jobName = jobKey.getName();

                    String jobGroup = jobKey.getGroup();

                    List<Trigger> triggers = (List<Trigger>) scheduler

                            .getTriggersOfJob(jobKey);
                    Date nextFireTime = triggers.get(0).getNextFireTime();
                    System.out.println("[jobName] : " + jobName + " [groupName] : " + jobGroup + " - " + nextFireTime);
                }
            }
        } catch (Exception e) {

        }
       // treeGrid.setItems(runningJobs, jobDefinitionService ::getChildJobManager);

    }
    private void updateGrid() {
        logPannel.logMessage(Constants.INFO, "updateGrid() for Updating TreeGrid");
        List<CronInfo> runningJobs = new ArrayList<>();

        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                    String jobId = jobKey.getName();
                    String jobGroup = jobKey.getGroup();

                    // Get triggers for the job
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);

                    // Fetch next fire time of the first trigger (if available)
                    Date nextFireTime = (triggers != null && !triggers.isEmpty()) ? triggers.get(0).getNextFireTime() : null;

                    System.out.println("[jobName] : " + jobId + " [groupName] : " + jobGroup + " - Next Fire Time: " + nextFireTime);

                    if (jobId.contains("alert")) {
                        jobId = jobId.replace("job-alert-cron-", "");
                        Configuration configuration = configurationService.findByIdConfiguration(Long.valueOf(jobId));
                        if (configuration != null) {
                            runningJobs.add(new CronInfo(configuration.getName(), jobGroup, nextFireTime));
                            } else {
                                System.out.println("JobManager not found for job: " + configuration.getName());
                            }

                    } else {
                        jobId = jobId.replace("job-cron-", "");
                        JobManager jobManager = jobDefinitionService.getJobManagerById(Integer.valueOf(jobId));
                        if (jobManager != null) {
                            runningJobs.add(new CronInfo(jobManager.getName(), jobGroup, nextFireTime));
                        } else {
                            System.out.println("JobManager not found for job: " + jobManager.getName());
                        }
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            logPannel.logMessage(Constants.ERROR, "Error in updateGrid: " + e.getMessage());
        }

        // Update the grid with running jobs
        grid.setItems(runningJobs);
    }

}

package com.example.application.views;

import com.example.application.data.entity.JobManager;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.service.EmailService;
import com.example.application.service.JobSchedulerService;
import com.example.application.utils.Constants;
import com.example.application.utils.LogPannel;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PageTitle("Cron Infoview")
@Route(value = "cronInfo", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FVM"})
public class CronInfoView extends VerticalLayout {

    private JobDefinitionService jobDefinitionService;
    private ConfigurationService configurationService;
    private JobSchedulerService jobSchedulerService;
    private Crud<JobManager> crud;
    private Grid<JobManager> jobDefinitionGrid;
    private Dialog dialog;
    private Dialog resetPasswordDialog;
    private Grid<JobManager> grid = new Grid<>();
    private Scheduler scheduler;
    public Button allCronButton = new Button("Cron Start");
    // private Button allStopButton = new Button("All Stop");
    // private Button sendMailButton = new Button("Testmail");

    private static final Set<SerializableConsumer<String>> subscribers = new HashSet<>();
    private static final Set<SerializableConsumer<String>> start_subscribers = new HashSet<>();
    private static final ExecutorService notifierThread = Executors.newSingleThreadExecutor();
    private static final ExecutorService startnotifierThread = Executors.newSingleThreadExecutor();
    private SerializableConsumer<String> subscriber;
    Map<Integer, Button> startButtons = new HashMap<>();
    Map<Integer, Button> stopButtons = new HashMap<>();
    List<JobManager> listOfJobManager;
    List<JobManager> listOfGridItem;
    static boolean isContinueChildJob = false;
    static boolean isJobChainRunning = false;
    static int jobChainId;
    static int chainCount = 0;
    private LogPannel logPannel;
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;

    Button menuButton = new Button("Show/Hide Columns");

    public CronInfoView(JobDefinitionService jobDefinitionService, ConfigurationService configurationService, JobSchedulerService jobSchedulerService) {

        this.jobDefinitionService = jobDefinitionService;
        this.configurationService = configurationService;
        this.jobSchedulerService = jobSchedulerService;

        allCronButton.setVisible(false);

        logPannel = new LogPannel();
        logPannel.logMessage(Constants.INFO, "Starting CronInfoView");

        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        HorizontalLayout hl = new HorizontalLayout(new H2("Cron Infoview"), menuButton);
        hl.setAlignItems(Alignment.BASELINE);
        add(hl);

        if (MainLayout.isAdmin) {
            allCronButton.setVisible(true);
        }



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
    private Grid<JobManager> createTreeGrid() {
        logPannel.logMessage(Constants.INFO, "Starting createTreeGrid");

        updateGrid();
        // Add the hierarchy column for displaying the hierarchical data
        grid.addColumn(JobManager::getName).setHeader("Name").setAutoWidth(true).setResizable(true);

        // Add other columns except id and pid
        TreeGrid.Column<JobManager> namespaceColumn = grid.addColumn(JobManager::getNamespace).setHeader("Namespace").setAutoWidth(true).setResizable(true);
        grid.addColumn(JobManager::getCommand).setHeader("Command").setWidth("120px").setResizable(true);
        grid.addColumn(JobManager::getCron).setHeader("Cron").setAutoWidth(true).setResizable(true);
        grid.addColumn(JobManager::getTyp).setHeader("Typ").setAutoWidth(true).setResizable(true);
        grid.addColumn(JobManager::getParameter).setHeader("Parameter").setWidth("100px").setResizable(true);
        grid.addColumn(JobManager::getAktiv).setHeader("Aktiv").setAutoWidth(true).setResizable(true);

        Grid.Column<JobManager> scriptPathColumn = grid.addColumn(JobManager::getScriptpath)
                .setHeader("ScriptPath").setAutoWidth(true).setResizable(true);

        Grid.Column<JobManager> mailBetreffColumn = grid.addColumn(JobManager::getMailBetreff)
                .setHeader("MailBetreff").setAutoWidth(true).setResizable(true);

        Grid.Column<JobManager> mailTextColumn = grid.addColumn(JobManager::getMailText)
                .setHeader("MailText").setAutoWidth(true).setResizable(true);

        Grid.Column<JobManager> mailEmpfaengerColumn = grid.addColumn(JobManager::getMailEmpfaenger)
                .setHeader("MAIL_EMPFAENGER").setAutoWidth(true).setResizable(true);

        Grid.Column<JobManager> mailCcEmpfaengerColumn = grid.addColumn(JobManager::getMailCcEmpfaenger)
                .setHeader("MAIL_CC_EMPFAENGER").setAutoWidth(true).setResizable(true);

        Grid.Column<JobManager> verbindungColumn = grid.addColumn(jobManager -> {
            // Retrieve the connection ID from the Configuration object
            return jobManager.getConnection() != null ? jobManager.getConnection().getName() : "N/A";
        }).setHeader("Verbindung").setAutoWidth(true).setResizable(true);

        grid.setThemeName("dense");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);

        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        scriptPathColumn.setVisible(false);
        mailBetreffColumn.setVisible(false);
        mailTextColumn.setVisible(false);
        mailEmpfaengerColumn.setVisible(false);
        mailCcEmpfaengerColumn.setVisible(false);
        verbindungColumn.setVisible(false);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(menuButton);
        columnToggleContextMenu.addColumnToggleItem("Namespace", namespaceColumn);
        columnToggleContextMenu.addColumnToggleItem("ScriptPath", scriptPathColumn);
        columnToggleContextMenu.addColumnToggleItem("MailBetreff", mailBetreffColumn);
        columnToggleContextMenu.addColumnToggleItem("MailText", mailTextColumn);
        columnToggleContextMenu.addColumnToggleItem("MAIL_EMPFAENGER", mailEmpfaengerColumn);
        columnToggleContextMenu.addColumnToggleItem("MAIL_CC_EMPFAENGER", mailCcEmpfaengerColumn);
        columnToggleContextMenu.addColumnToggleItem("Verbindung", verbindungColumn);

        logPannel.logMessage(Constants.INFO, "Ending createTreeGrid");
        return grid;
    }
    private static class ColumnToggleContextMenu extends ContextMenu {
        public ColumnToggleContextMenu(Component target) {
            super(target);
            setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<JobManager> column) {
            MenuItem menuItem = this.addItem(label, e -> {
                column.setVisible(e.getSource().isChecked());
            });
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
        }
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
        List<JobManager> runningJobs = new ArrayList<>();

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

                    jobId = jobId.replace("job-cron-", "");
                    // Assuming you have a method to fetch the JobManager by name
                    JobManager jobManager = jobDefinitionService.getJobManagerById(Integer.valueOf(jobId));
                    if (jobManager != null) {
                        runningJobs.add(jobManager);
                    } else {
                        System.out.println("JobManager not found for job: " + jobManager.getName());
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

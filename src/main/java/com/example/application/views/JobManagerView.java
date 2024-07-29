package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.JobManager;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.utils.Constants;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.example.application.utils.LogPannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@PageTitle("Job Manager")
@Route(value = "jobManager", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class JobManagerView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${script.path}")
    private String scriptPath;
    private JobDefinitionService jobDefinitionService;
    private ConfigurationService configurationService;
    private Crud<JobManager> crud;
    private Grid<JobManager> jobDefinitionGrid;
    private Dialog dialog;
    private Dialog resetPasswordDialog;
    private TreeGrid<JobManager> treeGrid;
    private Scheduler scheduler;
    private Button allStartButton = new Button("All Start");
    private Button allStopButton = new Button("All Stop");
    private final UI ui;
    private static final Set<SerializableConsumer<String>> subscribers = new HashSet<>();
    private static final Set<SerializableConsumer<String>> start_subscribers = new HashSet<>();
    private static final ExecutorService notifierThread = Executors.newSingleThreadExecutor();
    private static final ExecutorService startnotifierThread = Executors.newSingleThreadExecutor();
    private SerializableConsumer<String> subscriber;
    Map<Integer, Button> startButtons = new HashMap<>();
    Map<Integer, Button> stopButtons = new HashMap<>();
    List<JobManager> listOfJobManager;
    boolean isContinueChildJob = false;
    boolean isJobChainRunning = false;
    private int jobChainId;
    private int chainCount = 0;
    private LogPannel logPannel;
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;

    public JobManagerView(JobDefinitionService jobDefinitionService, ConfigurationService configurationService) {

        this.jobDefinitionService = jobDefinitionService;
        this.configurationService = configurationService;

        logPannel = new LogPannel();
        logPannel.logMessage(Constants.INFO, "Starting JobManagerView");

        addAttachListener(event -> updateJobManagerSubscription());
        addDetachListener(event -> updateJobManagerSubscription());

        this.ui = UI.getCurrent();

        HorizontalLayout hl = new HorizontalLayout(new H2("Job Manager"),  allStartButton, allStopButton);
        hl.setAlignItems(Alignment.BASELINE);
        add(hl);

        allStopButton.setEnabled(false);

        HorizontalLayout treehl = new HorizontalLayout();
        treehl.setHeightFull();
        treehl.setWidthFull();
        treehl.setSizeFull();

        TreeGrid tg= createTreeGrid();

        treehl.add(tg);
        treehl.setFlexGrow(1, tg);

        //  treehl.setWidthFull();
        treehl.setAlignItems(Alignment.BASELINE);
        setHeightFull();
        setSizeFull();

        add(treehl);
        logPannel.setVisible(false);
        add(logPannel);

        if(MainLayout.isAdmin) {

            UI.getCurrent().addShortcutListener(
                    () -> {
                        isLogsVisible = !isLogsVisible;
                        logPannel.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.CONTROL);
        }

        logPannel.logMessage(Constants.INFO, "Ending JobManagerView");
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
    //    restoreGlobalButtonStates();
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void restoreGlobalButtonStates() {
        // Restore button states from the UI instance
        Boolean allStartEnabled = (Boolean) ui.getSession().getAttribute("allStartEnabled");
        Boolean allStopEnabled = (Boolean) ui.getSession().getAttribute("allStopEnabled");

        if (allStartEnabled != null) {
            allStartButton.setEnabled(allStartEnabled);
        }

        if (allStopEnabled != null) {
            allStopButton.setEnabled(allStopEnabled);
        }
    }

    private TreeGrid<JobManager> createTreeGrid() {
        logPannel.logMessage(Constants.INFO, "Starting createTreeGrid");
        treeGrid = new TreeGrid<>();
        updateGrid();

        // Add the hierarchy column for displaying the hierarchical data
        treeGrid.addHierarchyColumn(JobManager::getName).setHeader("Name").setAutoWidth(true);

        // Add other columns except id and pid
        treeGrid.addColumn(JobManager::getNamespace).setHeader("Namespace").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getCommand).setHeader("Command").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getCron).setHeader("Cron").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getTyp).setHeader("Typ").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getParameter).setHeader("Parameter").setAutoWidth(true);
        treeGrid.addColumn(jobManager -> {
            // Retrieve the connection ID from the Configuration object
            return jobManager.getConnection() != null ? jobManager.getConnection().getName() : "N/A";
        }).setHeader("Verbindung").setAutoWidth(true);

      //  treeGrid.setWidth("350px");
        treeGrid.addExpandListener(event -> System.out.println(String.format("Expanded %s item(s)", event.getItems().size())));
        treeGrid.addCollapseListener(event -> System.out.println(String.format("Collapsed %s item(s)", event.getItems().size())));

        treeGrid.setThemeName("dense");
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);

        treeGrid.addComponentColumn(jobManager -> {
            // Create a layout to hold the buttons for each row
            HorizontalLayout buttonsLayout = new HorizontalLayout();
            if (!jobManager.getTyp().equals("Node")) {
                // Instantiate new buttons for each row
                Button startBtn = new Button("Start");
                Button stopBtn = new Button("Stop");

                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);

                // Retrieve session attributes for this row's buttons
                Boolean startBtnEnabled = (Boolean) ui.getSession().getAttribute("startBtnEnabled_" + jobManager.getId());
                Boolean stopBtnEnabled = (Boolean) ui.getSession().getAttribute("stopBtnEnabled_" + jobManager.getId());

                if (startBtnEnabled != null) {
                    startBtn.setEnabled(startBtnEnabled);
                }
                if (stopBtnEnabled != null) {
                    stopBtn.setEnabled(stopBtnEnabled);
                }

                restoreGlobalButtonStates();

                startButtons.put(jobManager.getId(), startBtn);
                stopButtons.put(jobManager.getId(), stopBtn);
                //     System.out.println("...........................................xxxxxx "+ startButtons.size() + " xxxxxxx.........................................");
                // Add click listeners for the buttons
                startBtn.addClickListener(event -> {
                    try {

                        List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);
                        boolean hasChildren = !childJobs.isEmpty();
                        if (hasChildren) {
                            if (jobManager.getTyp().equals("Jobchain")) {
                                jobChainId = jobManager.getId();
                                isJobChainRunning = true;
                                isContinueChildJob = true;
                                chainCount = countJobChainChildren(jobManager.getId());
                                triggerChildJobs(jobManager.getId());
                                notifySubscribers(",," + jobManager.getId());
                            } else {
                                showJobDialog(jobManager);
                            }
                        } else {
                            executeJob(jobManager);
                        }
                    } catch (Exception e) {
                        Notification.show("Error starting job: " + jobManager.getName() + " - " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                    }
                });

                stopBtn.addClickListener(event -> {
                    stopJob(  jobManager);
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    // Update session attributes when button states change
//                ui.getSession().setAttribute("startBtnEnabled_" + jobManager.getId(), true);
//                ui.getSession().setAttribute("stopBtnEnabled_" + jobManager.getId(), false);
                });

                // Add buttons to the layout
                buttonsLayout.add(startBtn, stopBtn);
            }

            return buttonsLayout;
        }).setHeader("Actions").setAutoWidth(true);


        updateJobManagerSubscription();
//        treeGrid.asSingleSelect().addValueChangeListener(event -> {
//            JobManager selectedJob = event.getValue();
//            if (selectedJob != null) {
//                try {
//                    scheduleJob(selectedJob);
//                } catch (SchedulerException e) {
//                    e.getMessage();
//                    Notification.show("job running error", 5000, Notification.Position.MIDDLE);
//                }
//            }
//        });

        allStartButton.addClickListener(event -> {

            List<JobManager> jobManagerList = jobDefinitionService.findAll();
         //   Notification.show("start running...", 3000, Notification.Position.MIDDLE);
            allStartButton.setEnabled(false);
            allStopButton.setEnabled(true);
//            ui.getSession().setAttribute("allStartEnabled", false);
//            ui.getSession().setAttribute("allStopEnabled", true);
            notifySubscribers("start running all...");
            for (JobManager jobManager : jobManagerList) {
                try {
                    String type = jobManager.getTyp();
                    if(jobManager.getCron() != null && !type.equals("Node") && !type.equals("Jobchain")) {
                        scheduleJob(jobManager);
                    }
                } catch (Exception e) {
                    Notification.show("Error executing job: " + jobManager.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            }

        });

        allStopButton.addClickListener(event -> {
            List<JobManager> jobManagerList = jobDefinitionService.findAll();
            Notification.show("stop running...", 3000, Notification.Position.MIDDLE);
            allStartButton.setEnabled(true);
            allStopButton.setEnabled(false);
//            ui.getSession().setAttribute("allStartEnabled", true);
//            ui.getSession().setAttribute("allStopEnabled", false);
            for (JobManager jobManager : jobManagerList) {
                try {
                    if(jobManager.getCron() != null) {
                        stopJob(jobManager);
                    }
                } catch (Exception e) {
                    Notification.show("Error stopping job: "+ jobManager.getName()+" " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            }
        });

         if (MainLayout.isAdmin) {
             GridContextMenu<JobManager> contextMenu = treeGrid.addContextMenu();
             GridMenuItem<JobManager> editItem = contextMenu.addItem("Edit", event -> {
                 showEditAndNewDialog(event.getItem().get(), "Edit");
             });
             GridMenuItem<JobManager> newItem = contextMenu.addItem("New", event -> {
                 showEditAndNewDialog(event.getItem().get(), "New");
             });
             GridMenuItem<JobManager> deleteItem = contextMenu.addItem("Delete", event -> {
                 deleteTreeGridItem(event.getItem().get());
             });
         }
        logPannel.logMessage(Constants.INFO, "Ending createTreeGrid");
        return treeGrid;
    }


    private void expandAll(List<JobManager> rootItems) {
        for (JobManager rootItem : rootItems) {
            treeGrid.expand(rootItem);
            List<JobManager> childItems = jobDefinitionService.getChildJobManager(rootItem);
            expandAll(childItems);
        }
    }

    private void showJobDialog(JobManager jobManager) {
        logPannel.logMessage(Constants.INFO, "Starting showJobDialog for new, edit and delete");
        Dialog dialog = new Dialog();
        dialog.add(new Text(" Do you want to execute only this job or the entire job chain?"));

        Button onlyThisJobButton = new Button("Only this job", event -> {
            isContinueChildJob = false;
            executeJob(jobManager);
            dialog.close();
        });

        Button entireChainButton = new Button("Entire job chain", event -> {
            executeJobChain(jobManager);
            dialog.close();
        });

        HorizontalLayout dialogButtons = new HorizontalLayout(onlyThisJobButton, entireChainButton);
        dialog.add(dialogButtons);
        dialog.open();
        logPannel.logMessage(Constants.INFO, "Ending showJobDialog for new, edit and delete");
    }

    private void executeJob(JobManager jobManager) {
        try {
            scheduleJobWithoutCorn(jobManager);

            notifySubscribers(",," + jobManager.getId());

        } catch (Exception e) {
            Notification.show("Error starting job: " + jobManager.getName() + " - " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void executeJobChain(JobManager jobManager) {
        Notification.show("Executing job chain starting with: " + jobManager.getName());
        isContinueChildJob = true;
        executeJob(jobManager);

//        int exitCode = jobManager.getExitCode();
//        if (exitCode == 0) {
//            List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);
//            for (JobManager childJobManager : childJobs) {
//                executeJobChain(childJobManager);
//            }
//        }
    }

    private VerticalLayout showEditAndNewDialog(JobManager jobManager, String context){
        logPannel.logMessage(Constants.INFO, "Starting showEditAndNewDialog");
        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        JobManager newJobDefination = new JobManager();

        if(context.equals("New")){
            List<JobManager> jobManagerList = jobDefinitionService.findAll();
        //    newJobDefination.setId( (jobManagerList.size() + 1));
            newJobDefination.setPid(jobManager.getPid());
            dialog.add(editJobDefinition(newJobDefination, true)); // For adding new entry
        } else {
            dialog.add(editJobDefinition(jobManager, false)); // For editing existing entry
        }

        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1000px");
        dialog.setHeight("400px");
        Button cancelButton = new Button("Cancel");
        Button saveButton = new Button(context.equals("Edit") ? "Save" : "Add");
        dialog.getFooter().add(saveButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        saveButton.addClickListener(saveEvent -> {
            if(context.equals("New")) {
                saveSqlDefinition(newJobDefination);
            } else {
                saveSqlDefinition(jobManager);
            }
            updateGrid();
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();
        logPannel.logMessage(Constants.INFO, "Ending showEditAndNewDialog");
        return dialogLayout;

    }
    public JobManager saveSqlDefinition(JobManager jobManager) {
        logPannel.logMessage(Constants.INFO,  jobManager.getName() +" save in DB");
        return jobDefinitionService.save(jobManager);
    }

    private Component editJobDefinition(JobManager jobManager, boolean isNew) {
        logPannel.logMessage(Constants.INFO, "Starting editJobDefinition");
        VerticalLayout content = new VerticalLayout();

        // Create and initialize fields

        IntegerField id = new IntegerField("ID");
        id.setValue(jobManager.getId() != 0 ? jobManager.getId() : 0);
        id.setWidthFull();
        id.setReadOnly(true);

        TextField name = new TextField("NAME");
        name.setValue(isNew ? "" : (jobManager.getName() != null ? jobManager.getName() : ""));
        name.setWidthFull();

        TextField namespace = new TextField("NAMESPACE");
        namespace.setValue(isNew ? "" : (jobManager.getNamespace() != null ? jobManager.getNamespace() : ""));
        namespace.setWidthFull();

        TextField command = new TextField("COMMAND");
        command.setValue(isNew ? "" : (jobManager.getCommand() != null ? jobManager.getCommand() : ""));
        command.setWidthFull();

        TextField cron = new TextField("CRON");
        cron.setValue(isNew ? "" : (jobManager.getCron() != null ? jobManager.getCron() : ""));
        cron.setWidthFull();

//        TextField typ = new TextField("TYP");
//        typ.setValue(isNew ? "" : (jobManager.getTyp() != null ? jobManager.getTyp() : ""));
//        typ.setWidthFull();

        TextField parameter = new TextField("PARAMETER");
        parameter.setValue(isNew ? "" : (jobManager.getParameter() != null ? jobManager.getParameter() : ""));
        parameter.setWidthFull();

        IntegerField pid = new IntegerField("PID");
        pid.setValue(jobManager.getPid() != 0 ? jobManager.getPid() : 0);
        pid.setWidthFull();

        List<String> uniqueTyps = jobDefinitionService.getUniqueTypList();

        ComboBox<String> typComboBox = new ComboBox<>("Typ");
        if (uniqueTyps != null && !uniqueTyps.isEmpty()) {
            typComboBox.setItems(uniqueTyps);
            typComboBox.setValue(isNew ? "" : (jobManager.getTyp() != null ? jobManager.getTyp() : ""));
        }

        ComboBox<Configuration> verbindungComboBox = new ComboBox<>("Verbindung");
        verbindungComboBox.setEnabled(false);
        if(jobManager.getTyp().equals("sql_procedure")) {
            verbindungComboBox.setEnabled(true);
        }
        if(jobManager.getTyp().equals("Shell")) {
            verbindungComboBox.isReadOnly();
        }
        try {
            List<Configuration> configList = configurationService.findMessageConfigurations();
            if (configList != null && !configList.isEmpty()) {
                verbindungComboBox.setItems(configList);
                verbindungComboBox.setItemLabelGenerator(Configuration::getName);
                verbindungComboBox.setValue(jobManager.getConnection());
            }

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }



        // Add value change listeners to update the jobDefinition object
        name.addValueChangeListener(event -> jobManager.setName(event.getValue()));
        namespace.addValueChangeListener(event -> jobManager.setNamespace(event.getValue()));
        command.addValueChangeListener(event -> jobManager.setCommand(event.getValue()));
        cron.addValueChangeListener(event -> jobManager.setCron(event.getValue()));
        typComboBox.addValueChangeListener(event -> {
            String selectedTyp = event.getValue();
            typComboBox.setValue(selectedTyp);
            jobManager.setTyp(selectedTyp);
            if ("sql_procedure".equals(selectedTyp)) {
                verbindungComboBox.setEnabled(true);
            } else {
                verbindungComboBox.setEnabled(false);
            }
        });
        parameter.addValueChangeListener(event -> jobManager.setParameter(event.getValue()));
        verbindungComboBox.addValueChangeListener(event -> {
            verbindungComboBox.setValue(event.getValue());
            jobManager.setConnection(event.getValue());
        });
        pid.addValueChangeListener(event -> {
            try {
                if (event.getValue() != 0) {
                    jobManager.setPid(event.getValue());
                } else {
                    jobManager.setPid(0);
                }
            } catch (NumberFormatException e) {
                Notification.show("Invalid PID format", 5000, Notification.Position.MIDDLE);
            }
        });

        // Add all fields to the content layout
        content.add(id, name, namespace, command, cron, typComboBox, parameter , pid, verbindungComboBox);
        logPannel.logMessage(Constants.INFO, "Ending editJobDefinition");
        return content;
    }

    private Component deleteTreeGridItem(JobManager jobManager) {
        logPannel.logMessage(Constants.INFO, "Starting deleteTreeGridItem");
        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("500px");
        dialog.setHeight("150px");
        Button cancelButton = new Button("Cancel");
        Button deleteButton = new Button("Delete");
        Text deleteConfirmationText = new Text("Are you sure you want to delete?");
        dialog.add(deleteConfirmationText);
        dialog.getFooter().add(deleteButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        deleteButton.addClickListener(saveEvent -> {
            jobDefinitionService.deleteById(jobManager.getId());

            updateGrid();
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();
        logPannel.logMessage(Constants.INFO, "Ending deleteTreeGridItem");
        return dialogLayout;
    }

    private void updateGrid() {
        logPannel.logMessage(Constants.INFO, "updateGrid() for Updating TreeGrid");
        listOfJobManager = jobDefinitionService.findAll();
        List<JobManager> rootItems = jobDefinitionService.getRootJobManager();
        treeGrid.setItems(rootItems, jobDefinitionService ::getChildJobManager);
        expandAll(rootItems);
    }

    private void scheduleJob(JobManager jobManager) throws SchedulerException {
        logPannel.logMessage(Constants.INFO, "Starting scheduleJob with cron for " + jobManager.getName());
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
      //  notifySubscribers(",,"+jobManager.getId());
        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("jobManager", JobDefinitionUtils.serializeJobDefinition(jobManager));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("scriptPath", scriptPath);
        jobDataMap.put("startType", "cron");

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
        logPannel.logMessage(Constants.INFO, "Ending scheduleJob with cron for " + jobManager.getName());
    }

    private void scheduleJobWithoutCorn(JobManager jobManager) throws SchedulerException {
        logPannel.logMessage(Constants.INFO, "Starting scheduleJob manualy for " + jobManager.getName());
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("jobManager", JobDefinitionUtils.serializeJobDefinition(jobManager));
        } catch (JsonProcessingException e) {
            Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }

        jobDataMap.put("scriptPath", scriptPath);
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
        logPannel.logMessage(Constants.INFO, "Ending scheduleJob manualy for " + jobManager.getName());
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

    public void stopJob(JobManager jobManager) {
        logPannel.logMessage(Constants.INFO, "Starting stopJob for " + jobManager.getName());
       // JobKey jobKey = new JobKey("job-" + jobManager.getId(), "group1");
        try {
            JobKey cronJobKey = new JobKey("job-cron-" + jobManager.getId(), "group1");
            JobKey manualJobKey = new JobKey("job-manual-" + jobManager.getId(), "group1");
            String typ = jobManager.getTyp();
            // Try stopping cron job
            if (scheduler.checkExists(cronJobKey)) {
                if (scheduler.deleteJob(cronJobKey)) {
                    if(typ.equals("Shell")) {
                        JobExecutor.stopProcess(jobManager.getId());
                    } else if(typ.equals("sql_procedure")) {
                        JobExecutor.stopSQLProcedure(jobManager.getId());
                    }
                    notifySubscribers("Cron job " + jobManager.getName() + " stopped successfully,," + jobManager.getId());
                }
            }

            // Try stopping manual job
            if (scheduler.checkExists(manualJobKey)) {
                if (scheduler.deleteJob(manualJobKey)) {
                    if(typ.equals("Shell")) {
                        JobExecutor.stopProcess(jobManager.getId());
                    } else if(typ.equals("sql_procedure")) {
                        JobExecutor.stopSQLProcedure(jobManager.getId());
                    }
                    notifySubscribers("Manual job " + jobManager.getName() + " stopped successfully,," + jobManager.getId());
                }
            }
        } catch (SchedulerException e) {
            // Handle the exception and add an error message
            logPannel.logMessage(Constants.ERROR, "error while stopJob for " + jobManager.getName());
            notifySubscribers("Error stopping job: " + jobManager.getName() + " - " + e.getMessage()+",,"+jobManager.getId());
        }
        logPannel.logMessage(Constants.INFO, "Ending stopJob for " + jobManager.getName());
    }

    private void executeJobold(JobManager jobManager) {
        System.out.println("Executing job: " + jobManager.getName());

        try {
            switch (jobManager.getTyp()) {
                case "SQL":
                    executeSQLJob(jobManager);
                    break;
                case "Command":
                    executeCommandJob(jobManager);
                    break;
                case "Shell":
                    executeShellJob(jobManager);
                    break;
                default:
                    throw new Exception("Unsupported job type: " + jobManager.getTyp());
            }
        } catch (Exception e) {
            Notification.show("Error executing job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void executeSQLJob(JobManager jobManager) throws Exception {
        // Dummy database connection example
        String jdbcUrl = "jdbc:your_database_url";
        String username = "your_db_username";
        String password = "your_db_password";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(jobManager.getCommand())) {
            while (rs.next()) {
                // Process the result set
                System.out.println(rs.getString(1));
            }
        }
    }

    private void executeCommandJob(JobManager jobManager) throws Exception {
        String command = jobManager.getCommand();
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Command execution failed with exit code " + exitCode);
        }
    }

    private Process process;
    private void executeShellJob(JobManager jobManager) throws Exception {
        //   String scriptPath = "D:\\file\\executer.cmd"; // Absolute path to the script
        String jobName = jobManager.getName();
        String sPath = scriptPath + jobManager.getCommand();


        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Quote the script path to handle spaces and special characters
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "\"" + sPath + "\"", jobName, jobManager.getParameter());
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", "\"" + sPath + "\" " + jobName + " " + jobManager.getParameter());
        }
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();

        // Capture the output
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Shell script execution failed with exit code " + exitCode + "\nOutput:\n" + output);
        }

        // Print the successful output for debugging
        System.out.println("Shell script executed successfully:\n" + output);
    }

    private void updateJobManagerSubscription() {
//        UI ui = getUI().orElse(null);

        if (ui != null) {
            if (subscriber != null) {
                return; // Already subscribed
            }

            subscriber = message -> ui.access(() -> {
                if (!ui.isAttached()) {
                    return; // UI is detached, stop processing
                }
                handleIncomingMessage(ui, message);
            });

            subscribe(subscriber);
        } else {
            unsubscribe();
        }
    }

    private void subscribe(SerializableConsumer<String> subscriber) {
        synchronized (subscribers) {
            subscribers.add(subscriber);
        }
    }

    private void unsubscribe() {
        if (subscriber == null) {
            return; // Already unsubscribed
        }
        synchronized (subscribers) {
            subscribers.remove(subscriber);
        }
        subscriber = null;
    }

    public static void notifySubscribers(String message) {
        Set<SerializableConsumer<String>> subscribersSnapshot;
        synchronized (subscribers) {
            subscribersSnapshot = new HashSet<>(subscribers);
        }

        for (SerializableConsumer<String> subscriber : subscribersSnapshot) {
            notifierThread.execute(() -> {
                try {
                    subscriber.accept(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleIncomingMessage(UI ui, String message) {
        String[] parts = message.split(",,");
        System.out.println(message);
        String displayMessage = parts[0].trim();
        int jobId;
        boolean isCron = false;
        if (parts.length == 2) {
            jobId = Integer.parseInt(parts[1].trim());
        } else if (parts.length == 3) {
            jobId = Integer.parseInt(parts[1].trim());
            isCron = parts[2].trim().contains("cron");
        } else {
            jobId = 0;
        }
        if (displayMessage != null && !displayMessage.isEmpty()) {
            Notification.show(displayMessage, 5000, Notification.Position.MIDDLE);
        }
        updateUIBasedOnMessage(ui, displayMessage, jobId, isCron);
    }

    private void updateUIBasedOnMessage(UI ui, String displayMessage, int jobId, boolean isCron) {
        if (displayMessage.contains("start running all")) {
            updateAllButtonsState(ui, false, true);
        } else if (displayMessage.contains("stopped successfully") || displayMessage.contains("not found running") || displayMessage.contains("Error stopping job")) {
            if(!isAnyCronJobRunning()) {
                updateAllButtonsState(ui, true, false);
            }

            if (jobId != 0) {
                updateJobButtonsState(ui, jobId, true, false);
            }
        }

        if (jobId != 0) {
            if (displayMessage.equals("")) {
                updateJobButtonsState(ui, jobId, false, true);
            } else if (displayMessage.contains("executed successfully") || displayMessage.contains("Error while Job")) {
                if (isCron) {
                    updateJobButtonsState(ui, jobId, true, false);
                } else {
                    chainCount = chainCount -1;
                    updateJobButtonsState(ui, jobId, true, false);
                    triggerChildJobs(jobId);
                }
            } else  if (displayMessage.contains("Jobchain execution done")) {
                updateJobButtonsState(ui, jobId, true, false);
            }
        }
    }

    private void updateAllButtonsState(UI ui, boolean startEnabled, boolean stopEnabled) {
        ui.access(() -> {
            allStartButton.setEnabled(startEnabled);
            allStopButton.setEnabled(stopEnabled);
            ui.getSession().setAttribute("allStartEnabled", startEnabled);
            ui.getSession().setAttribute("allStopEnabled", stopEnabled);
        });
    }

    private void updateJobButtonsState(UI ui, int jobId, boolean startEnabled, boolean stopEnabled) {
        ui.access(() -> {
            Button startBtn = startButtons.get(jobId);
            Button stopBtn = stopButtons.get(jobId);

            if (startBtn != null && stopBtn != null) {
                startBtn.setEnabled(startEnabled);
                stopBtn.setEnabled(stopEnabled);
                ui.getSession().setAttribute("startBtnEnabled_" + jobId, startEnabled);
                ui.getSession().setAttribute("stopBtnEnabled_" + jobId, stopEnabled);
                System.out.println("Job id = " + jobId + " startBtn = " + startEnabled + " stopBtn = " + stopEnabled);
            }
        });
    }

    private void triggerChildJobs(int jobId) {
        Map<Integer, JobManager> jobManagerMap = jobDefinitionService.getJobManagerMap();
        JobManager jobManager = jobManagerMap.get(jobId);
        logPannel.logMessage(Constants.INFO, "Starting stopJob for " + jobManager.getName());
        if (jobManager != null) {
            if(isJobChainRunning && chainCount == 0) {
                isJobChainRunning = false;
                notifySubscribers("Jobchain execution done,," + jobChainId);
            }
            // Execute child jobs for Jobchain type without checking the exit code
            if ("Jobchain".equals(jobManager.getTyp())) {
                List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);
                for (JobManager childJob : childJobs) {
                    try {
                        scheduleJobWithoutCorn(childJob);
                        notifySubscribers(",," + childJob.getId());
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
                        notifySubscribers(",," + childJob.getId());
                    } catch (SchedulerException e) {
                        System.out.println("Error scheduling child job: " + childJob.getName() + " - " + e.getMessage());
                    }
                }
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
}
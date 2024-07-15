package com.example.application.views;

import com.example.application.data.entity.JobManager;
import com.example.application.data.entity.User;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.service.MessageService;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@PageTitle("Job Manager")
@Route(value = "jobManager", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class JobManagerView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${script.path}")
    private String scriptPath;

    @Value("${run.id}")
    private String runID;
    private JobDefinitionService jobDefinitionService;
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

    public JobManagerView(JobDefinitionService jobDefinitionService) {

        this.jobDefinitionService = jobDefinitionService;

        addAttachListener(event -> updateJobManagerSubscription());
        addDetachListener(event -> updateJobManagerSubscription());

        this.ui = UI.getCurrent();
        HorizontalLayout hl = new HorizontalLayout(new H2("Job Manager"), allStartButton, allStopButton);
        hl.setAlignItems(Alignment.BASELINE);
        add(hl);
        System.out.println("How to solve............................................");
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

        System.out.println("allStartEnabled "+allStartEnabled);
        if (allStartEnabled != null) {
            allStartButton.setEnabled(allStartEnabled);
        }

        if (allStopEnabled != null) {
            allStopButton.setEnabled(allStopEnabled);
        }
    }

    private TreeGrid<JobManager> createTreeGrid() {
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

        // Set additional properties for the tree grid
        treeGrid.setWidth("350px");
        treeGrid.addExpandListener(event -> System.out.println(String.format("Expanded %s item(s)", event.getItems().size())));
        treeGrid.addCollapseListener(event -> System.out.println(String.format("Collapsed %s item(s)", event.getItems().size())));

        treeGrid.setThemeName("dense");
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);


        treeGrid.addComponentColumn(jobManager -> {
            // Create a layout to hold the buttons for each row
            HorizontalLayout buttonsLayout = new HorizontalLayout();

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

            // Add click listeners for the buttons
            startBtn.addClickListener(event -> {
                try {
                    scheduleJobWithoutCorn(jobManager);
//                    startBtn.setEnabled(false);
//                    stopBtn.setEnabled(true);
                    notifySubscribers(",,"+jobManager.getId());
                    // Update session attributes when button states change
//                    ui.getSession().setAttribute("startBtnEnabled_" + jobManager.getId(), false);
//                    ui.getSession().setAttribute("stopBtnEnabled_" + jobManager.getId(), true);
                } catch (Exception e) {
                    Notification.show("Error starting job: " + jobManager.getName() + " - " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });

            stopBtn.addClickListener(event -> {
                stopJob(jobManager);
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                // Update session attributes when button states change
//                ui.getSession().setAttribute("startBtnEnabled_" + jobManager.getId(), true);
//                ui.getSession().setAttribute("stopBtnEnabled_" + jobManager.getId(), false);
            });

            // Add buttons to the layout
            buttonsLayout.add(startBtn, stopBtn);
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
                    if(jobManager.getCron() != null) {
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

        return treeGrid;
    }
    private VerticalLayout showEditAndNewDialog(JobManager jobManager, String context){
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
            System.out.println("saved data....");
            if(context.equals("New")) {
                saveSqlDefinition(newJobDefination);
            } else {
                saveSqlDefinition(jobManager);
            }
            updateGrid();
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;

    }
    public JobManager saveSqlDefinition(JobManager jobManager) {
        return jobDefinitionService.save(jobManager);
    }

    private Component editJobDefinition(JobManager jobManager, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        // Create and initialize fields
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

        TextField typ = new TextField("TYP");
        typ.setValue(isNew ? "" : (jobManager.getTyp() != null ? jobManager.getTyp() : ""));
        typ.setWidthFull();

        TextField parameter = new TextField("PARAMETER");
        parameter.setValue(isNew ? "" : (jobManager.getParameter() != null ? jobManager.getParameter() : ""));
        parameter.setWidthFull();

        IntegerField pid = new IntegerField("PID");
        pid.setValue(jobManager.getPid() != 0 ? jobManager.getPid() : 0);
        pid.setWidthFull();

        // Add value change listeners to update the jobDefinition object
        name.addValueChangeListener(event -> jobManager.setName(event.getValue()));
        namespace.addValueChangeListener(event -> jobManager.setNamespace(event.getValue()));
        command.addValueChangeListener(event -> jobManager.setCommand(event.getValue()));
        cron.addValueChangeListener(event -> jobManager.setCron(event.getValue()));
        typ.addValueChangeListener(event -> jobManager.setTyp(event.getValue()));
        parameter.addValueChangeListener(event -> jobManager.setParameter(event.getValue()));
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
        content.add(name, namespace, command, cron, typ, parameter , pid);
        return content;
    }

    private Component deleteTreeGridItem(JobManager jobManager) {

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

        return dialogLayout;
    }

    private void updateGrid() {
        listOfJobManager = jobDefinitionService.findAll();
        treeGrid.setItems(jobDefinitionService.getRootProjects(), jobDefinitionService ::getChildProjects);
    }

    private void scheduleJob(JobManager jobManager) throws SchedulerException {
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
        jobDataMap.put("runID", runID);
        jobDataMap.put("startType", "cron");

        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
                .withIdentity("job-" + jobManager.getId(), "group1")
                .usingJobData(jobDataMap)
                .build();

        // Using the cron expression from the JobManager
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + jobManager.getId(), "group1")
                .withSchedule(CronScheduleBuilder.cronSchedule(jobManager.getCron()))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void scheduleJobWithoutCorn(JobManager jobManager) throws SchedulerException {
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
        jobDataMap.put("runID", runID);
        jobDataMap.put("startType", "manual");

        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
                .withIdentity("job-" + jobManager.getId(), "group1")
                .usingJobData(jobDataMap)
                .build();

        // Create a trigger to run immediately
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + jobManager.getId(), "group1")
                .startNow() // Trigger will start immediately
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void stopJob(JobManager jobManager) {
        JobKey jobKey = new JobKey("job-" + jobManager.getId(), "group1");
        try {
            // scheduler.interrupt(jobKey);
            if (scheduler.deleteJob(jobKey)) {
                JobExecutor.stopProcess(jobManager.getId());
                // Job was found and deleted successfully
                notifySubscribers("Job " + jobManager.getName() + " stopped successfully,,"+jobManager.getId());

            } else {
                // Job was not found
                notifySubscribers("Job " + jobManager.getName() + " not found running,,"+jobManager.getId());
            }
         //   JobExecutor.stopProcess(jobManager.getId());
        //    stopNotifiers();
        } catch (SchedulerException e) {
            // Handle the exception and add an error message
            notifySubscribers("Error stopping job: " + jobManager.getName() + " - " + e.getMessage()+",,"+jobManager.getId());
        }
    }

    private void executeJob(JobManager jobManager) {
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
        //  String runID = "777"; // RunID for test purposes
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
        UI ui = getUI().orElse(null);

        // Subscribe if the view is attached
        if (ui != null) {
            if (subscriber != null) {
                // Already subscribed
                return;
            }

            subscriber = message -> {
                ui.access(() -> {
                    if (!ui.isAttached()) {
                        // UI is detached, stop processing
                        return;
                    }
                    String[] parts = message.split(",,");
                    System.out.println(message + "######################################");
                    String displayMessage = parts[0].trim();
                    int jobId = 0;
                    if (parts.length == 2) {
                        jobId = Integer.parseInt(parts[1].trim());
                    }
                    if (displayMessage != null && !displayMessage.isEmpty()) {
                        Notification.show(displayMessage, 5000, Notification.Position.MIDDLE);
                    }
                    if (displayMessage.contains("start running all")) {
                        allStartButton.setEnabled(false);
                        allStopButton.setEnabled(true);
                        ui.getSession().setAttribute("allStartEnabled", false);
                        ui.getSession().setAttribute("allStopEnabled", true);
                    } else if (displayMessage.contains("stopped successfully") || displayMessage.contains("not found running") || displayMessage.contains("Error stopping job")) {
                        allStartButton.setEnabled(true);
                        allStopButton.setEnabled(false);
                        ui.getSession().setAttribute("allStartEnabled", true);
                        ui.getSession().setAttribute("allStopEnabled", false);
                        if(jobId != 0) {

                            Button startBtn = startButtons.get(jobId);
                            Button stopBtn = stopButtons.get(jobId);

                            startBtn.setEnabled(true);
                            stopBtn.setEnabled(false);
                            ui.getSession().setAttribute("startBtnEnabled_" + jobId, true);
                            ui.getSession().setAttribute("stopBtnEnabled_" + jobId, false);
                        }
                    }

                    if (jobId != 0) {                                                        
                        Button startBtn = startButtons.get(jobId);
                        Button stopBtn = stopButtons.get(jobId);
                        System.out.println(startBtn + ".......startbtn" + startButtons.size());
                        System.out.println(stopBtn + ".......startbtn" + stopButtons.size());
                        if (displayMessage.equals("")) {
                            startBtn.setEnabled(false);
                            stopBtn.setEnabled(true);
                            ui.getSession().setAttribute("startBtnEnabled_" + jobId, false);
                            ui.getSession().setAttribute("stopBtnEnabled_" + jobId, true);
                        }  else if (displayMessage.contains("executed successfully") || displayMessage.contains("Error while Job")) {
                            System.out.println(jobId +" jobid ...........");
                            startBtn.setEnabled(true);
                            stopBtn.setEnabled(false);
                            ui.getSession().setAttribute("startBtnEnabled_" + jobId, true);
                            ui.getSession().setAttribute("stopBtnEnabled_" + jobId, false);
                        }
                    }
                });
            };

            synchronized (subscribers) {
                subscribers.add(subscriber);
            }
        } else {
            if (subscriber == null) {
                // Already unsubscribed
                return;
            }

            synchronized (subscribers) {
                subscribers.remove(subscriber);
            }
            subscriber = null;
        }
    }


    public static void notifySubscribers(String message) {
        Set<SerializableConsumer<String>> subscribersSnapshot;
        synchronized (subscribers) {
            subscribersSnapshot = new HashSet<>(subscribers);
        }

        for (SerializableConsumer<String> subscriber : subscribersSnapshot) {
            notifierThread.execute(
                    () -> {
                        try {
                            subscriber.accept(message);


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
    }

    public void stopShellJob() {
     //   if (process != null) {
            process.destroy(); // Terminate the running process
      //  }
    }
}
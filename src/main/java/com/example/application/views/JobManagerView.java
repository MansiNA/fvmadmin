package com.example.application.views;

import com.example.application.data.entity.JobManager;
import com.example.application.data.service.JobDefinitionService;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@PageTitle("Job Manager")
@Route(value = "jobManager", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class JobManagerView extends VerticalLayout {

    private JobDefinitionService jobDefinitionService;
    private Crud<JobManager> crud;
    private Grid<JobManager> jobDefinitionGrid;
    private Dialog dialog;
    private Dialog resetPasswordDialog;
    private TreeGrid<JobManager> treeGrid;
    private Scheduler scheduler;

    public JobManagerView(JobDefinitionService jobDefinitionService) {

        this.jobDefinitionService = jobDefinitionService;

        add(new H2("Job Manager"));

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

    private TreeGrid<JobManager> createTreeGrid() {
        treeGrid = new TreeGrid<>();
        jobDefinitionService.findAll();
        treeGrid.setItems(jobDefinitionService.getRootProjects(), jobDefinitionService::getChildProjects);

        // Add the hierarchy column for displaying the hierarchical data
        treeGrid.addHierarchyColumn(JobManager::getName).setHeader("Name").setAutoWidth(true);

        // Add other columns except id and pid
        treeGrid.addColumn(JobManager::getNamespace).setHeader("Namespace").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getCommand).setHeader("Command").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getCron).setHeader("Cron").setAutoWidth(true);
        treeGrid.addColumn(JobManager::getTyp).setHeader("Typ").setAutoWidth(true);

        // Set additional properties for the tree grid
        treeGrid.setWidth("350px");
        treeGrid.addExpandListener(event -> System.out.println(String.format("Expanded %s item(s)", event.getItems().size())));
        treeGrid.addCollapseListener(event -> System.out.println(String.format("Collapsed %s item(s)", event.getItems().size())));

        treeGrid.setThemeName("dense");
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);

        // Add a column with a button to start the job manually
        treeGrid.addComponentColumn(jobManager -> {
            Button startButton = new Button("Start");
            startButton.addClickListener(event -> {
                try {
                    scheduleJob(jobManager);
                //   executeJob(jobManager);
                } catch (Exception e) {
                    Notification.show("Error executing job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            return startButton;
        }).setHeader("Start Actions").setAutoWidth(true);

        treeGrid.addComponentColumn(jobManager -> {
            Button stopButton = new Button("Stop");
            stopButton.addClickListener(event -> {
                try {
                    stopJob(jobManager);
                    Notification.show("Job stopped successfully", 3000, Notification.Position.MIDDLE);
                } catch (Exception e) {
                    Notification.show("Error stopping job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            return stopButton;
        }).setHeader("Stop Action").setAutoWidth(true);
//
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
            newJobDefination.setId( (jobManagerList.size() + 1));
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

        IntegerField pid = new IntegerField("PID");
        pid.setValue(jobManager.getPid() != 0 ? jobManager.getPid() : 0);
        pid.setWidthFull();

        // Add value change listeners to update the jobDefinition object
        name.addValueChangeListener(event -> jobManager.setName(event.getValue()));
        namespace.addValueChangeListener(event -> jobManager.setNamespace(event.getValue()));
        command.addValueChangeListener(event -> jobManager.setCommand(event.getValue()));
        cron.addValueChangeListener(event -> jobManager.setCron(event.getValue()));
        typ.addValueChangeListener(event -> jobManager.setTyp(event.getValue()));
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
        content.add(name, namespace, command, cron, typ, pid);
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
        jobDefinitionService.findAll();
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

    public void stopJob(JobManager jobManager) throws SchedulerException {
        JobKey jobKey = new JobKey("job-" + jobManager.getId(), "group1");
        scheduler.deleteJob(jobKey);
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

    private void executeShellJob(JobManager jobManager) throws Exception {
        String scriptPath = "D:\\file\\executer.cmd"; // Absolute path to the script
        String jobName = jobManager.getCommand(); // Assuming this is the Jobname
        String runID = "777"; // RunID for test purposes

        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Quote the script path to handle spaces and special characters
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "\"" + scriptPath + "\"", jobName, runID);
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", "\"" + scriptPath + "\" " + jobName + " " + runID);
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

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

}
package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.JobManager;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.service.EmailService;
import com.example.application.utils.Constants;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.example.application.utils.LogPannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
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
@RolesAllowed({"ADMIN","FVM"})
public class JobManagerView extends VerticalLayout implements BeforeEnterObserver {

    private JobDefinitionService jobDefinitionService;
    private ConfigurationService configurationService;
    private Crud<JobManager> crud;
    private Grid<JobManager> jobDefinitionGrid;
    private Dialog dialog;
    private Dialog resetPasswordDialog;
    private TreeGrid<JobManager> treeGrid;
    private Scheduler scheduler;
    public  Button allCronButton = new Button("Cron Start");
   // private Button allStopButton = new Button("All Stop");
   // private Button sendMailButton = new Button("Testmail");

    private final UI ui;
    private static final Set<SerializableConsumer<String>> subscribers = new HashSet<>();
    private static final Set<SerializableConsumer<String>> start_subscribers = new HashSet<>();
    private static final ExecutorService notifierThread = Executors.newSingleThreadExecutor();
    private static final ExecutorService startnotifierThread = Executors.newSingleThreadExecutor();
    private SerializableConsumer<String> subscriber;
    Map<Integer, Button> startButtons = new HashMap<>();
    Map<Integer, Button> stopButtons = new HashMap<>();
    List<JobManager> listOfJobManager;
    List<JobManager> listOfGridItem;
    boolean isContinueChildJob = false;
    boolean isJobChainRunning = false;
    private int jobChainId;
    private int chainCount = 0;
    private LogPannel logPannel;
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;
    private final EmailService emailService;
    Button menuButton = new Button("Show/Hide Columns");

    public JobManagerView(EmailService emailService, JobDefinitionService jobDefinitionService, ConfigurationService configurationService) {

        this.emailService = emailService;
        this.jobDefinitionService = jobDefinitionService;
        this.configurationService = configurationService;
        allCronButton.setVisible(false);

        logPannel = new LogPannel();
        logPannel.logMessage(Constants.INFO, "Starting JobManagerView");

        addAttachListener(event -> updateJobManagerSubscription());
        addDetachListener(event -> updateJobManagerSubscription());

        this.ui = UI.getCurrent();

        HorizontalLayout hl = new HorizontalLayout(new H2("Job Manager"),  allCronButton, menuButton);
        hl.setAlignItems(Alignment.BASELINE);
        add(hl);

        if(MainLayout.isAdmin) {
            allCronButton.setVisible(true);
        }

        HorizontalLayout treehl = new HorizontalLayout();
        treehl.setHeightFull();
        treehl.setWidthFull();
        treehl.setSizeFull();

        TreeGrid tg= createTreeGrid();
        tg.setHeightFull();
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
       getDefaultScheduler();
    }

    private void getDefaultScheduler() {
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
            allCronButton.setEnabled(allStartEnabled);
        }

//        if (allStopEnabled != null) {
//            allStopButton.setEnabled(allStopEnabled);
//        }
    }

    private TreeGrid<JobManager> createTreeGrid() {
        logPannel.logMessage(Constants.INFO, "Starting createTreeGrid");
        treeGrid = new TreeGrid<>();
        updateGrid();
        getDefaultScheduler();
        // Add the hierarchy column for displaying the hierarchical data
        treeGrid.addHierarchyColumn(JobManager::getName).setHeader("Name").setAutoWidth(true).setResizable(true);

        // Add other columns except id and pid
        TreeGrid.Column<JobManager> namespaceColumn = treeGrid.addColumn(JobManager::getNamespace).setHeader("Namespace").setAutoWidth(true).setResizable(true);
        treeGrid.addColumn(JobManager::getCommand).setHeader("Command").setAutoWidth(true).setResizable(true);
        treeGrid.addColumn(JobManager::getCron).setHeader("Cron").setAutoWidth(true).setResizable(true);
        treeGrid.addColumn(JobManager::getTyp).setHeader("Typ").setAutoWidth(true).setResizable(true);
        treeGrid.addColumn(JobManager::getParameter).setHeader("Parameter").setAutoWidth(true).setResizable(true);
//        treeGrid.addColumn(JobManager::getScriptpath).setHeader("ScriptPath").setAutoWidth(true).setResizable(true);
//        treeGrid.addColumn(JobManager::getMailBetreff).setHeader("MailBetreff").setAutoWidth(true).setResizable(true);
//        treeGrid.addColumn(JobManager::getMailText).setHeader("MailText").setAutoWidth(true).setResizable(true);
//        treeGrid.addColumn(JobManager::getMailEmpfaenger).setHeader("MAIL_EMPFAENGER").setAutoWidth(true).setResizable(true);
//        treeGrid.addColumn(JobManager::getMailCcEmpfaenger).setHeader("MAIL_CC_EMPFAENGER").setAutoWidth(true).setResizable(true);
//        treeGrid.addColumn(jobManager -> {
//            // Retrieve the connection ID from the Configuration object
//            return jobManager.getConnection() != null ? jobManager.getConnection().getName() : "N/A";
//        }).setHeader("Verbindung").setAutoWidth(true).setResizable(true);

        TreeGrid.Column<JobManager> scriptPathColumn = treeGrid.addColumn(JobManager::getScriptpath)
                .setHeader("ScriptPath").setAutoWidth(true).setResizable(true);

        TreeGrid.Column<JobManager> mailBetreffColumn = treeGrid.addColumn(JobManager::getMailBetreff)
                .setHeader("MailBetreff").setAutoWidth(true).setResizable(true);

        TreeGrid.Column<JobManager> mailTextColumn = treeGrid.addColumn(JobManager::getMailText)
                .setHeader("MailText").setAutoWidth(true).setResizable(true);

        TreeGrid.Column<JobManager> mailEmpfaengerColumn = treeGrid.addColumn(JobManager::getMailEmpfaenger)
                .setHeader("MAIL_EMPFAENGER").setAutoWidth(true).setResizable(true);

        TreeGrid.Column<JobManager> mailCcEmpfaengerColumn = treeGrid.addColumn(JobManager::getMailCcEmpfaenger)
                .setHeader("MAIL_CC_EMPFAENGER").setAutoWidth(true).setResizable(true);

        TreeGrid.Column<JobManager> verbindungColumn = treeGrid.addColumn(jobManager -> {
            // Retrieve the connection ID from the Configuration object
            return jobManager.getConnection() != null ? jobManager.getConnection().getName() : "N/A";
        }).setHeader("Verbindung").setAutoWidth(true).setResizable(true);

        //  treeGrid.setWidth("350px");
        treeGrid.addExpandListener(event -> System.out.println(String.format("Expanded %s item(s)", event.getItems().size())));
        treeGrid.addCollapseListener(event -> System.out.println(String.format("Collapsed %s item(s)", event.getItems().size())));

        treeGrid.setThemeName("dense");
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);


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

        if(isAnyCronJobRunning()) {
            updateAllButtonsState(ui, "Cron Stop");
            //  updateAllButtonsState(ui, false, true);
        } else {
            updateAllButtonsState(ui, "Cron Start");
            //  updateAllButtonsState(ui, true, false);
        }
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
                            expandChild(jobManager);
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
        }).setHeader("Actions").setAutoWidth(true).setResizable(true);


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

        allCronButton.addClickListener(event -> {

            if (allCronButton.getText().equals("Cron Start")) {
                allCronJobSart();
            } else {
                // Stop the cron jobs
                allCronButton.setText("Cron Start");
                List<JobManager> jobManagerList = jobDefinitionService.findAll();
                Notification.show("stop running...", 3000, Notification.Position.MIDDLE);

                for (JobManager jobManager : jobManagerList) {
                    try {
                        if(jobManager.getCron() != null) {
                            stopJob(jobManager);
                        }
                    } catch (Exception e) {
                        allCronButton.setText("Cron Stop");
                        Notification.show("Error stopping job: "+ jobManager.getName()+" " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                    }
                }
            }
        });



        if (MainLayout.isAdmin) {

            GridContextMenu<JobManager> contextMenu = treeGrid.addContextMenu();
            //   if (listOfJobManager != null && !listOfJobManager.isEmpty()) {

            contextMenu.addItem("Edit", event -> {
                System.out.println("edityyyyyyyyyy......."+listOfGridItem);
                Optional<JobManager> selectedItem = event.getItem();
                if (selectedItem.isPresent() && listOfGridItem != null && !listOfGridItem.isEmpty()) {
                    System.out.println("edityyyyyyyyyy......."+listOfGridItem);
                    showEditAndNewDialog(event.getItem().get(), "Edit");
                }
            });

            contextMenu.addItem("Delete", event -> {
                Optional<JobManager> selectedItem = event.getItem();
                if (selectedItem.isPresent() && listOfGridItem != null && !listOfGridItem.isEmpty()) {
                    System.out.println("deleteyyyyyyyyyy......."+listOfGridItem);
                    deleteTreeGridItem(event.getItem().get());
                }
            });

            // "New" option is always available
            contextMenu.addItem("New", event -> {
                System.out.println("newyyyyyyyyyy......."+listOfGridItem);
                Optional<JobManager> selectedItem = event.getItem();
                if (selectedItem.isPresent() && listOfGridItem != null && !listOfGridItem.isEmpty()) {
                    System.out.println("------------with parent");
                    showEditAndNewDialog(event.getItem().get(), "New");
                } else {
                    System.out.println("------------no parent");
                    JobManager jobManager = new JobManager();
                    jobManager.setId(0);
                    showEditAndNewDialog(jobManager, "New");
                }
            });

//            treeGrid.addContextMenu().setDynamicContentHandler(selectedItem -> {
//
//                // Conditionally show "Edit" and "Delete" items based on selection
//                // Always show "New" item
//                contextMenu.getItems().stream()
//                        .filter(item -> item.getText().equals("New"))
//                        .forEach(item -> item.setVisible(true));
//
//                // Conditionally show "Edit" and "Delete" items based on selection
//                boolean hasSelection = selectedItem != null;
//                contextMenu.getItems().stream()
//                        .filter(item -> item.getText().equals("Edit") || item.getText().equals("Delete"))
//                        .forEach(item -> item.setVisible(hasSelection));
//
//                // Return whether there is a selection
//                return hasSelection;
//            });
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

    private void expandChild(JobManager rootItem) {
        treeGrid.expand(rootItem);
        List<JobManager> childItems = jobDefinitionService.getChildJobManager(rootItem);
        expandAll(childItems);
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
//        dialogLayout.setWidth("1200px");
//        dialogLayout.setHeight("800px");
        Dialog dialog = new Dialog();

        JobManager newJobDefination = new JobManager();

        if(context.equals("New")){
            System.out.println("##########"+jobManager);
            newJobDefination.setPid(jobManager.getId());
            dialog.add(editJobDefinition(newJobDefination, true)); // For adding new entry
        } else {
            dialog.add(editJobDefinition(jobManager, false)); // For editing existing entry
        }

        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1300px");
        dialog.setHeight("900px");
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
        HorizontalLayout hl1 = new HorizontalLayout();
        HorizontalLayout hl2 = new HorizontalLayout();
        HorizontalLayout hl3 = new HorizontalLayout();
        HorizontalLayout hl4 = new HorizontalLayout();

        IntegerField id = new IntegerField("ID");
        id.setValue(jobManager.getId() != null ? jobManager.getId() : null);
        id.setWidth("50px");
        id.setReadOnly(true);

        TextField name = new TextField("NAME");
        name.setValue(isNew ? "" : (jobManager.getName() != null ? jobManager.getName() : ""));
        name.setWidth("500px");

        TextField namespace = new TextField("NAMESPACE");
        namespace.setValue(isNew ? "" : (jobManager.getNamespace() != null ? jobManager.getNamespace() : ""));
        namespace.setWidthFull();

        TextField command = new TextField("COMMAND");
        command.setValue(isNew ? "" : (jobManager.getCommand() != null ? jobManager.getCommand() : ""));
        command.setWidth("500px");
        command.setTooltipText("Auszuführendes Programm, oder EXCEL_REPORT für Reporting.");

        TextField cron = new TextField("CRON");
        cron.setValue(isNew ? "" : (jobManager.getCron() != null ? jobManager.getCron() : ""));
        cron.setWidthFull();

//        TextField typ = new TextField("TYP");
//        typ.setValue(isNew ? "" : (jobManager.getTyp() != null ? jobManager.getTyp() : ""));
//        typ.setWidthFull();

        TextField parameter = new TextField("PARAMETER");
        parameter.setValue(isNew ? "" : (jobManager.getParameter() != null ? jobManager.getParameter() : ""));
        parameter.setWidthFull();
        parameter.setTooltipText("dem Script zu übergebene Parameter. Bei EXCEL_REPORT im Format Filename;Sheetname:SQL");

        IntegerField pid = new IntegerField("PID");
        pid.setTooltipText("ID des Vorgängers");
        pid.setValue(jobManager.getPid() != null ? jobManager.getPid() : 0);
        pid.setWidth("50px");

        TextField scriptpath = new TextField("SCRIPTPATH");
        scriptpath.setValue(isNew ? "" : (jobManager.getScriptpath() != null ? jobManager.getScriptpath() : ""));
        scriptpath.setTooltipText("Verzeichnis, in dem das Script abgelegt ist");
        scriptpath.setWidthFull();

        TextField mailBetreff = new TextField("MAIL_BETREFF");
        mailBetreff.setValue(isNew ? "" : (jobManager.getMailBetreff() != null ? jobManager.getMailBetreff() : ""));
        mailBetreff.setWidthFull();
        mailBetreff.setTooltipText("Text im Betreff dr Mail");

        TextArea mailText = new TextArea("MAIL_TEXT");
        mailText.setTooltipText("Inhalt der Mail");
        mailText.setHeight("120px");
        mailText.setValue(isNew ? "" : (jobManager.getMailText() != null ? jobManager.getMailText() : ""));
        mailText.setWidthFull();

        TextField mailEmpfaenger = new TextField("MAIL_EMPFAENGER");
        mailEmpfaenger.setValue(isNew ? "" : (jobManager.getMailEmpfaenger() != null ? jobManager.getMailEmpfaenger() : ""));
        mailEmpfaenger.setWidthFull();

        TextField mailCcEmpfaenger = new TextField("MAIL_CC_EMPFAENGER");
        mailCcEmpfaenger.setValue(isNew ? "" : (jobManager.getMailCcEmpfaenger() != null ? jobManager.getMailCcEmpfaenger() : ""));
        mailCcEmpfaenger.setWidthFull();

        List<String> uniqueTyps = jobDefinitionService.getUniqueTypList();

        ComboBox<String> typComboBox = new ComboBox<>("Typ");
        if (uniqueTyps != null && !uniqueTyps.isEmpty()) {
            typComboBox.setItems(uniqueTyps);
            typComboBox.setValue(isNew ? "" : (jobManager.getTyp() != null ? jobManager.getTyp() : ""));
        }

        ComboBox<Configuration> verbindungComboBox = new ComboBox<>("Verbindung");
        verbindungComboBox.setEnabled(false);
        if(!isNew) {
            if (jobManager.getTyp().equals("sql_procedure") || jobManager.getTyp().equals("sql_report")
                    || jobManager.getTyp().equals("sql_statement") ) {
                verbindungComboBox.setEnabled(true);
            }
            if (jobManager.getTyp().equals("Shell")) {
                verbindungComboBox.isReadOnly();
            }
            if (!("sql_report".equals(jobManager.getTyp()))) {
                mailEmpfaenger.setEnabled(false);
                mailCcEmpfaenger.setEnabled(false);
                mailText.setEnabled(false);
                mailBetreff.setEnabled(false);
            }
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
        scriptpath.addValueChangeListener(event -> jobManager.setScriptpath(event.getValue()));
        mailBetreff.addValueChangeListener(event -> jobManager.setMailBetreff(event.getValue()));
        mailText.addValueChangeListener(event -> jobManager.setMailText(event.getValue()));
        mailEmpfaenger.addValueChangeListener(event -> jobManager.setMailEmpfaenger(event.getValue()));
        mailCcEmpfaenger.addValueChangeListener(event -> jobManager.setMailCcEmpfaenger(event.getValue()));
        typComboBox.addValueChangeListener(event -> {
            String selectedTyp = event.getValue();
            typComboBox.setValue(selectedTyp);
            jobManager.setTyp(selectedTyp);
            if ("sql_procedure".equals(selectedTyp) || "sql_report".equals(selectedTyp) || "sql_statement".equals(selectedTyp)) {
                verbindungComboBox.setEnabled(true);
            } else {
                verbindungComboBox.setEnabled(false);
            }
            if (!("sql_report".equals(selectedTyp))) {
                mailEmpfaenger.setEnabled(false);
                mailCcEmpfaenger.setEnabled(false);
                mailBetreff.setEnabled(false);
                mailText.setEnabled(false);
            } else {
                mailEmpfaenger.setEnabled(true);
                mailCcEmpfaenger.setEnabled(true);
                mailBetreff.setEnabled(true);
                mailText.setEnabled(true);
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

        hl1.add(id,pid, name, namespace);
        hl1.setWidthFull();
        hl2.setWidthFull();
        hl2.add(command, scriptpath );
        hl3.add(cron, typComboBox, verbindungComboBox);
        mailEmpfaenger.setWidthFull();
        mailCcEmpfaenger.setWidthFull();
        hl4.add(mailEmpfaenger, mailCcEmpfaenger);
        hl4.setWidthFull();
        //hl5.add(mailBetreff, mailText);
        // Add all fields to the content layout
        content.add(hl1, hl2, parameter, hl3, hl4, mailBetreff,mailText );
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
            List<JobManager> childJobs = jobDefinitionService.getChildJobManager(jobManager);
            boolean hasChildren = !childJobs.isEmpty();
            if(hasChildren) {
                Notification.show(jobManager.getName()+"- Child entries exist!  No delete." , 5000, Notification.Position.MIDDLE);
            } else {
                jobDefinitionService.deleteById(jobManager.getId());

                updateGrid();
            }

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
        TreeDataProvider<JobManager> dataProvider = (TreeDataProvider<JobManager>) treeGrid.getDataProvider();
         listOfGridItem = dataProvider.getTreeData().getRootItems();
      //  expandAll(rootItems);
    }

    private void  allCronJobSart(){
        List<JobManager> jobManagerList = jobDefinitionService.findAll();

        allCronButton.setText("Cron Stop");
        notifySubscribers("start running all...");
        for (JobManager jobManager : jobManagerList) {
            try {
                String type = jobManager.getTyp();
                if(jobManager.getCron() != null && !type.equals("Node") && !type.equals("Jobchain")) {
                    scheduleJob(jobManager);
                }
            } catch (Exception e) {
                allCronButton.setText("Cron Start");
                Notification.show("Error executing job: " + jobManager.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }
    }
    public void scheduleJob(JobManager jobManager) throws SchedulerException {
        logPannel.logMessage(Constants.INFO, "Starting scheduleJob with cron for " + jobManager.getName());
        getDefaultScheduler();
        scheduler.start();
      //  notifySubscribers(",,"+jobManager.getId());
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
        System.out.println("thisssssss....................stop................................");
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
            System.out.println(displayMessage);
            if(!isCron) {
                Notification.show(displayMessage, 5000, Notification.Position.MIDDLE);
            }
        }
        updateUIBasedOnMessage(ui, displayMessage, jobId, isCron);
    }

    private void updateUIBasedOnMessage(UI ui, String displayMessage, int jobId, boolean isCron) {
        if (displayMessage.contains("start running all")) {
            updateAllButtonsState(ui, "Cron Stop");
          //  updateAllButtonsState(ui, false, true);
        } else if (displayMessage.contains("stopped successfully") || displayMessage.contains("not found running") || displayMessage.contains("Error stopping job")) {
            if(!isAnyCronJobRunning()) {
                updateAllButtonsState(ui, "Cron Start");
              //  updateAllButtonsState(ui, true, false);
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
//            allStartButton.setEnabled(startEnabled);
//            allStopButton.setEnabled(stopEnabled);
//            ui.getSession().setAttribute("allStartEnabled", startEnabled);
//            ui.getSession().setAttribute("allStopEnabled", stopEnabled);
        });
    }
    private void updateAllButtonsState(UI ui, String action) {
        ui.access(() -> {
            allCronButton.setText(action);
//            allStopButton.setEnabled(stopEnabled);
            ui.getSession().setAttribute("action", action);
//            ui.getSession().setAttribute("allStopEnabled", stopEnabled);
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
}
package com.example.application.views;

import com.example.application.data.entity.*;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.MailboxService;
import com.example.application.data.service.ProtokollService;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.MailboxWatchdogJobExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.application.Application.mailboxen;

@PageTitle("Mailbox Watcher")
@Route(value = "mailbox-watcher", layout= MainLayout.class)
@RolesAllowed({"ADMIN"})
public class MailboxWatcher  extends VerticalLayout {

    JdbcTemplate jdbcTemplate;
    private ConfigurationService service;
    private MailboxService mailboxService;
    private ProtokollService protokollService;
    private ComboBox<Configuration> comboBox;
    public Grid<Mailbox> grid = new Grid<>(Mailbox.class, false);
    Button refresh = new Button("refresh");
    //private List<MailboxShutdown> affectedMailboxes;
  //  private List<Mailbox> mailboxen;
    private String switchLable;
    @Value("${spring.datasource.jdbc-url}")
    private String defaultJdbcUrl;

    @Value("${spring.datasource.username}")
    private String defaultUsername;

    @Value("${spring.datasource.password}")
    private String defaultPassword;
    private ContextMenu check_menu;
    private Span syscheck;
    private static final Logger logger = LoggerFactory.getLogger(MailboxWatcher.class);
    private static final Set<SerializableConsumer<String>> subscribers = new HashSet<>();
    private static final ExecutorService notifierThread = Executors.newSingleThreadExecutor();
    private SerializableConsumer<String> subscriber;
    private UI ui;

    public MailboxWatcher(ConfigurationService service, ProtokollService protokollService, MailboxService mailboxService, JdbcTemplate jdbcTemplate)  {
        logger.info("Starting MailboxWatcher");
        this.service = service;
        this.protokollService = protokollService;
        this.mailboxService = mailboxService;
        this.jdbcTemplate = jdbcTemplate;

        ui = UI.getCurrent();
        comboBox = new ComboBox<>("Verbindung");
        comboBox.setPlaceholder("auswählen");
        try {
            List<Configuration> configList = service.findMessageConfigurations();
            if (configList != null && !configList.isEmpty()) {

                List<Configuration> filteredConfigList = configList.stream()
                        .filter(config -> config.getIsWatchdog() != null && config.getIsWatchdog() == 1)
                        .toList();
                if (!filteredConfigList.isEmpty()) {
                    comboBox.setItems(filteredConfigList);
                    comboBox.setItemLabelGenerator(Configuration::getName);
                  //  comboBox.setValue(filteredConfigList.get(0)); // Set the first item as the default value
                } else {
                    Notification.show("No configurations available for watchdog.", 5000, Notification.Position.MIDDLE);
                }
            }
        //    updateTreeGrid();
            //    updateGrid();
        } catch (Exception e) {
            // Display the error message to the user
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }


        comboBox.setPlaceholder("auswählen");
        configureMailboxGrid();
        updateList();

        // Add "On" menu item and mark it checkable
        syscheck = new Span();
        check_menu = new ContextMenu();
        check_menu.setTarget(syscheck);
       // setChecker("On");

        MenuItem onMenuItemChecker = check_menu.addItem("On", event -> {

            //System.out.println("Background Job for checker eingeschaltet");
            logger.info("Background Job for checker eingeschaltet");
            setChecker("On");
            mailboxService.updateIsMBWatchdogJobActive(1, comboBox.getValue());
            checkMailboxWatchdogProcess();
        });
        onMenuItemChecker.setCheckable(true); // Ensure the "On" item is checkable

        // Add "Off" menu item and mark it checkable
        MenuItem offMenuItemChecker = check_menu.addItem("Off", event -> {
            //System.out.println("Background Job for checker ausgeschaltet");
            logger.info("Switch of Background Job for WWatchdog");
            setChecker("Off");
            mailboxService.updateIsMBWatchdogJobActive(0, comboBox.getValue());
            checkMailboxWatchdogProcess();
        });
        offMenuItemChecker.setCheckable(true);

        // Add "Cron Expression" menu item
        check_menu.addItem("Konfiguration", event -> {
            System.out.println("Call Dialog for cron expression");
            mailboxWatchdogJobConfigurationDialog();
        });

        //mailboxService.createFvmMonitorAlertingTable(comboBox.getValue());


       // setWatchdogStatus(comboBox.getValue());



        Div checkInfo = new Div(new Span("Watchdog-Job: "), syscheck);
        syscheck.getStyle().set("font-weight", "bold");

        HorizontalLayout hl = new HorizontalLayout();
        //   hl.add(comboBox,button,refresh);
        hl.add(comboBox,refresh, checkInfo);
        hl.setAlignItems(Alignment.BASELINE);
        setSizeFull();
        add(hl);

     //   affectedMailboxes = new ArrayList<>();

        //  grid.setItems(mailboxen);
        Span title = new Span("Mailbox-Watcher");
        title.getStyle().set("font-weight", "bold");
        HorizontalLayout headerLayout = new HorizontalLayout(title);
        headerLayout.setAlignItems(Alignment.BASELINE);
        headerLayout.setFlexGrow(1, title);

        add(headerLayout,grid);
        addAttachListener(event -> updateJobManagerSubscription());
        addDetachListener(event -> updateJobManagerSubscription());

        refresh.addClickListener(e -> {
            if (comboBox.getValue() != null) {
                mailboxen = mailboxService.getMailboxes(comboBox.getValue());
            }
            updateList();
        });

        //  updateJobManagerSubscription();
        // button.addClickListener(clickEvent -> {
        comboBox.addValueChangeListener(event->{

            UI ui = UI.getCurrent();
            grid.setItems();
            mailboxen=null;
            setWatchdogStatus(comboBox.getValue());
            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    //System.out.println("Hole Mailbox Infos");

                    logger.debug("Get Mailbox Infos");

                    mailboxen = mailboxService.getMailboxes(comboBox.getValue());

                    //Thread.sleep(2000); //2 Sekunden warten
                    Thread.sleep(20); //2 Sekunden warten

                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (Exception e) {
                    // Need to use access() when running from background thread
                    ui.access(() -> {
                        logger.error("Error: " + e.getMessage());
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                    });
                    //  return; // Exit if an exception occurs
                }

                // Need to use access() when running from background thread
                ui.access(() -> {
                    // Stop polling and hide spinner
                    ui.setPollInterval(-1);

                    if (mailboxen == null || mailboxen.isEmpty()) {
                        if(mailboxen != null && mailboxen.isEmpty()) {
                            logger.debug("Found no information in database!");
                            Notification.show("Keine Mailbox Infos gefunden!", 5000, Notification.Position.MIDDLE);
                        }
                        return;
                    } else {
                  //      affectedMailboxes = fetchTableData();
                  //      logger.debug("affectedMailboxes: " + affectedMailboxes.size());
                        grid.setItems(mailboxen);
                    }

                });
            });


        });


    }

    private void setWatchdogStatus(Configuration value) {

        MonitorAlerting  monitorAlerting = mailboxService.fetchEmailConfiguration(value);

        if (monitorAlerting != null && monitorAlerting.getIsMBWatchdogActive() != null && monitorAlerting.getIsMBWatchdogActive() != 0) {

            logger.debug("Setze Watchdog Schalter: on");

            setChecker("On");
        } else {
            logger.debug("Setze Watchdog Schalter: off");
            setChecker("Off");
        }
    }

    private void configureMailboxGrid() {

        // grid.setSelectionMode(Grid.SelectionMode.MULTI);

        //grid.addColumn(createEmployeeTemplateRenderer()).setHeader("Name des Postfachs")
        //        .setAutoWidth(true).setResizable(true);

        grid.addColumn(Mailbox::getNAME).setHeader("Name")
                .setWidth("22em").setFlexGrow(0).setResizable(true).setSortable(true);

        Grid.Column<Mailbox> DaystoExpireColumn = grid.addColumn((Mailbox::getDAYSTOEXPIRE)).setHeader("Zert. Ablauf")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> RoleIDColumn = grid.addColumn((Mailbox::getROLEID)).setHeader("Role ID")
                .setWidth("12em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> EgvpPFColumn = grid.addColumn((Mailbox::getStatus)).setHeader("EGVP-E PF Status")
                .setWidth("10em").setFlexGrow(0).setResizable(true).setSortable(true);
        grid.addColumn((Mailbox::getIn_egvp_wartend)).setHeader("wartend in EGVP-E")
                .setWidth("10em").setFlexGrow(0).setResizable(true).setSortable(true);
        //    .setWidth("4em").setFlexGrow(0);
        Grid.Column<Mailbox> inVerarbeitungColumn = grid.addColumn((Mailbox::getAktuell_in_eKP_verarbeitet)).setHeader("in Verarbeitung")
                .setWidth("10em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> haengendColumn = grid.addColumn((Mailbox::getIn_ekp_haengend)).setHeader("hängend")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> FHColumn = grid.addColumn((Mailbox::getIn_ekp_fehlerhospital)).setHeader("im FH")
                .setWidth("6em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> KONVERTIERUNGSDIENSTEColumn = grid.addColumn(Mailbox::getKONVERTIERUNGSDIENSTE).setHeader("hat Konvertierungsdienst")
                .setWidth("12em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> maxMessageCountColumn = grid.addColumn(Mailbox::getMAX_MESSAGE_COUNT).setHeader("Max Message Count")
                .setWidth("12em").setFlexGrow(0).setResizable(true).setSortable(true);
        grid.addColumn(new ComponentRenderer<>(item -> {

            ProgressBar progressBar = new ProgressBar();

            // Wert zwischen 0 und 1
            Double p = Double.valueOf(item.getAktuell_in_eKP_verarbeitet()) / Double.valueOf(item.getMAX_MESSAGE_COUNT());
            progressBar.setValue(p);
            Double rounded;
            p = p*100;
            rounded = (double) Math.round(p);
            Text t = new Text(rounded.toString() + "%");
            HorizontalLayout hl = new HorizontalLayout();
            hl.add(t,progressBar);

            return hl;
        })).setHeader("Auslastung").setWidth("100px").setResizable(true);
        grid.addColumn(createStatusComponentRenderer()).setHeader("Status")
                .setAutoWidth(true).setResizable(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setThemeName("dense");

//        grid.addComponentColumn(mb -> createStatusIcon(mb.getQUANTIFIER()))
//                //.setTooltipGenerator(person -> person.getStatus())
//                .setWidth("4em").setFlexGrow(0)
//                .setHeader("Status");

//        grid.addColumn(
//                new NativeButtonRenderer<>(
////                          "Switch",
//                        item -> {
//                            // Determine the switch label based on the QUANTIFIER value
//                            return item.getQUANTIFIER() == 0 ? "Einschalten" : "Ausschalten";
//                        },
//                        clickedItem -> {
//                            String verbindung = comboBox.getValue().getName();
//                            if (clickedItem.getQUANTIFIER()==0) {
//
//                                //   clickedItem.setQUANTIFIER(1);
//                                String result = mailboxService.updateMessageBox(clickedItem,"1", comboBox.getValue());
//                                if(result.equals("Ok")) {
//                                    Notification.show("Postfach " + clickedItem.getUSER_ID() + " wird eingeschaltet...");
//                                    protokollService.logAction(verbindung,clickedItem.getUSER_ID() + " wurde eingeschaltet.", "");
//                                    updateList();
//                                } else {
//                                    Notification.show(result).addThemeVariants(NotificationVariant.LUMO_ERROR);;
//                                }
//
//                            }
//                            else {
//                                showShutdownReasonDialog(reason -> {
//                                    String result = mailboxService.updateMessageBox(clickedItem, "0", comboBox.getValue());
//                                    if(result.equals("Ok")) {
//                                        Notification.show("Postfach " + clickedItem.getUSER_ID() + " wird ausgeschaltet...");
//                                        protokollService.logAction(verbindung,clickedItem.getUSER_ID() + " wurde ausgeschaltet.", reason);
//                                        updateList();
//                                    }  else {
//                                        Notification.show(result).addThemeVariants(NotificationVariant.LUMO_ERROR);;
//                                    }
//                                });
//                            }
//                            // clickedItem.setIsActive(false);
//                            // clickedItem.setLastName("Huhu");
//
//
//                        })
//        );




        grid.setItemDetailsRenderer(createPersonDetailsRenderer());
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);


        //  inVerarbeitungColumn.setVisible(false);
        RoleIDColumn.setVisible(false);
        EgvpPFColumn.setVisible(false);
        DaystoExpireColumn.setVisible(false);

        haengendColumn.setVisible(false);
        FHColumn.setVisible(false);
        KONVERTIERUNGSDIENSTEColumn.setVisible(false);

        Button menuButton = new Button("Show/Hide Columns");
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(menuButton);
        columnToggleContextMenu.addColumnToggleItem("Zert. Ablauf", DaystoExpireColumn);
        columnToggleContextMenu.addColumnToggleItem("Role-ID", RoleIDColumn);
        columnToggleContextMenu.addColumnToggleItem("EGVP PF Status", EgvpPFColumn);
        columnToggleContextMenu.addColumnToggleItem("in Verarbeitung", inVerarbeitungColumn);
        columnToggleContextMenu.addColumnToggleItem("hängende Nachrichten", haengendColumn);
        columnToggleContextMenu.addColumnToggleItem("im Fehlerhospital", FHColumn);
        columnToggleContextMenu.addColumnToggleItem("Konvertierungsdienste", KONVERTIERUNGSDIENSTEColumn);

    }

    private void setChecker(String status) {
        // Update checked state of menu items
        check_menu.getItems().forEach(item -> item.setChecked(item.getText().equals(status)));
        syscheck.setText(status);
        logger.info("setChecker: " +status);


    }
    private void showLogDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("500px");

        Grid<Protokoll> grid = new Grid<>(Protokoll.class);
        List<Protokoll> protokollList = protokollService.findAllLogsOrderedByZeitpunktDesc();
        String targetVerbindung = comboBox.getValue().getName();  // Replace with the verbindung value you're searching for

        List<Protokoll> filteredProtokollList = protokollList.stream()
                .filter(protokoll -> targetVerbindung.equals(protokoll.getVerbindung()))
                .collect(Collectors.toList());

        grid.setItems(filteredProtokollList);

        grid.setColumns("id", "username", "zeitpunkt", "info", "shutdownReason", "verbindung");
        grid.getColumnByKey("id").setHeader("ID").setResizable(true).setAutoWidth(true);
        grid.getColumnByKey("username").setHeader("Username").setResizable(true).setAutoWidth(true);
        grid.getColumnByKey("verbindung").setHeader("Verbindung").setResizable(true).setAutoWidth(true);
        grid.getColumnByKey("zeitpunkt").setHeader("Zeitpunkt").setResizable(true).setAutoWidth(true);
        grid.getColumnByKey("info").setHeader("Info").setResizable(true).setAutoWidth(true);
        grid.getColumnByKey("shutdownReason").setHeader("Shutdown Reason").setResizable(true).setAutoWidth(true);

        dialog.add(grid);

        Button cancelButton = new Button("Close");
        dialog.getFooter().add(cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();


    }

    private void showShutdownReasonDialog(Consumer<String> onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Bitte Shutdown Grund angeben");

        TextArea reasonTextArea = new TextArea("Grund");
        reasonTextArea.setWidthFull();

        Button confirmButton = new Button("Bestätigen", event -> {
            String reason = reasonTextArea.getValue();
            if (reason != null && !reason.isEmpty()) {
                onConfirm.accept(reason);
                dialog.close();
            } else {
                Notification.show("Bitte geben Sie einen Grund an", 3000, Notification.Position.MIDDLE);
            }
        });

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        VerticalLayout dialogLayout = new VerticalLayout(reasonTextArea, buttons);

        dialog.add(dialogLayout);
        dialog.open();
    }

    public void checkMailboxWatchdogProcess() {
        Configuration configuration = comboBox.getValue();
        // Only proceed if alerting is set to "On"
        if ("On".equals(syscheck.getText())) {
            try {
                Notification.show("Starting MailboxWatchdog job executing.... " + configuration.getName(), 5000, Notification.Position.MIDDLE);
               // BackgroundJobExecutor.stopJob = false;
                logger.info("checkMailboxWatchdogProcess: Starting MailboxWatchdog job executing for " + configuration.getName());
                scheduleMBWatchdogJob(configuration);
            } catch (Exception e) {
                Notification.show("Error executing job: " +  configuration.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                logger.error("Error executing job: " +  configuration.getName() + " " + e.getMessage());
            }
        } else {
            // If status is "Off", stop all scheduled jobs
            logger.info("checkMailboxWatchdogProcess: Stopping MailboxWatchdog job executing for " + configuration.getName());
            stopMBWatchdogScheduledJobs(configuration);
        }
    }
    public void scheduleMBWatchdogJob(Configuration configuration) throws SchedulerException {
        logger.info("Starting scheduleMBWatchdogJob");
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

        // Fetch monitorAlerting configuration to get the interval
        MonitorAlerting monitorAlerting = mailboxService.fetchEmailConfiguration(configuration);

        jobDataMap.put("startType", "cron");
        jobDataMap.put("monitorAlerting", monitorAlerting);

        JobDetail jobDetail = JobBuilder.newJob(MailboxWatchdogJobExecutor.class)
                .withIdentity("job-mbWatchdog-cron-" + configuration.getId(), "mbWatchdog_group")
                .usingJobData(jobDataMap)
                .build();


        if (monitorAlerting == null || monitorAlerting.getMbWatchdogCron() == null) {
            //System.out.println("No interval set for the configuration. Job will not be scheduled.");
            logger.error("No interval set for the configuration. Job will not be scheduled.");
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

    private void stopMBWatchdogScheduledJobs(Configuration configuration) {
        logger.info("Executing stopMBWatchdogScheduledJobs");
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            JobKey cronJobKey = new JobKey("job-mbWatchdog-cron-" + configuration.getId(), "mbWatchdog_group");

            // Try stopping cron job
            if (scheduler.checkExists(cronJobKey)) {
                if (scheduler.deleteJob(cronJobKey)) {
                    MailboxWatchdogJobExecutor.stopJob = true;
                    System.out.println("stop MBWatchdog job successful "+ configuration.getName());
                    Notification.show("MBWatchdog Cron job " + configuration.getName() + " stopped successfully,," + configuration.getId());
                }
            }

        } catch (Exception e) {
            logger.error("Executing stopMBWatchdogScheduledJobs: Error stopping jobs:");
            Notification.show("Error stopping jobs: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void restartBackgroundCron() {
        try {
            Configuration configuration = comboBox.getValue ();
            if(syscheck.getText().equals("On")) {
                stopMBWatchdogScheduledJobs(configuration);
                scheduleMBWatchdogJob(configuration);
            } else {
                stopMBWatchdogScheduledJobs(configuration);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Failed to restart alert job: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void mailboxWatchdogJobConfigurationDialog() {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Mailbox Watchdog Job Konfiguration");

        TextField watchdogMailEmpfaengerField = new TextField("WATCHDOG_MAIL_EMPFAENGER");
        TextField watchdogMailCCEmpfaengerField = new TextField("WATCHDOG_MAIL_CC_EMPFAENGER");
        TextField watchdogMailBetreffField = new TextField("WATCHDOG_MAIL_BETREFF");
        TextArea watchdogMailTextArea = new TextArea("WATCHDOG_MAIL_TEXT");
        TextField cronField = new TextField("CRON_EXPRESSION");
        Checkbox aktiv = new Checkbox("Mailbox Watchdog aktiv");

        watchdogMailEmpfaengerField.setWidth("100%");
        watchdogMailCCEmpfaengerField.setWidth("100%");
        watchdogMailBetreffField.setWidth("100%");
        watchdogMailTextArea.setWidth("100%");
        cronField.setWidth("100%");
        aktiv.setWidth("100%");

        MonitorAlerting monitorAlerting = mailboxService.fetchEmailConfiguration(comboBox.getValue());

        Optional.ofNullable(monitorAlerting.getMbWatchdogCron()).ifPresent(cronField::setValue);
        aktiv.setValue(monitorAlerting.getIsMBWatchdogActive() != null && monitorAlerting.getIsMBWatchdogActive() != 0);
        Optional.ofNullable(monitorAlerting.getWatchdogMailEmpfaenger()).ifPresent(watchdogMailEmpfaengerField::setValue);
        Optional.ofNullable(monitorAlerting.getWatchdogMailCCEmpfaenger()).ifPresent(watchdogMailCCEmpfaengerField::setValue);
        Optional.ofNullable(monitorAlerting.getWatchdogMailBetreff()).ifPresent(watchdogMailBetreffField::setValue);
        Optional.ofNullable(monitorAlerting.getWatchdogMailText()).ifPresent(watchdogMailTextArea::setValue);

        Button saveButton = new Button("Save", event -> {

            monitorAlerting.setWatchdogMailEmpfaenger(watchdogMailEmpfaengerField.getValue());
            monitorAlerting.setWatchdogMailCCEmpfaenger(watchdogMailCCEmpfaengerField.getValue());
            monitorAlerting.setWatchdogMailBetreff(watchdogMailBetreffField.getValue());
            monitorAlerting.setWatchdogMailText(watchdogMailTextArea.getValue());
            monitorAlerting.setMbWatchdogCron(cronField.getValue());
            monitorAlerting.setIsMBWatchdogActive(aktiv.getValue() ? 1: 0);

            boolean isSuccess = mailboxService.saveMailboxJobConfiguration(monitorAlerting, comboBox.getValue());
            if(isSuccess) {
                if(aktiv.getValue()) {
                    setChecker("On");
                } else {
                    setChecker("Off");
                }
                restartBackgroundCron();
            }

            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Create layout and add components
        VerticalLayout layout = new VerticalLayout(
                watchdogMailEmpfaengerField,
                watchdogMailCCEmpfaengerField,
                watchdogMailBetreffField,
                watchdogMailTextArea,
                cronField,
                aktiv,
                new HorizontalLayout(saveButton, cancelButton)
        );
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setMargin(true);
        layout.setWidth("500px");

        // Add layout to dialog
        dialog.add(layout);
        dialog.setWidth("600px");
        dialog.open();
    }

    private Icon createStatusIcon(Integer quantifier) {
        //boolean isAvailable = "Available".equals(status);

        Icon icon;
        if (quantifier == 1) {
            icon = VaadinIcon.CHECK.create();
            switchLable = "Ausschalten";
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            switchLable = "Einschalten";
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }


    private static class ColumnToggleContextMenu extends ContextMenu {
        public ColumnToggleContextMenu(Component target) {
            super(target);
            setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<Mailbox> column) {
            MenuItem menuItem = this.addItem(label, e -> {
                column.setVisible(e.getSource().isChecked());
            });
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
        }
    }


    private boolean tableExists(String tableName) {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM all_tables WHERE table_name = ?";
            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);

            return tableCount > 0;
        } catch (Exception e) {
            System.out.println("Exception while checking table existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private void connectWithDefaultDatabase() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(defaultJdbcUrl);
        ds.setUsername(defaultUsername);
        ds.setPassword(defaultPassword);
        this.jdbcTemplate = new JdbcTemplate(ds);
    }

    private List<MailboxShutdown> fetchTableData() {

        String tableName = getTableName();
        List<MailboxShutdown> results = new ArrayList<>();

        try {
            connectWithDefaultDatabase();

            if (tableExists(tableName)) {

                String sql = "SELECT * FROM \"" + tableName + "\"";

                System.out.println("Executing SQL: " + sql);

                results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    MailboxShutdown mailboxShutdown = new MailboxShutdown();
                    mailboxShutdown.setMailboxId(rs.getString("mailbox_id"));
                    mailboxShutdown.setShutdownReason(rs.getString("shutdown_reason"));
                    return mailboxShutdown;
                });

                System.out.println("Data fetched successfully.");
            } else {
                System.out.println("Table does not exist: " + tableName);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose(jdbcTemplate);
        }

        return results;
    }

    private String getTableName() {
        Configuration conf = comboBox.getValue();
        return "FVMADMIN_MB_" + conf.getName().replace("-", "_").replace(" ", "").trim() + "_SHUTDOWN";
    }


    private void updateList() {

     //mailboxen = mailboxService.getMailboxes(comboBox.getValue());

        // people.get(1).setLastName("hhh");

        if(mailboxen != null && comboBox.getValue() != null) {
            grid.setItems(mailboxen);
        }
   //     System.out.println("update mailbox: "+mailboxen.size());
   //     for (Mailbox mailbox : mailboxen) {
   //         System.out.println("Mailbox Name: " + mailbox.getNAME() +
   //                 ", Aktuell_in_eKP_verarbeitet: " + mailbox.getAktuell_in_eKP_verarbeitet());
   //     }
    }

    private static Renderer<Mailbox> createEmployeeTemplateRenderer() {
  /*      return LitRenderer.<Mailbox>of(
                        "<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <h4> ${item.Name} </h4>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      (${item.User_ID})" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.Court_ID} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      (${item.Typ})" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "</vaadin-horizontal-layout>")
                .withProperty("Name", Mailbox::getNAME)
                .withProperty("User_ID", Mailbox::getUSER_ID)
                .withProperty("Court_ID", Mailbox::getCOURT_ID)
                .withProperty("Quantifier", Mailbox::getQUANTIFIER)
                .withProperty("Typ", Mailbox::getTYP)
                .withProperty("Konvertierungsdienste", Mailbox::getKONVERTIERUNGSDIENSTE)
                ;
*/
        return LitRenderer.<Mailbox>of(
                        "<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <h4> ${item.Name} </h4>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      (${item.User_ID})" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "</vaadin-horizontal-layout>")
                .withProperty("Name", Mailbox::getNAME)
                .withProperty("User_ID", Mailbox::getUSER_ID)
                .withProperty("Court_ID", Mailbox::getCOURT_ID)
                .withProperty("Quantifier", Mailbox::getQUANTIFIER)
                .withProperty("Typ", Mailbox::getTYP)
                .withProperty("Konvertierungsdienste", Mailbox::getKONVERTIERUNGSDIENSTE)
                ;

    }

  /*  private static final SerializableBiConsumer<Span, Mailbox> statusComponentUpdater = (span, Mailbox) -> {

        if (Mailbox.getQUANTIFIER()==0){
            String theme = String.format("badge %s", "error");
            span.getElement().setAttribute("theme", theme);
            span.setText("offline");
            }
            else
            {
                String theme = String.format("badge %s", "success");
                span.getElement().setAttribute("theme", theme);
                span.setText("online");
            }



    };

    private static ComponentRenderer<Span, Mailbox> createStatusComponentRenderer() {
        return new ComponentRenderer<>(Span::new, statusComponentUpdater);
    }*/

    private static final SerializableBiConsumer<Button, Mailbox> statusComponentUpdater = (button, Mailbox) -> {

        if (Mailbox.getQUANTIFIER()==0){
            String theme = String.format("badge %s", "error");
            button.getElement().setAttribute("theme", theme);
            button.setText("offline");
        }
        else
        {
            String theme = String.format("badge %s", "success");
            button.getElement().setAttribute("theme", theme);
            button.setText("online");
        }



    };

    private static ComponentRenderer<Button, Mailbox> createStatusComponentRenderer() {
        return new ComponentRenderer<>(Button::new, statusComponentUpdater);
    }


    private static ComponentRenderer<PersonDetailsFormLayout, Mailbox> createPersonDetailsRenderer() {
        return new ComponentRenderer<>(PersonDetailsFormLayout::new,
                PersonDetailsFormLayout::setPerson);
    }
    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField safeIDField = new TextField("Safe-ID");
        private final TextField courtIDField = new TextField("Court-ID");
        private final TextField TypField = new TextField("Typ");
        private final TextField MaxMessageCountField = new TextField("Max_Message_Count");

        public PersonDetailsFormLayout() {
            Stream.of(safeIDField, courtIDField, TypField,MaxMessageCountField).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });

            setResponsiveSteps(new ResponsiveStep("0", 4));
            //setColspan(safeIDField, 2);
            //setColspan(courtIDField, 2);
            //setColspan(TypField, 2);
        }

        public void setPerson(Mailbox mailbox) {
            safeIDField.setValue(mailbox.getUSER_ID());
            courtIDField.setValue(mailbox.getCOURT_ID());
            TypField.setValue(mailbox.getTYP());
            if(mailbox.getMAX_MESSAGE_COUNT() != null) {
                MaxMessageCountField.setValue(mailbox.getMAX_MESSAGE_COUNT());
            }
        }
    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(com.example.application.data.entity.Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(com.example.application.data.entity.Configuration.decodePassword(conf.getPassword()));
        try {
             jdbcTemplate.setDataSource(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            jdbcTemplate.getDataSource().getConnection().close();
//            connection = jdbcTemplate.getDataSource().getConnection();
//            dataSource = jdbcTemplate.getDataSource();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();

                    if (dataSource instanceof HikariDataSource) {
                        ((HikariDataSource) dataSource).close();
                    }

                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }
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
                    //e.printStackTrace();
                    logger.info("Einer der Subscriber konnte nicht informiert werden über message: >" + message + "<");

                }
            });
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

    private void updateJobManagerSubscription() {
    //    UI ui = getUI().orElse(null);

        if (ui != null) {
            if (subscriber != null) {
                return; // Already subscribed
            }

            subscriber = message -> ui.access(() -> {
                if (!ui.isAttached()) {
                    return; // UI is detached, stop processing
                }
                updateList();
            });

            subscribe(subscriber);
        } else {
            unsubscribe();
        }
    }

    private void updateGridItem(String message) {
        String[] parts = message.split(",,");
        String userID  = parts[0].trim();
        long configId = Integer.parseInt(parts[1].trim());
        Configuration configuration = service.findByIdConfiguration(configId);
        logger.info("updateGridItem: updatedMailbox in grid  "+message);

        Mailbox updatedMailbox = mailboxService.getUpdatedMailboxe(configuration, userID);
        if (userID != null) {
            mailboxen.replaceAll(mailbox ->
                    mailbox.getUSER_ID().equals(updatedMailbox.getUSER_ID()) ? updatedMailbox : mailbox
            );
            grid.setItems(mailboxen);
            System.out.println("Grid update sucessfully----------------------------");
        }

    }
}

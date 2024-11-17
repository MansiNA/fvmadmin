package com.example.application.views;

import com.example.application.data.entity.*;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.data.service.UserService;
import com.example.application.security.AuthenticatedUser;
import com.example.application.service.CockpitService;
import com.example.application.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

//@Route(value = "")

@CssImport(value = "./styles/textfield.css", themeFor = "vaadin-text-area")
public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;
    private JobDefinitionService jobDefinitionService;
    private ConfigurationService configurationService;
    private CockpitService cockpitService;

    private final UserService userService;

    public static boolean isAdmin;
    boolean isPFUser =checkPFRole();

    boolean isUser =checkUserRole();
    public static List<String> userRoles;
    public static String userName;

    private boolean cronAutostart;
    private static int count = 0;
    public MainLayout( @Value("${cron.autostart}") boolean cronAutostart, AuthenticatedUser authenticatedUser, UserService userService, ConfigurationService configurationService, CockpitService cockpitService){


        this.authenticatedUser = authenticatedUser;
        this.userService = userService;
        this.cronAutostart = cronAutostart;
        this.configurationService = configurationService;
        this.cockpitService = cockpitService;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Get all roles assigned to the user
        userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        userRoles.forEach(role -> System.out.println("-----------------Role: " + role));

        isAdmin = checkAdminRole();
        userName = authentication.getName();

//        if (count == 0) {
//            count++; // Increment count only once
//
//            if (cronAutostart) {
//                allCronJobSart();
//            }
//
//            if ("On".equals(emailAlertingAutostart)) {
//                allMonitorCronStart();
//            }
//        }

        createHeader();
        createDrawer();

    }

    private void allMonitorCronStart() {
        List<Configuration> configList = configurationService.findMessageConfigurations();
        List<Configuration> monitoringConfigs = configList.stream()
                .filter(config -> config.getIsMonitoring() != null && config.getIsMonitoring() == 1)
                .collect(Collectors.toList());
      //  for(Configuration configuration)
        // Fetch email configurations
        for(Configuration configuration : monitoringConfigs) {
            try {
                cockpitService.deleteLastAlertTimeInDatabase(configuration);
                scheduleEmailMonitorJob(configuration);
            } catch (Exception e) {
                //        JobManagerView.allCronButton.setText("Cron Start");
                Notification.show("Error executing job: " + configuration.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }
    }

    public void scheduleEmailMonitorJob(Configuration configuration) throws SchedulerException {
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


        // Fetch monitorAlerting configuration to get the interval
        MonitorAlerting monitorAlerting = cockpitService.fetchEmailConfiguration(configuration);
        if (monitorAlerting == null || monitorAlerting.getCron() == null) {
            System.out.println("No interval set for the configuration. Job will not be scheduled.");
            return;
        }

       // int interval = monitorAlerting.getIntervall(); // assuming this returns the interval in minutes
        String cronExpression = monitorAlerting.getCron();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-alert-cron-" + configuration.getId(), "Email_group")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private String createCronExpression(int interval) {
        // Cron expression format for every N minutes
        return "0 0/" + interval + " * * * ?";
    }
    private void  allCronJobSart(){
        System.out.println("all cron start");
        jobDefinitionService = SpringContextHolder.getBean(JobDefinitionService.class);

      //  List<JobManager> jobManagerList = jobDefinitionService.findAll();
        List<JobManager> filterJobsList = jobDefinitionService.getFilteredJobManagers();
        JobManagerView.notifySubscribers("start running all...");
        for (JobManager jobManager : filterJobsList) {
            try {
                System.out.println(jobManager.getName()+"mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm");
                if(jobManager.getAktiv() == 1) {
                    String type = jobManager.getTyp();
                    if (jobManager.getCron() != null && !type.equals("Node") ) {
                        System.out.println(jobManager.getName()+"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
                        scheduleJob(jobManager,  Constants.CRON);
                    }
                }
            } catch (Exception e) {
        //        JobManagerView.allCronButton.setText("Cron Start");
                Notification.show("Error executing job: " + jobManager.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
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

//    @EventListener(ApplicationReadyEvent.class)
//    public void onApplicationReady() {
//        if (cronAutostart) {
//            System.out.println("Cron jobs will be started automatically");
//            startAllCronJobs();
//        } else {
//            System.out.println("Cron jobs will not be started automatically");
//        }
//    }
//
//    private void startAllCronJobs() {
//        JobDefinitionService jobDefinitionService = SpringContextHolder.getBean(JobDefinitionService.class);
//        List<JobManager> jobManagerList = jobDefinitionService.findAll();
//
//        //   JobManagerView.allCronButton.setText("Cron Stop");
//        //   JobManagerView.notifySubscribers("Start running all cron jobs...");
//
//        for (JobManager jobManager : jobManagerList) {
//            try {
//                String type = jobManager.getTyp();
//                if (jobManager.getCron() != null && !type.equals("Node") && !type.equals("Jobchain")) {
//                    scheduleJob(jobManager);
//                }
//            } catch (Exception e) {
//                //         JobManagerView.allCronButton.setText("Cron Start");
//                //          Notification.show("Error executing job: " + jobManager.getName() + " " + e.getMessage(), 5000, Notification.Position.MIDDLE);
//            }
//        }
//    }
//
//    public void scheduleJob(JobManager jobManager) throws SchedulerException {
//        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
//        scheduler.start();
//
//        JobDataMap jobDataMap = new JobDataMap();
//        try {
//            jobDataMap.put("jobManager", JobDefinitionUtils.serializeJobDefinition(jobManager));
//        } catch (JsonProcessingException e) {
//            //      Notification.show("Error serializing job definition: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
//            return;
//        }
//
//        jobDataMap.put("startType", "cron");
//
//        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
//                .withIdentity("job-cron-" + jobManager.getId(), "group1")
//                .usingJobData(jobDataMap)
//                .build();
//
//        Trigger trigger = TriggerBuilder.newTrigger()
//                .withIdentity("trigger-cron-" + jobManager.getId(), "group1")
//                .withSchedule(CronScheduleBuilder.cronSchedule(jobManager.getCron()))
//                .forJob(jobDetail)
//                .build();
//
//        scheduler.scheduleJob(jobDetail, trigger);
//    }
    private void createHeader() {
        H1 logo = new H1("eKP Web-Admin");
        logo.addClassNames("text-l","m-m");

      /*  String principal = "Michael@dbuss.de";
        String credentials ="gfdgfd";
        Authentication user= new UsernamePasswordAuthenticationToken(principal, credentials);
        SecurityContextHolder.getContext().setAuthentication(user);*/



        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();

        Button logout = new Button("Log out " + currentUserName, e -> {
            authenticatedUser.logout();
        });
        Button resetPassword = new Button("Reset Password");
        resetPassword.setVisible(false);

        if (currentUserName.equals("anonymousUser"))
        {
            logout.setVisible(false);
        } else {
            logout.setVisible(true);
            resetPassword.setVisible(true);
            if(userRoles.size() == 1 && isAdmin) {
                resetPassword.setVisible(false);
            }
        }

        resetPassword.addClickListener(event -> {
            Optional<User> user = authenticatedUser.get();
            resetPasswordDialog(user.get());
        });
        if (isAdmin) {

            System.out.println("Ein Admin ist angemeldet!");
            // Benutzer ist ein Administrator
            // Führen Sie hier den entsprechenden Code aus
        } else {

            System.out.println("Ein normaler User ist angemeldet!");
            // Benutzer ist kein Administrator
            // Führen Sie hier den entsprechenden Code aus
        }



        Image image = new Image("images/dataport.png", "Dataport Image");

        System.out.println("Betriebssystem: " + OSInfoUtil.getOsName());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("angemeldeter User: " + auth.getName());


        HorizontalLayout header= new HorizontalLayout(new DrawerToggle(),logo, logout, resetPassword);

        Span sp= new Span("V1.02");

        header.add(image,sp);


        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        addToNavbar(header);
    }

    private void resetPasswordDialog(User user) {
        VerticalLayout content = new VerticalLayout();
        Dialog resetPasswordDialog = new Dialog();
        resetPasswordDialog.open();
        PasswordField password = new PasswordField("Password");
        PasswordField confirmPassword = new PasswordField("Confirm password");

        FormLayout formLayout = new FormLayout();
        formLayout.add(password, confirmPassword);
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            resetPasswordDialog.close();
        });
        Button addButton = new Button("add");
        addButton.addClickListener(e -> {
            String passwordValue = password.getValue();
            String confirmPasswordValue = confirmPassword.getValue();
            String bCryptPassword = BCrypt.hashpw(passwordValue, BCrypt.gensalt());
            if (validatePassword(passwordValue, confirmPasswordValue)) {
                user.setHashedPassword(bCryptPassword);
                try {
                    userService.update(user);
                    Notification.show("Password reset successfuly", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    resetPasswordDialog.close();
                } catch (Exception ex) {
                    ex.getMessage();
                    Notification.show("Error: Password is not reset", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

            }
        });

        HorizontalLayout hl = new HorizontalLayout(closeButton, addButton);
        hl.setAlignItems(FlexComponent.Alignment.BASELINE);
        content.add(formLayout, hl);
        resetPasswordDialog.add(content);
    }

    private boolean validatePassword(String password, String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            Notification.show("Passwords do not match", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
        return true;
    }
    private boolean checkAdminRole() {

        // Überprüfen, ob der angemeldete Benutzer zur Gruppe "Admin" gehört

        // Erhalten Sie den angemeldeten Benutzer
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Überprüfen, ob der Benutzer authentifiziert ist und nicht anonym
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();

            // Überprüfen, ob der angemeldete Benutzer ein UserDetails-Objekt ist
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;

                // Überprüfen, ob der angemeldete Benutzer die Berechtigung "ROLE_ADMIN" hat
                return userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            }
        }

        return false;

    }


    private boolean checkPFRole() {

        // Überprüfen, ob der angemeldete Benutzer zur Gruppe "Admin" gehört

        // Erhalten Sie den angemeldeten Benutzer
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Überprüfen, ob der Benutzer authentifiziert ist und nicht anonym
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();

            // Überprüfen, ob der angemeldete Benutzer ein UserDetails-Objekt ist
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;

                // Überprüfen, ob der angemeldete Benutzer die Berechtigung "ROLE_ADMIN" hat
                return userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_PF_ADMIN"));

            }
        }

        return false;

    }

    private boolean checkUserRole() {

        // Überprüfen, ob der angemeldete Benutzer zur Gruppe "Admin" gehört

        // Erhalten Sie den angemeldeten Benutzer
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Überprüfen, ob der Benutzer authentifiziert ist und nicht anonym
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();

            // Überprüfen, ob der angemeldete Benutzer ein UserDetails-Objekt ist
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;

                // Überprüfen, ob der angemeldete Benutzer die Berechtigung "ROLE_ADMIN" hat
                return userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"));

            }
        }

        return false;

    }



    private void createDrawer() {
        boolean isFVM = userRoles.contains("ROLE_FVM");
        boolean isCOKPIT = userRoles.contains("ROLE_COKPIT");
        boolean isTVM = userRoles.contains("ROLE_TVM");
//        System.out.println("isFVM "+isFVM);
//        System.out.println("isCOKPIT "+isCOKPIT);
//        System.out.println("isTVM "+isTVM);

        VerticalLayout drawerLayout = new VerticalLayout();

        RouterLink listView = new RouterLink("Info", ListView.class);
        RouterLink mailboxConfig = new RouterLink("Postfach Verwaltung", MailboxConfigView.class);
        RouterLink messageExport = new RouterLink("Message Exporter", MessageExportView.class);
        RouterLink elaFavoriten = new RouterLink("ELA-Upload (geplant)", ElaFavoritenView.class);
        RouterLink tableExport = new RouterLink("Table Export", TableExportView.class);
        RouterLink quarantaeneView = new RouterLink("Quarantäne Info", QuarantaeneView.class);
        RouterLink metadatenView = new RouterLink("METADATEN", MetadatenView.class);
        RouterLink hangingMessagesView = new RouterLink("Hängende Nachrichten", HangingMessagesView.class);
        RouterLink tableView = new RouterLink("Table Viewer", TableView.class);
        RouterLink cockpitView = new RouterLink("eKP-Cockpit", CockpitView.class);
        RouterLink dashboardView = new RouterLink("Dashboard (geplant)", DashboardView.class);
        RouterLink fileBrowserView = new RouterLink("LogFileBrowser", FileBrowserView.class);
        RouterLink configureView = new RouterLink("Configuration", ConfigurationView.class);
        RouterLink userConfigView = new RouterLink("User Configuration", UserConfigurationView.class);
        RouterLink serverConfigView = new RouterLink("Server Configuration", ServerConfigurationView.class);
        RouterLink jobManagerView = new RouterLink("Job Manager", JobManagerView.class);
        RouterLink cronInfoView = new RouterLink("Cron Infoview", CronInfoView.class);
        listView.setHighlightCondition(HighlightConditions.sameLocation());

        RouterLink link = new RouterLink("Login", LoginView.class);

        if (isAdmin) {
            drawerLayout.add(
                    //    tableExport,
                    //    mailboxConfig,
                    //    elaFavoriten,
                    //    listView,
                    tableView,
                    metadatenView,
                    messageExport,
                    mailboxConfig,
                    cockpitView,
                    fileBrowserView,
                    quarantaeneView,
                    hangingMessagesView,
                    elaFavoriten,
                    dashboardView,
                    jobManagerView,
                    userConfigView,
                    serverConfigView,
                    cronInfoView
            );
        }

        if (isPFUser) {
            drawerLayout.add(mailboxConfig);
        }

        if (isUser) {
            drawerLayout.add(tableView, metadatenView);
        }

        if (isFVM) {
            drawerLayout.add(
                    tableView,
                    metadatenView,
                    messageExport,
                    cockpitView,
                    fileBrowserView,
                    jobManagerView,
                    hangingMessagesView,
                    quarantaeneView,
                    cronInfoView
            );
        }

        if (isCOKPIT) {
            drawerLayout.add(cockpitView);
        }

        if (isTVM) {
            drawerLayout.add(tableView);
        }

        // If the user doesn't have any of the expected roles, show the login link
        if (drawerLayout.getComponentCount() == 0) {
            drawerLayout.add(link);
        }

        // Add the layout to the drawer
        addToDrawer(drawerLayout);

    }
}

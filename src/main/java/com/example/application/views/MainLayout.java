package com.example.application.views;

import com.example.application.data.entity.User;
import com.example.application.data.service.UserService;
import com.example.application.security.AuthenticatedUser;
import com.example.application.utils.OSInfoUtil;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//@Route(value = "")

@CssImport(value = "./styles/textfield.css", themeFor = "vaadin-text-area")
public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;
    private final UserService userService;

    public static boolean isAdmin;
    boolean isPFUser =checkPFRole();

    boolean isUser =checkUserRole();
    public static List<String> userRoles;
    public static String userName;

    public MainLayout(AuthenticatedUser authenticatedUser, UserService userService){


        this.authenticatedUser = authenticatedUser;
        this.userService = userService;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Get all roles assigned to the user
        userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        userRoles.forEach(role -> System.out.println("-----------------Role: " + role));

        isAdmin = checkAdminRole();
        userName = authentication.getName();
        createHeader();
        createDrawer();
    }

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
        System.out.println("isFVM "+isFVM);
        System.out.println("isCOKPIT "+isCOKPIT);
        System.out.println("isTVM "+isTVM);

        RouterLink listView = new RouterLink("Info", ListView.class);
        RouterLink mailboxConfig = new RouterLink("Postfach Verwaltung", MailboxConfigView.class);
        RouterLink MessageExport = new RouterLink("Message Exporter", MessageExportView.class);
        RouterLink elaFavoriten = new RouterLink("ELA-Upload (geplant)", ElaFavoritenView.class);
        RouterLink tableExport = new RouterLink("Table Export", TableExportView.class);
        RouterLink quarantaeneView = new RouterLink("Quarantäne Info", QuarantaeneView.class);
        RouterLink metadatenView = new RouterLink("METADATEN", MetadatenView.class);
        RouterLink hangingMessagesView = new RouterLink("Hängende Nachrichten", HangingMessagesView.class);
        RouterLink tableView = new RouterLink("Table Viewer", TableView.class);
        RouterLink cockpitView = new RouterLink("eKP-Cockpit", CockpitView.class);
        RouterLink DashboardView = new RouterLink("Dashboard (geplant)", DashboardView.class);
        RouterLink fileBrowserView = new RouterLink("LogFileBrowser", FileBrowserView.class);
        RouterLink configureView = new RouterLink("Configuration", ConfigurationView.class);
        RouterLink userConfigView = new RouterLink("User Configuration", UserConfigurationView.class);
        RouterLink serverConfigView = new RouterLink("Server Configuration", ServerConfigurationView.class);
        RouterLink jobManagerView = new RouterLink("Job Manager", JobManagerView.class);
        listView.setHighlightCondition(HighlightConditions.sameLocation());


        RouterLink link = new RouterLink("Login", LoginView.class);


        if (isAdmin) {
            addToDrawer(new VerticalLayout(
                    //    tableExport,
                    //    mailboxConfig,
                    //    elaFavoriten,
                    //    listView,
                    tableView,
                    metadatenView,
                    MessageExport,
                    mailboxConfig,
                    cockpitView,
                    fileBrowserView,
                    quarantaeneView,
                    hangingMessagesView,
                    elaFavoriten,
                    DashboardView,
                    //   configureView,
                    jobManagerView,
                    userConfigView,
                    serverConfigView
            ));

        } else if (isPFUser) {
            addToDrawer(new VerticalLayout(
                    mailboxConfig
            ));
        } else if (isUser) {
            addToDrawer(new VerticalLayout(
                    tableView,
                    metadatenView
                    //    mailboxConfig
                    //    cockpitView,
                    //     quarantaeneView
            ));
        } else if (isFVM) {
            addToDrawer(new VerticalLayout(
            tableView,
            metadatenView,
            cockpitView,
            fileBrowserView,
                    jobManagerView,
                    hangingMessagesView,
            fileBrowserView,
            quarantaeneView

            ));
        } else if (isCOKPIT) {
            addToDrawer(new VerticalLayout(
                    cockpitView
            ));
        } else if (isTVM) {
            addToDrawer(new VerticalLayout(
                    tableView
            ));
        } else {
            addToDrawer(new VerticalLayout(link));
        }


    }



}

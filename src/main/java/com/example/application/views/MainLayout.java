package com.example.application.views;

import com.example.application.security.SecurityService;
import com.example.application.utils.OSInfoUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

//@Route(value = "")

@CssImport(value = "./styles/textfield.css", themeFor = "vaadin-text-area")
public class MainLayout extends AppLayout {

    private final SecurityService securityService;

    public static boolean isAdmin;
    boolean isPFUser =checkPFRole();

    boolean isUser =checkUserRole();
    public static List<String> userRoles;
    public MainLayout(SecurityService securityService){


        this.securityService = securityService;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Get all roles assigned to the user
        userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        isAdmin = checkAdminRole();

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
            securityService.logout();
        });
        if (currentUserName.equals("anonymousUser"))
        {
            logout.setVisible(false);
        }
        else
        {
            logout.setVisible(true);
        }



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


        HorizontalLayout header= new HorizontalLayout(new DrawerToggle(),logo, logout);

        Span sp= new Span("V1.02");

        header.add(image,sp);


        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        addToNavbar(header);
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


        RouterLink listView = new RouterLink ("Info", ListView.class);
        RouterLink mailboxConfig = new RouterLink ("Postfach Verwaltung", MailboxConfigView.class);
        RouterLink MessageExport = new RouterLink ("Message Exporter", MessageExportView.class);
        RouterLink elaFavoriten = new RouterLink ("ELA-Upload (geplant)", ElaFavoritenView.class);
        RouterLink tableExport = new RouterLink ("Table Export", TableExportView.class);
        RouterLink quarantaeneView = new RouterLink ("Quarantäne Info", QuarantaeneView.class);
        RouterLink metadatenView = new RouterLink ("METADATEN", MetadatenView.class);
        RouterLink hangingMessagesView = new RouterLink ("Hängende Nachrichten", HangingMessagesView.class);
        RouterLink tableView = new RouterLink ("Table Viewer", TableView.class);
        RouterLink cockpitView = new RouterLink ("eKP-Cockpit", CockpitView.class);
        RouterLink DashboardView = new RouterLink ("Dashboard (geplant)", DashboardView.class);
        RouterLink fileBrowserView = new RouterLink ("LogFileBrowser", FileBrowserView.class);
        RouterLink configureView = new RouterLink ("Configuration", ConfigurationView.class);
        RouterLink userConfigView = new RouterLink ("User Configuration", UserConfigurationView.class);
        listView.setHighlightCondition(HighlightConditions.sameLocation() );


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
                    configureView,
                    userConfigView
            ));

        }
        else if (isPFUser){
            addToDrawer(new VerticalLayout(
                    mailboxConfig
            ));
        }
        else if (isUser){
            addToDrawer(new VerticalLayout(
                    tableView,
                    metadatenView
                //    mailboxConfig
                //    cockpitView,
               //     quarantaeneView
            ));
        }
        else
        {
            addToDrawer(new VerticalLayout(link));
        }





    }



}

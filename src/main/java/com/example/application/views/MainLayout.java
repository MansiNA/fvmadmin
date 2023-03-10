package com.example.application.views;

import com.example.application.security.SecurityService;
import com.example.application.utils.OSInfoUtil;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

//@Route(value = "")
public class MainLayout extends AppLayout {

    private final SecurityService securityService;
    public MainLayout(SecurityService securityService){
        createHeader();
        createDrawer();
        this.securityService = securityService;
    }

    private void createHeader() {
        H1 logo = new H1("FVM Admin Tool");
        logo.addClassNames("text-l","m-m");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();

        Button logout = new Button("Log out " + currentUserName, e -> securityService.logout());

        Image image = new Image("images/dataport.png", "Dataport Image");

        System.out.println("Betriebssystem: " + OSInfoUtil.getOsName());

        HorizontalLayout header= new HorizontalLayout(new DrawerToggle(),logo, logout);
        header.add(image);


        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        addToNavbar(header);
    }

    private void createDrawer() {


        RouterLink listView = new RouterLink ("Info", ListView.class);
        RouterLink mailboxConfig = new RouterLink ("Postfach Verwaltung", MailboxConfigView.class);
        RouterLink MessageExport = new RouterLink ("Message Exporter", MessageExportView.class);
        RouterLink elaFavoriten = new RouterLink ("ELA-Upload (geplant)", ElaFavoritenView.class);
        RouterLink tableExport = new RouterLink ("Table Export", TableExportView.class);
        RouterLink quarantaeneView = new RouterLink ("Quarantäne Info", QuarantaeneView.class);
        RouterLink metadatenView = new RouterLink ("METADATEN", MetadatenView.class);
        RouterLink hangingMessagesView = new RouterLink ("Hängende Nachrichten (geplant)", HangingMessagesView.class);
        RouterLink tableView = new RouterLink ("Table Viewer", TableView.class);
        RouterLink DashboardView = new RouterLink ("Dashboard (geplant)", DashboardView.class);
        RouterLink fileBrowserView = new RouterLink ("LogFileBrowser (geplant)", FileBrowserView.class);
        listView.setHighlightCondition(HighlightConditions.sameLocation() );

        addToDrawer(new VerticalLayout(
            //    tableExport,
            //    mailboxConfig,
            //    elaFavoriten,
            //    listView,
                tableView,
                metadatenView,
                MessageExport,
                mailboxConfig,
                quarantaeneView,
                hangingMessagesView,
                fileBrowserView,
                elaFavoriten,
                DashboardView
        ));

    }



}

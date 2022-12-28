package com.example.application.views;

import com.example.application.uils.OSInfoUtil;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;

//@Route(value = "")
public class MainLayout extends AppLayout {


    public MainLayout(){
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Dataport FVM Admin Tool");
        logo.addClassNames("text-l","m-m");


        Image image = new Image("images/dataport.png", "Dataport Image");

        System.out.println("Betriebssystem: " + OSInfoUtil.getOsName());

        HorizontalLayout header= new HorizontalLayout(new DrawerToggle(),logo);
        header.add(image);


        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        addToNavbar(header);
    }

    private void createDrawer() {


        RouterLink listView = new RouterLink ("Info", ListView.class);
        RouterLink mailboxConfig = new RouterLink ("Postfach Verwaltung (geplant)", MailboxConfigView.class);
        RouterLink MessageExport = new RouterLink ("Message Exporter", MessageExportView.class);
        RouterLink elaFavoriten = new RouterLink ("ELA-Upload (geplant)", ElaFavoritenView.class);
        RouterLink tableExport = new RouterLink ("Table Export", TableExportView.class);
        RouterLink tableView = new RouterLink ("Table Viewer", TableView.class);
        RouterLink DashboardView = new RouterLink ("Dashboard (geplant)", DashboardView.class);
        listView.setHighlightCondition(HighlightConditions.sameLocation() );

        addToDrawer(new VerticalLayout(
            //    tableExport,
            //    mailboxConfig,
            //    elaFavoriten,
            //    listView,
                tableView,
                MessageExport,
                elaFavoriten,
                mailboxConfig,
                DashboardView
        ));

    }



}

package com.example.application.views;

import com.example.application.views.ListView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {
    public MainLayout(){
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Dataport FVM Admin Tool");
        logo.addClassNames("text-l","m-m");

        HorizontalLayout header= new HorizontalLayout(new DrawerToggle(),logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        addToNavbar(header);
    }

    private void createDrawer() {
        RouterLink listView = new RouterLink ("Info", ListView.class);
      //  RouterLink DashboardView = new RouterLink ("ELA Favoriten", DashboardView.class);
      //  RouterLink MailboxView = new RouterLink ("Postfachverwaltung", ListView.class);
      //  RouterLink TableExportView = new RouterLink ("Tabellen Export", ListView.class);
       // RouterLink DashboardView = new RouterLink ("Dashboard", views.DashboardView.class);
        listView.setHighlightCondition(HighlightConditions.sameLocation() );
        addToDrawer(new VerticalLayout(
            listView//, DashboardView, MailboxView, TableExportView
            , new RouterLink("Dashboard", DashboardView.class)
        ));
    }
}

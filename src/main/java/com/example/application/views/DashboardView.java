package com.example.application.views;

import com.example.application.data.service.CrmService;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@PageTitle("Dashboard | by DBUSS GmbH")
@Route(value = "dashboard", layout= MainLayout.class)
@AnonymousAllowed
public class DashboardView extends VerticalLayout{

    private CrmService service;

    public DashboardView(CrmService service){
        this.service = service;
        //add(new H1("FVM-Status Dashboard"));


        Paragraph paragraph= new Paragraph("Hier ist die Anzeige von aktuellen Metriken aus der DB geplant");
        paragraph.setMaxHeight("400px");


        add(paragraph);

    }



}

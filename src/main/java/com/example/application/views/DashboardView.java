package com.example.application.views;

import com.example.application.data.service.CrmService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Dashboard | by DBUSS GmbH")
@Route(value = "dashboard", layout= MainLayout.class)
public class DashboardView extends VerticalLayout{

    private CrmService service;

    public DashboardView(CrmService service){
        this.service = service;
        add(new H1("FVM-Status Dashboard 1"));


        RichTextEditor rte = new RichTextEditor();
        rte.setMaxHeight("400px");

        rte.setValue("Das ist der Text im Editor!");
        rte.setReadOnly(true);
        add(rte);

    }



}

package com.example.application.views;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Quarantäne Verwaltung")
@Route(value = "quarantaene", layout= MainLayout.class)
public class QuarantaeneView extends VerticalLayout {

    public QuarantaeneView() {

        Paragraph paragraph= new Paragraph("Hier erfolgt eine Auflistung von aktuellen EGVP-E Quarantäne-Nachrichten");
        paragraph.setMaxHeight("400px");


        add(paragraph);

    }
}

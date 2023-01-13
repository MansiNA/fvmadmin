package com.example.application.views;

import com.example.application.data.entity.Metadaten;
import com.example.application.data.service.MetadatenService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Tabelle Metadaten")
@Route(value = "metadaten", layout= MainLayout.class)
//@Route(value = "")
public class MetadatenView extends VerticalLayout {

    Grid<Metadaten> grid = new Grid<>(Metadaten.class);
    TextField filterText = new TextField();
    private MetadatenService service;

    public MetadatenView (MetadatenService service){

        this.service=service;

        add(new H3("Die Tabelle Metadaten"));

        addClassName("list-view");
        setSizeFull();

        configureGrid();
        updateList();

    }

    private void configureGrid() {
        grid.addClassName("metadaten-grid");
        grid.setSizeFull();
        grid.setColumns("NACHRICHTIDINTERN","NACHRICHTIDEXTERN", "STATUS", "FEHLERTAG", "VERARBEITET", "LOESCHTAG"); // primitive Variablen der Klasse können direkt angegeben werden.

        grid.getColumns().forEach(col -> col.setAutoWidth(true));


    }

    private void updateList() {
        grid.setItems(service.findAllContacts((filterText.getValue())));
    }


}

package com.example.application.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Upload ELA-Favoriten | by DBUSS GmbH")
@Route(value = "ela-upload", layout= MainLayout.class)
public class ElaFavoritenView extends VerticalLayout {

    public ElaFavoritenView(){

        add(new H1("Upload der ELA-Favoriten Excel-Liste"));
    }
}

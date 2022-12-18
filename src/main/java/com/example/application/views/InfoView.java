package com.example.application.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("FVM-Admin Tool")
@Route(value = "", layout= MainLayout.class)
public class InfoView extends VerticalLayout {

    public InfoView(){

        add(new H1("Willkommen"));
    }


}

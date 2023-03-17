package com.example.application.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import javax.annotation.security.RolesAllowed;

@PageTitle("Verwaltung hängende Nachrichten")
@Route(value = "hangingmassages", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class HangingMessagesView extends VerticalLayout {
    public HangingMessagesView() {
        add(new H2("Bearbeitung hängende Nachrichten"));
    }
}

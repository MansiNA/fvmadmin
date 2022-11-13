package com.example.application.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Mailbox Verwaltung")
@Route(value = "mailbox-config", layout= MainLayout.class)
public class MailboxConfigView  extends VerticalLayout {

    public MailboxConfigView(){

        add(new H1("Postfach Verwaltung"));
    }
}

package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.service.ConfigurationService;
import com.example.application.views.list.ConfigForm;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Configuration")
@Route(value = "config", layout= MainLayout.class)
public class ConfigurationView extends VerticalLayout {
    ConfigForm cf;
    Configuration config;
    private ConfigurationService service;

    public ConfigurationView(ConfigurationService service) {
        this.service = service;

        cf = new ConfigForm();
        cf.setWidth("25em");
        add(new H1("Konfiguration"));
        add (cf);
        updateView();

       // closeEditor();

        config=new Configuration("User","Password","URL");
        cf.setConfiguration(config);

    }

    private void closeEditor() {
        cf.setConfiguration(null);
        cf.setVisible(false);
        removeClassName("editing");
    }

    private void updateView() {

    }

}

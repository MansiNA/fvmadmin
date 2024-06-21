package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ServerConfiguration;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.ServerConfigurationService;
import com.example.application.views.list.ConfigForm;
import com.example.application.views.list.ServerConfigForm;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;


@PageTitle("Server Configuration")
@Route(value = "serverconfig", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class ServerConfigurationView extends VerticalLayout {
    ServerConfigForm cf;
    Configuration config;
    private ServerConfigurationService service;

    Grid<ServerConfiguration> grid = new Grid<>(ServerConfiguration.class);
    TextField filterText = new TextField();

    public ServerConfigurationView(ServerConfigurationService service) {
        this.service = service;

        addClassName("configuration-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());

        updateList();
        closeEditor();
        //    cf = new ConfigForm();
        //    cf.setWidth("25em");
        //    add(new H1("Konfiguration"));
        //    add (cf);
        //    updateView();

        // closeEditor();

        //     config=new Configuration("User","Password","URL");
        //     cf.setConfiguration(config);

    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, cf);
        content.setFlexGrow(2, grid);


        content.setFlexGrow(1, cf);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        grid.addClassNames("configuration-grid");
        grid.setSizeFull();
        grid.setColumns("hostName", "userName","pathList");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(e->editConfig(e.getValue()));
    }

    private void updateList() {
        grid.setItems(service.findAllConfigurations());
    }

    private void configureForm() {
        cf = new ServerConfigForm();
        cf.setWidth("25em");

        cf.addListener(ServerConfigForm.SaveEvent.class, this::saveConfig);
        cf.addListener(ServerConfigForm.DeleteEvent.class, this::deleteConfig);
        cf.addListener(ServerConfigForm.CloseEvent.class, e-> closeEditor());

    }
    private void saveConfig(ServerConfigForm.SaveEvent event) {
        service.saveConfiguration(event.getConfiguration());
        updateList();
        closeEditor();
    }

    private void deleteConfig(ServerConfigForm.DeleteEvent event) {
        service.deleteConfiguration(event.getConfiguration());
        updateList();
        closeEditor();
    }
    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addContactButton = new Button("Neu");
        addContactButton.addClickListener((e->addContact()));
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addContactButton);


        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addContact() {
        grid.asSingleSelect().clear();
        editConfig(new ServerConfiguration());
    }
    private void closeEditor() {
        cf.setConfiguration(null);
        cf.setVisible(false);
        removeClassName("editing");
    }

    private void updateView() {

    }
    private void editConfig(ServerConfiguration conf) {
        if(conf == null){
            closeEditor();
        } else {
            cf.setConfiguration(conf);
            cf.setVisible(true);
            addClassName("editing");
        }
    }
}

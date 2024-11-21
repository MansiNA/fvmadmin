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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@PageTitle("Server Configuration")
@Route(value = "serverconfig", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class ServerConfigurationView extends VerticalLayout {
    ServerConfigForm cf;
    Configuration config;
    private ServerConfigurationService service;

    Grid<ServerConfiguration> grid = new Grid<>(ServerConfiguration.class);
    TextField filterText = new TextField();
    private static final Logger logger = LoggerFactory.getLogger(ServerConfigurationView.class);
    public ServerConfigurationView(ServerConfigurationService service) {
        logger.info("Starting ServerConfigurationView");
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
        logger.info("starting getContent()");
        HorizontalLayout content = new HorizontalLayout(grid, cf);
        content.setFlexGrow(2, grid);


        content.setFlexGrow(1, cf);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        logger.info("configureGrid(): configure grid for ServerConfiguration");
        grid.addClassNames("configuration-grid");
        grid.setSizeFull();
        grid.setColumns("hostName", "hostAlias", "userName","pathList");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(e->editConfig(e.getValue()));
    }

    private void updateList() {
        logger.info("updateList(): update grid Items");
        grid.setItems(service.findAllConfigurations());
    }

    private void configureForm() {
        logger.info("configureForm(): add form for ServerConfiguration");
        cf = new ServerConfigForm();
        cf.setWidth("25em");

        cf.addListener(ServerConfigForm.SaveEvent.class, this::saveConfig);
        cf.addListener(ServerConfigForm.DeleteEvent.class, this::deleteConfig);
        cf.addListener(ServerConfigForm.CloseEvent.class, e-> closeEditor());

    }
    private void saveConfig(ServerConfigForm.SaveEvent event) {
        try {
            String  result = service.saveConfiguration(event.getConfiguration());
            if(result.equals("Ok")) {
                logger.info("saveConfig(): save ServerConfiguration");
                Notification.show("Upload sceessfully", 3000, Notification.Position.MIDDLE);
            } else {
                logger.error("saveConfig(): Error:"+result);
                Notification.show(result,5000, Notification.Position.MIDDLE);
            }
            updateList();
            closeEditor();
        } catch (Exception e) {
            Notification.show(e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void deleteConfig(ServerConfigForm.DeleteEvent event) {
        try {
            service.deleteConfiguration(event.getConfiguration());
            logger.info("deleteConfig(): delete ServerConfiguration");
            Notification.show("delete sceessfully", 3000, Notification.Position.MIDDLE);
            updateList();
            closeEditor();
        } catch (Exception e) {
            logger.error("deleteConfig(): delete ServerConfiguration");
            Notification.show(e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }
    private HorizontalLayout getToolbar() {
        logger.info("getToolbar(): set toolbar layout");
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
        logger.info("addContact(): add new ServerConfiguration");
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
            logger.info("editConfig(): edit ServerConfiguration");
            cf.setConfiguration(conf);
            cf.setVisible(true);
            addClassName("editing");
        }
    }
}

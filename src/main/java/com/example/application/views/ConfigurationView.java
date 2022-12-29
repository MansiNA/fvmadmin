package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.service.ConfigurationService;
import com.example.application.views.list.ConfigForm;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Configuration")
@Route(value = "config", layout= MainLayout.class)
public class ConfigurationView extends VerticalLayout {
    ConfigForm cf;
    Configuration config;
    private ConfigurationService service;

    Grid<Configuration> grid = new Grid<>(Configuration.class);
    TextField filterText = new TextField();

    public ConfigurationView(ConfigurationService service) {
        this.service = service;

        addClassName("configuration-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());

        updateList();

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
        grid.setColumns("land", "umgebung", "userName", "password","db_Url");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));


    }

    private void updateList() {
        grid.setItems(service.findAllConfigurations());
    }

    private void configureForm() {
        cf = new ConfigForm();
        cf.setWidth("25em");
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addContactButton = new Button("Neu");

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addContactButton);


        toolbar.addClassName("toolbar");
        return toolbar;
    }


    private void closeEditor() {
        cf.setConfiguration(null);
        cf.setVisible(false);
        removeClassName("editing");
    }

    private void updateView() {

    }

}

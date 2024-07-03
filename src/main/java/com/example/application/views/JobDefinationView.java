package com.example.application.views;

import com.example.application.data.GenericDataProvider;
import com.example.application.data.Role;
import com.example.application.data.entity.JobDefinition;
import com.example.application.data.entity.SqlDefinition;
import com.example.application.data.entity.User;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.data.service.SqlDefinitionService;
import com.example.application.data.service.UserService;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@PageTitle("Job Defination")
@Route(value = "jobDefination", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class JobDefinationView extends VerticalLayout {

    private JobDefinitionService jobDefinitionService;
    private Crud<JobDefinition> crud;
    private Grid<JobDefinition> jobDefinitionGrid;
    private Dialog dialog;
    private Dialog resetPasswordDialog;
    private TreeGrid<JobDefinition> treeGrid;

    public JobDefinationView(JobDefinitionService jobDefinitionService) {

        this.jobDefinitionService = jobDefinitionService;

        add(new H2("Job Defination"));

        HorizontalLayout treehl = new HorizontalLayout();
        treehl.setHeightFull();
        treehl.setWidthFull();
        treehl.setSizeFull();

        TreeGrid tg= createTreeGrid();

        treehl.add(tg);
        treehl.setFlexGrow(1, tg);

        //  treehl.setWidthFull();
        treehl.setAlignItems(Alignment.BASELINE);
        setHeightFull();
        setSizeFull();

        add(treehl);


    }

    private TreeGrid<JobDefinition> createTreeGrid() {
        treeGrid = new TreeGrid<>();

        treeGrid.setItems(jobDefinitionService.getRootProjects(), jobDefinitionService::getChildProjects);

        // Add the hierarchy column for displaying the hierarchical data
        treeGrid.addHierarchyColumn(JobDefinition::getName).setHeader("Name").setAutoWidth(true);

        // Add other columns except id and pid
        treeGrid.addColumn(JobDefinition::getNamespace).setHeader("Namespace").setAutoWidth(true);
        treeGrid.addColumn(JobDefinition::getCommand).setHeader("Command").setAutoWidth(true);
        treeGrid.addColumn(JobDefinition::getCron).setHeader("Cron").setAutoWidth(true);
        treeGrid.addColumn(JobDefinition::getTyp).setHeader("Typ").setAutoWidth(true);

        // Set additional properties for the tree grid
        treeGrid.setWidth("350px");
        treeGrid.addExpandListener(event -> System.out.println(String.format("Expanded %s item(s)", event.getItems().size())));
        treeGrid.addCollapseListener(event -> System.out.println(String.format("Collapsed %s item(s)", event.getItems().size())));

        treeGrid.setThemeName("dense");
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_COMPACT);

        treeGrid.asSingleSelect().addValueChangeListener(event -> {
            JobDefinition selectedJob = event.getValue();
            if (selectedJob != null) {
                try {
                    scheduleJob(selectedJob);
                } catch (SchedulerException e) {
                    e.getMessage();
                    Notification.show("job running error", 5000, Notification.Position.MIDDLE);
                }
            }
        });

         if (MainLayout.isAdmin) {
             GridContextMenu<JobDefinition> contextMenu = treeGrid.addContextMenu();
             GridMenuItem<JobDefinition> editItem = contextMenu.addItem("Edit", event -> {
                 showEditAndNewDialog(event.getItem().get(), "Edit");
             });
             GridMenuItem<JobDefinition> newItem = contextMenu.addItem("New", event -> {
                 showEditAndNewDialog(event.getItem().get(), "New");
             });
             GridMenuItem<JobDefinition> deleteItem = contextMenu.addItem("Delete", event -> {
                 deleteTreeGridItem(event.getItem().get());
             });
         }

        return treeGrid;
    }
    private VerticalLayout showEditAndNewDialog(JobDefinition jobDefinition, String context){
        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        JobDefinition newJobDefination = new JobDefinition();

        if(context.equals("New")){
            List<JobDefinition> jobDefinitionList = jobDefinitionService.findAll();
            newJobDefination.setId( (jobDefinitionList.size() + 1));
            newJobDefination.setPid(jobDefinition.getPid());
            dialog.add(editJobDefinition(newJobDefination, true)); // For adding new entry
        } else {
            dialog.add(editJobDefinition(jobDefinition, false)); // For editing existing entry
        }

        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1000px");
        dialog.setHeight("400px");
        Button cancelButton = new Button("Cancel");
        Button saveButton = new Button(context.equals("Edit") ? "Save" : "Add");
        dialog.getFooter().add(saveButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        saveButton.addClickListener(saveEvent -> {
            System.out.println("saved data....");
            if(context.equals("New")) {
                saveSqlDefinition(newJobDefination);
            } else {
                saveSqlDefinition(jobDefinition);
            }
            updateGrid();
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;

    }
    public JobDefinition saveSqlDefinition(JobDefinition jobDefinition) {
        return jobDefinitionService.save(jobDefinition);
    }

    private Component editJobDefinition(JobDefinition jobDefinition, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        // Create and initialize fields
        TextField name = new TextField("NAME");
        name.setValue(isNew ? "" : (jobDefinition.getName() != null ? jobDefinition.getName() : ""));
        name.setWidthFull();

        TextField namespace = new TextField("NAMESPACE");
        namespace.setValue(isNew ? "" : (jobDefinition.getNamespace() != null ? jobDefinition.getNamespace() : ""));
        namespace.setWidthFull();

        TextField command = new TextField("COMMAND");
        command.setValue(isNew ? "" : (jobDefinition.getCommand() != null ? jobDefinition.getCommand() : ""));
        command.setWidthFull();

        TextField cron = new TextField("CRON");
        cron.setValue(isNew ? "" : (jobDefinition.getCron() != null ? jobDefinition.getCron() : ""));
        cron.setWidthFull();

        TextField typ = new TextField("TYP");
        typ.setValue(isNew ? "" : (jobDefinition.getTyp() != null ? jobDefinition.getTyp() : ""));
        typ.setWidthFull();

        IntegerField pid = new IntegerField("PID");
        pid.setValue(jobDefinition.getPid() != 0 ? jobDefinition.getPid() : 0);
        pid.setWidthFull();

        // Add value change listeners to update the jobDefinition object
        name.addValueChangeListener(event -> jobDefinition.setName(event.getValue()));
        namespace.addValueChangeListener(event -> jobDefinition.setNamespace(event.getValue()));
        command.addValueChangeListener(event -> jobDefinition.setCommand(event.getValue()));
        cron.addValueChangeListener(event -> jobDefinition.setCron(event.getValue()));
        typ.addValueChangeListener(event -> jobDefinition.setTyp(event.getValue()));
        pid.addValueChangeListener(event -> {
            try {
                if (event.getValue() != 0) {
                    jobDefinition.setPid(event.getValue());
                } else {
                    jobDefinition.setPid(0);
                }
            } catch (NumberFormatException e) {
                Notification.show("Invalid PID format", 5000, Notification.Position.MIDDLE);
            }
        });

        // Add all fields to the content layout
        content.add(name, namespace, command, cron, typ, pid);
        return content;
    }

    private Component deleteTreeGridItem(JobDefinition jobDefinition) {

        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("500px");
        dialog.setHeight("150px");
        Button cancelButton = new Button("Cancel");
        Button deleteButton = new Button("Delete");
        Text deleteConfirmationText = new Text("Are you sure you want to delete?");
        dialog.add(deleteConfirmationText);
        dialog.getFooter().add(deleteButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        deleteButton.addClickListener(saveEvent -> {
            jobDefinitionService.deleteById(jobDefinition.getId());

            updateGrid();
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;
    }

    private void updateGrid() {
        jobDefinitionService.findAll();
        treeGrid.setItems(jobDefinitionService.getRootProjects(), jobDefinitionService ::getChildProjects);
    }

    private void scheduleJob(JobDefinition jobDefinition) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        try {
            jobDataMap.put("jobDefinition", JobDefinitionUtils.serializeJobDefinition(jobDefinition));
        } catch (JsonProcessingException e) {
            e.getMessage();
            return;
        }

        JobDetail jobDetail = JobBuilder.newJob(JobExecutor.class)
                .withIdentity("job-" + jobDefinition.getId(), "group1")
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + jobDefinition.getId(), "group1")
                .startNow()
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}
package com.example.application.views;

import com.example.application.data.GenericDataProvider;
import com.example.application.data.entity.*;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.FvmSendmailService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@PageTitle("SendMail")
@Route(value = "sendmail", layout= MainLayout.class)
@RolesAllowed({"ADMIN"})
public class SendMailView extends VerticalLayout {

    private FvmSendmailService fvmSendmailService;
    private ComboBox<FVMSendmail> versenderComboBox;
    private ComboBox<FVMSendmail> empfängerComboBox;
    private ComboBox<FVMSendmail> nachrichtComboBox;
    private IntegerField anzahl = new IntegerField();
    String command;

    TextField readonlyField = new TextField();

    private Crud<FVMSendmail> crud;
    private Grid<FVMSendmail> grid;
    Button sendbutton = new Button("send");
    private Button configurationButton = new Button("Konfiguration");
    private static final Logger logger = LoggerFactory.getLogger(SendMailView.class);

    public SendMailView(FvmSendmailService fvmSendmailService) {
        logger.info("Starting SendMailView");
        this.fvmSendmailService = fvmSendmailService;

        List<FVMSendmail> fvmSendmails = fvmSendmailService.findAll();
        //System.out.println("*****************************fvmSendmails = "+fvmSendmails.size());
        logger.info("fvmSendmails: " + fvmSendmails.size());
        List<FVMSendmail> filteredVersender = fvmSendmails.stream()
                .filter(entity -> "Versender".equals(entity.getEntryTyp()))
                .collect(Collectors.toList());
        versenderComboBox = new ComboBox<>("Absender");
        versenderComboBox.setWidth("800px");
        versenderComboBox.setPlaceholder("auswählen");

        if (filteredVersender != null && !filteredVersender.isEmpty()) {
            versenderComboBox.setItems(filteredVersender);
            versenderComboBox.setItemLabelGenerator(FVMSendmail::getValue);
        }

        versenderComboBox.addValueChangeListener(event -> {

            FVMSendmail sm = event.getValue();
            logger.info(" versenderComboBox.addValueChangeListener: Versender = "+ sm.getValue());
            command=changeValueForOption(command, "-s", sm.getValue());
            readonlyField.setValue(command);
        });

        List<FVMSendmail> filteredEmpfaenger = fvmSendmails.stream()
                .filter(entity -> "Empfaenger".equals(entity.getEntryTyp()))
                .collect(Collectors.toList());
        empfängerComboBox = new ComboBox<>("Empänger");
        empfängerComboBox.setPlaceholder("auswählen");
        empfängerComboBox.setWidth("800px");

        if (filteredEmpfaenger != null && !filteredEmpfaenger.isEmpty()) {
            empfängerComboBox.setItems(filteredEmpfaenger);
            empfängerComboBox.setItemLabelGenerator(FVMSendmail::getValue);
        }

        empfängerComboBox.addValueChangeListener(event -> {
            FVMSendmail sm = event.getValue();
            logger.info("empfängerComboBox.addValueChangeListener: Versender = "+ sm.getValue());
            command=changeValueForOption(command, "-r", sm.getValue());
            readonlyField.setValue(command);
        });

        List<FVMSendmail> filteredNachricht = fvmSendmails.stream()
                .filter(entity -> "Nachricht".equals(entity.getEntryTyp()))
                .collect(Collectors.toList());
        nachrichtComboBox = new ComboBox<>("Nachricht");
        nachrichtComboBox.setPlaceholder("auswählen");
        nachrichtComboBox.setWidth("800px");

        if (filteredNachricht != null && !filteredNachricht.isEmpty()) {
            nachrichtComboBox.setItems(filteredNachricht);
            nachrichtComboBox.setItemLabelGenerator(FVMSendmail::getValue);
        }

        nachrichtComboBox.addValueChangeListener(event -> {
            FVMSendmail sm = event.getValue();
            logger.info(" nachrichtComboBox.addValueChangeListener: Empänger = "+ sm.getValue());

            command=command_builder("Parameter_8", sm.getValue());
            readonlyField.setValue(command);

        });

        sendbutton.addClickListener(clickEvent -> {
            logger.info("sendbutton.addClickListener: send");
        });

        configurationButton.addClickListener(clickEvent -> {
            logger.info("configurationButton.addClickListener: configuration of send mail");
            setSendMailConfigurationDialog();
        });

        anzahl.setLabel("Anzahl");
        anzahl.setValue(1);
        anzahl.setStepButtonsVisible(true);
        anzahl.setMin(1);
        anzahl.setMax(1000);
        anzahl.addValueChangeListener(clickEvent -> {

            command=changeValueForOption(command, "-n", anzahl.getValue().toString());
            readonlyField.setValue(command);


        });




        String server = fvmSendmails.stream()
                .filter(entity -> "Server".equals(entity.getEntryTyp()))
                .map(FVMSendmail::getValue)
                .findFirst()
                .orElse(null);



        String shellcommand = fvmSendmails.stream()
                .filter(entity -> "ShellCommand".equals(entity.getEntryTyp()))
                .map(FVMSendmail::getValue)
                .findFirst()
                .orElse(null);
        command=command_builder("ShellCommand", shellcommand);


        String verzeichnis = fvmSendmails.stream()
                .filter(entity -> "Verzeichnis".equals(entity.getEntryTyp()))
                .map(FVMSendmail::getValue)
                .findFirst()
                .orElse(null);


        readonlyField.setReadOnly(true);
        readonlyField.setLabel("Command executed on Server: " + server + " (Directory: " + verzeichnis + ")");
        readonlyField.setValue(command);
        readonlyField.setWidthFull();


        sendbutton.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(versenderComboBox);
        hl.add(configurationButton);
        hl.setAlignItems(Alignment.BASELINE);
        add(hl);

        add(empfängerComboBox);
        add(nachrichtComboBox);
        add(anzahl);
        add(readonlyField);
        add(sendbutton);


        setSizeFull();



    }

    private String command_builder(String param, String value) {

        String Vorgabe = "$ShellCommand$ -R Test -n 1 -a LPT -t Testnachricht-Dataport -D -s versender -r empfänger nachricht";
                          //ekpsend.sh   -R "Test"  -n 1       -a LPT     -t Testnachricht-Dataport -D         -s $Versender -r $Empfaenger$ ./$Nachricht$

        if(param.equals("ShellCommand")) {
            command=Vorgabe.replace("$ShellCommand$", value);

        }

        if(param.equals("Parameter_8")) //Die Nachricht selbst
        {
            String[] words = command.split(" ");
            words[words.length - 1] = value;

            // Zeichenkette wieder zusammenfügen
            return String.join(" ", words);

        }


        logger.info("Parameter: " + param + " Wert" + value);
        //command=shellcommand + " " + param1 + " " + param2 + " " + param3 + " " + param4;
        return command;
    }


    public String changeValueForOption(String input, String option, String newValue) {

        logger.info("Zerlege " + input);

        // Zeichenkette in Teile zerlegen
        String[] parts = input.split(" ");


        // Neue Zeichenkette erstellen
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(option) && i + 1 < parts.length) {
                // Wenn die Option gefunden wird, ersetze den nächsten Wert
                logger.info("Found " + option);
                parts[i + 1] = String.valueOf(newValue);
            }
            // Baue die Zeichenkette wieder zusammen
            result.append(parts[i]).append(" ");
        }

        return result.toString().trim(); // Entferne das letzte Leerzeichen
    }

    private void setSendMailConfigurationDialog() {
        logger.info("Opening SendMail Configuration Dialog");
        
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setHeightFull();
        Dialog sendMailDialog = new Dialog();
        sendMailDialog.setDraggable(true);
        sendMailDialog.setResizable(true);

        setupSendMailCrud();

        // Add a close button
        Button closeButton = new Button("Close", e -> sendMailDialog.close());
        HorizontalLayout buttons = new HorizontalLayout(closeButton);
        buttons.setAlignItems(Alignment.BASELINE);

        // Add components to the dialog
        content.add(crud, buttons);
        sendMailDialog.add(content);
        sendMailDialog.setWidth("1000px");
        sendMailDialog.setHeight("800px");
        sendMailDialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);

        // Open the dialog
        sendMailDialog.open();
    }

    private CrudEditor<FVMSendmail> createSendMailEditor() {
        // Fields for the editor
        IntegerField id = new IntegerField("Id");
        id.setReadOnly(true);
        ComboBox<String> entryType = new ComboBox<>("Entry Type");
        entryType.setItems("Empfaenger", "Nachricht", "Versender");
        TextField value = new TextField("Value");

        // Arrange fields in a form layout
        FormLayout formLayout = new FormLayout(id, entryType, value);

        // Binder for FVMSendmail entity
        Binder<FVMSendmail> binder = new Binder<>(FVMSendmail.class);
        binder.forField(id).withConverter(
                        new Converter<Integer, Long>() {
                            @Override
                            public Result<Long> convertToModel(Integer value, ValueContext context) {
                                if (value == null) {
                                    return Result.ok(null);
                                }
                                return Result.ok(value.longValue());
                            }

                            @Override
                            public Integer convertToPresentation(Long value, ValueContext context) {
                                return value == null ? null : value.intValue();
                            }
                        })
                .bind(FVMSendmail::getId, FVMSendmail::setId);
        binder.forField(entryType).bind(FVMSendmail::getEntryTyp, FVMSendmail::setEntryTyp);
        binder.forField(value).bind(FVMSendmail::getValue, FVMSendmail::setValue);

        // Return the editor
        return new BinderCrudEditor<>(binder, formLayout);
    }

    private void setupSendMailCrud() {
        VerticalLayout content = new VerticalLayout();
        crud = new Crud<>(FVMSendmail.class, createSendMailEditor());
        setupGrid();
        content.add(crud);

        grid.addItemDoubleClickListener(event -> {
            FVMSendmail selectedEntity = event.getItem();
            crud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
         //   crud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crud.setToolbarVisible(true);
        updateGrid();
        setupDataProviderEvent();
        crud.setHeightFull();
        content.setHeightFull();
    }

    private void updateGrid() {
        logger.info("updateGrid: update Sendmail in grid");
        List<FVMSendmail> listOfSendmail = fvmSendmailService.findAll();
        GenericDataProvider dataProvider = new GenericDataProvider(listOfSendmail);
        grid.setDataProvider(dataProvider);
    }

    private void setupGrid() {
        String ID = "id";
        String ENTRYTYP = "entryTyp";
        String VALUE = "value";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        grid = crud.getGrid();
        //   userGrid.setColumns(USERNAME, NAME, ROLES, IS_AD);

        grid.getColumnByKey(ID).setHeader("Id").setWidth("50px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(ENTRYTYP).setHeader("ENTRYTYP").setWidth("600px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(VALUE).setHeader("VALUE").setWidth("600px").setFlexGrow(0).setResizable(true);

     //   grid.removeColumn(grid.getColumnByKey(ID));
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)

        grid.setColumnOrder( grid.getColumnByKey(ID)
                , grid.getColumnByKey(ENTRYTYP)
                , grid.getColumnByKey(VALUE)
                , grid.getColumnByKey(EDIT_COLUMN));

//grid.setWidthFull();
//grid.setHeightFull();

        crud.setToolbarVisible(false);
    }

    private void setupDataProviderEvent() {
        logger.info("setupDataProviderEvent: set delete and save event");
        GenericDataProvider userdataProvider = new GenericDataProvider(getGenericCommentsDataProviderAllItems());

        crud.addDeleteListener(
                deleteEvent -> {
                    fvmSendmailService.delete(deleteEvent.getItem().getId());
                    updateGrid();
                });
        crud.addSaveListener(
                saveEvent -> {
                    System.out.println(saveEvent.getItem().getId() +"..........saveeeeee");
                    FVMSendmail sendmail = saveEvent.getItem();
                    fvmSendmailService.save(sendmail);
                    updateGrid();
                });
    }
    private List<FVMSendmail> getGenericCommentsDataProviderAllItems() {
        logger.info("getGenericCommentsDataProviderAllItems: get data from grid");
        DataProvider<FVMSendmail, Void> existDataProvider = (DataProvider<FVMSendmail, Void>) grid.getDataProvider();
        List<FVMSendmail> listOfGenericComments = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfGenericComments;
    }
}
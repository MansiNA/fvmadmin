package com.example.application.views.list;

import com.example.application.data.entity.fvm_monitoring;
import com.example.application.utils.myCallback;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

public class MonitoringForm extends FormLayout {

    TextField titel = new TextField ("Titel");
    static TextArea sql_text = new TextArea ("SQL-Abfrage");
    Checkbox checkbox = new Checkbox("aktiv");
    static RichTextEditor rte_Beschreibung = new RichTextEditor("nix");
    static RichTextEditor rte_Handlungsanweisung = new RichTextEditor("nix");


    Button save = new Button("save");
    Button delete = new Button("delete");
    Button cancel = new Button("cancel");
    myCallback callback;

   // RichTextEditor rte;
 /*   static Tab details = new Tab("Beschreibung");
    static Tab payment= new Tab("Handlungsanweisung");*/



    fvm_monitoring monitor;

    static VerticalLayout content = new VerticalLayout();;
    public MonitoringForm(fvm_monitoring monitor, myCallback callback) {
        addClassName("monitoring-form");
    //    VerticalLayout dialogInhalt;
        this.callback=callback;
        this.monitor=monitor;


    //    VerticalLayout editorInhalt = new VerticalLayout();



      //  Tabs tabs = new Tabs();

      //  tabs.addSelectedChangeListener(
      //          event -> setContent(event.getSelectedTab(),monitor));


        //tabs.add(details, payment);


        //content.setSpacing(false);
        //content.add(tabs);

        //setContent(tabs.getSelectedTab(),monitor);

      //  dialogInhalt = new VerticalLayout();
        //dialogInhalt.add(tabs,content);

        add(
                titel,
                checkbox,
                sql_text,
                rte_Beschreibung,
                rte_Handlungsanweisung,
                createButtonLayout()
        );

        setColspan(sql_text, 2);
        setColspan(rte_Beschreibung, 2);
        setColspan(rte_Handlungsanweisung, 2);

    }


  /*  private static void setContent(Tab tab,fvm_monitoring inhalt ) {

        if(content != null ) {
            content.removeAll();
        }

        if (tab == null) {
            return;
        }

        rte_Handlungsanweisung.setWidthFull();
        rte_Handlungsanweisung.setHeightFull();
        rte_Beschreibung.setWidthFull();
        rte_Beschreibung.setHeightFull();

        if (tab.equals(details)) {

            if (inhalt.getBeschreibung() != null) {
                rte_Beschreibung.asHtml().setValue(inhalt.getBeschreibung());
            }
            else
            {
                rte_Beschreibung.setValue("noch keine Beschreibung vorhanden...");
            }
            content.add(rte_Beschreibung);

        } else if (tab.equals(payment)) {

            if (inhalt.getHandlungs_INFO() != null) {
                rte_Handlungsanweisung.asHtml().setValue(inhalt.getHandlungs_INFO());
            }
            else
            {
                rte_Handlungsanweisung.setValue("noch keine Handlungsanweisung vorhanden...");
            }

            content.add(rte_Handlungsanweisung);
        } else  {
            if (inhalt.getSQL() != null) {
                sql_text.setValue(inhalt.getSQL());
            }
            else
            {
                sql_text.setValue("Noc kein SQL hinterlegt...");
            }
            content.add(sql_text);}

        //content.add(inhalt.getHandlungs_INFO());


    }*/


    private Component createButtonLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        save.addClickListener(e ->saveMonitor());
        cancel.addClickShortcut(Key.ESCAPE);

        return new HorizontalLayout(save, delete,cancel);
    }

    private void saveMonitor() {

        monitor.setTitel(titel.getValue());
        monitor.setActive(checkbox.getValue());
        //monitor.setSQL(sql.getElement()..getText());
     //   monitor.setWarning_Schwellwert();
     //   monitor.setError_Schwellwert();
     //   monitor.setSQL();
        monitor.setHandlungs_INFO(rte_Handlungsanweisung.getValue());
     //   monitor.setCheck_Intervall();
     //   monitor.setBeschreibung();


      //  System.out.println("Save Monitor gedrückt...Titel: " + monitor.getTitel());

        callback.save(monitor);


    }
}

//rte.setMaxHeight("400px");
//rte.setMinHeight("200px");

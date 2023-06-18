package com.example.application.views.list;

import com.example.application.data.entity.fvm_monitoring;
import com.example.application.utils.myCallback;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;

import java.util.List;

public class MonitoringForm extends FormLayout {


    TextField Titel = new TextField ("Titel");
    TextArea SQL = new TextArea ("SQL-Abfrage");
    Checkbox isactive = new Checkbox("aktiv");
    RichTextEditor Beschreibung = new RichTextEditor("nix");
    RichTextEditor Handlungs_INFO = new RichTextEditor("nix");

    IntegerField Check_Intervall = new IntegerField("Check-Intervall");

    IntegerField Warning_Schwellwert = new IntegerField("Warning Schwellwert");
    IntegerField Error_Schwellwert = new IntegerField("Error Schwellwert");


    Button save = new Button("save");
    Button delete = new Button("delete");
    Button cancel = new Button("cancel");

    Binder<fvm_monitoring> binder = new BeanValidationBinder<>(fvm_monitoring.class);

   // myCallback callback;

   // RichTextEditor rte;
 /*   static Tab details = new Tab("Beschreibung");
    static Tab payment= new Tab("Handlungsanweisung");*/



    //fvm_monitoring monitor;

  //  static VerticalLayout content = new VerticalLayout();;
    //public MonitoringForm(fvm_monitoring monitor, myCallback callback) {
    public MonitoringForm() {
        addClassName("monitoring-form");

        binder.bindInstanceFields(this);
    //    VerticalLayout dialogInhalt;
        //this.callback=callback;
        //this.monitor=monitor;


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

        Label descriptionLb = new Label("Beschreibung:");
        Label directiveLb = new Label("Handlungsanweisung:");

        add(
                Titel,
                SQL,
                Check_Intervall,
                Warning_Schwellwert,
               Error_Schwellwert,
                isactive,
                descriptionLb,
                Beschreibung,
                directiveLb,
                Handlungs_INFO,
                createButtonLayout()
        );

        setColspan(Titel, 2);
        setColspan(SQL, 2);
        setColspan(Beschreibung, 2);
        setColspan(Handlungs_INFO, 2);

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
      //  save.addClickListener(e ->saveMonitor());
        cancel.addClickShortcut(Key.ESCAPE);

        return new HorizontalLayout(save, delete,cancel);
    }




}

//rte.setMaxHeight("400px");
//rte.setMinHeight("200px");

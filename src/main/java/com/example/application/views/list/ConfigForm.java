package com.example.application.views.list;

import com.example.application.data.entity.Configuration;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

public class ConfigForm extends FormLayout {
    private Configuration conf = new Configuration();
    Binder<Configuration> binder = new BeanValidationBinder<>(Configuration.class);
    TextField land = new TextField("Land");
    TextField umgebung = new TextField("Umgebung");
    TextField userName = new TextField("Username");
    PasswordField password = new PasswordField("Password");
    TextField db_Url = new TextField("DB-ConnectionString");

    Button save=new Button("Save");
    Button cancel = new Button("Cancel");

    public ConfigForm() {
        addClassName("config-form");

       binder.bindInstanceFields(this);

        add(
                land,
                umgebung,
                userName,
                password,
                db_Url,
                createButtonLayout()
        );
    }

    private Component createButtonLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancel.addClickShortcut(Key.ESCAPE);

      //  save.addClickListener(event -> validateAndSave());
      //  cancel.addClickListener(event -> fireEvent(new CloseEvent(this)));


        return new HorizontalLayout(save,cancel);


    }

    private void validateAndSave() {
        try {
            binder.writeBean(conf);
            fireEvent(new SaveEvent(this,conf));
        }
        catch (ValidationException e){
            e.printStackTrace();
        }
    }

    public void setConfiguration(Configuration config){
        this.conf = config;
        binder.readBean(config);
    }


    // Events
    public static abstract class ConfigFormEvent extends ComponentEvent<ConfigForm> {
        private Configuration config;

        protected ConfigFormEvent(ConfigForm source, Configuration config) {
            super(source, false);
            this.config = config;
        }

        public Configuration getConfiguration() {
            return config;
        }
    }

    public static class SaveEvent extends ConfigFormEvent {
        SaveEvent(ConfigForm source, Configuration config) {
            super(source, config);
        }
    }

//    public static class DeleteEvent extends ContactFormEvent {
//        DeleteEvent(ContactForm source, Contact contact) {
//            super(source, contact);
//        }
//
//    }

    public static class CloseEvent extends ConfigFormEvent {
        CloseEvent(ConfigForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

}

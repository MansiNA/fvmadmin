package com.example.application.views.list;

import com.example.application.data.entity.Configuration;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.shared.Registration;

public class ConfigForm extends FormLayout {

    Binder<Configuration> binder = new BeanValidationBinder<>(Configuration.class);
    IntegerField id = new IntegerField("Id");
    TextField name = new TextField("Name");
    TextField userName = new TextField("Username");
    PasswordField password = new PasswordField("Password");
    TextField db_Url = new TextField("DB-ConnectionString");
    Checkbox isMonitoring = new Checkbox("Is Monitoring");
    Checkbox isWatchdog = new Checkbox("Is Watchdog");

    Button save=new Button("Save");
    Button delete = new Button("Delete");
    Button cancel = new Button("Cancel");

    private Configuration configuration;

    private String originalPassword;
    private boolean hasChanges = false;
    public ConfigForm() {
        addClassName("config-form");

      //  binder.bindInstanceFields(this);
        // Bind fields without "isMonitoring" which needs special handling
        id.setReadOnly(true);
        binder.forField(id)
                .withConverter(new Converter<Integer, Long>() {
                            @Override
                            public Result<Long> convertToModel(Integer fieldValue, ValueContext context) {
                                // Convert Integer to Long
                                return Result.ok(fieldValue != null ? fieldValue.longValue() : null);
                            }

                            @Override
                            public Integer convertToPresentation(Long idValue, ValueContext context) {
                                // Convert Long to Integer
                                return idValue != null ? idValue.intValue() : null;
                            }
                        })
                .bind("id");
        binder.forField(name).bind("name");
        binder.forField(userName).bind("userName");
        binder.forField(password).bind("password");
        binder.forField(db_Url).bind("db_Url");

        // Custom binding for isMonitoring with a converter from Integer to Boolean
        binder.forField(isMonitoring)
                .withConverter(new Converter<Boolean, Integer>() {
                    @Override
                    public Result<Integer> convertToModel(Boolean fieldValue, ValueContext context) {
                        // Convert Boolean to Integer
                        return Result.ok(fieldValue ? 1 : 0);
                    }

                    @Override
                    public Boolean convertToPresentation(Integer integer, ValueContext context) {
                        // Convert Integer to Boolean
                        return integer != null && integer == 1;
                    }
                })
                .bind("isMonitoring");

        // Custom binding for isMonitoring with a converter from Integer to Boolean
        binder.forField(isWatchdog)
                .withConverter(new Converter<Boolean, Integer>() {
                    @Override
                    public Result<Integer> convertToModel(Boolean fieldValue, ValueContext context) {
                        // Convert Boolean to Integer
                        return Result.ok(fieldValue ? 1 : 0);
                    }

                    @Override
                    public Boolean convertToPresentation(Integer integer, ValueContext context) {
                        // Convert Integer to Boolean
                        return integer != null && integer == 1;
                    }
                })
                .bind("isWatchdog");

        add(
                id,
                name,
                userName,
                password,
                db_Url,
                isMonitoring,
                isWatchdog,
                createButtonLayout()
        );

    }
    public void setConfiguration(Configuration config){
        this.configuration = config;

        if(config != null) {
            originalPassword = config.getPassword();
            config.setPassword("");
        }

        binder.readBean(config);
    }

    // Events
    public static abstract class ConfigFormEvent extends ComponentEvent<ConfigForm> {
        private Configuration configuration;

        protected ConfigFormEvent(ConfigForm source, Configuration configuration) {
            super(source, false);
            this.configuration = configuration;
        }

        public Configuration getConfiguration() {
            return configuration;

        }
    }

    public static class SaveEvent extends ConfigFormEvent {
        SaveEvent(ConfigForm source, Configuration config) { super(source, config);  }
    }

    public static class DeleteEvent extends ConfigFormEvent {
        DeleteEvent(ConfigForm source, Configuration config) {
            super(source, config);
        }

    }

    public static class CloseEvent extends ConfigFormEvent {
        CloseEvent(ConfigForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }



    private Component createButtonLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addClickShortcut(Key.ENTER);
        cancel.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, configuration)));
        cancel.addClickListener(event -> fireEvent(new CloseEvent(this)));
      //  delete.setEnabled(false);

        binder.addStatusChangeListener(e -> {
            boolean hasChanges = binder.hasChanges();
            save.setEnabled(hasChanges);
            System.out.println( binder.hasChanges()+"________________________________________");
        });

        //   binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid() && hasChanges));
        return new HorizontalLayout(save,delete, cancel);


    }

    private void validateAndSave() {
        try {

            binder.writeBean(configuration);
            if(configuration.getPassword().equals("")) {
                configuration.setPassword(originalPassword);
            }

            fireEvent(new SaveEvent(this,configuration));
            save.setEnabled(false);
        }
        catch (ValidationException e){
            e.printStackTrace();
        }
    }

    private void onValueChange() {
        System.out.println("hashtag"+ hasChanges);
        hasChanges = true;
        //  save.setEnabled(true);
    }



}

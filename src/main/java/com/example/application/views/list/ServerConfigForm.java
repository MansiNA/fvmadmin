package com.example.application.views.list;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ServerConfiguration;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

public class ServerConfigForm extends FormLayout {

    Binder<ServerConfiguration> binder = new BeanValidationBinder<>(ServerConfiguration.class);
    TextField hostName = new TextField("Host Name");
    TextField hostAlias = new TextField("Host Alias");
    TextField userName = new TextField("Username");
    TextField sshPort = new TextField("SSH_PORT");
    TextField pathList = new TextField("PATH_LIST");
    TextArea sshKey = new TextArea("SSH_KEY");
    Button save=new Button("Save");
    Button delete = new Button("Delete");
    Button cancel = new Button("Cancel");

    private ServerConfiguration serverConfiguration;

    private String originalPassword;
    private boolean hasChanges = false;
    public ServerConfigForm() {
        addClassName("config-form");

        sshKey.setHeight("200px");
        binder.forField(hostName).bind(ServerConfiguration::getHostName, ServerConfiguration::setHostName);
        binder.forField(userName).bind(ServerConfiguration::getUserName, ServerConfiguration::setUserName);
        binder.forField(pathList).bind(ServerConfiguration::getPathList, ServerConfiguration::setPathList);
        binder.forField(sshKey).bind(ServerConfiguration::getSshKey, ServerConfiguration::setSshKey);
        binder.forField(hostAlias).bind(ServerConfiguration::getHostAlias, ServerConfiguration::setHostAlias);

        binder.forField(sshPort)
                .withValidator(value -> value != null && value.matches("\\d+"), "SSH Port must be a numeric value")
                .bind(ServerConfiguration::getSshPort, ServerConfiguration::setSshPort);
        add(
                hostName,
                hostAlias,
                userName,
                sshPort,
                pathList,
                sshKey,
                createButtonLayout()
        );

    }
    public void setConfiguration(ServerConfiguration config){
        this.serverConfiguration = config;

        binder.readBean(config);
    }

    // Events
    public static abstract class ConfigFormEvent extends ComponentEvent<ServerConfigForm> {
        private ServerConfiguration serverConfiguration;

        protected ConfigFormEvent(ServerConfigForm source, ServerConfiguration configuration) {
            super(source, false);
            this.serverConfiguration = configuration;
        }

        public ServerConfiguration getConfiguration() { return serverConfiguration; }
    }

    public static class SaveEvent extends ConfigFormEvent {
        SaveEvent(ServerConfigForm source, ServerConfiguration config) { super(source, config);  }
    }

    public static class DeleteEvent extends ConfigFormEvent {
        DeleteEvent(ServerConfigForm source, ServerConfiguration config) {
            super(source, config);
        }

    }

    public static class CloseEvent extends ConfigFormEvent {
        CloseEvent(ServerConfigForm source) {
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
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, serverConfiguration)));
        cancel.addClickListener(event -> fireEvent(new CloseEvent(this)));

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

            binder.writeBean(serverConfiguration);

            fireEvent(new SaveEvent(this,serverConfiguration));
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

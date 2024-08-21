package com.example.application;

import com.example.application.data.entity.JobManager;
import com.example.application.data.service.JobDefinitionService;
import com.example.application.utils.JobDefinitionUtils;
import com.example.application.utils.JobExecutor;
import com.example.application.utils.SpringContextHolder;
import com.example.application.views.JobManagerView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@Push
@SpringBootApplication
@Theme(value = "fvmadmin")
@PWA(name = "FVM Admin Tool", shortName = "FVM Admin", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@ConfigurationPropertiesScan
public class Application implements AppShellConfigurator {

    @Value("${cron.autostart}")
    private boolean cronAutostart;
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }

}

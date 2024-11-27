package com.example.application.views;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Route("monitoring")
@PageTitle("Monitoring")
@AnonymousAllowed
public class MonitoringView extends VerticalLayout {

    public MonitoringView() {
        add(new H1("Spring Boot Actuator Monitoring"));

        // Beispiel: Gesundheitscheck anzeigen
        Button healthCheckButton = new Button("Check Health", click -> {
            String healthStatus = getHealthStatus();
            Notification.show("Health Status: " + healthStatus, 15000, Notification.Position.MIDDLE);
        });

        add(healthCheckButton);
    }

    private String getHealthStatus() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://localhost:8080/actuator/jvm.memory.used";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
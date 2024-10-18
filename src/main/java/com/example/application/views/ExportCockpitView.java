package com.example.application.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "cockpit_view/export_id/:id?")
@AnonymousAllowed
public class ExportCockpitView extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Get the ID from the route
        Optional<String> idParam = event.getRouteParameters().get("id");

        if (idParam.isPresent()) {
            int id = Integer.parseInt(idParam.get());
            System.out.println("id>>>>>>>>>>>>>>>>>>>>> = "+id);
          //  displayMonitoringData(id);
        } else {
            displayPlainTextResponse("No ID provided.");
        }
    }

    private void displayMonitoringData(int id) {
        // SQL query to check if monitoring is enabled
        String sqlCheck = "SELECT IS_MONITORING FROM SQL_CONFIGURATION WHERE ID = ?";
        Boolean isMonitoring = jdbcTemplate.queryForObject(sqlCheck, new Object[]{id}, Boolean.class);

        if (Boolean.TRUE.equals(isMonitoring)) {
            // SQL query to fetch monitoring data
            String monitoringDataSql = "SELECT * FROM FVM_MONITORING WHERE ID = ?";
            List<Map<String, Object>> data = jdbcTemplate.queryForList(monitoringDataSql, id);

            if (data.isEmpty()) {
                displayPlainTextResponse("No data found for ID: " + id);
            } else {
                // Format the data for Prometheus
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> row : data) {
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        sb.append("fvm_monitoring_").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
                    }
                }

                // Return the formatted data
                displayPlainTextResponse(sb.toString());
            }
        } else {
            // Return "no values found"
            displayPlainTextResponse("no values found");
        }
    }

    private void displayPlainTextResponse(String responseText) {
        // Manipulate the response to return plain text
        UI.getCurrent().getPage().executeJs(
                "document.open();document.write($0);document.close();", responseText);
    }
}


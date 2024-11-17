package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.fvm_monitoring;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "cockpit_view/export_id/:id?") // The route for the view
@AnonymousAllowed
public class ExportCockpitView extends VerticalLayout implements BeforeEnterObserver {

    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Get the ID from the route
        Optional<String> idParam = event.getRouteParameters().get("id");
        // prometheus --config.file=prometheus.yml
        if (idParam.isPresent()) {
            int id = Integer.parseInt(idParam.get());
            System.out.println("id>>>>>>>>>>>>>>>>>>>>> = " + id);
            displayMonitoringData(id); // Call to fetch and display data
        } else {
            displayPlainTextResponse("No ID provided.");
        }
    }

    public JdbcTemplate getJdbcTemplateWithDBConnetion(com.example.application.data.entity.Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(com.example.application.data.entity.Configuration.decodePassword(conf.getPassword()));
        try {
            jdbcTemplate.setDataSource(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            jdbcTemplate.getDataSource().getConnection().close();
//            connection = jdbcTemplate.getDataSource().getConnection();
//            dataSource = jdbcTemplate.getDataSource();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();

                    if (dataSource instanceof HikariDataSource) {
                        ((HikariDataSource) dataSource).close();
                    }

                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }
    }

    public String fetchMetrics() {
        String sqlQuery = "SELECT b.titel, a.result FROM fvm_monitor_result a, fvm_monitoring b WHERE a.id = b.id AND a.is_active = 1";

        List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(sqlQuery);

        StringBuilder metricsBuilder = new StringBuilder();

        for (Map<String, Object> row : queryResult) {
            String title = (String) row.get("titel");
            BigDecimal result = (BigDecimal) row.get("result");

            // Format the metrics into Prometheus format
            String metricLine = String.format("ekp_metric{title=\"%s\"} %s\n", escapeString(title), result);

            metricsBuilder.append(metricLine);
        }

        // Ensure the last line ends with a line feed character
        if (metricsBuilder.length() > 0 && metricsBuilder.charAt(metricsBuilder.length() - 1) != '\n') {
            metricsBuilder.append('\n');
        }

        // Append # EOF at the end
        metricsBuilder.append("# EOF\n");

        return metricsBuilder.toString();
    }

    // Helper method to escape special characters in label values
    private String escapeString(String str) {
        // Escape backslashes, double-quotes, and line feeds
        return str
                .replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\"", "\\\\\"")
                .replaceAll("\n", "\\\\n");
    }

    private void displayMonitoringData(int id) {
        Optional<Configuration> conf = configurationService.findById((long) id);
        String metric = "";
        if(conf.isPresent()) {
            getJdbcTemplateWithDBConnetion(conf.get());
            Boolean isMonitoring = conf.get().getIsMonitoring() == 1 ? true : false;


            if (isMonitoring) {
                metric = fetchMetrics();
            } else {
                metric = "no values found";
            }
            connectionClose(jdbcTemplate);
        } else {
            metric = "ID not exist!";
        }
        // Display the results to the UI
        displayPlainTextResponse(metric);
    }

    private String createToPrometheusFormat(fvm_monitoring monitoring) {
        StringBuilder sb = new StringBuilder();
        // Format each field for Prometheus
        sb.append("fvm_monitoring_ID ").append(monitoring.getID()).append("\n")
                .append("fvm_monitoring_Pid ").append(monitoring.getPid()).append("\n")
                .append("fvm_monitoring_SQL \"").append(monitoring.getSQL() != null ? monitoring.getSQL().replace("\"", "\\\"") : "").append("\"\n")
                .append("fvm_monitoring_SQL_Detail \"").append(monitoring.getSQL_Detail() != null ? monitoring.getSQL_Detail().replace("\"", "\\\"") : "").append("\"\n")
                .append("fvm_monitoring_Titel \"").append(monitoring.getTitel() != null ? monitoring.getTitel().replace("\"", "\\\"") : "").append("\"\n")
                .append("fvm_monitoring_Beschreibung \"").append(monitoring.getBeschreibung() != null ? monitoring.getBeschreibung().replace("\"", "\\\"") : "").append("\"\n")
                .append("fvm_monitoring_Handlungs_INFO \"").append(monitoring.getHandlungs_INFO() != null ? monitoring.getHandlungs_INFO().replace("\"", "\\\"") : "").append("\"\n")
                .append("fvm_monitoring_Check_Intervall ").append(monitoring.getCheck_Intervall() != null ? monitoring.getCheck_Intervall() : 0).append("\n")
                .append("fvm_monitoring_Warning_Schwellwert ").append(monitoring.getWarning_Schwellwert() != null ? monitoring.getWarning_Schwellwert() : 0).append("\n")
                .append("fvm_monitoring_Error_Schwellwert ").append(monitoring.getError_Schwellwert() != null ? monitoring.getError_Schwellwert() : 0).append("\n")
                .append("fvm_monitoring_Error_Prozent ").append(monitoring.getError_Prozent() != null ? monitoring.getError_Prozent() : 0.0).append("\n")
                .append("fvm_monitoring_IS_ACTIVE{is_active=\"").append(monitoring.getIS_ACTIVE()).append("\"} 1\n") // Using a label for IS_ACTIVE
                .append("fvm_monitoring_Bereich \"").append(monitoring.getBereich() != null ? monitoring.getBereich().replace("\"", "\\\"") : "").append("\"\n")
                .append("fvm_monitoring_retentionTime ").append(monitoring.getRetentionTime() != 0 ? monitoring.getRetentionTime() : 0).append("\n")
                .append("fvm_monitoring_Zeitpunkt ").append(monitoring.getZeitpunkt() != null ? monitoring.getZeitpunkt().getTime() : 0).append("\n"); // Using epoch time for Date

        return sb.toString();
    }

    private void displayPlainTextResponse(String responseText) {
        // Manipulate the response to return plain text
        UI.getCurrent().getPage().executeJs(
                "document.open();document.write($0);document.close();", responseText);

        UI.getCurrent().getPage().executeJs(
                "document.contentType = 'text/plain';");
    }
}

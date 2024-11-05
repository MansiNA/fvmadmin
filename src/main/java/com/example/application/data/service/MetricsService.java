package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MetricsService {

    private JdbcTemplate jdbcTemplate;
    private ConfigurationService configurationService;

    public MetricsService(JdbcTemplate jdbcTemplate, ConfigurationService configurationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.configurationService = configurationService;
    }

    public String displayMonitoringData(int id) {
        Optional<Configuration> conf = configurationService.findById((long) id);
        String metric = "";
        if(conf.isPresent()) {
            jdbcTemplate = getNewJdbcTemplateWithDatabase(conf.get());
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
      return  metric;
    }

    public JdbcTemplate getNewJdbcTemplateWithDatabase(Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {
            return new JdbcTemplate(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            dataSource = jdbcTemplate.getDataSource();
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

}

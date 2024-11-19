package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Quarantine;
import com.example.application.data.entity.TableInfo;
import com.example.application.data.service.ConfigurationService;
import com.example.application.security.ApplicationUserRole;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.example.application.security.ApplicationUserRole.PF_ADMIN;
import static org.apache.xmlbeans.impl.store.Public2.getStream;

@PageTitle("Quarantäne Verwaltung")
@Route(value = "quarantaene", layout= MainLayout.class)
@RolesAllowed({"ADMIN","FVM"})
public class QuarantaeneView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private ConfigurationService service;
    static ComboBox<Configuration> comboBox;
    Button button = new Button("Refresh");
    Integer ret = 0;
    Grid<Quarantine> qgrid = new Grid<>(Quarantine.class, false);

    List<Quarantine> listOfQuarantine = new ArrayList<>();
    private Button downloadButton = new Button("Download");
    ComboBox<String> FehlertypCB = new ComboBox<>("Fehlertyp");
    UI ui;


    public QuarantaeneView(JdbcTemplate jdbcTemplate, ConfigurationService service) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;

        Paragraph paragraph = new Paragraph("Hier erfolgt eine Auflistung von aktuellen EGVP-E Quarantäne-Nachrichten");
        paragraph.setMaxHeight("400px");

        ui = UI.getCurrent();

        comboBox = new ComboBox<>("Verbindung");
        
        List<Configuration> configList = service.findMessageConfigurations();
        if (configList != null && !configList.isEmpty()) {
            comboBox.setItems(configList);
            comboBox.setItemLabelGenerator(Configuration::getName);
            comboBox.setValue(configList.get(0));
        }

        comboBox.addValueChangeListener(event -> {
            updateGrid();
        });

        configureQuarantaeneGrid();
        updateGrid();
    //    List<String> errorList = List.of("MESSAGE_INCOMPLETE", "CHECK_FILENAMES", "SERVER_NOT_REACHABLE","RECEIVERID_NOT_FOUND");
        List<String> exceptionCodes = listOfQuarantine.stream()
                .map(Quarantine::getEXCEPTIONCODE)
                .distinct()
                .collect(Collectors.toList());
        FehlertypCB = new ComboBox<>("Fehlertyp");
        if(exceptionCodes != null) {
            FehlertypCB.setItems(exceptionCodes);
        }
        FehlertypCB.setWidth("400px");
        FehlertypCB.setPlaceholder("select Fehlertyp");

        FehlertypCB.addValueChangeListener(event -> {
            System.out.println("now FehlertypCB: "+event.getValue());
            String selectedValue = event.getValue();
            if (selectedValue != null) {
                // Filter listOfQuarantine by selected EXCEPTIONCODE
                List<Quarantine> filteredList = listOfQuarantine.stream()
                        .filter(q -> selectedValue.equals(q.getEXCEPTIONCODE()))
                        .collect(Collectors.toList());
                // Update Grid with filtered data
                qgrid.setItems(filteredList);
            } else {
                // If no selection, reset Grid to original data
                qgrid.setItems(listOfQuarantine);
            }
        });








        
        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox, FehlertypCB, button);
        hl.setAlignItems(Alignment.BASELINE);
        setSizeFull();

        add(hl, downloadButton, qgrid);

        button.addClickListener(clickEvent -> {

        //    Notification.show("hole Daten...",2000, Notification.Position.TOP_END);
        //    qgrid.setItems();

            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Quarantäne Infos");

                    // updateGrid();
                    listOfQuarantine = getQuarantaene(ui);

                    //Thread.sleep(2000); //2 Sekunden warten
                    Thread.sleep(20); //2 Sekunden warten

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Need to use access() when running from background thread
                ui.access(() -> {
                    // Stop polling and hide spinner
                    ui.setPollInterval(-1);

                    if (ret != 0) {
                        System.out.println("Keine Quarantäne Infos gefunden!");

                        return;
                    }
                    qgrid.setItems(listOfQuarantine);

                    refreshGrid();
                });
            });


        });


        downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        downloadButton.addClickListener(clickEvent -> {
            Notification.show("Exportiere Liste ");
            try {

                generateExcel( "quarantaene_export.xlsx");

            } catch (Exception e) {
                e.getMessage();
            }

        });

    }

    private void updateGrid() {
        listOfQuarantine = getQuarantaene(ui);
        if (listOfQuarantine != null) {
            downloadButton.setEnabled(true);
            qgrid.setItems(listOfQuarantine);
        } else {
            downloadButton.setEnabled(false);
        }
    }

    private void configureQuarantaeneGrid() {

        qgrid.addColumn(createNachrichtIDRenderer()).setKey("ID").setHeader("Nachricht-ID").setAutoWidth(true).setSortable(true).setResizable(true).setComparator(Quarantine::getID).setFooter("Anzahl Einträge: 0");
        qgrid.addColumn(Quarantine::getEXCEPTIONCODE).setHeader("Exception-Code").setAutoWidth(true).setResizable(true).setSortable(true);
        qgrid.addColumn(createDateRenderer()).setHeader("Date").setAutoWidth(true).setSortable(true).setResizable(true).setComparator(Quarantine::getENTRANCEDATE);

//        qgrid.addColumn(Quarantine::getID).setHeader("Nachricht Extern-ID").setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);
//        qgrid.addColumn(Quarantine::getNACHRICHTIDINTERN).setHeader("Nachricht-ID Intern").setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);

//        qgrid.addColumn(Quarantine::getENTRANCEDATE).setHeader("Entrance-Date").setAutoWidth(true).setResizable(true).setSortable(true);
//        qgrid.addColumn(Quarantine::getCREATIONDATE).setHeader("Creation-Date").setAutoWidth(true).setResizable(true).setSortable(true);
        qgrid.addColumn(Quarantine::getPOBOX).setHeader("POSTBOX").setAutoWidth(true).setResizable(true).setSortable(true);

        qgrid.addColumn(createReceiverRenderer()).setHeader("Receiver").setAutoWidth(true).setSortable(true).setResizable(true);
        qgrid.addColumn(createSenderRenderer()).setHeader("Sender").setAutoWidth(true).setSortable(true).setResizable(true);

        //qgrid.addColumn(Quarantine::getRECEIVERID).setHeader("Receiver-ID").setAutoWidth(true).setResizable(true).setSortable(true);
        //qgrid.addColumn(Quarantine::getRECEIVERNAME).setHeader("Receiver-Name").setAutoWidth(true).setResizable(true).setSortable(true);
        //qgrid.addColumn(Quarantine::getSENDERID).setHeader("Sender-ID").setAutoWidth(true).setResizable(true).setSortable(true);
        //qgrid.addColumn(Quarantine::getSENDERNAME).setHeader("Sender-Name").setAutoWidth(true).setResizable(true).setSortable(true);
        //   qgrid.addColumn(Quarantine::getART).setHeader("ART").setAutoWidth(true).setResizable(true).setSortable(true);
        qgrid.addColumn(Quarantine::getFEHLERTAG).setHeader("Im FH").setAutoWidth(true).setResizable(true).setSortable(true);
        //  qgrid.addColumn(Quarantine::getVERARBEITET).setHeader("Verarbeitet").setAutoWidth(true).setResizable(true).setSortable(true);
        //  qgrid.addColumn(Quarantine::getLOESCHTAG).setHeader("Löschtag").setAutoWidth(true).setResizable(true).setSortable(true);

        qgrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        qgrid.setColumnReorderingAllowed(true);
    }

    private InputStream getStream(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return stream;
    }

    private void generateExcel(String fileName) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Quarantine Data");

            // Create the header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("NACHRICHTIDINTERN");
            headerRow.createCell(2).setCellValue("ENTRANCEDATE");
            headerRow.createCell(3).setCellValue("CREATIONDATE");
            headerRow.createCell(4).setCellValue("POBOX");
            headerRow.createCell(5).setCellValue("EXCEPTIONCODE");
            headerRow.createCell(6).setCellValue("RECEIVERID");
            headerRow.createCell(7).setCellValue("RECEIVERNAME");
            headerRow.createCell(8).setCellValue("SENDERID");
            headerRow.createCell(9).setCellValue("SENDERNAME");
            headerRow.createCell(10).setCellValue("ART");
            headerRow.createCell(11).setCellValue("FEHLERTAG");
            headerRow.createCell(12).setCellValue("VERARBEITET");
            headerRow.createCell(13).setCellValue("LOESCHTAG");

            // Populate data rows
            int rowIndex = 1;
            for (Quarantine quarantine : listOfQuarantine) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(quarantine.getID());
                dataRow.createCell(1).setCellValue(quarantine.getNACHRICHTIDINTERN() != null ? quarantine.getNACHRICHTIDINTERN() : 0);
                dataRow.createCell(2).setCellValue(quarantine.getENTRANCEDATE());
                dataRow.createCell(3).setCellValue(quarantine.getCREATIONDATE());
                dataRow.createCell(4).setCellValue(quarantine.getPOBOX());
                dataRow.createCell(5).setCellValue(quarantine.getEXCEPTIONCODE());
                dataRow.createCell(6).setCellValue(quarantine.getRECEIVERID());
                dataRow.createCell(7).setCellValue(quarantine.getRECEIVERNAME());
                dataRow.createCell(8).setCellValue(quarantine.getSENDERID());
                dataRow.createCell(9).setCellValue(quarantine.getSENDERNAME());
                dataRow.createCell(10).setCellValue(quarantine.getART());
                dataRow.createCell(11).setCellValue(quarantine.getFEHLERTAG());
                dataRow.createCell(12).setCellValue(quarantine.getVERARBEITET());
                dataRow.createCell(13).setCellValue(quarantine.getLOESCHTAG());
            }

            // Write workbook to output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            // Create a StreamResource for downloading
            StreamResource streamResource = new StreamResource(fileName,
                    () -> new ByteArrayInputStream(outputStream.toByteArray()));

            // Create a hidden download anchor and trigger the download
            Anchor downloadAnchor = new Anchor(streamResource, "Download Excel");
            downloadAnchor.getElement().setAttribute("download", true);
            downloadAnchor.getElement().getStyle().set("display", "none");
            downloadAnchor.setHref(streamResource);
            downloadAnchor.add(new Button("Download Excel"));

            // Add the anchor to the UI and simulate a click
            add(downloadAnchor);
            downloadAnchor.getElement().callJsFunction("click");

        } catch (IOException e) {
            Notification.show("Error creating Excel file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Renderer<Quarantine> createNachrichtIDRenderer() {
        return LitRenderer.<Quarantine> of(
                   "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.NachrichtIDExtern} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.NachrichtIDIntern}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                )
                .withProperty("NachrichtIDIntern", Quarantine::getNACHRICHTIDINTERN)
                .withProperty("NachrichtIDExtern", Quarantine::getID);
    }


    private static Renderer<Quarantine> createDateRenderer() {
        return LitRenderer.<Quarantine> of(
                        "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.EntranceDate} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.CreationDate}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                )
                .withProperty("EntranceDate", Quarantine::getENTRANCEDATE)
                .withProperty("CreationDate", Quarantine::getCREATIONDATE);
    }

    private static Renderer<Quarantine> createReceiverRenderer() {
        return LitRenderer.<Quarantine> of(
                        "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.ReceiverName} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.ReceiverID}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                )
                .withProperty("ReceiverName", Quarantine::getRECEIVERNAME)
                .withProperty("ReceiverID", Quarantine::getRECEIVERID);
    }

    private static Renderer<Quarantine> createSenderRenderer() {
        return LitRenderer.<Quarantine> of(
                        "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.SenderName} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.SenderID}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                )
                .withProperty("SenderName", Quarantine::getSENDERNAME)
                .withProperty("SenderID", Quarantine::getSENDERID);
    }

    private void refreshGrid(){

        Integer anz_MessageCheckFilenames = 0;

        for ( Quarantine item: listOfQuarantine){
            if (item.getEXCEPTIONCODE().contains("CHECK_FILENAMES")){
                anz_MessageCheckFilenames++;
            }
        }

        qgrid.getFooterRows().get(0).getCell(qgrid.getColumnByKey("ID")).setText(String.format("Gesamt: %s", listOfQuarantine.size() ) + "  Anzahl CheckFilenames: " + anz_MessageCheckFilenames);

//        qgrid.getDataProvider().refreshAll();
        Notification.show("Daten wurden aktualisiert",3000, Notification.Position.TOP_END);
    }

    private List<Quarantine> getQuarantaene(UI ui) {
        //String sql = "select substr(stacktrace,1,16) as Tag, EXCEPTIONCODE, count(*) as Anzahl, 4 as AnzahlInFH from QUARANTINE a where to_date(substr(stacktrace,1,10),'YYYY-MM_DD') > sysdate-30  group by substr(stacktrace,1,16), EXCEPTIONCODE order by 1 desc";
        //String sql = "select substr(stacktrace,1,10) as Tag, EXCEPTIONCODE, count(*) as Anzahl from EGVP.QUARANTINE@EGVP a where to_date(substr(stacktrace,1,10),'YYYY-MM_DD') > sysdate-30  group by substr(stacktrace,1,10), EXCEPTIONCODE order by 1 desc";

        /*String sql= "select egvp.id, egvp.entrancedate,egvp.creationdate, egvp.pobox, egvp.exceptioncode, egvp.receiverid, egvp.receivername, egvp.senderid, egvp.Sendername\n" +
                ", m.nachrichtidintern, m.art, m.fehlertag, m.verarbeitet,m.loeschtag \n" +
                "from EGVP.QUARANTINE@EGVP egvp\n" +
                "left outer join ekp.metadaten m\n" +
                "on egvp.id=m.NACHRICHTIDEXTERN\n" +
                "where (egvp.entrancedate is not null or egvp.creationdate is not null or to_date(substr(egvp.stacktrace,1,10),'YYYY-MM-DD') > sysdate -30)\n" +
                "and nvl(egvp.entrancedate,sysdate) >= sysdate -30\n" +
                "order by egvp.creationdate desc, egvp.entrancedate desc\n";*/

        String sql= "select * from ekp.v_egvp_quarantaene";
//
//        if (!FehlertypCB.isEmpty()){
//            sql = sql + " where EXCEPTIONCODE='" + FehlertypCB.getValue() + "'";
//        }

        System.out.println("Abfrage EGVP-Quarantäne: " + sql);

//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        Configuration conf;
//
//        conf = comboBox.getValue();
//
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        //ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        //ds.setUsername("SYSTEM");
        //ds.setPassword("Michael123");

        try {

//            jdbcTemplate.setDataSource(ds);
            getJdbcTemplateWithDBConnetion(comboBox.getValue());

            listOfQuarantine = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Quarantine.class));



            System.out.println("EGVP-Quarantäne eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            ui.access(() -> {
                Notification.show("Fehler: " + e.getMessage(),4000, Notification.Position.MIDDLE);
            });

            return null;
        } finally {
            connectionClose(jdbcTemplate);
        }
        return listOfQuarantine;
    }
    public StreamResource getStreamResource(String filename, String content) {
        return new StreamResource(filename,
                () -> new ByteArrayInputStream(content.getBytes()));
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
}
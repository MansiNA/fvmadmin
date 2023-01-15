package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Quarantine;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@PageTitle("Quarantäne Verwaltung")
@Route(value = "quarantaene", layout= MainLayout.class)
public class QuarantaeneView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private ConfigurationService service;
    private ComboBox<Configuration> comboBox;

    Button button = new Button("Refresh");
    Integer ret = 0;
    Grid<Quarantine> qgrid = new Grid<>(Quarantine.class, false);

    List<Quarantine> lq;

    public QuarantaeneView(ConfigurationService service) {
        this.service = service;

        Paragraph paragraph = new Paragraph("Hier erfolgt eine Auflistung von aktuellen EGVP-E Quarantäne-Nachrichten");
        paragraph.setMaxHeight("400px");

        comboBox = new ComboBox<>("Verbindung");
        comboBox.setItems(service.findMessageConfigurations());
        comboBox.setItemLabelGenerator(Configuration::get_Message_Connection);

        comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());

        qgrid.addColumn(Quarantine::getID).setHeader("Nachricht Extern-ID");
        qgrid.addColumn(Quarantine::getENTRANCEDATE).setHeader("Entrance-Date");
        qgrid.addColumn(Quarantine::getCREATIONDATE).setHeader("Creation-Date");
        qgrid.addColumn(Quarantine::getPOBOX).setHeader("POSTBOX");
        qgrid.addColumn(Quarantine::getEXCEPTIONCODE).setHeader("Exception-Code");
        qgrid.addColumn(Quarantine::getRECEIVERID).setHeader("Receiver-ID");
        qgrid.addColumn(Quarantine::getRECEIVERNAME).setHeader("Receiver-Name");
        qgrid.addColumn(Quarantine::getSENDERID).setHeader("Sender-ID");
        qgrid.addColumn(Quarantine::getSENDERNAME).setHeader("Sender-Name");
        qgrid.addColumn(Quarantine::getART).setHeader("ART");
        qgrid.addColumn(Quarantine::getFEHLERTAG).setHeader("Fehlertag");
        qgrid.addColumn(Quarantine::getVERARBEITET).setHeader("Verarbeitet");
        qgrid.addColumn(Quarantine::getLOESCHTAG).setHeader("Löschtag");

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox,button);
        hl.setAlignItems(Alignment.BASELINE);

      //  add(comboBox, button, paragraph, qgrid);
        add(hl, qgrid);

        button.addClickListener(clickEvent -> {

            UI ui = UI.getCurrent();

            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Quarantäne Infos");

                    lq = getQuarantaene();

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
                    qgrid.setItems(lq);
                });
            });


        });


    }

    private List<Quarantine> getQuarantaene() {
        //String sql = "select substr(stacktrace,1,16) as Tag, EXCEPTIONCODE, count(*) as Anzahl, 4 as AnzahlInFH from QUARANTINE a where to_date(substr(stacktrace,1,10),'YYYY-MM_DD') > sysdate-30  group by substr(stacktrace,1,16), EXCEPTIONCODE order by 1 desc";
        //String sql = "select substr(stacktrace,1,10) as Tag, EXCEPTIONCODE, count(*) as Anzahl from EGVP.QUARANTINE@EGVP a where to_date(substr(stacktrace,1,10),'YYYY-MM_DD') > sysdate-30  group by substr(stacktrace,1,10), EXCEPTIONCODE order by 1 desc";

        String sql= "select egvp.id, egvp.entrancedate,egvp.creationdate, egvp.pobox, egvp.exceptioncode, egvp.receiverid, egvp.receivername, egvp.senderid, egvp.Sendername\n" +
                ", m.nachrichtidintern, m.art, m.fehlertag, m.verarbeitet,m.loeschtag \n" +
                "from EGVP.QUARANTINE@EGVP egvp\n" +
                "left outer join ekp.metadaten m\n" +
                "on egvp.id=m.NACHRICHTIDEXTERN\n" +
                "where (egvp.entrancedate is not null or egvp.creationdate is not null or to_date(substr(egvp.stacktrace,1,10),'YYYY-MM-DD') > sysdate -30)\n" +
                "and nvl(egvp.entrancedate,sysdate) >= sysdate -30\n" +
                "order by egvp.creationdate desc, egvp.entrancedate desc\n";

        System.out.println("Abfrage EGVP-Quarantäne");

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;

        conf = comboBox.getValue();


        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        //ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        //ds.setUsername("SYSTEM");
        //ds.setPassword("Michael123");

        try {

            jdbcTemplate.setDataSource(ds);

            lq = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Quarantine.class));



            System.out.println("EGVP-Quarantäne eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return lq;
    }

}
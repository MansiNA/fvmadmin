package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Quarantine;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayList;
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

    List<Quarantine> lq = new ArrayList<>();

    public QuarantaeneView(ConfigurationService service) {
        this.service = service;

        Paragraph paragraph = new Paragraph("Hier erfolgt eine Auflistung von aktuellen EGVP-E Quarantäne-Nachrichten");
        paragraph.setMaxHeight("400px");

        comboBox = new ComboBox<>("Verbindung");
        comboBox.setItems(service.findMessageConfigurations());
        comboBox.setItemLabelGenerator(Configuration::get_Message_Connection);

        comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());

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

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox,button);
        hl.setAlignItems(Alignment.BASELINE);
        setSizeFull();
      //  add(comboBox, button, paragraph, qgrid);
        add(hl, qgrid);

        button.addClickListener(clickEvent -> {

            Notification.show("hole Daten...");
            qgrid.setItems();

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

                    refreshGrid();

                });
            });


        });


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
        Notification.show("Daten wurden aktualisiert");



        Integer anz_MessageCheckFilenames = 0;

        for ( Quarantine item: lq){
            if (item.getEXCEPTIONCODE().contains("CHECK_FILENAMES")){
                anz_MessageCheckFilenames++;
            }
        }

        qgrid.getFooterRows().get(0).getCell(qgrid.getColumnByKey("ID")).setText(String.format("Gesamt: %s", lq.size() ) + "  Anzahl CheckFilenames: " + anz_MessageCheckFilenames);

//        qgrid.getDataProvider().refreshAll();

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